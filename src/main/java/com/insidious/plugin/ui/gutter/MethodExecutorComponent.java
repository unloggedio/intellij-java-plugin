package com.insidious.plugin.ui.gutter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.ui.Components.AgentResponseComponent;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import jdk.jfr.internal.JVM;

import javax.swing.*;
import java.awt.*;
import java.util.*;
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
    private JPanel borderParent;
    private JPanel topParent;
    private JPanel responseRenderSection;
    private JPanel ResponseRendererPanel;
    private JPanel topAligner;
    private List<TestCandidateMetadata> methodTestCandidates;
    private ScrollablePanel scrollablePanel;

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
//            TestCandidateMetadata mostRecentTestCandidate = methodTestCandidates.get(methodTestCandidates.size() - 1);
//            List<String> methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
//                    mostRecentTestCandidate);
//            execute(mostRecentTestCandidate, methodArgumentValues);
            clearResponsePanel();
            JvmParameter[] parameters=null;
            if(methodElement!=null)
            {
                parameters= methodElement.getParameters();
            }
            for(TestCandidateMetadata candidateMetadata : methodTestCandidates)
            {
                List<String> methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
                    candidateMetadata);
                logger.info("[EXEC SENDING REQUEST FOR IP] "+methodArgumentValues.toString());

                Map<String,String> parameterInputMap = new TreeMap<>();
                if(parameters!=null)
                {
                    for (int i = 0; i < parameters.length; i++) {
                        JvmParameter methodParameter = parameters[i];
                        String parameterValue = methodArgumentValues == null ? "" : methodArgumentValues.get(i);
                        parameterInputMap.put(methodParameter.getName(),parameterValue);
                    }

                }
                execute(candidateMetadata, methodArgumentValues, parameterInputMap);
            }
        });

//        executeButton.addActionListener(e -> {
//            List<String> methodArgumentValues = new ArrayList<>();
//            for (ParameterInputComponent parameterInputComponent : parameterInputComponents) {
//                methodArgumentValues.add(parameterInputComponent.getParameterValue());
//            }
//            execute(null, methodArgumentValues);
//        });

//        executeAndShowDifferencesButton.addActionListener(e -> {
//            clearResponsePanel();
//            tryTestDiff();
//        });

        setupScrollablePanel();
    }

    public void clearResponsePanel()
    {
        this.scrollablePanel.removeAll();
        this.scrollablePanel.revalidate();
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

    public void execute(TestCandidateMetadata mostRecentTestCandidate, List<String> methodArgumentValues, Map<String,String> parameters) {
        insidiousService.reExecuteMethodInRunningProcess(methodElement, methodArgumentValues,
                (agentCommandRequest, agentCommandResponse) -> {
                    logger.warn("Agent command execution response: " + agentCommandResponse);
                    renderResponse(mostRecentTestCandidate, agentCommandResponse, parameters);
                });
    }

    private void renderResponse(TestCandidateMetadata mostRecentTestCandidate, AgentCommandResponse agentCommandResponse,
                                Map<String,String> parameters) {
        // render differences table
        // append to output panel
        if(mostRecentTestCandidate!=null) {
            String returnvalue = new String(
                    ((MethodCallExpression) mostRecentTestCandidate.getMainMethod()).getReturnDataEvent()
                            .getSerializedValue());
            addResponse(returnvalue,String.valueOf(agentCommandResponse.getMethodReturnValue()),parameters);
        }
        else
        {
            addResponse(null,String.valueOf(agentCommandResponse.getMethodReturnValue()),parameters);
        }
    }

    public JComponent getContent() {
        return rootContent;
    }

    public void addResponse(String candidateValue, String returnvalue, Map<String,String> parameters) {
        AgentResponseComponent response = new AgentResponseComponent(candidateValue, returnvalue, this.insidiousService, parameters);
        scrollablePanel.add(response.getComponenet(),0);
        scrollablePanel.revalidate();
    }

    //temporary function to test mock differneces between responses (json)
    public void tryTestDiff()
    {
        try {
            addResponse(null,null,null);
        } catch (Exception e) {
            System.out.println("TestDiff Exception: "+e);
            e.printStackTrace();
        }
    }

    public void setupScrollablePanel()
    {
        scrollablePanel = new ScrollablePanel();
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
        JBScrollPane scrollPane = new JBScrollPane();
        scrollPane.setViewportView(scrollablePanel);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);

        this.borderParent.removeAll();
        borderParent.add(scrollPane, BorderLayout.CENTER);
        this.borderParent.revalidate();
    }
}
