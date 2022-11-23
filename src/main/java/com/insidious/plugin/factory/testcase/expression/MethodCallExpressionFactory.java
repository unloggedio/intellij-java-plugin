package com.insidious.plugin.factory.testcase.expression;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.TestCaseWriter;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.squareup.javapoet.ClassName;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class MethodCallExpressionFactory {

    public static final Parameter MockitoClass;
    //    public static final Parameter AssertClass;
    public static final Parameter GsonClass;

    static {
        MockitoClass = makeParameter("Mockito", "org.mockito.Mockito");
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

    public static Expression StringExpression(String s) {
        return new StringExpression(s);
    }

    public static MethodCallExpression MockitoWhen(MethodCallExpression methodCallExpression) {

        Parameter mainSubject = methodCallExpression.getSubject();
        DataInfo subjectProbeInfo = mainSubject.getProbeInfo();
        String callType = subjectProbeInfo.getAttribute("CallType", null);
        boolean isStatic = false;

        String param1;
        if (callType != null && callType.equals("Static")) {
            isStatic = true;
            String owner = subjectProbeInfo.getAttribute("Owner", null);
            String classSimpleName = owner.substring(owner.lastIndexOf('/') + 1);
            param1 = "() -> " + classSimpleName + "." + methodCallExpression.getMethodName() +
                    "(" + TestCaseWriter.createMethodParametersStringMock(methodCallExpression.getArguments()) + ")";
        } else {
            param1 = mainSubject.getName() + "." + methodCallExpression.getMethodName() +
                    "(" + TestCaseWriter.createMethodParametersStringMock(methodCallExpression.getArguments()) + ")";
        }

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);


        Parameter callSubject = MockitoClass;
        if (isStatic) {
            callSubject = mainSubject;
        }
        MethodCallExpression callExpression = MethodCallExpression("when", callSubject,
                VariableContainer.from(List.of(whenExpression)), null
        );
        return callExpression;

    }

    public static MethodCallExpression MockClass(ClassName targetClassname) {

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProb(new DataEventWithSessionId());


        MethodCallExpression mock = new MethodCallExpression("mock", MockitoClass, List.of(whenExpression), null, 0);
        mock.setStaticCall(true);
        mock.setMethodAccess(Opcodes.ACC_PUBLIC);
        return mock;

    }

    public static MethodCallExpression MockStaticClass(ClassName targetClassname) {

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProb(new DataEventWithSessionId());


        MethodCallExpression mockStatic = new MethodCallExpression("mockStatic", MockitoClass, List.of(whenExpression), null, 0);
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

    public static Expression MockitoAssert(
            Parameter returnValue,
            Parameter returnSubjectInstanceName,
            TestCaseGenerationConfiguration testConfiguration) {
        MethodCallExpression assertEquals = MethodCallExpression(
                testConfiguration.getTestFramework().AssertEqualMethod(),
                testConfiguration.getTestFramework().AssertClassParameter(),
                VariableContainer.from(List.of(returnValue, returnSubjectInstanceName)), null
        );
        assertEquals.setStaticCall(true);
        assertEquals.setMethodAccess(Opcodes.ACC_PUBLIC);
        return assertEquals;
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

    public static MethodCallExpression CloseStaticMock(Parameter object){
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
