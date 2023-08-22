package com.insidious.plugin.util;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TestCaseUtils {

    private static final Logger logger = LoggerUtil.getInstance(TestCaseUtils.class);

    public static void generateAllTestCandidateCases(InsidiousService insidiousService) throws Exception {
        SessionInstance sessionInstance = insidiousService.getClient().getSessionInstance();
        TestCaseService testCaseService = new TestCaseService(sessionInstance);

        TestCaseGenerationConfiguration generationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUnit5, MockFramework.Mockito, JsonFramework.Gson, ResourceEmbedMode.IN_FILE
        );

        sessionInstance.getAllTestCandidates(testCandidateMetadata -> {
            @NotNull TestCaseUnit testCaseUnit;
            try {
                Parameter testSubject = testCandidateMetadata.getTestSubject();
                if (testSubject.isException()) {
                    return;
                }
                MethodCallExpression callExpression = testCandidateMetadata.getMainMethod();
                logger.warn(
                        "Generating test case: " + testSubject.getType() + "." + callExpression.getMethodName() + "()");
                generationConfiguration.getTestCandidateMetadataList().clear();
                generationConfiguration.getTestCandidateMetadataList().add(testCandidateMetadata);

                generationConfiguration.getCallExpressionList().clear();
                generationConfiguration.getCallExpressionList().addAll(testCandidateMetadata.getCallsList());

                testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
                List<TestCaseUnit> testCaseUnit1 = new ArrayList<>();
                testCaseUnit1.add(testCaseUnit);
                TestSuite testSuite = new TestSuite(testCaseUnit1);
                insidiousService.getJUnitTestCaseWriter().saveTestSuite(testSuite);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }

}
