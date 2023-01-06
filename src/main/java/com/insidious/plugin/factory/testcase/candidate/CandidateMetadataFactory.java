package com.insidious.plugin.factory.testcase.candidate;

import com.insidious.plugin.client.ParameterNameFactory;
import com.insidious.plugin.factory.testcase.TestGenerationState;
import com.insidious.plugin.factory.testcase.expression.MethodCallExpressionFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.factory.testcase.writer.ObjectRoutineScript;
import com.insidious.plugin.factory.testcase.writer.PendingStatement;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CandidateMetadataFactory {
    public final static Logger logger = LoggerUtil.getInstance(TestCandidateMetadata.class);

    public static ObjectRoutineScript callMocksToObjectScript(
            TestGenerationState testGenerationState,
            TestCaseGenerationConfiguration testConfiguration,
            List<MethodCallExpression> callsList,
            VariableContainer fields) {
        ParameterNameFactory nameFactory = testGenerationState.getParameterNameFactory();
        ObjectRoutineScript objectRoutineScript = new ObjectRoutineScript(testGenerationState.getVariableContainer(),
                testConfiguration, testGenerationState);

        Parameter testTarget = testConfiguration.getTestCandidateMetadataList()
                .get(0)
                .getTestSubject();


        Collection<MethodCallExpression> callToMock = new ArrayList<>();
        Set<MethodCallExpression> staticCallsList = new HashSet<>();

        Map<String, Boolean> mockedStaticTypes = new HashMap<>();

        for (MethodCallExpression e : callsList) {
            if (!testConfiguration.getCallExpressionList()
                    .contains(e)) {
                logger.warn("Skip unselected call expression to be mocked - " + e);
                continue;
            }
            if (e.getReturnValue() == null) {
                logger.info("MCE to mock without a return value - " + e);
                continue;
            }
            if (e.getMethodName().equals("toString")) {
                continue;
            }
            if (e.isStaticCall() && e.getUsesFields()) {
                // all static calls need to be mocked
                // even if they have no return value

                if (e.getSubject()
                        .getType()
                        .equals(testTarget.getType())) {
                    // we do not want to mock static calls on the class itself being tested
                    // since we most likely have injected all the fields anyways
                    continue;
                }
                staticCallsList.add(e);
                mockedStaticTypes.put(e.getSubject()
                        .getType(), true);
                continue;
            }
            if (e.getSubject()
                    .getType()
                    .startsWith("com.google.")) {
                continue;
            }
            if (!e.isMethodPublic() && !e.isMethodProtected()) {
                continue;
            }
            if (e.getMethodName()
                    .startsWith("<")) {
                // constructors need not be mocked
                continue;
            }

            if (e.getSubject() == null) {
                // not a static call, but we failed to identify subject
                // this is potentially a bug, and the fix is inside scan implementation
                continue;
            }
            if (e.getReturnValue()
                    .getType() == null
                    || e.getReturnValue()
                    .getType()
                    .equals("V")
                    || e.getReturnValue()
                    .getProb() == null) {
                // either the function has no return value (need not be mocked) or
                // we failed to identify the return value in the scan, in that case this is a bug
                continue;
            }
            if (e.getSubject()
                    .getType()
                    .contains("com.google")) {
                // this is hard coded to skip mocking Gson class
                continue;
            }

            if (fields.getParametersById(e.getSubject()
                    .getProb()
                    .getValue()) == null) {
                // the subject should ideally be one of the already identified fields.
                continue;
            }

            // finally add this call in the list of calls that will be actually mocked
            callToMock.add(e);

        }

        if (callToMock.size() > 0) {

            Map<String, List<MethodCallExpression>> grouped = callToMock.stream()
                    .collect(Collectors.groupingBy(e -> e.getSubject()
                                    .getValue() + e.getMethodName() + buildCallSignature(e),
                            Collectors.toList()));
            for (Map.Entry<String, List<MethodCallExpression>> stringListEntry : grouped.entrySet()) {
                List<MethodCallExpression> callsOnSubject = stringListEntry.getValue();
                callsOnSubject.sort((e1, e2) ->
                        Math.toIntExact(e1.getEntryProbe()
                                .getNanoTime() - e2.getEntryProbe()
                                .getNanoTime()));
                Parameter subject = callsOnSubject
                        .get(0)
                        .getSubject();
                boolean firstCall = true;
                PendingStatement pendingStatement = null;
                Parameter previousReturnValue = null;

                MethodCallExpression firstCallExpression = callsOnSubject.get(0);
                firstCallExpression.writeCallArguments(
                        objectRoutineScript, testConfiguration, testGenerationState);

                objectRoutineScript.addComment("");

                for (MethodCallExpression methodCallExpression : callsOnSubject) {
                    Parameter newReturnValue = methodCallExpression.getReturnValue();
                    if (previousReturnValue != null && newReturnValue.getValue() == previousReturnValue.getValue()) {
                        continue;
                    }
                    previousReturnValue = newReturnValue;
                    nameFactory.getNameForUse(previousReturnValue, methodCallExpression.getMethodName());

                    String returnTypeFromProbe = ClassTypeUtils.getDottedClassName(previousReturnValue.getProbeInfo()
                            .getAttribute("Type", previousReturnValue.getType()));
                    if (previousReturnValue.isPrimitiveType() && returnTypeFromProbe != null && !returnTypeFromProbe.equals("java.lang.Object")) {
                        previousReturnValue.setTypeForced(returnTypeFromProbe);
                    }

                    if (firstCall) {
                        methodCallExpression.writeCommentTo(objectRoutineScript);
                        methodCallExpression.writeReturnValue(
                                objectRoutineScript, testConfiguration, testGenerationState);
                        pendingStatement = PendingStatement.in(objectRoutineScript, testGenerationState)
                                .writeExpression(
                                        MethodCallExpressionFactory.MockitoWhen(methodCallExpression,
                                                testConfiguration, testGenerationState));
                    }
                    firstCall = false;


                    Parameter returnValue = methodCallExpression.getReturnValue();
                    if (returnValue.getException()) {
                        pendingStatement.writeExpression(MethodCallExpressionFactory.MockitoThenThrow(returnValue));

                    } else {
                        pendingStatement.writeExpression(MethodCallExpressionFactory.MockitoThen(returnValue));
                    }

                }
                if (pendingStatement != null) {
                    pendingStatement.endStatement();
                }
                objectRoutineScript.addComment("");


            }


        }


        if (staticCallsList.size() > 0) {


            Map<String, List<MethodCallExpression>> grouped = staticCallsList.stream()
                    .collect(Collectors.groupingBy(e -> e.getSubject()
                                    .getValue() + e.getMethodName() + buildCallSignature(e),
                            Collectors.toList()));
            for (Map.Entry<String, List<MethodCallExpression>> stringListEntry : grouped.entrySet()) {
                List<MethodCallExpression> callsOnSubject = stringListEntry.getValue();
                callsOnSubject.sort((e1, e2) ->
                        Math.toIntExact(e1.getEntryProbe()
                                .getNanoTime() - e2.getEntryProbe()
                                .getNanoTime()));

                boolean firstCall = true;
                PendingStatement pendingStatement = null;
                Parameter previousReturnValue = null;

                MethodCallExpression firstCallExpression = callsOnSubject.get(0);
                firstCallExpression.writeCallArguments(
                        objectRoutineScript, testConfiguration, testGenerationState);

                objectRoutineScript.addComment("");
                Parameter staticCallSubjectMockInstance = callsOnSubject.get(0)
                        .getSubject();

                if (!objectRoutineScript.getCreatedVariables()
                        .contains( nameFactory.getNameForUse( staticCallSubjectMockInstance, null))) {
                    @NotNull Parameter subjectStaticFieldMock = Parameter.cloneParameter(
                            staticCallSubjectMockInstance);

                    subjectStaticFieldMock.setContainer(true);
                    Parameter childParameter = new Parameter();
                    childParameter.setType(staticCallSubjectMockInstance.getType());
                    subjectStaticFieldMock.setType("org.mockito.MockedStatic");
                    childParameter.setName("E");
                    subjectStaticFieldMock.getTemplateMap()
                            .add(childParameter);

                    objectRoutineScript.addStaticMock(subjectStaticFieldMock);
                }


                for (MethodCallExpression methodCallExpression : callsOnSubject) {
                    Parameter newReturnValue = methodCallExpression.getReturnValue();
                    if (previousReturnValue != null && newReturnValue.getValue() == previousReturnValue.getValue()) {
                        continue;
                    }
                    previousReturnValue = newReturnValue;
                    nameFactory.getNameForUse(previousReturnValue, methodCallExpression.getMethodName());

                    String returnTypeFromProbe = ClassTypeUtils.getDottedClassName(previousReturnValue.getProbeInfo()
                            .getAttribute("Type", previousReturnValue.getType()));
                    if (previousReturnValue.isPrimitiveType() && returnTypeFromProbe != null && !returnTypeFromProbe.equals("java.lang.Object")) {
                        previousReturnValue.setTypeForced(returnTypeFromProbe);
                    }

                    if (firstCall) {
                        methodCallExpression.writeCommentTo(objectRoutineScript);
                        methodCallExpression.writeReturnValue(
                                objectRoutineScript, testConfiguration, testGenerationState);
                        pendingStatement = PendingStatement.in(objectRoutineScript, testGenerationState)
                                .writeExpression(
                                        MethodCallExpressionFactory.MockitoWhen(methodCallExpression,
                                                testConfiguration, testGenerationState));
                    }
                    firstCall = false;


                    Parameter returnValue = methodCallExpression.getReturnValue();
                    if (returnValue.getException()) {
                        pendingStatement.writeExpression(MethodCallExpressionFactory.MockitoThenThrow(returnValue));

                    } else {
                        pendingStatement.writeExpression(MethodCallExpressionFactory.MockitoThen(returnValue));
                    }

                }
                if (pendingStatement != null) {
                    pendingStatement.endStatement();
                }
                objectRoutineScript.addComment("");


            }
        }
        return objectRoutineScript;

    }

    public static ObjectRoutineScript mainMethodToObjectScript(
            TestCandidateMetadata testCandidateMetadata,
            TestGenerationState testGenerationState,
            TestCaseGenerationConfiguration testConfiguration
    ) {
        ObjectRoutineScript objectRoutineScript = new ObjectRoutineScript(testGenerationState.getVariableContainer(),
                testConfiguration, testGenerationState);

        if (testCandidateMetadata.getMainMethod() instanceof MethodCallExpression) {

            MethodCallExpression mainMethod = (MethodCallExpression) testCandidateMetadata.getMainMethod();

            if (mainMethod.getMethodName()
                    .equals("<init>")) {
                objectRoutineScript.getCreatedVariables()
                        .add(mainMethod.getSubject());
                mainMethod.setReturnValue(mainMethod.getSubject());
            }

            if (mainMethod.isMethodPublic() && mainMethod.getReturnValue() != null) {
                mainMethod.writeTo(objectRoutineScript, testConfiguration, testGenerationState);
            }


        } else {
            testCandidateMetadata.getMainMethod()
                    .writeTo(objectRoutineScript, testConfiguration, testGenerationState);
        }
        return objectRoutineScript;

    }


    @NotNull
    private static String buildCallSignature(MethodCallExpression methodCallExpression) {
        Parameter subject = methodCallExpression.getSubject();

        StringBuilder callBuilder = new StringBuilder();
        callBuilder.append(subject.getValue())
                .append(".")
                .append(methodCallExpression.getMethodName());

        for (Parameter parameter : methodCallExpression.getArguments()) {
            if (parameter.getProb() != null && parameter.getProb()
                    .getSerializedValue().length > 0) {
                callBuilder.append(new String(parameter.getProb()
                        .getSerializedValue()));
            } else {
                callBuilder.append(parameter.getValue());
            }
        }
        return callBuilder.toString();
    }
}
