package com.insidious.plugin.factory.testcase.expression;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.writer.TestCaseWriter;
import com.insidious.plugin.pojo.ConstructorType;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;

import java.util.List;

public class MethodCallExpressionFactory {

    public static final Parameter MockitoClass;
    public static final Parameter AssertClass;
    public static final Parameter GsonClass;

    static {
        MockitoClass = makeParameter("Mockito", "org.mockito.Mockito", ConstructorType.SINGLETON);
        AssertClass = makeParameter("Assert", "org.junit.Assert", ConstructorType.SINGLETON);
        GsonClass = makeParameter("gson", "com.google.gson.Gson", ConstructorType.INIT);
    }

    private static Parameter makeParameter(String name, String type, ConstructorType constructorType) {
        Parameter param = new Parameter();
        param.setName(name);
        param.setType(type);
        param.setConstructorType(constructorType);
        return param;
    }


    public static MethodCallExpression MethodCallExpression(String methodName, Parameter subjectParameter,
                                                            VariableContainer from, Parameter callReturnParameter) {
        return new MethodCallExpression(methodName, subjectParameter, from, callReturnParameter, 0);
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
        return MethodCallExpression(
                "when", callSubject,
                VariableContainer.from(List.of(whenExpression)),
                null
        );

    }

    public static MethodCallExpression MockClass(ClassName targetClassname) {

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProb(new DataEventWithSessionId());


        return new MethodCallExpression(
                "mock", MockitoClass,
                VariableContainer.from(List.of(whenExpression)), null, 0);

    }

    public static MethodCallExpression MockStaticClass(ClassName targetClassname) {

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProb(new DataEventWithSessionId());


        return new MethodCallExpression(
                "mockStatic", MockitoClass,
                VariableContainer.from(List.of(whenExpression)), null, 0);

    }

    public static MethodCallExpression InitNoArgsConstructor(Parameter targetClassname) {

        // new $T()

        return new MethodCallExpression(
                "<init>", null,
                VariableContainer.from(List.of()), targetClassname, 0);

    }

    public static Expression MockitoThen(Parameter returnValue) {
        return MethodCallExpression("thenReturn", null,
                VariableContainer.from(List.of(returnValue)),
                null);

//        PlainValueExpression parameter = new PlainValueExpression(".thenReturn(" + thingToReturn + ")");
//        return parameter;
    }

    public static Expression MockitoThenThrow(Parameter exceptionValue) {

        return MethodCallExpression("thenThrow", null,
                VariableContainer.from(List.of(exceptionValue)),
                null);

//        PlainValueExpression parameter = new PlainValueExpression(".thenReturn(" + thingToReturn + ")");
//        return parameter;
    }

    public static Expression ToJson(Parameter object) {
        return MethodCallExpression("toJson", GsonClass,
                VariableContainer.from(List.of(object)),
                null);
    }

    public static Expression MockitoAssert(Parameter returnValue, Parameter returnSubjectInstanceName) {
        return MethodCallExpression("assertEquals", AssertClass,
                VariableContainer.from(List.of(returnValue, returnSubjectInstanceName)),
                null
        );
    }

    public static MethodCallExpression FromJson(Parameter object) {
        return MethodCallExpression("fromJson", GsonClass,
                VariableContainer.from(List.of(object)),
                null);
    }
}
