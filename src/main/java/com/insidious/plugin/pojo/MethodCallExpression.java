package com.insidious.plugin.pojo;

import com.esotericsoftware.asm.Opcodes;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.ParameterNameFactory;
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
import com.intellij.openapi.util.text.Strings;
import com.squareup.javapoet.ClassName;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private long parentId = -1;
    private List<DataEventWithSessionId> argumentProbes = new ArrayList<>();
    private DataEventWithSessionId returnDataEvent;
    private boolean usesFields;

    private boolean isUIselected = false;
    private int methodDefinitionId;

    public MethodCallExpression() {
    }

    public MethodCallExpression(
            String methodName,
            Parameter subject,
            List<Parameter> arguments,
            Parameter returnValue,
            int callStack) {
        this.methodName = methodName;
        this.subject = subject;
        this.arguments = arguments;
        this.returnValue = returnValue;
        this.callStack = callStack;
    }

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }


    public boolean isUIselected() {
        return isUIselected;
    }

    public void setUIselected(boolean UIselected) {
        isUIselected = UIselected;
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

    public void setArguments(List<Parameter> arguments) {
        this.arguments = arguments;
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
    private Parameter generateParameterName(Parameter parameter, ObjectRoutineScript ors) {
        ParameterNameFactory nameFactory = ors.getTestGenerationState()
                .getParameterNameFactory();
        String lhsExprName = nameFactory.getNameForUse(parameter, this.methodName);
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
            String oldName = nameFactory.getNameForUse(sameNameParamExisting, null);
            String newName = generateNextName(ors, oldName);
            parameter.getNamesList()
                    .remove(newName);
            parameter.getNamesList()
                    .remove(oldName);
            parameter.setName(newName);
        }
        return parameter;
    }

    // eg: name => name0 => name1 ... so on
    private String generateNextName(ObjectRoutineScript ors, String name) {
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

        mainMethodReturnValue = generateParameterName(mainMethodReturnValue, objectRoutineScript);

        if (mainMethodReturnValue == null) {
            logger.error("writing a call without a return value is skipped - " + this);
            return;
        }

        if (getMethodName().equals("mock")) {
            PendingStatement.in(objectRoutineScript, testGenerationState)
                    .assignVariable(mainMethodReturnValue)
                    .writeExpression(this)
                    .endStatement();
            return;
        }


//        objectRoutineScript.addComment("");


        DataEventWithSessionId mainMethodReturnValueProbe = mainMethodReturnValue.getProb();
        if (mainMethodReturnValueProbe == null) {
            return;
        }
        if (entryProbe != null) {
            DataEventWithSessionId returnProbe = getReturnValue().getProb();
            objectRoutineScript.addComment("Test candidate method [" + getMethodName() + "] " +
                    "[" + entryProbe.getNanoTime() + "," + entryProbe.getThreadId() + "] - took " +
                    Long.valueOf((returnProbe.getRecordedAt() - entryProbe.getRecordedAt()) / (1000000))
                            .intValue() + "ms");
        }

        List<Parameter> arguments = getArguments();
        if (arguments != null) {
            for (Parameter parameter : arguments) {
                if (parameter.isPrimitiveType() || parameter.getValue() < 1) {
                    // we don't need boolean values in a variable, always use boolean values directly
                    continue;
                }

                parameter = generateParameterName(parameter, objectRoutineScript);

                PendingStatement.in(objectRoutineScript, testGenerationState)
                        .assignVariable(parameter)
                        .fromRecordedValue(testConfiguration)
                        .endStatement();
            }
        }

        //////////////////////// FUNCTION CALL ////////////////////////

        // return type == V ==> void return type => no return value
        boolean isException = mainMethodReturnValue.getProbeInfo()
                .getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT;
        if (isException) {
            PendingStatement.in(objectRoutineScript, testGenerationState)
                    .assignVariable(mainMethodReturnValue)
                    .writeExpression(this)
                    .endStatement();
            return;
        } else {
            PendingStatement.in(objectRoutineScript, testGenerationState)
                    .assignVariable(mainMethodReturnValue)
                    .writeExpression(this)
                    .endStatement();
        }


        if (getMethodName().equals("<init>")) {
            // there is no verification required (?) after calling constructors or methods which
            // throw an exception
            return;
        }


        if (mainMethodReturnValue.getType() == null || mainMethodReturnValue.getType()
                .equals("V")) {
            return;
        }
        ParameterNameFactory nameFactory = testGenerationState.getParameterNameFactory();
        String returnSubjectInstanceName = nameFactory.getNameForUse(mainMethodReturnValue, this.methodName);


        //////////////////////////////////////////////// VERIFICATION ////////////////////////////////////////////////


        // deserialize and compare objects
        byte[] serializedBytes = mainMethodReturnValueProbe.getSerializedValue();


        Parameter returnSubjectExpectedObject;
        // not sure why there was a if condition since both the blocks are same ?
//        if (serializedBytes.length > 0) {
        String expectedParameterName = returnSubjectInstanceName + "Expected";
        returnSubjectExpectedObject = Parameter.cloneParameter(mainMethodReturnValue);
        Instant now = Instant.now();
        returnSubjectExpectedObject.setValue(now.getEpochSecond() + now.getNano());
        // we will set a new name for this parameter
        returnSubjectExpectedObject.clearNames();
        returnSubjectExpectedObject.setName(expectedParameterName);

        if (testConfiguration.getResourceEmbedMode()
                .equals(ResourceEmbedMode.IN_CODE) || returnSubjectExpectedObject.isPrimitiveType()) {
            if (returnSubjectExpectedObject.isPrimitiveType()) {
                PendingStatement.in(objectRoutineScript, testGenerationState)
                        .assignVariable(returnSubjectExpectedObject)
                        .fromRecordedValue(testConfiguration)
                        .endStatement();

            } else {
                PendingStatement.in(objectRoutineScript, testGenerationState)
                        .assignVariable(returnSubjectExpectedObject)
                        .writeExpression(MethodCallExpressionFactory.StringExpression(new String(serializedBytes)))
                        .endStatement();
            }

        } else if (testConfiguration.getResourceEmbedMode()
                .equals(ResourceEmbedMode.IN_FILE)) {

            String nameForObject = testGenerationState.addObjectToResource(mainMethodReturnValue);
            @NotNull Parameter jsonParameter = Parameter.cloneParameter(mainMethodReturnValue);
            DataEventWithSessionId prob = new DataEventWithSessionId();
            prob.setSerializedValue(nameForObject.getBytes(StandardCharsets.UTF_8));
            jsonParameter.setProb(prob);
            MethodCallExpression jsonFromFileCall = null;
            jsonFromFileCall = MethodCallExpressionFactory.FromJsonFetchedFromFile(jsonParameter);

            PendingStatement.in(objectRoutineScript, testGenerationState)
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


        // If the type of the returnSubjectExpectedObject is a array (int[], long[], byte[])
        // then use assertArrayEquals
        if (returnSubjectExpectedObject.getType()
                .endsWith("[]")) {
            PendingStatement.in(objectRoutineScript, testGenerationState)
                    .writeExpression(MethodCallExpressionFactory
                            .MockitoAssertArrayEquals(returnSubjectExpectedObject, mainMethodReturnValue,
                                    testConfiguration))
                    .endStatement();
        } else {
            PendingStatement.in(objectRoutineScript, testGenerationState)
                    .writeExpression(MethodCallExpressionFactory
                            .MockitoAssertEquals(returnSubjectExpectedObject, mainMethodReturnValue, testConfiguration))
                    .endStatement();
        }
    }

    public void writeCommentTo(ObjectRoutineScript objectRoutine) {
        VariableContainer variableContainer = objectRoutine.getCreatedVariables();
        Parameter exception = getException();
        ParameterNameFactory nameFactory = objectRoutine.getTestGenerationState()
                .getParameterNameFactory();
        String callArgumentsString = getArguments().size() + " arguments";


        String subjectName = "";
        if (subject != null) {
            subjectName = nameFactory.getNameForUse(getSubject(), this.methodName);
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
                if (nameFactory.getNameForUse(returnValue, methodName) == null) {
                    returnValue.setName(variableName);
                }
            }


            String returnValueType = returnValue.getType() == null ? "" : ClassName.bestGuess(returnValue.getType())
                    .simpleName();
            objectRoutine.addComment(
                    returnValueType + " " + nameFactory.getNameForUse(returnValue, getMethodName())
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
            String owner1 = subject.getProbeInfo()
                    .getAttribute("Owner", null);
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
                methodCallOnVariableString = "new " + ClassName.bestGuess(subject.getType())
                        .simpleName();
            } else {
                methodCallOnVariableString = ClassName.bestGuess(subject.getType())
                        .simpleName() + "." + methodName1;
            }
        }
        return
//                ((returnValue == null || returnValue.getName() == null || returnValue.getException()) ? "" : (returnValue.getName() + " = ")) +
                methodCallOnVariableString + "(" + Strings.join(arguments.stream()
                        .map(Parameter::getType)
                        .collect(Collectors.toList()), ", ") + " args)" +
                        (returnValue != null && returnValue.getException() ? " throws " + returnValue.getType() : "");
    }

    public void writeReturnValue(
            ObjectRoutineScript objectRoutine,
            TestCaseGenerationConfiguration testCaseGenerationConfiguration,
            TestGenerationState testGenerationState
    ) {
        Parameter returnValue = getReturnValue();
        if (returnValue.getType() == null || returnValue.getValue() == 0) {
            return;
        }

        if (returnValue.getException()) {

        } else {
            if (returnValue.getCreatorExpression() == null) {
                if (!returnValue.isPrimitiveType()) {
                    //check if the same name is used already with different type
                    returnValue = generateParameterName(returnValue, objectRoutine);

                    // if it is not a primitive type, then we assign to a variable first and
                    // then use the variable in actual usage
                    PendingStatement.in(objectRoutine, testGenerationState)
                            .assignVariable(returnValue)
                            .fromRecordedValue(testCaseGenerationConfiguration)
                            .endStatement();
                }
            } else {
                MethodCallExpression creatorExpression = returnValue.getCreatorExpression();

                List<Parameter> arguments = creatorExpression.getArguments();
                for (Parameter parameter : arguments) {
                    //check if the same name is used already with different type
                    parameter = generateParameterName(returnValue, objectRoutine);

                    PendingStatement.in(objectRoutine, testGenerationState)
                            .assignVariable(parameter)
                            .fromRecordedValue(testCaseGenerationConfiguration)
                            .endStatement();
                }

                PendingStatement.in(objectRoutine, testGenerationState)
                        .assignVariable(creatorExpression.getReturnValue())
                        .writeExpression(creatorExpression)
                        .endStatement();
            }

            logger.warn("Mocked method [" + this.getMethodName() + "] expected return => " + returnValue);
        }


    }

    public void writeCallArguments(
            ObjectRoutineScript objectRoutine,
            TestCaseGenerationConfiguration testCaseGenerationConfiguration,
            TestGenerationState testGenerationState) {
        List<Parameter> argsContainer = getArguments();
        ParameterNameFactory nameFactory = testGenerationState.getParameterNameFactory();
        if (argsContainer != null) {
            for (Parameter argument : argsContainer) {
                Parameter existingParameter = objectRoutine.getCreatedVariables()
                        .getParameterByValue(argument.getValue());
                String nameForUse;
                if (existingParameter != null && nameFactory.getNameForUse(existingParameter, null) != null) {
                    nameForUse = nameFactory.getNameForUse(argument, existingParameter.getName());
                } else {
                    nameForUse = nameFactory.getNameForUse(argument, methodName);
                }
                if (nameForUse != null) {
                    if (argument.isPrimitiveType()) {
                        // boolean values to be used directly without their names
                        // since a lot of them conflict with each other
                        continue;
                    }

                    String argumentType = argument.getType();
                    if ((argumentType.length() == 1 || argumentType.startsWith("java.lang."))
                            && !argumentType.contains(".Object")
                    ) {
                        PendingStatement.in(objectRoutine, testGenerationState)
                                .assignVariable(argument)
                                .fromRecordedValue(testCaseGenerationConfiguration)
                                .endStatement();
                    }
                }
            }
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

    public int getMethodDefinitionId() {
        return methodDefinitionId;
    }

    public void setMethodDefinitionId(int methodDefinitionId) {
        this.methodDefinitionId = methodDefinitionId;
    }
}
