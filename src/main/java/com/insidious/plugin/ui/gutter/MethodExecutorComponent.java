package com.insidious.plugin.ui.gutter;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MethodExecutorComponent {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;
    private PsiMethod methodElement;
    private JPanel rootContent;
    private JButton executeAndShowDifferencesButton;
    private JPanel executeFromRecordedPanel;
    private JPanel methodParameterContainer;
    private JPanel executeUsingNewParameterPanel;
    private JButton executeButton;
    private JLabel candidateCountLabel;

    public MethodExecutorComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        executeButton.addActionListener(e -> logger.warn("Execute button clicked"));
    }

    public void renderForMethod(PsiMethod methodElement) {
        this.methodElement = methodElement;
        JvmParameter[] methodParameters = methodElement.getParameters();

        List<TestCandidateMetadata> methodTestCandidates = this.insidiousService.getSessionInstance()
                .getTestCandidatesForAllMethod(
                        methodElement.getContainingClass().getName(), methodElement.getName(), false
                );

        methodParameterContainer.removeAll();

        if (methodParameters.length > 0) {
            methodParameterContainer.setLayout(new GridLayout(0, 1));
            for (int i = 0; i < methodParameters.length; i++) {
                JvmParameter methodParameter = methodParameters[i];
                ParameterInputComponent parameterContainer = new ParameterInputComponent(methodParameter);
                methodParameterContainer.add(parameterContainer.getContent());
            }
        } else {
            JBLabel noParametersLabel = new JBLabel("Method has no parameters");
            methodParameterContainer.add(noParametersLabel);
        }

    }

    public JComponent getContent() {
        return rootContent;
    }
}
