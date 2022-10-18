package com.insidious.plugin.pojo;

import com.esotericsoftware.asm.Opcodes;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.ParameterFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;
import com.insidious.plugin.pojo.dao.ProbeInfo;
import com.squareup.javapoet.ClassName;

import java.util.LinkedList;
import java.util.List;

public class MethodCallExpression implements Expression {

    private int callStack;
    private List<Parameter> arguments;
    private String methodName;
    private boolean isStaticCall;
    private Parameter subject;

    private ProbeInfo entryProbeInfo;
    private Parameter returnValue;
    private DataEventWithSessionId entryProbe;
    private int methodAccess;
    private long id;
    private List<DataEventWithSessionId> argumentProbes = new LinkedList<>();
    private DataEventWithSessionId returnDataEvent;

    public MethodCallExpression() {
    }


    public MethodCallExpression(
            String methodName,
            Parameter subject,
            List<Parameter> arguments,
            Parameter returnValue,
            int size) {
        this.methodName = methodName;
        this.subject = subject;
        this.arguments = arguments;
        this.returnValue = returnValue;
        this.callStack = size;
    }

    public static PendingStatement in(ObjectRoutineScript objectRoutine) {
        return new PendingStatement(objectRoutine);
    }

    public int getCallStack() {
        return callStack;
    }

    public void setCallStack(int callStack) {
        this.callStack = callStack;
    }

    public ProbeInfo getEntryProbeInfo() {
        return entryProbeInfo;
    }

    public void setEntryProbeInfo(DataInfo entryProbeInfo) {
        this.entryProbeInfo = ProbeInfo.FromProbeInfo(entryProbeInfo);
    }

    public Parameter getSubject() {
        return subject;
    }

    public void setSubject(Parameter testSubject) {
        this.subject = testSubject;
    }

    public List<Parameter> getArguments() {
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
        if (returnValue != null && returnValue.exception) {
            return returnValue;
        }
        return null;
    }

    @Override
    public void writeTo(ObjectRoutineScript objectRoutineScript) {

        Parameter mainMethodReturnValue = getReturnValue();
        if (mainMethodReturnValue == null) {
            return;
        }

        if (getMethodName().equals("mock")) {
            in(objectRoutineScript).assignVariable(mainMethodReturnValue).writeExpression(this).endStatement();
            return;
        }


        objectRoutineScript.addComment("");


        DataEventWithSessionId mainMethodReturnValueProbe = mainMethodReturnValue.getProb();
        if (mainMethodReturnValueProbe == null) {
            return;
        }
        objectRoutineScript.addComment("Test candidate method [" + getMethodName() + "] " +
                "[ " + getReturnValue().getProb().getNanoTime() + "] - took " +
                Long.valueOf(getReturnValue().getProb().getNanoTime() / (1000000)).intValue() + "ms");

        List<Parameter> arguments = getArguments();
        if (arguments != null && arguments.size() > 0) {

            objectRoutineScript.addComment("");
            for (Parameter parameter : arguments) {
                if (parameter.getName() == null &&
                        parameter.getProb() != null &&
                        parameter.getProb().getSerializedValue().length > 0
                ) {
//                    String serializedValue = new String(parameter.getProb().getSerializedValue());
//                    if (parameter.getType().equals("java.lang.String")) {
//                        serializedValue = '"' + serializedValue + '"';
//                    }
//                    parameter.setValue(serializedValue);
                }
                objectRoutineScript.addParameterComment(parameter);
            }
            objectRoutineScript.addComment("");
            objectRoutineScript.addComment("");

        }


//
        if (getArguments() != null) {
            for (Parameter parameter : getArguments()) {
                in(objectRoutineScript).assignVariable(parameter).fromRecordedValue().endStatement();
            }
        }
//

        //////////////////////// FUNCTION CALL ////////////////////////

        // return type == V ==> void return type => no return value
        in(objectRoutineScript).assignVariable(mainMethodReturnValue).writeExpression(this).endStatement();
        boolean isException = mainMethodReturnValue.getProbeInfo().getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT;


        if (getMethodName().equals("<init>") || isException) {
            // there is no verification required (?) after calling constructors or methods which
            // throw an exception
            return;
        }


        String returnSubjectInstanceName = mainMethodReturnValue.getName();


        //////////////////////////////////////////////// VERIFICATION ////////////////////////////////////////////////


        // deserialize and compare objects
        byte[] serializedBytes = mainMethodReturnValueProbe.getSerializedValue();


        Parameter returnSubjectExpectedJsonString = null;
        if (serializedBytes.length > 0) {
            returnSubjectExpectedJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "ExpectedJson");
            in(objectRoutineScript)
                    .assignVariable(returnSubjectExpectedJsonString)
                    .writeExpression(MethodCallExpressionFactory.StringExpression(new String(serializedBytes)))
                    .endStatement();
        } else {
            returnSubjectExpectedJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "ExpectedJson");

            in(objectRoutineScript)
                    .assignVariable(returnSubjectExpectedJsonString)
                    .writeExpression(MethodCallExpressionFactory.StringExpression(new String(serializedBytes)))
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


        returnSubjectExpectedJsonString.setValue(-1L);
        in(objectRoutineScript)
                .writeExpression(MethodCallExpressionFactory
                        .MockitoAssert(returnSubjectExpectedJsonString, returnSubjectJsonString))
                .endStatement();

    }

    public void writeCommentTo(ObjectRoutineScript objectRoutine) {
        VariableContainer variableContainer = objectRoutine.getCreatedVariables();
        Parameter returnValue = getReturnValue();
        Parameter exception = getException();
        String callArgumentsString = getArguments().size() + " arguments";


        if (returnValue != null) {

            String variableName = ClassTypeUtils.createVariableNameFromMethodName(getMethodName(), getReturnValue().getType());

            Object value = returnValue.getValue();
            boolean overrideName = true;
            if (value instanceof String) {
                String valueString = (String) value;
                if (valueString.equals("1") || valueString.equals("0")) {
                    overrideName = false;
                }
            }


            Parameter existingVariableById = variableContainer.getParametersById((long) value);
            if (existingVariableById != null) {
                if (overrideName && !returnValue.getName().equals(existingVariableById.getName())) {
                    returnValue.setName(existingVariableById.getName());
                }
            } else {
                if (returnValue.getName() == null) {
                    returnValue.setName(variableName);
                }
//                variableContainer.add(returnValue);
            }


            String returnValueType = ClassName.bestGuess(returnValue.getType()).simpleName();
            objectRoutine.addComment(
                    returnValueType + " " + returnValue.getName() + " = " +
                            getSubject().getName() + "." + getMethodName() +
                            "(" + callArgumentsString + "); // ==> "
                            + returnValue.getProb().getSerializedValue().length);
        } else if (exception != null) {
            objectRoutine.addComment(
                    getSubject().getName() + "." +
                            getMethodName() +
                            "(" + callArgumentsString + ");" +
                            " // ==>  throws exception " + exception.getType());
        }
    }

    @Override
    public String toString() {

        String owner = null;
        if (subject != null && subject.getProbeInfo() != null) {
            String owner1 = subject.getProbeInfo().getAttribute("Owner", null);
            if (owner1 != null) {
                owner = "[" + owner1 + "]";

            }
        }
        String name = "";
        if (subject != null) {
            name = subject.getName() + ".";
        }
        return ((returnValue == null || returnValue.getName() == null || returnValue.getException()) ?
                "" : (returnValue.getName() + " = ")) +
                name + methodName + "(" + arguments.size() + " args)" +
                (returnValue != null && returnValue.getException() ? " throws " + returnValue.getType() : "")
                + (owner == null ? "" : " in " + owner);
    }

    public void writeMockTo(ObjectRoutineScript objectRoutine) {
        Parameter returnValue = getReturnValue();
        if (returnValue == null) {
            return;
        }
        assert returnValue != null;

        // we don't want to write a mock call if the return value is null
        // since mocked classes return null by default and this mocking just adds noise to the generated test case
        if (returnValue.getProb() != null) {
            if (new String(returnValue.getProb().getSerializedValue()).equals("null")) {
                return;
            }
        }

        List<Parameter> argsContainer = getArguments();
        if (argsContainer != null) {
            for (Parameter argument : argsContainer) {
                if (argument.getName() != null) {

                    if (
                            (argument.getType().length() == 1 || argument.getType().startsWith("java.lang."))
                                    && !argument.getType().contains(".Object")
                    ) {
                        in(objectRoutine).assignVariable(argument).fromRecordedValue().endStatement();
                    }
                }
            }
        }

        if (returnValue.getException()) {
            Parameter exceptionValue = getException();
            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(this))
                    .writeExpression(MethodCallExpressionFactory.MockitoThenThrow(exceptionValue))
                    .endStatement();

        } else {
            if (returnValue.getCreatorExpression() == null) {
                in(objectRoutine).assignVariable(returnValue).fromRecordedValue().endStatement();
            } else {
                MethodCallExpression createrExpression = returnValue.getCreatorExpression();

                List<Parameter> arguments = createrExpression.getArguments();
                for (Parameter parameter : arguments) {
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

    public void setIsStatic(boolean aStatic) {
        this.isStaticCall = aStatic;
    }

    public boolean isStaticCall() {
        return isStaticCall;
    }

    public void setStaticCall(boolean staticCall) {
        isStaticCall = staticCall;
    }

    public DataEventWithSessionId getEntryProbe() {
        return entryProbe;
    }

    public void setEntryProbe(DataEventWithSessionId entryProbe) {
        this.entryProbe = entryProbe;
    }

    public boolean isMethodPublic() {
        return (methodAccess & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC;
    }

    public boolean isMethodProtected() {
        return (methodAccess & Opcodes.ACC_PROTECTED) == Opcodes.ACC_PROTECTED;
    }

    public int getMethodAccess() {
        return methodAccess;
    }

    public void setMethodAccess(int methodAccess) {
        this.methodAccess = methodAccess;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<DataEventWithSessionId> getArgumentProbes() {
        return argumentProbes;
    }

    public void setArgumentProbes(List<DataEventWithSessionId> argumentProbes) {
        this.argumentProbes = argumentProbes;
    }

    public void addArgument(Parameter existingParameter) {
        arguments.add(existingParameter);
    }

    public void addArgumentProbe(DataEventWithSessionId dataEvent) {
        argumentProbes.add(dataEvent);
    }

    public DataEventWithSessionId getReturnDataEvent() {
        return returnDataEvent;
    }

    public void setReturnDataEvent(DataEventWithSessionId returnDataEvent) {
        this.returnDataEvent = returnDataEvent;
    }
}
