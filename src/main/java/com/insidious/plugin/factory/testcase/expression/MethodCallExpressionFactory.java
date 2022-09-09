package com.insidious.plugin.factory.testcase.expression;

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

    public static MethodCallExpression MockitoWhen(MethodCallExpression methodCallExpression) {


        /**
         *
         * objectRoutine.addStatement(
         *                             "$T.when($L.$L($L)).thenReturn($L)",
         *                             mockitoClass, methodCallExpression.getSubject().getName(),
         *                             methodCallExpression.getMethodName(), callArgumentsString,
         *                             callReturnValue
         *                     );
         */


        String param1 =
                methodCallExpression.getSubject().getName() + "." + methodCallExpression.getMethodName() +
                        "(" + TestCaseWriter.createMethodParametersStringMock(methodCallExpression.getArguments()) + ")";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);


        return ExpressionFactory.MethodCallExpression(
                "when", MockitoClass,
                VariableContainer.from(List.of(whenExpression)),
                null, null
        );

    }

    public static MethodCallExpression MockClass(ClassName targetClassname) {

        // $T.mock($T.class)

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);
        whenExpression.setType("java.lang.Class");
        whenExpression.setProb(new DataEventWithSessionId());


        return new MethodCallExpression(
                "mock", MockitoClass,
                VariableContainer.from(List.of(whenExpression)),
                null, null
        );

    }

    public static MethodCallExpression InitNoArgsConstructor(Parameter targetClassname) {

        // new $T()

        return new MethodCallExpression(
                "<init>", null,
                VariableContainer.from(List.of()), targetClassname, null
        );

    }

    public static Expression MockitoThen(Parameter returnValue) {
        return ExpressionFactory.MethodCallExpression("thenReturn", null,
                VariableContainer.from(
                        List.of(returnValue)
                ), null, null);

//        PlainValueExpression parameter = new PlainValueExpression(".thenReturn(" + thingToReturn + ")");
//        return parameter;
    }

    public static Expression MockitoThenThrow(Parameter exceptionValue) {

        return ExpressionFactory.MethodCallExpression(
                "thenThrow", null,
                VariableContainer.from(List.of(exceptionValue)),
                null, null);

//        PlainValueExpression parameter = new PlainValueExpression(".thenReturn(" + thingToReturn + ")");
//        return parameter;
    }

    public static Expression ToJson(Parameter object) {
        return ExpressionFactory.MethodCallExpression(
                "toJson", GsonClass,
                VariableContainer.from(List.of(object)),
                null, null);
    }

    public static Expression MockitoAssert(Parameter returnValue, Parameter returnSubjectInstanceName) {
        return ExpressionFactory.MethodCallExpression(
                "assertEquals", AssertClass,
                VariableContainer.from(List.of(returnValue, returnSubjectInstanceName)),
                null, null
        );
    }

    public static Expression FromJson(Parameter object) {

        Parameter returnTypeParameter = new Parameter();
        returnTypeParameter.setValue(object.getType().replace('$', '.'));
        Parameter jsonValue = new Parameter();
        jsonValue.setValue(new String(object.getProb().getSerializedValue()));
        return ExpressionFactory.MethodCallExpression(
                "fromJson", GsonClass,
                VariableContainer.from(List.of(jsonValue, returnTypeParameter)),
                null, null);
    }
}
