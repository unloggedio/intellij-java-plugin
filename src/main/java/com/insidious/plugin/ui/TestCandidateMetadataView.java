package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.TestSuite;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TestCandidateMetadataView {
    private final TestCandidateMetadata testCandidateMetadata;
    private final TestCaseService testCaseService;
    private final InsidiousService insidiousService;
    private JPanel contentPanel;
    private JLabel testCandidateName;
    private JButton generateTestCaseButton;

    public TestCandidateMetadataView(
            TestCandidateMetadata testCandidateMetadata,
            TestCaseService testCaseService,
            InsidiousService insidiousService
    ) {
        this.testCandidateMetadata = testCandidateMetadata;
        this.testCaseService = testCaseService;
        this.insidiousService = insidiousService;
        MethodCallExpression mainMethod = (MethodCallExpression) testCandidateMetadata.getMainMethod();
        testCandidateName.setText(mainMethod.getMethodName() + " at " + mainMethod.getEntryProbe().getNanoTime());
        generateTestCaseButton.addActionListener(e -> generateTestCase());
    }

    private void generateTestCase() {
        @NotNull TestCaseUnit testCaseUnit = testCaseService.getTestCaseUnit(testCandidateMetadata);
        TestSuite testSuite = new TestSuite(List.of(testCaseUnit));
        insidiousService.saveTestSuite(testSuite);
    }

    public Component getContentPanel() {
        return contentPanel;
    }
}
