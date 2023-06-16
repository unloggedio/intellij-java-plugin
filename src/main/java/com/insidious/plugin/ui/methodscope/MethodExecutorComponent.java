package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.datafile.AtomicRecordService;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.AtomicRecord;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.util.*;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.*;
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

    public MethodAdapter getCurrentMethod()
    {
        return methodElement;
    }

    public void compileAndExecuteAll() {
//        this.isDifferent = false;

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

    public void loadMethodCandidates() {
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
            StoredCandidate candidateMetadata = methodTestCandidates.get(i);
            TestCandidateListedItemComponent candidateListItem = new TestCandidateListedItemComponent(
                    candidateMetadata, methodElement, this, this, insidiousService);

            candidateComponentMap.put(candidateMetadata.getEntryProbeIndex(), candidateListItem);
            JPanel candidateDisplayPanel = candidateListItem.getComponent();
            gridPanel.add(candidateDisplayPanel, constraints);
            panelHeight += candidateDisplayPanel.getPreferredSize().getHeight() + 10;
        }
        panelHeight +=40;

        gridPanel.setBorder(JBUI.Borders.empty());
        JScrollPane scrollPane = new JBScrollPane(gridPanel);
        scrollPane.setBorder(JBUI.Borders.empty());

        scrollPane.setMaximumSize(new Dimension(-1, Math.min(300, panelHeight)));
//        centerPanel.setMinimumSize(new Dimension(-1, 500));
//        centerPanel.setMaximumSize(new Dimension(-1, Math.min(500, panelHeight)));
        centerParent.setMaximumSize(new Dimension(-1, Math.min(300, panelHeight)));
        centerParent.setMinimumSize(new Dimension(-1, 300));

        centerParent.add(scrollPane, BorderLayout.CENTER);
        centerParent.revalidate();
        centerParent.repaint();
    }

    private void showDirectInvokeNavButton() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());

        JButton takeToDirectInvokeButton = new JButton("Execute method directly");
        takeToDirectInvokeButton.setMaximumSize(new Dimension(100, 80));
        takeToDirectInvokeButton.setBackground(Color.BLUE);
        takeToDirectInvokeButton.setForeground(Color.WHITE);
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
                                DaemonCodeAnalyzer.getInstance(insidiousService.getProject())
                                        .restart(methodElement.getContainingFile());
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

        if (this.methodTestCandidates.size() == 0) {
            //moving to different view as this is not the intended screen for no available data.
            insidiousService.updateScaffoldForState(GutterState.PROCESS_RUNNING);
        } else {
            loadMethodCandidates();
            executeAndShowDifferencesButton.setEnabled(true);
            insidiousService.showNewTestCandidateGotIt();
        }
        executeAndShowDifferencesButton.revalidate();
        executeAndShowDifferencesButton.repaint();
        this.candidateCountLabel.setText(methodTestCandidates.size() + " Unique candidates");
        TitledBorder topPanelTitledBorder = (TitledBorder) topPanel.getBorder();
        topPanelTitledBorder.setTitle(
                methodElement.getContainingClass().getName() + "." + methodElement.getName() + "()");

        this.rootContent.revalidate();
        this.rootContent.repaint();
    }

    private void clearBoard() {
//        this.candidateListScroller.removeAll();
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

                        if(source.equals("all")) {
                            diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_REPLAY);
                        }
                        else
                        {
                            diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_INDIVIDUAL);
                            //check other statuses and add them for individual execution
                            String status = getExecutionStatusFromCandidates(testCandidate.getEntryProbeIndex());
                            System.out.println("STATUS ALL -> "+status);
                            if(status.equals("Diff") || status.equals("NoRun"))
                            {
                                diffResult.setGutterStatus(status);
                            }
                            else
                            {
                                diffResult.setGutterStatus("Same");
                            }
                        }
                        diffResult.setMethodAdapter(methodElement);
                        diffResult.setResponse(agentCommandResponse);
                        diffResult.setCommand(agentCommandRequest);

                        insidiousService.addDiffRecord(methodElement, diffResult);

                        //possible bug vector, equal case check
                        TestCandidateListedItemComponent candidateComponent =
                                candidateComponentMap.get(testCandidate.getEntryProbeIndex());

                        candidateComponent.setAndDisplayResponse(agentCommandResponse, diffResult);

                        agentCommandResponseListener.onSuccess(testCandidate, agentCommandResponse, diffResult);

                        DaemonCodeAnalyzer
                                .getInstance(insidiousService.getProject())
                                .restart(methodElement.getContainingFile());

                        agentCommandResponseListener.onSuccess(testCandidate, agentCommandResponse, diffResult);
                    });
        }
    }

    public String getExecutionStatusFromCandidates(long excludeKey)
    {
        boolean hasDiff = false;
        boolean hasNoRun = false;
        for(long key : candidateComponentMap.keySet())
        {
            if(key == excludeKey)
            {
                continue;
            }
            TestCandidateListedItemComponent component = candidateComponentMap.get(key);
            String status = component.getExecutionStatus().trim();
            if(status.isEmpty() || status.isBlank())
            {
                hasNoRun = true;
            }
            if(status.contains("Diff"))
            {
                hasDiff = true;
            }
        }
        if(hasDiff)
        {
            //has a diff case
            return "Diff";
        }
        else if(hasNoRun)
        {
            //has candidates not run
            return "NoRun";
        }
        return "Same";
    }

    public JComponent getContent() {
        return rootContent;
    }


    public void displayResponse(Supplier<Component> component) {
        scrollParent.setMinimumSize(new Dimension(-1,700));
        this.diffContentPanel.removeAll();
        this.diffContentPanel.setLayout(new GridLayout(1, 1));
//        GridConstraints constraints = new GridConstraints();
        diffContentPanel.setMinimumSize(new Dimension(-1, 700));
//        constraints.setRow(0);
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
            comp.setMethodHash(methodElement.getText().hashCode()+"");
            comp.setClassname(methodElement.getContainingClass().getQualifiedName());
            comp.setMethodSignature(methodElement.getJVMSignature());
            return comp;
        }
        if (agentCommandResponse.getResponseType() != null &&
                (agentCommandResponse.getResponseType().equals(ResponseType.FAILED) ||
                        agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION))) {
            System.out.println("Returning exception");
            AgentExceptionResponseComponent comp = new AgentExceptionResponseComponent(
                    testCandidateMetadata, agentCommandResponse, insidiousService);
            comp.setMethodHash(methodElement.getJVMSignature());
            comp.setMethodName(methodElement.getName());
            comp.setMethodHash(methodElement.getText().hashCode()+"");
            comp.setClassname(methodElement.getContainingClass().getQualifiedName());
            comp.setMethodSignature(methodElement.getJVMSignature());
            return comp;
        } else {
            System.out.println("Returning Diff view");
            AgentResponseComponent component = new AgentResponseComponent(
                    agentCommandResponse, testCandidateMetadata, true, insidiousService::generateCompareWindows,
                    insidiousService.getAtomicRecordService());
            component.setMethodName(methodElement.getName());
            component.setMethodHash(methodElement.getText().hashCode()+"");
            component.setClassname(methodElement.getContainingClass().getQualifiedName());
            component.setMethodSignature(methodElement.getJVMSignature());
            return component;
        }
    }
}
