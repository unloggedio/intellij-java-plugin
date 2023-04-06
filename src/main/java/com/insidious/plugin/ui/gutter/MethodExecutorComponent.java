package com.insidious.plugin.ui.gutter;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.Components.AgentResponseComponent;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MethodExecutorComponent {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private final boolean MOCK_MODE = false;
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
    private int componentCounter = 0;
    private int mockCallCount = 1;
    private boolean alt = true;
    private int callCount = 0;
    private boolean isDifferent=false;
    public MethodExecutorComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        executeAndShowDifferencesButton.addActionListener(e -> {
            executeAll(methodElement);
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
//            for(int i=0;i<mockCallCount;i++)
//            {
//                tryTestDiff();
//            }
//        });

        setupScrollablePanel();
    }

    public void executeAll(PsiMethod method)
    {
        this.isDifferent=false;
        clearResponsePanel();
        if (MOCK_MODE) {
            callCount=mockCallCount;
            for (int i = 0; i < mockCallCount; i++) {
                tryTestDiff();
            }
            return;
        }
        if (methodTestCandidates.size() == 0) {
            InsidiousNotification.notifyMessage(
                    "Please use the agent to record values for replay. No candidates found for " + methodElement.getName(),
                    NotificationType.WARNING
            );
            return;
        }
        callCount=methodTestCandidates.size();
        JvmParameter[] parameters = null;
        if (method != null) {
            parameters = method.getParameters();
        }
        for (TestCandidateMetadata candidateMetadata : methodTestCandidates) {
            List<String> methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
                    candidateMetadata);
            logger.info("[EXEC SENDING REQUEST FOR IP] " + methodArgumentValues.toString());

            Map<String, String> parameterInputMap = new TreeMap<>();
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    JvmParameter methodParameter = parameters[i];
                    String parameterValue = methodArgumentValues == null ? "" : methodArgumentValues.get(i);
                    parameterInputMap.put(methodParameter.getName(), parameterValue);
                }
            }
            execute(candidateMetadata, methodArgumentValues, parameterInputMap);
        }
    }

    public void clearResponsePanel() {
        this.componentCounter = 0;
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


        candidateCountLabel.setText(methodTestCandidates.size() + " set of inputs for " + methodElement.getName());
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

    public void execute(TestCandidateMetadata testCandidate, List<String> methodArgumentValues, Map<String, String> parameters) {
        insidiousService.reExecuteMethodInRunningProcess(methodElement, methodArgumentValues,
                (agentCommandRequest, agentCommandResponse) -> {
                    logger.warn("Agent command execution response: " + agentCommandResponse);
                    renderResponse(testCandidate, agentCommandResponse, parameters);
                });
    }

    private void renderResponse(TestCandidateMetadata testCandidateMetadata, AgentCommandResponse agentCommandResponse,
                                Map<String, String> parameters) {
        // render differences table
        // append to output panel
//        if (testCandidateMetadata != null) {
        addResponse(testCandidateMetadata, agentCommandResponse, parameters);
//        } else {
//            addResponse(null, String.valueOf(agentCommandResponse.getMethodReturnValue()), parameters);
//        }
    }

    public JComponent getContent() {
        return rootContent;
    }

    public void addResponse(TestCandidateMetadata testCandidateMetadata, AgentCommandResponse agentCommandResponse,
                            Map<String, String> parameters) {
        AgentResponseComponent response = new AgentResponseComponent(testCandidateMetadata, agentCommandResponse, this.insidiousService,
                parameters,alt);
        boolean isDiff = response.computeDifferences();
        alt=!alt;
        response.setBorderTitle(++this.componentCounter);
        scrollablePanel.add(response.getComponent(), 0);
        scrollablePanel.revalidate();
        if(isDiff)
        {
            this.isDifferent=true;
        }
        if((componentCounter)==callCount)
        {
            if(this.isDifferent)
            {
                insidiousService.getExecutionRecord().put(methodElement.getName(),true);
            }
            else
            {
                insidiousService.getExecutionRecord().put(methodElement.getName(),false);
            }
            DaemonCodeAnalyzer.getInstance(insidiousService.getProject()).restart(methodElement.getContainingFile());
        }
    }

    //temporary function to test mock differences between responses (json)
    public void tryTestDiff() {
        try {
            addResponse(null, null, null);
        } catch (Exception e) {
            System.out.println("TestDiff Exception: " + e);
            e.printStackTrace();
        }
    }

    public void setupScrollablePanel() {
        scrollablePanel = new ScrollablePanel();
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
        JBScrollPane scrollPane = new JBScrollPane();
        scrollPane.setViewportView(scrollablePanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);

        this.borderParent.removeAll();
        borderParent.add(scrollPane, BorderLayout.PAGE_START);
        this.borderParent.revalidate();
    }
}
