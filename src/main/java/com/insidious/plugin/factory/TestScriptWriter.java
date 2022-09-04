package com.insidious.plugin.factory;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.expression.Expression;
import com.insidious.plugin.factory.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.expression.StringExpression;
import com.insidious.plugin.factory.writer.PendingStatement;
import com.insidious.plugin.factory.writer.TestCaseWriter;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.HashMap;
import java.util.Map;

public class TestScriptWriter {

    private static final Logger logger = LoggerUtil.getInstance(TestScriptWriter.class);

    public static void
    generateTestScriptFromTestMetadataSet(
            TestCandidateMetadata testCandidateMetadata,
            ObjectRoutine objectRoutine) {

        if (testCandidateMetadata.getMainMethod() instanceof MethodCallExpression) {


            MethodCallExpression mainMethod = (MethodCallExpression) testCandidateMetadata.getMainMethod();

            objectRoutine.addComment("");
            Parameter mainMethodReturnValue = mainMethod.getReturnValue();


            DataEventWithSessionId mainMethodReturnValueProbe = mainMethodReturnValue.getProb();
            if (mainMethodReturnValueProbe == null) {
                return;
            }
            objectRoutine.addComment("Test candidate method [" + mainMethod.getMethodName() + "] " +
                    "[ " + mainMethodReturnValueProbe.getNanoTime() + "] - took " +
                    Long.valueOf(testCandidateMetadata.getCallTimeNanoSecond() / (1000000)).intValue() + "ms");

            VariableContainer arguments = mainMethod.getArguments();
            if (arguments.count() > 0) {


                objectRoutine.addComment("");
                for (Parameter parameter : arguments.all()) {
                    if (parameter.getName() == null && parameter.getProb().getSerializedValue().length > 0) {
                        String serializedValue = new String(parameter.getProb().getSerializedValue());
                        if (parameter.getType().equals("java.lang.String")) {
                            serializedValue = '"' + serializedValue + '"';
                        }
                        parameter.setValue(serializedValue);
                    }
                    objectRoutine.addParameterComment(parameter);
                }
                objectRoutine.addComment("");
                objectRoutine.addComment("");

                for (Parameter parameter : arguments.all()) {
//                    if (!objectRoutine.getCreatedVariables().contains(parameter.getName())) {
                        in(objectRoutine).assignVariable(parameter).fromRecordedValue().endStatement();
//                    }
                }

            }


            Map<String, MethodCallExpression> mockedCalls = new HashMap<>();
            if (testCandidateMetadata.getCallsList().size() > 0) {

                objectRoutine.addComment("");
                for (MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {
                    if (methodCallExpression.getException() != null &&
                            mockedCalls.containsKey(methodCallExpression.getMethodName())) {
                        continue;
                    }
                    TestCaseWriter.createMethodCallComment(objectRoutine, methodCallExpression);
                    TestCaseWriter.createMethodCallMock(objectRoutine, methodCallExpression);
                    mockedCalls.put(methodCallExpression.getMethodName(), methodCallExpression);
                }
                objectRoutine.addComment("");
                objectRoutine.addComment("");
            }
//
            for (TestCandidateMetadata metadatum : objectRoutine.getMetadata()) {
                Expression mainMethod1 = metadatum.getMainMethod();
                if (mainMethod1 instanceof MethodCallExpression) {
                    MethodCallExpression mainMethodExpression = (MethodCallExpression) mainMethod1;
                    for (Parameter parameter : mainMethodExpression.getArguments().all()) {
//                objectRoutine.getCreatedVariables().add(parameter);
                        if (!objectRoutine.getCreatedVariables().contains(parameter.getName())) {
                            in(objectRoutine).assignVariable(parameter).fromRecordedValue().endStatement();
                        }
                    }
                }
            }
//

            //////////////////////// FUNCTION CALL ////////////////////////

            // return type == V ==> void return type => no return value
            in(objectRoutine).assignVariable(mainMethodReturnValue).writeExpression(mainMethod).endStatement();


            if (mainMethod.getMethodName().equals("<init>")) {
                // there is no verification required (?) after calling constructors
                return;
            }
            String returnSubjectInstanceName = mainMethodReturnValue.getName();


            //////////////////////////////////////////////// VERIFICATION ////////////////////////////////////////////////


            // deserialize and compare objects
            byte[] serializedBytes = mainMethodReturnValueProbe.getSerializedValue();


            Parameter returnSubjectExpectedJsonString = null;
            if (serializedBytes.length > 0) {
                returnSubjectExpectedJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "ExpectedJson");
                in(objectRoutine)
                        .assignVariable(returnSubjectExpectedJsonString)
                        .writeExpression(new StringExpression(new String(serializedBytes)))
                        .endStatement();
            } else {
                returnSubjectExpectedJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "ExpectedJson");

                in(objectRoutine)
                        .assignVariable(returnSubjectExpectedJsonString)
                        .writeExpression(new StringExpression(new String(serializedBytes)))
                        .endStatement();
            }


            // reconstruct object from the serialized form to an object instance in the
            // test method to compare it with the new object, or do it the other way
            // round ? Maybe serializing the object and then comparing the serialized
            // string forms would be more readable ? string comparison would fail if the
            // serialization has fields serialized in random order
            Parameter returnSubjectJsonString = ParameterFactory.createStringByName(returnSubjectInstanceName + "Json");
            in(objectRoutine)
                    .assignVariable(returnSubjectJsonString)
                    .writeExpression(MethodCallExpressionFactory.ToJson(mainMethodReturnValue))
                    .endStatement();


            in(objectRoutine)
                    .writeExpression(MethodCallExpressionFactory
                            .MockitoAssert(returnSubjectJsonString, returnSubjectExpectedJsonString))
                    .endStatement();

        } else {
            Expression plainValueExpression = testCandidateMetadata.getMainMethod();
            logger.warn("PVE: " + plainValueExpression);
//            in(objectRoutine)
//                    .assignVariable()
//                    .writeExpression(MethodCallExpressionFactory
//                            .MockitoAssert(returnSubjectJsonString, returnSubjectExpectedJsonString))
//                    .endStatement();
        }
        objectRoutine.addComment("");

    }

    public static PendingStatement in(ObjectRoutine objectRoutine) {
        return new PendingStatement(objectRoutine);
    }

}
