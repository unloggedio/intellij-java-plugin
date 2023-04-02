package com.insidious.plugin.ui.gutter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.ui.Components.AgentResponseComponent;
import com.insidious.plugin.ui.Components.NavigationElement;
import com.insidious.plugin.ui.UIUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    private JPanel ResponseRendererPanel;
    private JPanel borderParent;
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

//        executeButton.addActionListener(e -> {
//            tryTestDiff();
//        });
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
        if(mostRecentTestCandidate!=null) {
            String returnvalue = new String(
                    ((MethodCallExpression) mostRecentTestCandidate.getMainMethod()).getReturnDataEvent()
                            .getSerializedValue());
            addResponse(mostRecentTestCandidate,String.valueOf(agentCommandResponse.getMethodReturnValue()));
        }
        else
        {
            addResponse(null,String.valueOf(agentCommandResponse.getMethodReturnValue()));
        }
    }

    public JComponent getContent() {
        return rootContent;
    }

    public void addResponse(TestCandidateMetadata mostRecentTestCandidate, String returnvalue) {
        this.borderParent.removeAll();
        AgentResponseComponent response = new AgentResponseComponent(mostRecentTestCandidate, returnvalue);
        this.borderParent.add(response.getComponenet(), BorderLayout.CENTER);
        this.borderParent.revalidate();
    }

    //temporary function to test mock differneces between responses (json)
    public void tryTestDiff()
    {
        ObjectMapper om = new ObjectMapper();
        try {
            addResponse(null,null);
        } catch (Exception e) {
            System.out.println("TestDiff Exception: "+e);
            e.printStackTrace();
        }
    }
}
