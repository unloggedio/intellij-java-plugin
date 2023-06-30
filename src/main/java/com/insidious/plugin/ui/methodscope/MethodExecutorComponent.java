package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.ui.Components.MethodExecutorComponentDTO;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.DiffUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.MethodUtils;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MethodExecutorComponent implements MethodExecutionListener, CandidateSelectedListener {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;
    private final Map<Long, TestCandidateListedItemComponent> candidateComponentMap = new HashMap<>();
    private final Map<Long, AgentCommandResponse<String>> candidateResponseMap = new HashMap<>();
    //    private List<ComponentContainer> components = new ArrayList<>();
    private MethodAdapter methodElement;
    private JPanel rootContent;
    private JButton executeAndShowDifferencesButton;
    private JPanel methodParameterContainer;
    private JLabel candidateCountLabel;
    private JScrollPane candidateListScroller;
    private JPanel diffContentPanel;
    private JPanel candidateDisplayPanel;
    private JPanel topPanel;
    //    private JPanel centerPanel;
    private JPanel centerParent;
    private JPanel topAligner;
    private JScrollPane scrollParent;
    private JPanel selectedCandidateInfoPanel;
    private JPanel borderParent;
    private List<StoredCandidate> methodTestCandidates;
    private int componentCounter = 0;
    private int callCount = 0;

    public MethodExecutorComponent(InsidiousService insidiousService) {
        System.out.println("In Constructor mec");
        this.insidiousService = insidiousService;
        executeAndShowDifferencesButton.addActionListener(e -> {
            if (methodTestCandidates == null || methodTestCandidates.size() == 0) {
                InsidiousNotification.notifyMessage(
                        "Please use the agent to record values for replay. " +
                                "No candidates found for " + methodElement.getName(),
                        NotificationType.WARNING
                );
                return;
            }
            executeAll();
        });
    }

    public MethodAdapter getCurrentMethod() {
        return methodElement;
    }

    public void compileAndExecuteAll() {

        ApplicationManager.getApplication().invokeLater(() -> {
            insidiousService.compile(methodElement.getContainingClass(),
                    (aborted, errors, warnings, compileContext) -> {
                        logger.warn("compiled class: " + compileContext);
                        if (aborted) {
                            InsidiousNotification.notifyMessage(
                                    "Re-execution cancelled", NotificationType.WARNING
                            );
                            return;
                        }
                        if (errors > 0) {
                            InsidiousNotification.notifyMessage(
                                    "Re-execution cancelled due to [" + errors + "] compilation errors",
                                    NotificationType.ERROR
                            );
                        }
                        executeAll();
                    }
            );
        });
    }

    public MethodExecutorComponentDTO loadMethodCandidates(List<StoredCandidate> methodTestCandidates, MethodAdapter methodAdapter) {

        Map<Long, TestCandidateListedItemComponent> components = new HashMap<>();
        int callToMake = methodTestCandidates.size();
        int gridRows = 3;
        if (callToMake > gridRows) {
            gridRows = callToMake;
        }
        GridLayout gridLayout = new GridLayout(gridRows, 1);
        gridLayout.setVgap(8);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        int panelHeight = 0;
        for (int i = 0; i < methodTestCandidates.size(); i++) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            StoredCandidate candidateMetadata = methodTestCandidates.get(i);
            //reduce no or args, remove insidiousService, user methodElem
            TestCandidateListedItemComponent candidateListItem = new TestCandidateListedItemComponent(
                    candidateMetadata, methodAdapter, this, this);
            components.put(candidateMetadata.getEntryProbeIndex(), candidateListItem);
            JPanel candidateDisplayPanel = candidateListItem.getComponent();
            gridPanel.add(candidateDisplayPanel, constraints);
            panelHeight += candidateDisplayPanel.getPreferredSize().getHeight() + 10;
        }
        panelHeight += 40;
        gridPanel.setBorder(JBUI.Borders.empty());
        JScrollPane scrollPane = new JBScrollPane(gridPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setMaximumSize(new Dimension(-1, Math.min(300, panelHeight)));
        return new MethodExecutorComponentDTO(components, scrollPane, panelHeight);
    }

    private void showDirectInvokeNavButton() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());

        JButton takeToDirectInvokeButton = new JButton("Execute method directly");
        takeToDirectInvokeButton.setMaximumSize(new Dimension(100, 80));
        takeToDirectInvokeButton.setBackground(JBColor.BLUE);
        takeToDirectInvokeButton.setForeground(JBColor.WHITE);
        takeToDirectInvokeButton.setBorderPainted(false);
        takeToDirectInvokeButton.setContentAreaFilled(false);
        takeToDirectInvokeButton.setOpaque(true);
        takeToDirectInvokeButton.addActionListener(e -> insidiousService.focusDirectInvokeTab());

        buttonPanel.add(takeToDirectInvokeButton, BorderLayout.NORTH);
        buttonPanel.setSize(new Dimension(-1, 100));
        buttonPanel.setBorder(JBUI.Borders.empty());

        centerParent.add(buttonPanel, BorderLayout.CENTER);
        centerParent.revalidate();
        centerParent.repaint();
    }


    public void executeAll() {
        ApplicationManager.getApplication().invokeLater(() -> {
            JSONObject eventProperties = new JSONObject();

            ClassUtils.chooseClassImplementation(methodElement.getContainingClass(), psiClass1 -> {
                eventProperties.put("className", psiClass1.getQualifiedName());
                eventProperties.put("methodName", methodElement.getName());
                UsageInsightTracker.getInstance().RecordEvent("REXECUTE_ALL", eventProperties);

                callCount = candidateComponentMap.size();
                componentCounter = 0;

                executeCandidate(methodTestCandidates, psiClass1, "all",
                        (testCandidate, agentCommandResponse, diffResult) -> {
                            componentCounter++;
                            if (componentCounter == callCount) {
                                insidiousService.updateMethodHashForExecutedMethod(methodElement);
                                insidiousService.triggerGutterIconReload();
                                ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
                            }
                        });
            });
        });

    }

    public void refreshAndReloadCandidates(MethodAdapter method, List<StoredCandidate> candidates) {
        clearBoard();
        this.methodElement = method;
        this.methodTestCandidates = candidates;
        MethodExecutorComponentDTO dto = loadMethodCandidates(candidates, methodElement);
        executeAndShowDifferencesButton.setEnabled(true);
        insidiousService.showNewTestCandidateGotIt();

        candidateComponentMap.putAll(dto.getComponentMap());
        renderComponentList(dto.getMainPanel(), dto.getPanelHeight());

        executeAndShowDifferencesButton.revalidate();
        executeAndShowDifferencesButton.repaint();
        candidateCountLabel.setText(methodTestCandidates.size() + " Unique candidates");
        TitledBorder topPanelTitledBorder = (TitledBorder) topPanel.getBorder();
        topPanelTitledBorder.setTitle(
                methodElement.getContainingClass().getName() + "." + methodElement.getName() + "()");

        rootContent.revalidate();
        rootContent.repaint();
    }

    private void renderComponentList(JScrollPane scrollPane, int panelHeight) {
        centerParent.setMaximumSize(new Dimension(-1, Math.min(300, panelHeight)));
        centerParent.setMinimumSize(new Dimension(-1, 300));

        centerParent.add(scrollPane, BorderLayout.CENTER);
        centerParent.revalidate();
        centerParent.repaint();
    }

    private void clearBoard() {
        candidateComponentMap.clear();
        centerParent.removeAll();
        diffContentPanel.removeAll();
        diffContentPanel.revalidate();
        centerParent.revalidate();
        centerParent.repaint();
    }

    @Override
    public void executeCandidate(
            List<StoredCandidate> testCandidateList,
            PsiClass psiClass,
            String source,
            AgentCommandResponseListener<String> agentCommandResponseListener
    ) {

        for (StoredCandidate testCandidate : testCandidateList) {

            List<String> methodArgumentValues = testCandidate.getMethodArguments();
            AgentCommandRequest agentCommandRequest = MethodUtils.createRequestWithParameters(
                    methodElement, psiClass, methodArgumentValues);

            insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                    (request, agentCommandResponse) -> {
                        candidateResponseMap.put(testCandidate.getEntryProbeIndex(), agentCommandResponse);
                        DifferenceResult diffResult = DiffUtils.calculateDifferences(testCandidate,
                                agentCommandResponse);
                        System.out.println("Source [EXEC]: " + source);
                        if (source.equals("all")) {
                            diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_REPLAY);
                            diffResult.setIndividualContext(false);
                        } else {
                            diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_INDIVIDUAL);
                            //check other statuses and add them for individual execution
                            String status = getExecutionStatusFromCandidates(testCandidate.getEntryProbeIndex());
                            String methodKey = methodElement.getContainingClass().getQualifiedName()
                                    + "#" + methodElement.getName() + "#" + methodElement.getJVMSignature();
                            if (status.equals("Diff") || status.equals("NoRun")) {
                                System.out.println("Setting status multi run : " + status);
                                insidiousService.getIndividualCandidateContextMap().put(methodKey, status);
                            } else {
                                System.out.println("Setting status multi run : Same");
                                insidiousService.getIndividualCandidateContextMap().put(methodKey, "Same");
                            }
                            diffResult.setIndividualContext(true);
                        }
                        diffResult.setMethodAdapter(methodElement);
                        diffResult.setResponse(agentCommandResponse);
                        diffResult.setCommand(agentCommandRequest);

                        insidiousService.addDiffRecord(methodElement, diffResult);

                        StoredCandidateMetadata meta = testCandidate.getMetadata();
                        if (meta == null) {
                            meta = new StoredCandidateMetadata();
                        }
                        meta.setTimestamp(agentCommandResponse.getTimestamp());
                        meta.setCandidateStatus(getStatusForState(diffResult.getDiffResultType()));
                        if (testCandidate.getCandidateId() != null) {
                            insidiousService.getAtomicRecordService().setCandidateStateForCandidate(
                                    testCandidate.getCandidateId(),
                                    methodElement.getContainingClass().getQualifiedName(),
                                    methodElement.getName() + "#" + methodElement.getJVMSignature(),
                                    testCandidate.getMetadata().getCandidateStatus());
                        }
                        //possible bug vector, equal case check
                        TestCandidateListedItemComponent candidateComponent =
                                candidateComponentMap.get(testCandidate.getEntryProbeIndex());

                        candidateComponent.setAndDisplayResponse(agentCommandResponse, diffResult);

                        agentCommandResponseListener.onSuccess(testCandidate, agentCommandResponse, diffResult);

                        insidiousService.triggerGutterIconReload();

                        agentCommandResponseListener.onSuccess(testCandidate, agentCommandResponse, diffResult);
                    });
        }
    }

    private StoredCandidateMetadata.CandidateStatus getStatusForState(DiffResultType type) {
        switch (type) {
            case SAME:
            case NO_ORIGINAL:
                return StoredCandidateMetadata.CandidateStatus.PASSING;
            default:
                return StoredCandidateMetadata.CandidateStatus.FAILING;
        }
    }

    //refactor pending - if there are stored candidates show only from stored, else others.
    public String getExecutionStatusFromCandidates(long excludeKey) {
        boolean hasDiff = false;
        boolean hasNoRun = false;
        for (long key : candidateComponentMap.keySet()) {
            if (key == excludeKey) {
                continue;
            }
            TestCandidateListedItemComponent component = candidateComponentMap.get(key);
            String status = component.getExecutionStatus().trim();
            if (status.isEmpty() || status.isBlank()) {
                hasNoRun = true;
            }
            if (status.contains("Diff")) {
                hasDiff = true;
            }
        }
        if (hasDiff) {
            return "Diff";
        } else if (hasNoRun) {
            return "NoRun";
        }
        return "Same";
    }

    public JComponent getContent() {
        return rootContent;
    }


    public void displayResponse(Supplier<Component> component) {
        scrollParent.setMinimumSize(new Dimension(-1, 700));
        this.diffContentPanel.removeAll();
        this.diffContentPanel.setLayout(new GridLayout(1, 1));
        diffContentPanel.setMinimumSize(new Dimension(-1, 700));
        Component comp = component.get();
        comp.setMinimumSize(new Dimension(-1, 700));
        this.diffContentPanel.add(comp);
        this.scrollParent.revalidate();
        this.scrollParent.repaint();
        this.diffContentPanel.revalidate();
        this.diffContentPanel.repaint();
    }


    public JPanel getComponent() {
        return this.rootContent;
    }

    @Override
    public void onCandidateSelected(StoredCandidate testCandidateMetadata) {

        AgentCommandResponse<String> agentCommandResponse = candidateResponseMap.get(
                testCandidateMetadata.getEntryProbeIndex());
        if (agentCommandResponse == null) {
            return;
        }
        Supplier<Component> response = createTestCandidateChangeComponent(testCandidateMetadata, agentCommandResponse);
        displayResponse(response);
    }

    @NotNull
    private Supplier<Component> createTestCandidateChangeComponent(
            StoredCandidate testCandidateMetadata,
            AgentCommandResponse<String> agentCommandResponse
    ) {
        if (testCandidateMetadata.isException()) {
            AgentExceptionResponseComponent comp = new AgentExceptionResponseComponent(
                    testCandidateMetadata, agentCommandResponse, insidiousService);
            comp.setMethodHash(methodElement.getJVMSignature());
            comp.setMethodName(methodElement.getName());
            comp.setMethodHash(methodElement.getText().hashCode() + "");
            comp.setClassname(methodElement.getContainingClass().getQualifiedName());
            comp.setMethodSignature(methodElement.getJVMSignature());
            return comp;
        }
        if (agentCommandResponse.getResponseType() != null &&
                (agentCommandResponse.getResponseType().equals(ResponseType.FAILED) ||
                        agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION))) {
            AgentExceptionResponseComponent comp = new AgentExceptionResponseComponent(
                    testCandidateMetadata, agentCommandResponse, insidiousService);
            comp.setMethodHash(methodElement.getJVMSignature());
            comp.setMethodName(methodElement.getName());
            comp.setMethodHash(methodElement.getText().hashCode() + "");
            comp.setClassname(methodElement.getContainingClass().getQualifiedName());
            comp.setMethodSignature(methodElement.getJVMSignature());
            return comp;
        } else {
            AgentResponseComponent component = new AgentResponseComponent(
                    agentCommandResponse, testCandidateMetadata, true, insidiousService::generateCompareWindows,
                    insidiousService.getAtomicRecordService());
            component.setMethodName(methodElement.getName());
            component.setMethodHash(methodElement.getText().hashCode() + "");
            component.setClassname(methodElement.getContainingClass().getQualifiedName());
            component.setMethodSignature(methodElement.getJVMSignature());
            return component;
        }
    }

    public void setMethod(MethodAdapter lastSelection) {
        System.out.println("In set method");
        if (lastSelection != null) {
            System.out.println("Set Method");
            this.methodElement = lastSelection;
        }
    }
}
