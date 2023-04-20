package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.MethodUtils;
import com.insidious.plugin.util.TestCandidateUtils;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MethodExecutorComponent implements MethodExecutionListener {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;
    private List<ComponentContainer> components = new ArrayList<>();
    private MethodAdapter methodElement;
    private JPanel rootContent;
    private JButton executeAndShowDifferencesButton;
    private JPanel methodParameterContainer;
    private JLabel candidateCountLabel;
    private JScrollPane candidateListScroller;
    private JPanel diffContentPanel;
    private JPanel candidateDisplayPanel;
    private JPanel topPanel;
    private JPanel centerPanel;
    private JPanel bottomPanel;
    private JPanel mainContent;
    private JPanel centerParent;
    private JPanel borderParent;
    private List<TestCandidateMetadata> methodTestCandidates;
    private int componentCounter = 0;
    private int mockCallCount = 1;
    private int callCount = 0;
    private boolean isDifferent = false;

    public MethodExecutorComponent(InsidiousService insidiousService) {
        System.out.println("In Constructor mec");
        this.insidiousService = insidiousService;
        executeAndShowDifferencesButton.addActionListener(e -> {
            if (methodTestCandidates.size() == 0) {
                InsidiousNotification.notifyMessage(
                        "Please use the agent to record values for replay. No candidates found for " + methodElement.getName(),
                        NotificationType.WARNING
                );
                return;
            }
            executeAll();
        });
    }

    public void loadMethodCandidates() {
        components.clear();
        centerPanel.removeAll();

        if (methodTestCandidates == null || methodTestCandidates.size() == 0) {

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new BorderLayout());

            JButton takeToDirectInvokeButton = new JButton("Execute method directly");
            takeToDirectInvokeButton.setMaximumSize(new Dimension(100, 80));
            takeToDirectInvokeButton.setBackground(Color.BLUE);
            takeToDirectInvokeButton.setForeground(Color.WHITE);
            takeToDirectInvokeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    insidiousService.focusDirectInvokeTab();
                }
            });

            buttonPanel.add(takeToDirectInvokeButton, BorderLayout.NORTH);
            buttonPanel.setSize(new Dimension(-1, 100));
            buttonPanel.setBorder(JBUI.Borders.empty());

            centerPanel.add(buttonPanel, BorderLayout.CENTER);
            centerPanel.revalidate();
            centerPanel.repaint();

            return;
        }

        int callToMake = methodTestCandidates.size();
        int GridRows = 3;
        if (callToMake > GridRows) {
            GridRows = callToMake;
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        gridLayout.setVgap(8);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        int panelHeight = 0;
        for (int i = 0; i < methodTestCandidates.size(); i++) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            TestCandidateMetadata candidateMetadata = methodTestCandidates.get(i);
            TestCandidateListedItemComponent comp = new TestCandidateListedItemComponent(
                    candidateMetadata, methodElement, this);
            ComponentContainer container = new ComponentContainer(comp);
            components.add(container);
            JPanel candidateDisplayPanel = comp.getComponent();
            gridPanel.add(candidateDisplayPanel, constraints);
            panelHeight += candidateDisplayPanel.getPreferredSize().getHeight();
        }

//        gridPanel.setSize(new Dimension(-1, panelHeight));
//        gridPanel.setMaximumSize(new Dimension(-1, panelHeight));
        gridPanel.setBorder(JBUI.Borders.empty());
        JScrollPane scrollPane = new JBScrollPane(gridPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
//        candidateListPanel.setPreferredSize(scrollPane.getSize());
//        candidateListPanel.add(scrollPane, BorderLayout.CENTER);
//        if (callToMake <= 3) {
//            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
//        }
//        this.candidateListPanel.revalidate();
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    public void executeAll() {
        JSONObject eventProperties = new JSONObject();
        ClassAdapter psiClass = methodElement.getContainingClass();
        eventProperties.put("className", psiClass.getQualifiedName());
        eventProperties.put("methodName", methodElement.getName());
        UsageInsightTracker.getInstance().RecordEvent("REXECUTE_ALL", eventProperties);

        this.isDifferent = false;
        callCount = this.components.size();
        componentCounter = 0;
        for (ComponentContainer component : this.components) {
            execute_save(component.getCandidateMetadata(), component.getMethodArgumentValues(), component);
        }
    }

    public void refresh() {
        if (methodElement == null) {
            return;
        }
        ApplicationManager.getApplication().runReadAction(() -> refreshAndReloadCandidates(methodElement));
    }

    public void refreshAndReloadCandidates(MethodAdapter method) {
//        boolean isNew = false;
//        if (method.equals(this.methodElement)) {
//            return;
//        }
//        if (this.methodElement != null && !this.methodElement.equals(method)) {
        clearBoard();
//            isNew = true;
//        }
        this.methodElement = method;
        String classQualifiedName = methodElement.getContainingClass().getQualifiedName();
        String methodName = methodElement.getName();
        List<TestCandidateMetadata> candidates = this.insidiousService
                .getSessionInstance()
                .getTestCandidatesForAllMethod(classQualifiedName, methodName, false);
        this.methodTestCandidates = deDuplicateList(candidates);
        if (this.methodTestCandidates.size() == 0) {
            this.components = new ArrayList<>();
        }
//        if (methodTestCandidates.size() > 0) {
        this.candidateCountLabel.setText(
                methodTestCandidates.size() + " unique candidates for " + method.getName());
        executeAndShowDifferencesButton.setEnabled(true);
        loadMethodCandidates();
//        } else {
//            this.candidateCountLabel.setText("No candidates for " + method.getName());
//            executeAndShowDifferencesButton.setEnabled(false);
//        }
//        if (methodTestCandidates.size() > 0) {
////            if (isNew) {
////                loadMethodCandidates();
////                return;
////            }
////            if (this.components != null) {
////                if (this.components.size() == 0) {
////                    loadMethodCandidates();
////                } else {
////                    mergeComponentList();
////                }
////            } else {
////                loadMethodCandidates();
////            }
//        }
    }

    private void clearBoard() {
//        this.candidateListScroller.removeAll();
        this.diffContentPanel.removeAll();
        this.diffContentPanel.revalidate();
        this.centerPanel.revalidate();
        this.centerPanel.repaint();
    }

    public List<TestCandidateMetadata> deDuplicateList(List<TestCandidateMetadata> list) {
        Map<Integer, TestCandidateMetadata> candidateHashMap = new TreeMap<>();
        for (TestCandidateMetadata metadata : list) {
            int candidateHash = TestCandidateUtils.getCandidateSimilarityHash(metadata);
            if (!candidateHashMap.containsKey(candidateHash)) {
                candidateHashMap.put(candidateHash, metadata);
            }
        }
        return new ArrayList<>(candidateHashMap.values());
    }

    public void execute(
            TestCandidateMetadata testCandidate,
            List<String> methodArgumentValues,
            ComponentContainer comp
    ) {

        AgentCommandRequest agentCommandRequest = MethodUtils.createRequestWithParameters(methodElement,
                methodArgumentValues);
        insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                (request, agentCommandResponse) -> {
                    logger.warn("Agent command execution response: " + agentCommandResponse);
                    if (agentCommandResponse.getResponseType() != null &&
                            (agentCommandResponse.getResponseType().equals(ResponseType.FAILED) ||
                                    agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION))) {
                        AgentExceptionResponseComponent exceptionResponseComponent = postProcessExecute_Exception(
                                testCandidate, agentCommandResponse, comp.getSource());
                        comp.setAndDisplayExceptionFlow(exceptionResponseComponent);
                    } else {
                        AgentResponseComponent responseComponent = postProcessExecute(testCandidate,
                                agentCommandResponse, comp.getSource());
                        comp.getSource().setAndDisplayResponse(responseComponent);
                        comp.setNormalResponse(responseComponent);
                    }
                });
    }

    public void execute_save(TestCandidateMetadata testCandidate, List<String> methodArgumentValues,
                             ComponentContainer comp) {
        AgentCommandRequest agentCommandRequest = MethodUtils.createRequestWithParameters(methodElement,
                methodArgumentValues);
        insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                (request, agentCommandResponse) -> {
                    logger.warn("Agent command execution response: " + agentCommandResponse);
                    if (testCandidate.getMainMethod().getReturnValue().isException()
                            || (agentCommandResponse.getResponseType() != null && agentCommandResponse.getResponseType()
                            .equals(ResponseType.FAILED) || agentCommandResponse.getResponseType()
                            .equals(ResponseType.EXCEPTION))) {
                        AgentExceptionResponseComponent exceptionResponseComponent = postProcessExecute_Exception(
                                testCandidate,
                                agentCommandResponse, comp.getSource());
                        comp.setExceptionResponse(exceptionResponseComponent);
                    } else {
                        AgentResponseComponent responseComponent = postProcessExecute_save(testCandidate,
                                agentCommandResponse, comp.getSource());
                        comp.setNormalResponse(responseComponent);
                    }
                });
    }

    public AgentResponseComponent postProcessExecute_save(TestCandidateMetadata metadata, AgentCommandResponse agentCommandResponse,
                                                          TestCandidateListedItemComponent controlComponent) {
        AgentResponseComponent response = new AgentResponseComponent(metadata, agentCommandResponse,
                this.insidiousService, controlComponent.getParameterMap(), true);
        Boolean isDiff = response.computeDifferences();
        if (isDiff == null) {
            //exception case
            isDiff = false;
        }
        componentCounter++;
        if (isDiff) {
            this.isDifferent = true;
        }
        if (componentCounter == callCount) {
            insidiousService.getExecutionRecord().put(methodElement.getName(), this.isDifferent);
            insidiousService.updateMethodHashForExecutedMethod(methodElement.getPsiMethod());
            DaemonCodeAnalyzer.getInstance(insidiousService.getProject()).restart(methodElement.getContainingFile());
            ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
        }
        return response;
    }

    public AgentExceptionResponseComponent postProcessExecute_Exception(TestCandidateMetadata metadata, AgentCommandResponse agentCommandResponse,
                                                                        TestCandidateListedItemComponent controlComponent) {
        AgentExceptionResponseComponent response = new AgentExceptionResponseComponent(metadata, agentCommandResponse,
                this.insidiousService);


        this.isDifferent = true;

        if (agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION)) {
            try {
                String responseClassName = agentCommandResponse.getResponseClassName();
                String expectedClassName = metadata.getMainMethod().getReturnValue().getType();

                this.isDifferent = responseClassName.equals(expectedClassName);

            } catch (Exception e) {
                logger.warn("failed to match expected and returned type: " + agentCommandResponse + "\n" + metadata, e);
            }
        }

        insidiousService.getExecutionRecord().put(methodElement.getName(), this.isDifferent);
        insidiousService.updateMethodHashForExecutedMethod(methodElement.getPsiMethod());
        // this is to update gutter icons
        DaemonCodeAnalyzer.getInstance(insidiousService.getProject()).restart(methodElement.getContainingFile());
        return response;
    }

    public AgentResponseComponent postProcessExecute(TestCandidateMetadata metadata, AgentCommandResponse agentCommandResponse,
                                                     TestCandidateListedItemComponent controlComponent) {
        AgentResponseComponent response = new AgentResponseComponent(metadata, agentCommandResponse,
                this.insidiousService, controlComponent.getParameterMap(), true);
        Boolean isDiff = response.computeDifferences();
        if (isDiff == null) {
            //exception case
            isDiff = false;
        }
        response.setBorderTitle(++this.componentCounter);
        this.isDifferent = isDiff;
        insidiousService.getExecutionRecord().put(methodElement.getName(), this.isDifferent);
        insidiousService.updateMethodHashForExecutedMethod(methodElement.getPsiMethod());
        // this is to update gutter icons
        DaemonCodeAnalyzer.getInstance(insidiousService.getProject()).restart(methodElement.getContainingFile());
        return response;
    }

    public JComponent getContent() {
        return rootContent;
    }

    public void renderComparison(AgentResponseComponent component) {
        this.diffContentPanel.removeAll();
        this.diffContentPanel.setLayout(new GridLayout(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(1);
        this.diffContentPanel.add(component.getComponent(), constraints);
        this.diffContentPanel.revalidate();
    }

    @Override
    public void executeCandidate(TestCandidateMetadata metadata,
                                 TestCandidateListedItemComponent component) {
        ComponentContainer cont = null;
        for (ComponentContainer container : components) {
            if (container.getSource().equals(component)) {
                cont = container;
                break;
            }
        }
        if (cont != null) {
            execute(metadata, component.getMethodArgumentValues(), cont);
        }
    }

    @Override
    public void displayResponse(AgentResponseComponent responseComponent) {
        renderComparison(responseComponent);
    }

    @Override
    public void displayExceptionResponse(AgentExceptionResponseComponent comp) {
        this.diffContentPanel.removeAll();
        this.diffContentPanel.setLayout(new GridLayout(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(1);
        this.diffContentPanel.add(comp.getComponent(), constraints);
        this.diffContentPanel.revalidate();
    }

    public JPanel getComponent() {
        return this.rootContent;
    }
}
