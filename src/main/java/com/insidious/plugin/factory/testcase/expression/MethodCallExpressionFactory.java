package com.insidious.plugin.factory.testcase.expression;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.TestCaseWriter;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.ClassName;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class MethodCallExpressionFactory {

    //    public static final Parameter MockitoClass;
    //    public static final Parameter AssertClass;
    public static final Parameter GsonClass;
    private static final Logger logger = LoggerUtil.getInstance(MethodCallExpressionFactory.class);

    static {
//        MockitoClass = makeParameter("Mockito", "org.mockito.Mockito");
//        AssertClass = makeParameter("Assert", "org.junit.Assert");
        GsonClass = makeParameter("gson", "com.google.gson.Gson");
    }

    public static Parameter makeParameter(String name, String type) {
        Parameter param = new Parameter();
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

    public static MethodCallExpression MockitoWhen(MethodCallExpression methodCallExpression,
                                                   TestCaseGenerationConfiguration configuration) {

        Parameter mainSubject = methodCallExpression.getSubject();
        DataInfo subjectProbeInfo = mainSubject.getProbeInfo();
        String callType = subjectProbeInfo.getAttribute("CallType", null);
        boolean isStatic = false;

        String param1;
        String methodParametersStringMock = TestCaseWriter.createMethodParametersStringMock(
                methodCallExpression.getArguments());
        logger.warn(
                "Create method call arguments mock: [" + methodCallExpression + "] => " + methodParametersStringMock);

        if (callType != null && callType.equals("Static")) {
            isStatic = true;
            String owner = subjectProbeInfo.getAttribute("Owner", null);
            String classSimpleName = owner.substring(owner.lastIndexOf('/') + 1);
            param1 = "() -> " + classSimpleName + "." + methodCallExpression.getMethodName() +
                    "(" + methodParametersStringMock + ")";
        } else {
            param1 = mainSubject.getNameForUse(null) + "." + methodCallExpression.getMethodName() +
                    "(" + methodParametersStringMock + ")";
        }

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);


        Parameter callSubject = configuration.getMockFramework()
                .getMockClassParameter();
        if (isStatic) {
            callSubject = mainSubject;
        }
        MethodCallExpression callExpression = MethodCallExpression("when", callSubject,
                VariableContainer.from(List.of(whenExpression)), null
        );
//        callExpression.setStaticCall(true);
        return callExpression;

    }

    public static MethodCallExpression MockClass(ClassName targetClassname, TestCaseGenerationConfiguration configuration) {

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProb(new DataEventWithSessionId());


        MethodCallExpression mock = new MethodCallExpression("mock",
                configuration.getMockFramework()
                        .getMockClassParameter(), List.of(whenExpression), null, 0);
        mock.setStaticCall(true);
        mock.setMethodAccess(Opcodes.ACC_PUBLIC);
        return mock;

    }

    public static MethodCallExpression MockStaticClass(ClassName targetClassname, TestCaseGenerationConfiguration configuration) {

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProb(new DataEventWithSessionId());


        MethodCallExpression mockStatic = new MethodCallExpression("mockStatic",
                configuration.getMockFramework()
                        .getMockClassParameter(), List.of(whenExpression), null, 0);
        mockStatic.setMethodAccess(Opcodes.ACC_PUBLIC);
        mockStatic.setStaticCall(true);
        return mockStatic;

    }

    public static MethodCallExpression InitNoArgsConstructor(Parameter targetClassname) {
        return new MethodCallExpression("<init>", null, List.of(), targetClassname, 0);

    }

    public static Expression MockitoThen(Parameter returnValue) {
        List<Parameter> valueToReturn;
        if (returnValue.isPrimitiveType()) {
            returnValue.clearNames();
            valueToReturn = List.of(returnValue);
        } else {
            valueToReturn = List.of(returnValue);
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
                VariableContainer.from(List.of(exceptionValue)),
                null);
        thenThrow.setMethodAccess(Opcodes.ACC_PUBLIC);
        return thenThrow;

//        PlainValueExpression parameter = new PlainValueExpression(".thenReturn(" + thingToReturn + ")");
//        return parameter;
    }

    public static Expression ToJson(Parameter object) {
        MethodCallExpression toJson = MethodCallExpression("toJson", GsonClass,
                VariableContainer.from(List.of(object)),
                null);
        toJson.setMethodAccess(Opcodes.ACC_PUBLIC);

        return toJson;
    }

    public static Expression MockitoAssertEquals(
            Parameter returnValue,
            Parameter returnSubjectInstanceName,
            TestCaseGenerationConfiguration testConfiguration) {
        MethodCallExpression assertEquals = MethodCallExpression(
                testConfiguration.getTestFramework()
                        .AssertEqualMethod(),
                testConfiguration.getTestFramework()
                        .AssertClassParameter(),
                VariableContainer.from(List.of(returnValue, returnSubjectInstanceName)), null
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
                testConfiguration.getTestFramework()
                        .AssertArrayEqualsMethod(),
                testConfiguration.getTestFramework()
                        .AssertClassParameter(),
                VariableContainer.from(List.of(returnValue, returnSubjectInstanceName)), null
        );

        assertArrayEquals.setStaticCall(true);
        assertArrayEquals.setMethodAccess(Opcodes.ACC_PUBLIC);
        return assertArrayEquals;
    }

    public static Expression MockitoAssertFalse(
            Parameter paramInstanceName,
            TestCaseGenerationConfiguration testConfiguration) {
        MethodCallExpression assertFalse = MethodCallExpression(
                testConfiguration.getTestFramework()
                        .AssertFalseMethod(),
                testConfiguration.getTestFramework()
                        .AssertClassParameter(),
                VariableContainer.from(List.of(paramInstanceName)), null
        );

        assertFalse.setStaticCall(true);
        assertFalse.setMethodAccess(Opcodes.ACC_PUBLIC);
        return assertFalse;
    }

    public static MethodCallExpression FromJson(Parameter object) {
        MethodCallExpression fromJson = MethodCallExpression("fromJson", GsonClass,
                VariableContainer.from(List.of(object)), null);
        fromJson.setMethodAccess(Opcodes.ACC_PUBLIC);
        return fromJson;
    }

    public static MethodCallExpression FromJsonFetchedFromFile(Parameter object) {
        MethodCallExpression valueOf = MethodCallExpression("ValueOf", null,
                VariableContainer.from(List.of(object)),
                null);
        valueOf.setStaticCall(true);
        valueOf.setMethodAccess(Opcodes.ACC_PUBLIC);
        return valueOf;
    }

    public static Expression createEnumExpression(Parameter enumParam) {

        @Nullable String enumTypeName = ClassTypeUtils.createTypeFromName(
                        ClassTypeUtils.getJavaClassName(enumParam.getType()))
                .toString();

        String value = new String(enumParam.getProb()
                .getSerializedValue());

        value = value.replace("\"", "");

        StringBuilder rhsExpressionBuilder = new StringBuilder();
        String enumSimpleTypeName = enumTypeName.substring(enumTypeName.lastIndexOf('.') + 1);
        rhsExpressionBuilder
                .append(enumSimpleTypeName)
                .append(".")
                .append(value);

        PlainValueExpression enumExp = new PlainValueExpression(rhsExpressionBuilder.toString());

        return enumExp;
    }

    public static MethodCallExpression CloseStaticMock(Parameter object) {
        Parameter closeExpression = new Parameter();
        closeExpression.setValue("");

        // TODO : need to add params ? do we need param inside close method of @AfterEach
        MethodCallExpression close = MethodCallExpression("close", object,
                VariableContainer.from(List.of(closeExpression)),
                null);

        close.setMethodAccess(Opcodes.ACC_PUBLIC);
        return close;
    }
}
