package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MethodExecutorComponent implements MethodExecutionListener {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;
    private final List<ParameterInputComponent> parameterInputComponents = new ArrayList<>();
    private List<ComponentContainer> components = new ArrayList<>();
    private MethodAdapter methodElement;
    private JPanel rootContent;
    private JPanel borderParentMain;
    private JPanel centerPanel;
    private JPanel topPanel;
    private JButton executeAndShowDifferencesButton;
    private JPanel methodParameterContainer;
    private JLabel candidateCountLabel;
    private JPanel borderParentScroll;
    private JPanel diffContentPanel;
    private JPanel compareViewer;
    private JPanel borderParent;
    private List<TestCandidateMetadata> methodTestCandidates;
    private int componentCounter = 0;
    private int mockCallCount = 1;
    private int callCount = 0;
    private boolean isDifferent = false;

    public MethodExecutorComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        executeAndShowDifferencesButton.addActionListener(e -> {
            executeAll();
        });
    }

    public void loadMethodCandidates() {
        components.clear();
        this.borderParentScroll.removeAll();
        if (methodTestCandidates == null || methodTestCandidates.size() == 0) {
            InsidiousNotification.notifyMessage(
                    "Please use the agent to record values for replay.",
                    NotificationType.WARNING
            );
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
        for (int i = 0; i < methodTestCandidates.size(); i++) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            TestCandidateMetadata candidateMetadata = methodTestCandidates.get(i);
            List<String> methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
                    candidateMetadata);
            CompareControlComponent comp = new CompareControlComponent(candidateMetadata,
                    methodArgumentValues, methodElement, this);
            ComponentContainer container = new ComponentContainer(comp);
            components.add(container);
            JPanel candidateDisplayPanel = comp.getComponent();
            int inputArgumentCount = candidateMetadata.getMainMethod().getArgumentProbes().size();
//            if (inputArgumentCount < 3) {
            candidateDisplayPanel.setMaximumSize(new Dimension(100, 50));
            candidateDisplayPanel.setPreferredSize(new Dimension(100, 50));
//            candidateDisplayPanel.revalidate();
//            candidateDisplayPanel.repaint();
//            }
            gridPanel.add(candidateDisplayPanel, constraints);
        }

        gridPanel.setBorder(JBUI.Borders.empty());
        JScrollPane scrollPane = new JBScrollPane(gridPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        borderParentScroll.setPreferredSize(scrollPane.getSize());
        borderParentScroll.add(scrollPane, BorderLayout.CENTER);
        if (callToMake <= 3) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        this.borderParentScroll.revalidate();
    }

    public void executeAll() {
        this.isDifferent = false;
        if (methodTestCandidates.size() == 0) {
            InsidiousNotification.notifyMessage(
                    "Please use the agent to record values for replay. No candidates found for " + methodElement.getName(),
                    NotificationType.WARNING
            );
            return;
        }
        loadMethodCandidates();
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
        boolean isNew = false;
        if (this.methodElement != null && !this.methodElement.equals(method)) {
            clearBoard();
            isNew = true;
        }
        this.methodElement = method;
        List<TestCandidateMetadata> candidates = this.insidiousService
                .getSessionInstance()
                .getTestCandidatesForAllMethod(methodElement.getContainingClass().getQualifiedName(),
                        methodElement.getName(),
                        false);
        this.methodTestCandidates = deDuplicateList(candidates);
        if (this.methodTestCandidates.size() == 0) {
            this.components = new ArrayList<>();
        }
        this.candidateCountLabel.setText(methodTestCandidates.size() + " unique candidates for " + method.getName());
        if (methodTestCandidates != null && methodTestCandidates.size() > 0) {
            if (isNew) {
                loadMethodCandidates();
                return;
            }
            if (this.components != null) {
                if (this.components.size() == 0) {
                    loadMethodCandidates();
                } else {
                    mergeComponentList();
                }
            } else {
                loadMethodCandidates();
            }
        }
    }

    private void clearBoard() {
        this.borderParentScroll.removeAll();
        this.diffContentPanel.removeAll();
        this.borderParentScroll.revalidate();
        this.diffContentPanel.revalidate();
    }

    private void mergeComponentList() {
        List<Integer> iohashes = new ArrayList<>();
        for (ComponentContainer component : this.components) {
            iohashes.add(component.getHash());
        }
        List<TestCandidateMetadata> toAdd = new ArrayList<>();
        for (TestCandidateMetadata metadata : this.methodTestCandidates) {
            List<String> inputs = insidiousService.buildArgumentValuesFromTestCandidate(metadata);
            String output = new String(metadata.getMainMethod().getReturnDataEvent().getSerializedValue());
            String concat = inputs + output;
            int hash = concat.hashCode();
            if (!iohashes.contains(hash)) {
                toAdd.add(metadata);
            }
        }
        if (toAdd.size() > 0) {
            this.borderParentScroll.removeAll();

            int callToMake = components.size() + toAdd.size();
            int GridRows = 3;
            if (callToMake > GridRows) {
                GridRows = callToMake;
            }
            GridLayout gridLayout = new GridLayout(GridRows, 1);
            gridLayout.setVgap(8);
            JPanel gridPanel = new JPanel(gridLayout);
            gridPanel.setBorder(JBUI.Borders.empty());
            int x = 0;
            for (int i = 0; i < components.size(); i++) {
                GridConstraints constraints = new GridConstraints();
                constraints.setRow(i);
                gridPanel.add(components.get(i).getComponent(), constraints);
                x++;
            }
            for (int j = 0; j < toAdd.size(); j++) {
                GridConstraints constraints = new GridConstraints();
                constraints.setRow(x + j);
                List<String> methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
                        toAdd.get(j));
                CompareControlComponent comp = new CompareControlComponent(toAdd.get(j),
                        methodArgumentValues, methodElement, this);
                ComponentContainer container = new ComponentContainer(comp);
                components.add(container);
                gridPanel.add(comp.getComponent(), constraints);
            }
            gridPanel.setBorder(JBUI.Borders.empty());
            JScrollPane scrollPane = new JBScrollPane(gridPanel);
            scrollPane.setBorder(JBUI.Borders.empty());
            borderParentScroll.setPreferredSize(scrollPane.getSize());
            borderParentScroll.add(scrollPane, BorderLayout.CENTER);
            if (callToMake <= 3) {
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            }
            this.borderParentScroll.revalidate();
        }
    }

    public List<TestCandidateMetadata> deDuplicateList(List<TestCandidateMetadata> list) {
        List<TestCandidateMetadata> resList = new ArrayList<>();
        Map<Integer, TestCandidateMetadata> ioHashMap = new TreeMap<>();
        for (TestCandidateMetadata metadata : list) {
            List<String> inputs = insidiousService.buildArgumentValuesFromTestCandidate(metadata);
            String output = new String(metadata.getMainMethod().getReturnDataEvent().getSerializedValue());
            String concat = inputs + output;
            int hash = concat.toString().hashCode();
            if (!ioHashMap.containsKey(hash)) {
                ioHashMap.put(hash, metadata);
            }
        }
        for (int key : ioHashMap.keySet()) {
            resList.add(ioHashMap.get(key));
        }
        return resList;
    }

    public void execute(TestCandidateMetadata testCandidate, List<String> methodArgumentValues,
                        ComponentContainer comp) {
        insidiousService.reExecuteMethodInRunningProcess(methodElement, methodArgumentValues,
                (agentCommandRequest, agentCommandResponse) -> {
                    logger.warn("Agent command execution response: " + agentCommandResponse);
                    if (testCandidate.getMainMethod().getReturnValue().isException()
                            || (agentCommandResponse.getResponseType() != null && agentCommandResponse.getResponseType()
                            .equals(ResponseType.FAILED) || agentCommandResponse.getResponseType()
                            .equals(ResponseType.EXCEPTION))) {
                        AgentExceptionResponseComponent exceptionResponseComponent = postProcessExecute_Exception(
                                testCandidate,
                                agentCommandResponse, comp.getSource());
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
        insidiousService.reExecuteMethodInRunningProcess(methodElement, methodArgumentValues,
                (agentCommandRequest, agentCommandResponse) -> {
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
                                                          CompareControlComponent controlComponent) {
        AgentResponseComponent response = new AgentResponseComponent(metadata, agentCommandResponse,
                this.insidiousService,
                controlComponent.getParameterMap(), true);
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
            if (this.isDifferent) {
                insidiousService.getExecutionRecord().put(methodElement.getName(), true);
            } else {
                insidiousService.getExecutionRecord().put(methodElement.getName(), false);
            }
            DaemonCodeAnalyzer.getInstance(insidiousService.getProject()).restart(methodElement.getContainingFile());
            ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
        }
        return response;
    }

    public AgentExceptionResponseComponent postProcessExecute_Exception(TestCandidateMetadata metadata, AgentCommandResponse agentCommandResponse,
                                                                        CompareControlComponent controlComponent) {
        AgentExceptionResponseComponent response = new AgentExceptionResponseComponent(metadata, agentCommandResponse,
                this.insidiousService);
        Boolean isDiff = true;

        if (isDiff) {
            this.isDifferent = true;
        }
        if (this.isDifferent) {
            insidiousService.getExecutionRecord().put(methodElement.getName(), true);
        } else {
            insidiousService.getExecutionRecord().put(methodElement.getName(), false);
        }
        DaemonCodeAnalyzer.getInstance(insidiousService.getProject()).restart(methodElement.getContainingFile());
        return response;
    }

    public AgentResponseComponent postProcessExecute(TestCandidateMetadata metadata, AgentCommandResponse agentCommandResponse,
                                                     CompareControlComponent controlComponent) {
        AgentResponseComponent response = new AgentResponseComponent(metadata, agentCommandResponse,
                this.insidiousService,
                controlComponent.getParameterMap(), true);
        Boolean isDiff = response.computeDifferences();
        if (isDiff == null) {
            //exception case
            isDiff = false;
        }
        response.setBorderTitle(++this.componentCounter);
        if (isDiff) {
            this.isDifferent = true;
        }
        if (this.isDifferent) {
            insidiousService.getExecutionRecord().put(methodElement.getName(), true);
        } else {
            insidiousService.getExecutionRecord().put(methodElement.getName(), false);
        }
        DaemonCodeAnalyzer.getInstance(insidiousService.getProject()).restart(methodElement.getContainingFile());
        return response;
    }

    public JComponent getContent() {
        return rootContent;
    }

    public void renderComparission(AgentResponseComponent component) {
        this.diffContentPanel.removeAll();
        this.diffContentPanel.setLayout(new GridLayout(1, 1));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(1);
        this.diffContentPanel.add(component.getComponent(), constraints);
        this.diffContentPanel.revalidate();
    }

    @Override
    public void executeCandidate(TestCandidateMetadata metadata,
                                 CompareControlComponent component) {
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
        renderComparission(responseComponent);
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
}
