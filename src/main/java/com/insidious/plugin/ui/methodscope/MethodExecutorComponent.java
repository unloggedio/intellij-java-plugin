package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.util.DiffUtils;
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
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import java.text.SimpleDateFormat;

import javax.swing.*;
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
    private JPanel selectedCandidateInfoPanel;
    private JPanel borderParent;
    private List<TestCandidateMetadata> methodTestCandidates;
    private int componentCounter = 0;
    private int mockCallCount = 1;
    private int callCount = 0;

    public MethodExecutorComponent(InsidiousService insidiousService) {
        System.out.println("In Constructor mec");
        this.insidiousService = insidiousService;
        executeAndShowDifferencesButton.addActionListener(e -> {
            if (methodTestCandidates == null || methodTestCandidates.size() == 0) {
                InsidiousNotification.notifyMessage(
                        "Please use the agent to record values for replay. No candidates found for " + methodElement.getName(),
                        NotificationType.WARNING
                );
                return;
            }
            executeAll();
        });
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
            TestCandidateMetadata candidateMetadata = methodTestCandidates.get(i);
            TestCandidateListedItemComponent candidateListItem = new TestCandidateListedItemComponent(
                    candidateMetadata, methodElement, this, this, insidiousService);

            candidateComponentMap.put(candidateMetadata.getEntryProbeIndex(), candidateListItem);
            JPanel candidateDisplayPanel = candidateListItem.getComponent();
            gridPanel.add(candidateDisplayPanel, constraints);
            panelHeight += candidateDisplayPanel.getPreferredSize().getHeight() + 10;
        }


        gridPanel.setBorder(JBUI.Borders.empty());
        JScrollPane scrollPane = new JBScrollPane(gridPanel);
        scrollPane.setBorder(JBUI.Borders.empty());

        scrollPane.setMaximumSize(new Dimension(-1, Math.min(380, panelHeight)));
//        centerPanel.setMinimumSize(new Dimension(-1, 500));
//        centerPanel.setMaximumSize(new Dimension(-1, Math.min(500, panelHeight)));
        centerParent.setMaximumSize(new Dimension(-1, Math.min(380, panelHeight)));
        centerParent.setMinimumSize(new Dimension(-1, 380));

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
            ClassAdapter psiClass = methodElement.getContainingClass();
            eventProperties.put("className", psiClass.getQualifiedName());
            eventProperties.put("methodName", methodElement.getName());
            UsageInsightTracker.getInstance().RecordEvent("REXECUTE_ALL", eventProperties);

            callCount = candidateComponentMap.size();
            componentCounter = 0;

            for (TestCandidateMetadata methodTestCandidate : this.methodTestCandidates) {
                List<String> methodArgumentValues = TestCandidateUtils.buildArgumentValuesFromTestCandidate(
                        methodTestCandidate);
                executeCandidate(methodTestCandidate, methodArgumentValues,
                        (testCandidate, agentCommandResponse, diffResult) -> {
                            componentCounter++;
                            if (componentCounter == callCount) {
                                insidiousService.updateMethodHashForExecutedMethod(methodElement);
                                DaemonCodeAnalyzer.getInstance(insidiousService.getProject())
                                        .restart(methodElement.getContainingFile());
                                ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
                            }
                        });
            }
        });

    }

    public void refresh() {
        if (methodElement == null) {
            return;
        }
        ApplicationManager.getApplication().runReadAction(() -> refreshAndReloadCandidates(methodElement));
    }

    public void refreshAndReloadCandidates(MethodAdapter method) {
        clearBoard();
        this.methodElement = method;
        String classQualifiedName = methodElement.getContainingClass().getQualifiedName();
        String methodName = methodElement.getName();
        List<TestCandidateMetadata> candidates = this.insidiousService
                .getSessionInstance()
                .getTestCandidatesForAllMethod(classQualifiedName, methodName, false);
        this.methodTestCandidates = deDuplicateList(candidates);
        if (this.methodTestCandidates.size() == 0) {
//            this.candidateComponentMap.clear();
//            this.candidateResponseMap.clear();
            //showDirectInvokeNavButton();
            //executeAndShowDifferencesButton.setEnabled(false);
            insidiousService.focusDirectInvokeTab();
            insidiousService.loadMethodInAtomicTests(new JavaMethodAdapter(method.getPsiMethod()));
        } else {
            loadMethodCandidates();
            executeAndShowDifferencesButton.setEnabled(true);
            insidiousService.showNewTestCandidateGotIt();
        }
        executeAndShowDifferencesButton.revalidate();
        executeAndShowDifferencesButton.repaint();
        this.candidateCountLabel.setText(methodTestCandidates.size() + " unique candidates for " + method.getName());
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

    @Override
    public void executeCandidate(
            TestCandidateMetadata testCandidate,
            List<String> methodArgumentValues,
            AgentCommandResponseListener<String> agentCommandResponseListener
    ) {
        AgentCommandRequest agentCommandRequest = MethodUtils.createRequestWithParameters(methodElement,
                methodArgumentValues);

        insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                (request, agentCommandResponse) -> {

                    candidateResponseMap.put(testCandidate.getEntryProbeIndex(), agentCommandResponse);
                    DifferenceResult diffResult = DiffUtils.calculateDifferences(testCandidate, agentCommandResponse);
                    insidiousService.addDiffRecord(methodElement, diffResult);

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


    public JComponent getContent() {
        return rootContent;
    }


    public void displayResponse(Supplier<Component> component) {
        this.diffContentPanel.removeAll();
        this.diffContentPanel.setLayout(new GridLayout(1, 1));
//        GridConstraints constraints = new GridConstraints();
        diffContentPanel.setMinimumSize(new Dimension(-1, 700));
//        constraints.setRow(0);
        Component comp = component.get();
        comp.setMinimumSize(new Dimension(-1, 700));
        this.diffContentPanel.add(comp);
        this.diffContentPanel.revalidate();
        this.diffContentPanel.repaint();
    }


    public JPanel getComponent() {
        return this.rootContent;
    }

    @Override
    public void onCandidateSelected(TestCandidateMetadata testCandidateMetadata) {

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
            TestCandidateMetadata testCandidateMetadata,
            AgentCommandResponse<String> agentCommandResponse
    ) {
        Supplier<Component> response;
        if (agentCommandResponse.getResponseType() != null &&
                (agentCommandResponse.getResponseType().equals(ResponseType.FAILED) ||
                        agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION))) {
            AgentExceptionResponseComponent resp = new AgentExceptionResponseComponent(
                    testCandidateMetadata, agentCommandResponse, insidiousService);
            resp.setInfoLabel("Recorded at "+formatDate(new Date())+" for "
                    +methodElement.getContainingClass().getName()+"."+methodElement.getName()+"()");
            response = resp;
        } else {
            DifferenceResult differences = DiffUtils.calculateDifferences(testCandidateMetadata,
                    agentCommandResponse);
            AgentResponseComponent response1 = new AgentResponseComponent(
                    agentCommandResponse, testCandidateMetadata,true, insidiousService::generateCompareWindows);
            response1.computeDifferences(differences);
            response1.setInfoLabel("Recorded at "+formatDate(new Date())+" for "
                    +methodElement.getContainingClass().getName()+"."+methodElement.getName()+"()");
            response = response1;

        }
        return response;
    }

    public String formatDate(Date date)
    {
        SimpleDateFormat formatter =
                new SimpleDateFormat("HH:mm:ss | dd MMM, yyyy");
        String formattedDate = formatter.format(date);
        return formattedDate;
    }
}
