package com.insidious.plugin.factory;

import com.insidious.plugin.pojo.ConstructorType;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.squareup.javapoet.ClassName;

import java.util.List;

public class MethodCallExpressionFactory {

    private static final ClassName mockitoClass = ClassName.bestGuess("org.mockito.Mockito");
    private static final Parameter MockitoClass;
    private static final Parameter GsonClass;

    static {
        MockitoClass = makeParameter("Mockito", "org.mockito.Mockito", ConstructorType.SINGLETON);
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
                        "(" + TestCaseWriter.createMethodParametersString(methodCallExpression.getArguments()) + ")";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);

        MethodCallExpression mockitoWhenCall = new MethodCallExpression(
                "when", MockitoClass,
                VariableContainer.from(List.of(whenExpression)),
                null, null
        );


        return mockitoWhenCall;

    }

    public static MethodCallExpression MockClass(ClassName targetClassname) {

        // $T.mock($T.class)

        String param1 = targetClassname.simpleName() + ".class";

        Parameter whenExpression = new Parameter();
        whenExpression.setValue(param1);

        MethodCallExpression mockitoWhenCall = new MethodCallExpression(
                "mock", MockitoClass,
                VariableContainer.from(List.of(whenExpression)),
                null, null
        );


        return mockitoWhenCall;

    }

    public static Expression MockitoThen(Parameter returnValue) {
        PlainValueExpression parameter = new PlainValueExpression(".thenReturn(" + returnValue.getName() + ")");
        return parameter;
    }

    public static Expression ToJson(Parameter object) {
        Expression expression = new MethodCallExpression(
                "toJson", GsonClass, VariableContainer.from(
                        List.of(
                                object
                        )
        ), null, null
        );
        return expression;
    }
}
