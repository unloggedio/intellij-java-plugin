package com.insidious.plugin.ui.gutter;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.Components.AgentResponseComponent;
import com.insidious.plugin.ui.Components.CompareControlComponent;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.ui.adapter.MethodAdapter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiMethod;
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
    private List<CompareControlComponent> components = new ArrayList<>();
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
    private boolean alt = true;
    private int callCount = 0;
    private boolean isDifferent = false;

    public MethodExecutorComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        executeAndShowDifferencesButton.addActionListener(e -> {
            executeAll(methodElement);
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
            List<String> methodArgumentValues = insidiousService.buildArgumentValuesFromTestCandidate(
                    methodTestCandidates.get(i));
            CompareControlComponent comp = new CompareControlComponent(methodTestCandidates.get(i),
                    methodArgumentValues, methodElement, this);
            components.add(comp);
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

    public void executeAll(MethodAdapter method) {
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
        for (CompareControlComponent component : this.components) {
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
        if (this.methodElement != null && !this.methodElement.equals(method)) {
            clearBoard();
        }
        this.methodElement = method;
        List<TestCandidateMetadata> candidates = this.insidiousService
                .getSessionInstance()
                .getTestCandidatesForAllMethod(methodElement.getContainingClass().getQualifiedName(),
                        methodElement.getName(),
                        false);
        this.methodTestCandidates = deDuplicateList(candidates);
        if (methodTestCandidates != null && methodTestCandidates.size() > 0) {
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
        if (this.methodTestCandidates.size() == 0) {
            this.components = new ArrayList<>();
        }
        this.candidateCountLabel.setText("" + components.size() + " candidates for " + method.getName());
    }

    private void clearBoard() {
        this.borderParentScroll.removeAll();
        this.borderParentScroll.revalidate();
    }

    private void mergeComponentList() {
        List<Integer> iohashes = new ArrayList<>();
        for (CompareControlComponent component : this.components) {
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
                components.add(comp);
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
            String concat = inputs.toString() + output;
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
                        CompareControlComponent controlComponent) {
        insidiousService.reExecuteMethodInRunningProcess(methodElement, methodArgumentValues,
                (agentCommandRequest, agentCommandResponse) -> {
                    logger.warn("Agent command execution response: " + agentCommandResponse);
                    if (agentCommandResponse.getResponseType() != null && agentCommandResponse.getResponseType()
                            .equals(ResponseType.FAILED)) {
                        InsidiousNotification.notifyMessage(
                                "Failed to execute method: " + agentCommandResponse.getMessage(),
                                NotificationType.ERROR
                        );
                    } else {
                        AgentResponseComponent responseComponent = postProcessExecute(testCandidate,
                                agentCommandResponse, controlComponent);
                        controlComponent.setAndDisplayResponse(responseComponent);
                    }
                });
    }

    public void execute_save(TestCandidateMetadata testCandidate, List<String> methodArgumentValues,
                             CompareControlComponent controlComponent) {
        insidiousService.reExecuteMethodInRunningProcess(methodElement, methodArgumentValues,
                (agentCommandRequest, agentCommandResponse) -> {
                    logger.warn("Agent command execution response: " + agentCommandResponse);
                    if (agentCommandResponse.getResponseType() != null && agentCommandResponse.getResponseType()
                            .equals(ResponseType.FAILED)) {
                        InsidiousNotification.notifyMessage(
                                "Failed to execute method: " + agentCommandResponse.getMessage(),
                                NotificationType.ERROR
                        );
                    } else {
                        AgentResponseComponent responseComponent = postProcessExecute_save(testCandidate,
                                agentCommandResponse, controlComponent);
                        controlComponent.setResposeComponent(responseComponent);
                    }
                });
    }

    public AgentResponseComponent postProcessExecute(TestCandidateMetadata metadata, AgentCommandResponse agentCommandResponse,
                                                     CompareControlComponent controlComponent) {
        AgentResponseComponent response = new AgentResponseComponent(metadata, agentCommandResponse,
                this.insidiousService,
                controlComponent.getParameterMap(), alt);
        boolean isDiff = response.computeDifferences();
        alt = !alt;
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

    public AgentResponseComponent postProcessExecute_save(TestCandidateMetadata metadata, AgentCommandResponse agentCommandResponse,
                                                          CompareControlComponent controlComponent) {
        AgentResponseComponent response = new AgentResponseComponent(metadata, agentCommandResponse,
                this.insidiousService,
                controlComponent.getParameterMap(), alt);
        boolean isDiff = response.computeDifferences();
        alt = !alt;
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
        execute(metadata, component.getMethodArgumentValues(), component);
    }

    @Override
    public void displayResponse(AgentResponseComponent responseComponent) {
        renderComparission(responseComponent);
    }
}
