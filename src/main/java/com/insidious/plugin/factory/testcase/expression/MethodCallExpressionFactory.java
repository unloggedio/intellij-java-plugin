package com.insidious.plugin.factory.testcase.expression;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.ParameterNameFactory;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ParameterUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MethodCallExpressionFactory {

    //    public static final Parameter MockitoClass;
    //    public static final Parameter AssertClass;
//    public static final Parameter GsonClass;
    private static final Logger logger = LoggerUtil.getInstance(MethodCallExpressionFactory.class);

    static {
//        MockitoClass = makeParameter("Mockito", "org.mockito.Mockito");
//        AssertClass = makeParameter("Assert", "org.junit.Assert");
//        GsonClass = makeParameter("gson", "com.google.gson.Gson");
    }

    public static Parameter makeParameter(String name, String type, long value) {
        Parameter param = new Parameter(value);
        param.setName(name);
        param.setType(type);
        return param;
    }


    public static MethodCallExpression MethodCallExpression(String methodName, Parameter subjectParameter,
                                                            VariableContainer from, Parameter callReturnParameter) {
        return new MethodCallExpression(methodName, subjectParameter, from.all(), callReturnParameter, 0);
    }

    public static Expression PlainValueExpression(String value) {
        return new PlainValueExpression(value);
    }

    public static Expression ClassValueExpression(String value) {
        return new ClassValueExpression(value);
    }

    public static Expression StringExpression(String s) {
        return new StringExpression(s);
    }

    public static Expression NullExpression() {
        return new NullExpression();
    }

    public static String
    createMethodParametersStringMock(List<Parameter> variableContainer, TestGenerationState testGenerationState) {
//        logger.warn("Create method parameters argument mock => " + variableContainer);
        if (variableContainer == null) {
            return "";
        }
        StringBuilder parameterStringBuilder = new StringBuilder();

        for (int i = 0; i < variableContainer.size(); i++) {
            Parameter parameter = variableContainer.get(i);

            if (i > 0) {
                parameterStringBuilder.append(", ");
            }

            Object compareAgainst;
            String parameterType = parameter.getType();
            if (parameterType != null && parameterType.endsWith("[]")) {
                compareAgainst = "";
            } else if (parameterType != null && parameterType.equals("java.lang.Class")) {
                compareAgainst = new String(parameter.getProb().getSerializedValue());
            } else if (parameter.getProb() != null
                    && parameter.getProb().getSerializedValue().length > 0
                    && (new String(parameter.getProb().getSerializedValue())).equals("null")
            ) {

                // if the serialized value is null just append null
                compareAgainst = "null";
            } else if (parameter.isPrimitiveType()) {
                if (parameter.isBoxedPrimitiveType()) {
                    String serialisedValue = new String(parameter.getProb().getSerializedValue());
                    if (!serialisedValue.isEmpty()) {
                        compareAgainst = serialisedValue;
                    } else {
                        compareAgainst = parameter.getValue();
                    }

                    compareAgainst = ParameterUtils.addParameterTypeSuffix(String.valueOf(compareAgainst),
                            parameterType);
                } else {
                    compareAgainst = ParameterUtils.makeParameterValueForPrimitiveType(parameter);
                }

            } else if (testGenerationState.getParameterNameFactory().getNameForUse(parameter, null) != null) {
                compareAgainst = testGenerationState.getParameterNameFactory().getNameForUse(parameter, null);
            } else {
                compareAgainst = parameter.getValue();
                if (parameter.isStringType()) {
                    if (parameter.getProb().getSerializedValue() != null &&
                            parameter.getProb().getSerializedValue().length > 0) {
                        compareAgainst = new String(parameter.getProb().getSerializedValue());
                    } else if (parameter.getValue() == 0L) {
                        compareAgainst = "null";
                    }
                }
            }

//            logger.warn("Argument [" + parameter + "] will be compared as => " + compareAgainst);

            if (compareAgainst != null && parameterType != null && parameterType.equals("java.lang.String")) {
                parameterStringBuilder
                        .append("eq(")
                        .append(compareAgainst)
                        .append(")");
            } else if (parameterType != null && compareAgainst != null
                    && (parameterType.length() == 1 || parameterType.startsWith("java.lang.")
                    && !parameterType.contains(".Object"))) {
                if (parameter.isBooleanType()) {
                    if (compareAgainst.equals("0")
                            || compareAgainst.equals(0L) // compare specifically to 0L, since comparing
                            // with 0 (int) sometimes (always) turn out to be false
                            || compareAgainst.equals(0)
                    ) {
                        compareAgainst = "false";
                    } else {
                        compareAgainst = "true";
                    }
                }
                parameterStringBuilder.append("eq(")
                        .append(compareAgainst)
                        .append(")");
            } else {
                if (parameter.getValue() == 0) {
                    parameterStringBuilder.append("any()");
                } else {
                    parameterStringBuilder.append("any(")
                            .append(ClassTypeUtils.getDescriptorToDottedClassName(parameterType))
                            .append(".class)");
                }
            }
        }


        return parameterStringBuilder.toString();
    }

    public static MethodCallExpression MockitoWhen(
            MethodCallExpression methodCallExpression,
            TestCaseGenerationConfiguration configuration,
            TestGenerationState testGenerationState) {

        Parameter mainSubject = methodCallExpression.getSubject();
        DataInfo subjectProbeInfo = mainSubject.getProbeInfo();

        String param1;
        String methodParametersStringMock = createMethodParametersStringMock(
                methodCallExpression.getArguments(), testGenerationState);
//        logger.warn(
//                "Create method call arguments mock: [" + methodCallExpression + "] => " + methodParametersStringMock);

        if (methodCallExpression.isStaticCall()) {
            String classSimpleName = mainSubject.getType().substring(mainSubject.getType().lastIndexOf('.') + 1);
            param1 = "() -> " + classSimpleName + "." + methodCallExpression.getMethodName() +
                    "(" + methodParametersStringMock + ")";
        } else {
            ParameterNameFactory parameterNameFactory = testGenerationState.getParameterNameFactory();
            param1 =
                    parameterNameFactory.getNameForUse(mainSubject, null) + "." + methodCallExpression.getMethodName() +
                            "(" + methodParametersStringMock + ")";
        }

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);


        Parameter callSubject = configuration.getMockFramework()
                .getMockClassParameter();
        if (methodCallExpression.isStaticCall()) {
            callSubject = mainSubject;
        }
        MethodCallExpression callExpression = MethodCallExpression("when", callSubject,
                VariableContainer.from(Collections.singletonList(whenExpression)), null
        );
//        callExpression.setStaticCall(true);
        return callExpression;

    }

    public static MethodCallExpression MockClass(TypeName targetClassname, TestCaseGenerationConfiguration configuration) {

        String param1 = targetClassname.toString();

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());


        MethodCallExpression mock = new MethodCallExpression("mock",
                configuration.getMockFramework()
                        .getMockClassParameter(), Collections.singletonList(whenExpression), null, 0);
        mock.setStaticCall(true);
        mock.setMethodAccess(Opcodes.ACC_PUBLIC);
        return mock;

    }

    public static MethodCallExpression MockStaticClass(ClassName targetClassname, TestCaseGenerationConfiguration configuration) {

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());


        MethodCallExpression mockStatic = new MethodCallExpression("mockStatic",
                configuration.getMockFramework().getMockClassParameter(),
                Collections.singletonList(whenExpression), null, 0);
        mockStatic.setMethodAccess(Opcodes.ACC_PUBLIC);
        mockStatic.setStaticCall(true);
        return mockStatic;

    }

    public static Expression MockitoThen(Parameter returnValue) {
        List<Parameter> valueToReturn;
        if (returnValue.isPrimitiveType()) {
            returnValue.clearNames();
            valueToReturn = Collections.singletonList(returnValue);
        } else {
            valueToReturn = Collections.singletonList(returnValue);
        }
        MethodCallExpression thenReturn = MethodCallExpression(
                "thenReturn", null, VariableContainer.from(valueToReturn), null);
        thenReturn.setMethodAccess(Opcodes.ACC_PUBLIC);
        return thenReturn;

//        PlainValueExpression parameter = new PlainValueExpression(".thenReturn(" + thingToReturn + ")");
//        return parameter;
    }

    public static Expression MockitoThenThrow(Parameter exceptionValue) {

        MethodCallExpression thenThrow = MethodCallExpression("thenThrow", null,
                VariableContainer.from(Collections.singletonList(exceptionValue)), null);
        thenThrow.setMethodAccess(Opcodes.ACC_PUBLIC);
        return thenThrow;

//        PlainValueExpression parameter = new PlainValueExpression(".thenReturn(" + thingToReturn + ")");
//        return parameter;
    }

    public static Expression ToJson(Parameter object, TestCaseGenerationConfiguration testConfiguration) {
        JsonFramework jsonFramework = testConfiguration.getJsonFramework();
        MethodCallExpression toJson = MethodCallExpression(jsonFramework.getToJsonMethodName(),
                jsonFramework.getInstance(), VariableContainer.from(Collections.singletonList(object)),
                null);
        toJson.setMethodAccess(Opcodes.ACC_PUBLIC);

        return toJson;
    }

    public static Expression MockitoAssertEquals(
            Parameter returnValue,
            Parameter returnSubjectInstanceName,
            TestCaseGenerationConfiguration testConfiguration) {
        MethodCallExpression assertEquals = MethodCallExpression(
                testConfiguration.getTestFramework().AssertEqualMethod(),
                testConfiguration.getTestFramework().AssertClassParameter(),
                VariableContainer.from(Arrays.asList(returnValue, returnSubjectInstanceName)), null
        );
        assertEquals.setStaticCall(true);
        assertEquals.setMethodAccess(Opcodes.ACC_PUBLIC);
        return assertEquals;
    }

    public static Expression MockitoAssertArrayEquals(
            Parameter returnValue,
            Parameter returnSubjectInstanceName,
            TestCaseGenerationConfiguration testConfiguration) {
        MethodCallExpression assertArrayEquals = MethodCallExpression(
                testConfiguration.getTestFramework().AssertArrayEqualsMethod(),
                testConfiguration.getTestFramework().AssertClassParameter(),
                VariableContainer.from(Arrays.asList(returnValue, returnSubjectInstanceName)), null
        );

        assertArrayEquals.setStaticCall(true);
        assertArrayEquals.setMethodAccess(Opcodes.ACC_PUBLIC);
        return assertArrayEquals;
    }

    public static Expression MockitoAssertFalse(
            Parameter paramInstanceName, TestCaseGenerationConfiguration testConfiguration) {
        MethodCallExpression assertFalse = MethodCallExpression(
                testConfiguration.getTestFramework().AssertFalseMethod(),
                testConfiguration.getTestFramework().AssertClassParameter(),
                VariableContainer.from(Collections.singletonList(paramInstanceName)), null
        );

        assertFalse.setStaticCall(true);
        assertFalse.setMethodAccess(Opcodes.ACC_PUBLIC);
        return assertFalse;
    }

    public static MethodCallExpression FromJson(Parameter object, TestCaseGenerationConfiguration testConfiguration) {
        JsonFramework jsonFramework = testConfiguration.getJsonFramework();
        MethodCallExpression fromJson = MethodCallExpression(jsonFramework.getFromJsonMethodName(),
                jsonFramework.getInstance(),
                VariableContainer.from(Collections.singletonList(object)), null);
        fromJson.setMethodAccess(Opcodes.ACC_PUBLIC);
        return fromJson;
    }

    public static MethodCallExpression FromJsonFetchedFromFile(Parameter object) {
        MethodCallExpression valueOf = MethodCallExpression("ValueOf", null,
                VariableContainer.from(Collections.singletonList(object)), null);
        valueOf.setStaticCall(true);
        valueOf.setMethodAccess(Opcodes.ACC_PUBLIC);
        return valueOf;
    }

    public static Expression createEnumExpression(Parameter enumParam) {

        TypeName enumTypeName = ClassTypeUtils.createTypeFromNameString(
                ClassTypeUtils.getDescriptorToDottedClassName(enumParam.getType()));

        String value = new String(enumParam.getProb()
                .getSerializedValue());

        value = value.replace("\"", "");

        StringBuilder rhsExpressionBuilder = new StringBuilder();
        String enumTypeNameString = enumTypeName.toString();
        String enumSimpleTypeName = enumTypeNameString.substring(enumTypeNameString.lastIndexOf('.') + 1);
        rhsExpressionBuilder
                .append(enumSimpleTypeName)
                .append(".")
                .append(value);

        return new PlainValueExpression(rhsExpressionBuilder.toString());
    }

    public static MethodCallExpression CloseStaticMock(Parameter object) {
        Parameter closeExpression = new Parameter();
        closeExpression.setValue("");

        // TODO : need to add params ? do we need param inside close method of @AfterEach
        MethodCallExpression close = MethodCallExpression("close", object,
                VariableContainer.from(Collections.singletonList(closeExpression)),
                null);

        close.setMethodAccess(Opcodes.ACC_PUBLIC);
        return close;
    }
}
