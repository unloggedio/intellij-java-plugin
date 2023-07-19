package com.insidious.plugin.assertions;

import com.insidious.plugin.client.ParameterNameFactory;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.expression.Expression;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TestAssertion implements Expression {

    String id = UUID.randomUUID().toString();
    AssertionType assertionType;
    Parameter expectedValue;
    Parameter actualValue;

    public TestAssertion() {
    }

    public TestAssertion(AssertionType assertionType, Parameter expectedValue, Parameter actualValue) {
        this.assertionType = assertionType;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AssertionType getAssertionType() {
        return assertionType;
    }

    public void setAssertionType(AssertionType assertionType) {
        this.assertionType = assertionType;
    }

    public Parameter getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(Parameter expectedValue) {
        this.expectedValue = expectedValue;
    }

    public Parameter getActualValue() {
        return actualValue;
    }

    public void setActualValue(Parameter actualValue) {
        this.actualValue = actualValue;
    }

    @Override
    public void writeTo(
            ObjectRoutineScript objectRoutineScript,
            TestCaseGenerationConfiguration testConfiguration,
            TestGenerationState testGenerationState) {
        if (actualValue == null) {
            return;
        }

        // deserialize and compare objects
        Parameter mainMethodReturnValue = actualValue;
        ParameterNameFactory nameFactory = testGenerationState.getParameterNameFactory();
        String returnSubjectInstanceName = nameFactory.getNameForUse(mainMethodReturnValue, null);
        DataEventWithSessionId expectedValueProbe = expectedValue.getProb();
        byte[] serializedBytes = expectedValueProbe.getSerializedValue();


        String expectedParameterName = returnSubjectInstanceName + "Expected";
        testGenerationState.getParameterNameFactory().setNameForParameter(expectedValue, expectedParameterName);


        if (testConfiguration.getResourceEmbedMode().equals(ResourceEmbedMode.IN_CODE)
                || expectedValue.isPrimitiveType()) {
            if (expectedValue.isPrimitiveType()) {
                PendingStatement.in(objectRoutineScript, testGenerationState)
                        .assignVariable(expectedValue)
                        .fromRecordedValue(testConfiguration)
                        .endStatement();

            } else {
                if (serializedBytes.length > 0) {
                    PendingStatement.in(objectRoutineScript, testGenerationState)
                            .assignVariable(expectedValue)
                            .writeExpression(MethodCallExpressionFactory.StringExpression(new String(serializedBytes)))
                            .endStatement();
                } else {
                    PendingStatement.in(objectRoutineScript, testGenerationState)
                            .assignVariable(expectedValue)
                            .writeExpression(MethodCallExpressionFactory.NullExpression())
                            .endStatement();

                }
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

            if (expectedValue.getIsEnum()) {
                PendingStatement.in(objectRoutineScript, testGenerationState)
                        .assignVariable(expectedValue)
                        .writeExpression(MethodCallExpressionFactory.createEnumExpression(expectedValue))
                        .endStatement();
            } else {
                PendingStatement.in(objectRoutineScript, testGenerationState)
                        .assignVariable(expectedValue)
                        .writeExpression(jsonFromFileCall)
                        .endStatement();
            }
        }

        expectedValue.setValue(-1L);


        // If the type of the returnSubjectExpectedObject is a array (int[], long[], byte[])
        // then use assertArrayEquals
        if (expectedValue.getType().endsWith("[]")) {
            PendingStatement
                    .in(objectRoutineScript, testGenerationState)
                    .writeExpression(
                            MethodCallExpressionFactory.MockitoAssertArrayEquals
                                    (expectedValue, mainMethodReturnValue, testConfiguration))
                    .endStatement();
        } else {
            PendingStatement.in(objectRoutineScript, testGenerationState)
                    .writeExpression(MethodCallExpressionFactory
                            .MockitoAssertEquals(expectedValue, mainMethodReturnValue, testConfiguration))
                    .endStatement();
        }

    }
}
