package com.insidious.plugin.ui.gutter;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MethodExecutorComponent {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private PsiMethod methodElement;
    private JPanel rootContent;
    private JButton executeAndShowDifferencesButton;
    private JPanel executeFromRecordedPanel;
    private JPanel methodParameterContainer;
    private JPanel executeUsingNewParameterPanel;
    private JButton executeButton;
    private JLabel candidateCountLabel;
    private List<TestCandidateMetadata> methodTestCandidates;

    public MethodExecutorComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        executeAndShowDifferencesButton.addActionListener(e -> {
            if (methodTestCandidates.size() == 0) {
                InsidiousNotification.notifyMessage(
                        "Please use the agent to record values for replay. No candidates found for " + methodElement.getName(),
                        NotificationType.WARNING
                );
                return;
            }
            TestCandidateMetadata mostRecentTestCandidate = methodTestCandidates.get(methodTestCandidates.size() - 1);
            List<String> methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
                    mostRecentTestCandidate);
            execute(mostRecentTestCandidate, methodArgumentValues);
        });

        executeButton.addActionListener(e -> {
            List<String> methodArgumentValues = new ArrayList<>();
            for (ParameterInputComponent parameterInputComponent : parameterInputComponents) {
                methodArgumentValues.add(parameterInputComponent.getParameterValue());
            }
            execute(null, methodArgumentValues);
        });
    }

    public void refresh() {
        if (methodElement == null) {
            return;
        }
        ApplicationManager.getApplication().runReadAction(() -> renderForMethod(methodElement));
    }

    public void renderForMethod(PsiMethod methodElement) {
        logger.warn("render method executor for: " + methodElement.getName());
        this.methodElement = methodElement;
        JvmParameter[] methodParameters = methodElement.getParameters();

        methodTestCandidates = this.insidiousService
                .getSessionInstance()
                .getTestCandidatesForAllMethod(methodElement.getContainingClass().getQualifiedName(),
                        methodElement.getName(),
                        false);


        candidateCountLabel.setText(methodTestCandidates.size() + " test candidates for " + methodElement.getName());
        TestCandidateMetadata mostRecentTestCandidate = null;
        List<String> methodArgumentValues = null;
        if (methodTestCandidates.size() > 0) {
            mostRecentTestCandidate = methodTestCandidates.get(methodTestCandidates.size() - 1);
            methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
                    mostRecentTestCandidate);
        }

        methodParameterContainer.removeAll();
        parameterInputComponents.clear();

        if (methodParameters.length > 0) {
            methodParameterContainer.setLayout(new GridLayout(0, 1));
            for (int i = 0; i < methodParameters.length; i++) {
                JvmParameter methodParameter = methodParameters[i];
                String parameterValue = methodArgumentValues == null ? "" : methodArgumentValues.get(i);
                ParameterInputComponent parameterContainer = new ParameterInputComponent(methodParameter,
                        parameterValue);
                parameterInputComponents.add(parameterContainer);
                methodParameterContainer.add(parameterContainer.getContent());
            }
        } else {
            JBLabel noParametersLabel = new JBLabel("Method has no parameters");
            methodParameterContainer.add(noParametersLabel);
        }

    }

    public void execute(TestCandidateMetadata mostRecentTestCandidate, List<String> methodArgumentValues) {
        insidiousService.reExecuteMethodInRunningProcess(methodElement, methodArgumentValues,
                (agentCommandRequest, agentCommandResponse) -> {
                    logger.warn("Agent command execution response: " + agentCommandResponse);
                    renderResponse(mostRecentTestCandidate, agentCommandResponse);
                });
    }

    private void renderResponse(TestCandidateMetadata mostRecentTestCandidate, AgentCommandResponse agentCommandResponse) {
        // render differences table
        // append to output panel
        String returnvalue = new String(
                ((MethodCallExpression) mostRecentTestCandidate.getMainMethod()).getReturnDataEvent()
                        .getSerializedValue());
    }

    public JComponent getContent() {
        return rootContent;
    }
}
