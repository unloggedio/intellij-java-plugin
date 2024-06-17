package com.insidious.plugin.util;

import com.insidious.plugin.client.SessionInstanceInterface;
import com.insidious.plugin.client.UnloggedClientInterface;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.ui.stomp.TestCandidateBareBone;
import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCaseUtils {

    private static final Logger logger = LoggerUtil.getInstance(TestCaseUtils.class);

    public static void generateAllTestCandidateCases(InsidiousService insidiousService, UnloggedClientInterface unloggedClientInterface) throws Exception {
        SessionInstanceInterface sessionInstance = unloggedClientInterface.getSessionInstance();
        TestCaseService testCaseService = new TestCaseService(sessionInstance, insidiousService.getProject());

        TestCaseGenerationConfiguration generationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUnit5, MockFramework.Mockito, JsonFramework.Gson, ResourceEmbedMode.IN_FILE
        );

        sessionInstance.getTestCandidates(testCandidateMetadata -> {

            for (TestCandidateBareBone testCandidateBareBone : testCandidateMetadata) {
                TestCandidateMetadata testCandidateMetadatum = null;
                try {
                    testCandidateMetadatum = insidiousService.getTestCandidateById(testCandidateBareBone.getId(), true);
                    TestCaseUnit testCaseUnit;
                    Parameter testSubject = testCandidateMetadatum.getTestSubject();
                    if (testSubject.isException()) {
                        return;
                    }
                    MethodCallExpression callExpression = testCandidateMetadatum.getMainMethod();
                    logger.warn(
                            "Generating test case: " + testSubject.getType() + "." + callExpression.getMethodName() + "()");
                    generationConfiguration.getTestCandidateMetadataList().clear();
                    generationConfiguration.getTestCandidateMetadataList().add(testCandidateMetadatum);

                    generationConfiguration.getCallExpressionList().clear();
                    generationConfiguration.getCallExpressionList().addAll(testCandidateMetadatum.getCallsList());

                    testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
                    List<TestCaseUnit> testCaseUnit1 = new ArrayList<>();
                    testCaseUnit1.add(testCaseUnit);
                    TestSuite testSuite = new TestSuite(testCaseUnit1);
                    insidiousService.getJUnitTestCaseWriter().saveTestSuite(testSuite);
                } catch (Exception e) {
                    logger.error("Failed to generate test case for [" + testCandidateMetadatum + "]", e);
                }

            }


        }, 0, new StompFilterModel(), new AtomicInteger(1));
    }

}
