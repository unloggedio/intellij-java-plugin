package com.insidious.plugin.pojo;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.ExpressionFactory;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;

import java.util.Optional;

public class MethodCallExpression implements Expression {
    private final VariableContainer arguments;
    private final String methodName;
    private final Parameter exception;
    private Parameter subject;
    private Parameter returnValue;

    public MethodCallExpression(
            String methodName,
            Parameter subject,
            VariableContainer arguments,
            Parameter returnValue,
            Parameter exception
    ) {
        this.methodName = methodName;
        this.subject = subject;
        this.arguments = arguments;
        this.returnValue = returnValue;
        this.exception = exception;
    }

    public static PendingStatement in(ObjectRoutineScript objectRoutine) {
        return new PendingStatement(objectRoutine);
    }

    public Parameter getSubject() {
        return subject;
    }

    public void setSubject(Parameter testSubject) {
        this.subject = testSubject;
    }

    public VariableContainer getArguments() {
        return arguments;
    }

    public Parameter getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(Parameter returnValue) {
        this.returnValue = returnValue;
    }

    public String getMethodName() {
        return methodName;
    }

    public Parameter getException() {
        return exception;
    }

    @Override
    public void writeTo(ObjectRoutineScript objectRoutineScript) {

        Parameter mainMethodReturnValue = getReturnValue();
        if (getMethodName().equals("mock")) {
            in(objectRoutineScript).assignVariable(mainMethodReturnValue).writeExpression(this).endStatement();
            return;
        }




        objectRoutineScript.addComment("");


        DataEventWithSessionId mainMethodReturnValueProbe = mainMethodReturnValue.getProb();
        if (mainMethodReturnValueProbe == null) {
            return;
        }

        VariableContainer arguments = getArguments();
        if (arguments.count() > 0) {

            objectRoutineScript.addComment("");
            for (Parameter parameter : arguments.all()) {
                if (parameter.getName() == null && parameter.getProb() != null && parameter.getProb().getSerializedValue().length > 0) {
                    String serializedValue = new String(parameter.getProb().getSerializedValue());
                    if (parameter.getType().equals("java.lang.String")) {
                        serializedValue = '"' + serializedValue + '"';
                    }
                    parameter.setValue(serializedValue);
                }
                objectRoutineScript.addParameterComment(parameter);
            }
            objectRoutineScript.addComment("");
            objectRoutineScript.addComment("");

            for (Parameter parameter : arguments.all()) {
                if (parameter.getName() != null) {
                    in(objectRoutineScript).assignVariable(parameter).fromRecordedValue().endStatement();
                }
            }

        }


//
        for (Parameter parameter : getArguments().all()) {
            in(objectRoutineScript).assignVariable(parameter).fromRecordedValue().endStatement();
        }
//

        //////////////////////// FUNCTION CALL ////////////////////////

        // return type == V ==> void return type => no return value
        in(objectRoutineScript).assignVariable(mainMethodReturnValue).writeExpression(this).endStatement();


        if (getMethodName().equals("<init>")) {
            // there is no verification required (?) after calling constructors
            return;
        }

        objectRoutineScript.addComment("Test candidate method [" + getMethodName() + "] " +
                "[ " + getReturnValue().getProb().getNanoTime() + "] - took " +
                Long.valueOf(getReturnValue().getProb().getNanoTime() / (1000000)).intValue() + "ms");


        String returnSubjectInstanceName = mainMethodReturnValue.getName();


        //////////////////////////////////////////////// VERIFICATION ////////////////////////////////////////////////


        // deserialize and compare objects
        byte[] serializedBytes = mainMethodReturnValueProbe.getSerializedValue();


        Parameter returnSubjectExpectedJsonString = null;
        if (serializedBytes.length > 0) {
            returnSubjectExpectedJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "ExpectedJson");
            in(objectRoutineScript)
                    .assignVariable(returnSubjectExpectedJsonString)
                    .writeExpression(ExpressionFactory.StringExpression(new String(serializedBytes)))
                    .endStatement();
        } else {
            returnSubjectExpectedJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "ExpectedJson");

            in(objectRoutineScript)
                    .assignVariable(returnSubjectExpectedJsonString)
                    .writeExpression(ExpressionFactory.StringExpression(new String(serializedBytes)))
                    .endStatement();
        }


        // reconstruct object from the serialized form to an object instance in the
        // test method to compare it with the new object, or do it the other way
        // round ? Maybe serializing the object and then comparing the serialized
        // string forms would be more readable ? string comparison would fail if the
        // serialization has fields serialized in random order
        Parameter returnSubjectJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "Json");
        in(objectRoutineScript)
                .assignVariable(returnSubjectJsonString)
                .writeExpression(MethodCallExpressionFactory.ToJson(mainMethodReturnValue))
                .endStatement();


        in(objectRoutineScript)
                .writeExpression(MethodCallExpressionFactory
                        .MockitoAssert(returnSubjectJsonString, returnSubjectExpectedJsonString))
                .endStatement();

    }

    public void writeCommentTo(ObjectRoutineScript objectRoutine) {
        VariableContainer variableContainer = objectRoutine.getCreatedVariables();
        Parameter returnValue = getReturnValue();
        Parameter exception = getException();
//        String callArgumentsString = createMethodParametersString(getArguments());


        if (returnValue != null) {

            String variableName =
                    ClassTypeUtils.createVariableNameFromMethodName(
                            getMethodName(),
                            getReturnValue().getType());

            Object value = returnValue.getValue();
            boolean overrideName = true;
            if (value instanceof String) {
                String valueString = (String) value;
                if (valueString.equals("1") || valueString.equals("0")) {
                    overrideName = false;
                }
            }


            Optional<Parameter> existingVariableById = variableContainer.getParametersById((String) value);
            if (existingVariableById.isPresent()) {
                if (overrideName && !returnValue.getName().equals(existingVariableById.get().getName())) {
                    returnValue.setName(existingVariableById.get().getName());
                }
            } else {
                if (returnValue.getName() == null) {
                    returnValue.setName(variableName);
                }
//                variableContainer.add(returnValue);
            }


//            objectRoutine.addComment(
//                    returnValue.getType() + " " + returnValue.getName() + " = " +
//                            getSubject().getName() + "." + getMethodName() +
//                            "(" + callArgumentsString + "); // ==> "
//                            + returnValue.getProb().getSerializedValue().length);
        } else if (exception != null) {
//            objectRoutine.addComment(
//                    getSubject().getName() + "." +
//                            getMethodName() +
//                            "(" + callArgumentsString + ");" +
//                            " // ==>  throws exception " + exception.getType());
        }
    }

    public void writeMockTo(ObjectRoutineScript objectRoutine) {
        Parameter returnValue = getReturnValue();

        for (Parameter argument : getArguments().all()) {
            if (argument.getName() != null) {
                in(objectRoutine).assignVariable(argument).fromRecordedValue().endStatement();
            }
        }

        if (returnValue == null) {
            Parameter exceptionValue = getException();
            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(this))
                    .writeExpression(MethodCallExpressionFactory.MockitoThenThrow(exceptionValue))
                    .endStatement();

        } else {
            if (returnValue.getCreaterExpression() == null) {
                in(objectRoutine).assignVariable(returnValue).fromRecordedValue().endStatement();
            } else {
                MethodCallExpression createrExpression = returnValue.getCreaterExpression();

                VariableContainer arguments = createrExpression.getArguments();
                for (Parameter parameter : arguments.all()) {
                    in(objectRoutine)
                            .assignVariable(parameter)
                            .fromRecordedValue()
                            .endStatement();
                }


                in(objectRoutine)
                        .assignVariable(createrExpression.getReturnValue())
                        .writeExpression(createrExpression)
                        .endStatement();
            }

            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(this))
                    .writeExpression(MethodCallExpressionFactory.MockitoThen(returnValue))
                    .endStatement();
        }

    }

}
