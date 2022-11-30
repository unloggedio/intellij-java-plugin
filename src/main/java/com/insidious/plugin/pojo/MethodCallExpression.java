package com.insidious.plugin.pojo;

import com.esotericsoftware.asm.Opcodes;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.ClassName;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MethodCallExpression implements Expression, Serializable {

    private static final Logger logger = LoggerUtil.getInstance(MethodCallExpression.class);
    private int callStack;
    private List<Parameter> arguments;
    private String methodName;
    private boolean isStaticCall;
    private Parameter subject;
    private DataInfo entryProbeInfo;
    private Parameter returnValue;
    private DataEventWithSessionId entryProbe;
    private int methodAccess;
    private long id;
    private List<DataEventWithSessionId> argumentProbes = new LinkedList<>();
    private DataEventWithSessionId returnDataEvent;
    private boolean usesFields;

    private boolean isUIselected = false;

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

    public boolean isUIselected() {
        return isUIselected;
    }

    public void setUIselected(boolean UIselected) {
        isUIselected = UIselected;
    }

    public static PendingStatement in(ObjectRoutineScript objectRoutine) {
        return new PendingStatement(objectRoutine);
    }

    public boolean getUsesFields() {
        return usesFields;
    }

    public void setUsesFields(boolean b) {
        this.usesFields = b;
    }

    public int getCallStack() {
        return callStack;
    }

    public void setCallStack(int callStack) {
        this.callStack = callStack;
    }

    public DataInfo getEntryProbeInfo() {
        return entryProbeInfo;
    }

    public void setEntryProbeInfo(DataInfo entryProbeInfo) {
        this.entryProbeInfo = entryProbeInfo;
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

    /*
        if the name is already used in the script and,
        if the parameter type and template map are not equal
        then Generates a New Name,
     */
    private Parameter generateParameterName(Parameter parameter,ObjectRoutineScript ors){
        String lhsExprName = parameter.getNameForUse(null);
        Parameter variableExistingParameter = ors
                .getCreatedVariables()
                .getParameterByNameAndType(lhsExprName, parameter);

        Parameter sameNameParamExisting = ors
                .getCreatedVariables()
                .getParameterByName(lhsExprName);
        // If the type and the template map does not match
        // then We should have a new name for the variable,
        // and it should be redeclared =>
        // eg "String var" instead of "var"
        if (sameNameParamExisting != null && variableExistingParameter == null) {
            // generate a next name from existing name
            //eg: name =>  name0 =>  name1
            String newName = generateNextName(ors, sameNameParamExisting.getName());
            parameter.setName(newName);
        }
        return parameter;
    }

    // eg: name => name0 => name1 ... so on
    private String generateNextName(ObjectRoutineScript ors,String name) {
        for (int i = 0; i < 100; i++) {
            if (!ors.getCreatedVariables()
                    .contains(name + i)) {
                return name + i;
            }
        }
        return "thisNeverHappened";
    }

    public Parameter getException() {
        if (returnValue != null && returnValue.exception) {
            return returnValue;
        }
        return null;
    }

    @Override
    public void writeTo(
            ObjectRoutineScript objectRoutineScript,
            TestCaseGenerationConfiguration testConfiguration,
            TestGenerationState testGenerationState) {

        Parameter mainMethodReturnValue = getReturnValue();

        mainMethodReturnValue = generateParameterName(mainMethodReturnValue,objectRoutineScript);

        if (mainMethodReturnValue == null) {
            logger.error("writing a call without a return value is skipped - " + this);
            return;
        }

        if (getMethodName().equals("mock")) {
            in(objectRoutineScript).assignVariable(mainMethodReturnValue).writeExpression(this).endStatement();
            return;
        }


//        objectRoutineScript.addComment("");


        DataEventWithSessionId mainMethodReturnValueProbe = mainMethodReturnValue.getProb();
        if (mainMethodReturnValueProbe == null) {
            return;
        }
        objectRoutineScript.addComment("Test candidate method [" + getMethodName() + "] " +
                "[ " + getReturnValue().getProb().getNanoTime() + "] - took " +
                Long.valueOf((getReturnValue().getProb().getRecordedAt() - getEntryProbe().getRecordedAt() ) / (1000000)).intValue() +
                "ms");

        List<Parameter> arguments = getArguments();
        if (arguments != null) {
            for (Parameter parameter : arguments) {
                if (parameter.isPrimitiveType() || parameter.getValue() < 1) {
                    // we don't need boolean values in a variable, always use boolean values directly
                    continue;
                }

                parameter = generateParameterName(parameter,objectRoutineScript);

                in(objectRoutineScript).assignVariable(parameter)
                        .fromRecordedValue(testConfiguration, testGenerationState)
                        .endStatement();
            }
        }

        //////////////////////// FUNCTION CALL ////////////////////////

        // return type == V ==> void return type => no return value
        boolean isException = mainMethodReturnValue.getProbeInfo().getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT;
        if (isException) {
            in(objectRoutineScript).assignVariable(mainMethodReturnValue).writeExpression(this).endStatement();
            return;
        } else {
            in(objectRoutineScript).assignVariable(mainMethodReturnValue).writeExpression(this).endStatement();
        }


        if (getMethodName().equals("<init>")) {
            // there is no verification required (?) after calling constructors or methods which
            // throw an exception
            return;
        }


        if (mainMethodReturnValue.getType().equals("V")) {
            return;
        }
        String returnSubjectInstanceName = mainMethodReturnValue.getNameForUse(this.methodName);


        //////////////////////////////////////////////// VERIFICATION ////////////////////////////////////////////////


        // deserialize and compare objects
        byte[] serializedBytes = mainMethodReturnValueProbe.getSerializedValue();


        Parameter returnSubjectExpectedObject;
        // not sure why there was a if condition since both the blocks are same ?
//        if (serializedBytes.length > 0) {
        String expectedParameterName = returnSubjectInstanceName + "Expected";
        returnSubjectExpectedObject = Parameter.cloneParameter(mainMethodReturnValue);
        // we will set a new name for this parameter
        returnSubjectExpectedObject.clearNames();
        returnSubjectExpectedObject.setName(expectedParameterName);

        if (testConfiguration.getResourceEmbedMode().equals(ResourceEmbedMode.IN_CODE) || returnSubjectExpectedObject.isPrimitiveType()) {
            if (returnSubjectExpectedObject.isPrimitiveType()) {
                in(objectRoutineScript)
                        .assignVariable(returnSubjectExpectedObject)
                        .fromRecordedValue(testConfiguration, testGenerationState)
                        .endStatement();

            } else {
                in(objectRoutineScript)
                        .assignVariable(returnSubjectExpectedObject)
                        .writeExpression(MethodCallExpressionFactory.StringExpression(new String(serializedBytes)))
                        .endStatement();
            }

        } else if (testConfiguration.getResourceEmbedMode().equals(ResourceEmbedMode.IN_FILE)) {

            String nameForObject = testGenerationState.addObjectToResource(mainMethodReturnValue);
            @NotNull Parameter jsonParameter = Parameter.cloneParameter(mainMethodReturnValue);
            DataEventWithSessionId prob = new DataEventWithSessionId();
            prob.setSerializedValue(nameForObject.getBytes(StandardCharsets.UTF_8));
            jsonParameter.setProb(prob);
            MethodCallExpression jsonFromFileCall = MethodCallExpressionFactory.FromJsonFetchedFromFile(jsonParameter);

            in(objectRoutineScript)
                    .assignVariable(returnSubjectExpectedObject)
                    .writeExpression(jsonFromFileCall)
                    .endStatement();
        }

        // reconstruct object from the serialized form to an object instance in the
        // test method to compare it with the new object, or do it the other way
        // round ? Maybe serializing the object and then comparing the serialized
        // string forms would be more readable ? string comparison would fail if the
        // serialization has fields serialized in random order

        // CHANGE -> we will compare the objects directly, and not their JSON values
//        Parameter returnSubjectJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "Json");
//        in(objectRoutineScript)
//                .assignVariable(returnSubjectJsonString)
//                .writeExpression(MethodCallExpressionFactory.ToJson(mainMethodReturnValue))
//                .endStatement();


        returnSubjectExpectedObject.setValue(-1L);
        in(objectRoutineScript)
                .writeExpression(MethodCallExpressionFactory
                        .MockitoAssert(returnSubjectExpectedObject, mainMethodReturnValue, testConfiguration))
                .endStatement();

    }

    public void writeCommentTo(ObjectRoutineScript objectRoutine) {
        VariableContainer variableContainer = objectRoutine.getCreatedVariables();
        Parameter exception = getException();
        String callArgumentsString = getArguments().size() + " arguments";


        String subjectName = "";
        if (subject != null) {
            subjectName = getSubject().getNameForUse(null);
        }
        if (returnValue != null) {

            String variableName = ClassTypeUtils.createVariableNameFromMethodName(methodName, returnValue.getType());

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
                if (overrideName && !Objects.equals(returnValue.getName(), existingVariableById.getName())) {
                    returnValue.setName(existingVariableById.getName());
                }
            } else {
                if (returnValue.getNameForUse(methodName) == null) {
                    returnValue.setName(variableName);
                }
            }


            String returnValueType = returnValue.getType() == null ? "" : ClassName.bestGuess(returnValue.getType()).simpleName();
            objectRoutine.addComment(
                    returnValueType + " " + returnValue.getNameForUse(getMethodName())
                            + " = " + subjectName + "." + getMethodName() + "(" + callArgumentsString + ");");
        } else if (exception != null) {
            objectRoutine.addComment(
                    subjectName + "." +
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
        String methodName1 = methodName;
        String methodCallOnVariableString = name + methodName1;
        if (subject != null && subject.getType() != null) {
            if (methodName.equals("<init>")) {
                methodCallOnVariableString = "new " + ClassName.bestGuess(subject.getType()).simpleName();
            } else {
                methodCallOnVariableString = ClassName.bestGuess(subject.getType()).simpleName() + "." + methodName1;
            }
        }
        return
//                ((returnValue == null || returnValue.getName() == null || returnValue.getException()) ? "" : (returnValue.getName() + " = ")) +
                methodCallOnVariableString + "(" + arguments.size() + " args)" +
                        (returnValue != null && returnValue.getException() ? " throws " + returnValue.getType() : "");
    }

    public void writeMockTo(
            ObjectRoutineScript objectRoutine,
            TestCaseGenerationConfiguration testCaseGenerationConfiguration,
            TestGenerationState testGenerationState
    ) {
        Parameter returnValue = getReturnValue();
        if (returnValue.getType() == null) {
            return;
        }

        // we don't want to write a mock call if the return value is null
        // since mocked classes return null by default and this mocking just adds noise to the generated test case
        // but if the return value was a boxed primitive (like Integer), then the mock return 0 by default instead of null
        // so we need to add an explicit call to return null for them and not return early here
        if (returnValue.getProb() != null && !returnValue.isPrimitiveType()) {
            if (new String(returnValue.getProb().getSerializedValue()).equals("null")) {
                return;
            }
        }

        List<Parameter> argsContainer = getArguments();
        if (argsContainer != null) {
            for (Parameter argument : argsContainer) {
                Parameter existingParameter = objectRoutine.getCreatedVariables().getParameterByValue(argument.getValue());
                String nameForUse;
                if (existingParameter != null && existingParameter.getName() != null) {
                    nameForUse = argument.getNameForUse(existingParameter.getName());
                } else {
                    nameForUse = argument.getNameForUse(methodName);
                }
                if (nameForUse != null) {
                    if (argument.isPrimitiveType()) {
                        // boolean values to be used directly without their names
                        // since a lot of them conflict with each other
                        continue;
                    }

                    if ((argument.getType().length() == 1 ||
                            argument.getType().startsWith("java.lang.")) && !argument.getType().contains(".Object")
                    ) {
                        in(objectRoutine)
                                .assignVariable(argument)
                                .fromRecordedValue(testCaseGenerationConfiguration, testGenerationState)
                                .endStatement();
                    }
                }
            }
        }

        if (returnValue.getException()) {
            Parameter exceptionValue = getException();
            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(this, objectRoutine.getGenerationConfiguration()))
                    .writeExpression(MethodCallExpressionFactory.MockitoThenThrow(exceptionValue))
                    .endStatement();

        } else {
            if (returnValue.getCreatorExpression() == null) {
                if (!returnValue.isPrimitiveType()) {
                    //check if the same name is used already with different type
                    returnValue = generateParameterName(returnValue,objectRoutine);

                    // if it is not a primitive type, then we assign to a variable first and
                    // then use the variable in actual usage
                    in(objectRoutine)
                            .assignVariable(returnValue)
                            .fromRecordedValue(testCaseGenerationConfiguration, testGenerationState)
                            .endStatement();
                }
            } else {
                MethodCallExpression creatorExpression = returnValue.getCreatorExpression();

                List<Parameter> arguments = creatorExpression.getArguments();
                for (Parameter parameter : arguments) {
                    //check if the same name is used already with different type
                    parameter = generateParameterName(returnValue,objectRoutine);

                    in(objectRoutine)
                            .assignVariable(parameter)
                            .fromRecordedValue(testCaseGenerationConfiguration, testGenerationState)
                            .endStatement();
                }


                in(objectRoutine)
                        .assignVariable(creatorExpression.getReturnValue())
                        .writeExpression(creatorExpression)
                        .endStatement();
            }

            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory.MockitoWhen(this, objectRoutine.getTestConfiguration()))
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

    public void setArguments(List<Parameter> arguments) {
        this.arguments = arguments;
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
