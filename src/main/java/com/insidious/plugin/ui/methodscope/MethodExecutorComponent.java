package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.ReplayAllExecutionContext;
import com.insidious.plugin.pojo.ResourceEmbedMode;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.pojo.dao.MethodDefinition;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.ui.assertions.SaveForm;
import com.insidious.plugin.util.*;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MethodExecutorComponent implements CandidateLifeListener {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;
    private final Map<String, AgentCommandResponse<String>> candidateResponseMap = new HashMap<>();
    private final JPanel gridPanel;
    private final Map<String, TestCandidateListedItemComponent> candidateComponentMap = new HashMap<>();
    private final JScrollPane candidateScrollPanelContainer;
    private final CoveragePanel coveragePanel;
    private int panelHeightMax = 300;
    private MethodAdapter methodElement;
    private JPanel rootContent;
    private JButton executeAndShowDifferencesButton;
    private JLabel candidateCountLabel;
    private JPanel topPanel;
    private JPanel centerParent;
    private JPanel topAligner;
    private JPanel controlsPanel;
    private JPanel scrollContainer;
    private int callCount = 0;
    private SaveForm saveFormReference;
    private CandidateFilterType candidateFilterType = CandidateFilterType.METHOD;
    private MethodDefinition methodInfo;
    private MethodUnderTest methodUnderTest;
    private ResponsePreviewComponent currentResponsePreviewComponent;
    private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    public MethodExecutorComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        executeAndShowDifferencesButton.addActionListener(this::actionPerformed);
        gridPanel = createCandidateScrollPanel();

        candidateScrollPanelContainer = new JBScrollPane(gridPanel);
        candidateScrollPanelContainer.setBorder(JBUI.Borders.empty(5));
        candidateScrollPanelContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        setFilterButtonsListeners();
        if (screenSize.getHeight() <= 1000) {
            panelHeightMax = 240;
        }
        centerParent.setMaximumSize(new Dimension(-1, Math.min(300, 40)));
        centerParent.setMinimumSize(new Dimension(-1, 300));

        centerParent.add(candidateScrollPanelContainer, BorderLayout.CENTER);
        centerParent.revalidate();
        centerParent.repaint();

        executeAndShowDifferencesButton.setIcon(UIUtils.REPLAY_PINK);

        coveragePanel = new CoveragePanel(insidiousService);
        JPanel coveragePanelContent = coveragePanel.getContent();
        coveragePanelContent.setMinimumSize(new Dimension(-1, 300));
        rootContent.add(coveragePanelContent, BorderLayout.SOUTH);
    }

    public void setFilterButtonsListeners() {
        ButtonGroup buttonGroup = new ButtonGroup();

        JBRadioButton allButton = new JBRadioButton("All");
        JBRadioButton classOnlyButton = new JBRadioButton("Class only");
        JBRadioButton methodOnlyButton = new JBRadioButton("Method only");

        methodOnlyButton.setSelected(true);

        buttonGroup.add(allButton);
        buttonGroup.add(classOnlyButton);
        buttonGroup.add(methodOnlyButton);

        allButton.addActionListener((e) -> {
            MethodExecutorComponent.this.candidateFilterType = CandidateFilterType.ALL;
            refreshSearchAndLoad();
        });


        classOnlyButton.addActionListener((e) -> {
            MethodExecutorComponent.this.candidateFilterType = CandidateFilterType.ALL;
            refreshSearchAndLoad();
        });


        methodOnlyButton.addActionListener((e) -> {
            MethodExecutorComponent.this.candidateFilterType = CandidateFilterType.ALL;
            refreshSearchAndLoad();
        });


//        filterButtonGroupPanel.add(allButton);
//        filterButtonGroupPanel.add(classOnlyButton);
//        filterButtonGroupPanel.add(methodOnlyButton);
    }

    public MethodAdapter getCurrentMethod() {
        return methodElement;
    }


    public JPanel createCandidateScrollPanel() {

        GridLayout gridLayout = new GridLayout(0, 1);
        gridLayout.setVgap(12);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        return gridPanel;
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

    public void compileAndExecuteAll() {

        ApplicationManager.getApplication().invokeLater(() -> {
            insidiousService.compile(methodElement.getContainingClass(),
                    (aborted, errors, warnings, compileContext) -> {
                        logger.warn("compiled class: " + compileContext);
                        if (aborted) {
                            InsidiousNotification.notifyMessage("Re-execution cancelled", NotificationType.WARNING);
                            return;
                        }
                        if (errors > 0) {
                            InsidiousNotification.notifyMessage(
                                    "Re-execution cancelled due to [" + errors + "] compilation errors",
                                    NotificationType.ERROR
                            );
                            return;
                        }
                        executeAll();
                    }
            );
        });
    }

    private List<StoredCandidate> getCandidatesFromComponents() {
        return candidateComponentMap.values().stream().
                map(TestCandidateListedItemComponent::getCandidateMetadata).collect(Collectors.toList());
    }

    public void executeAll() {
        clearBottomPanel();
        List<StoredCandidate> methodTestCandidates = getCandidatesFromComponents();
        String methodName = methodElement.getName();
        boolean hasStoredCandidates = methodTestCandidates.stream().anyMatch(e -> e.getCandidateId() != null);
        if (methodTestCandidates.size() == 0) {
            InsidiousNotification.notifyMessage(
                    "Please use the agent to record values for replay. " +
                            "No candidates found for " + methodName,
                    NotificationType.WARNING
            );
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            JSONObject eventProperties = new JSONObject();

            insidiousService.chooseClassImplementation(methodElement.getContainingClass().getQualifiedName(),
                    psiClass1 -> {
                        eventProperties.put("className", psiClass1.getQualifiedClassName());
                        eventProperties.put("methodName", methodName);
                        eventProperties.put("count", methodTestCandidates.size());
                        UsageInsightTracker.getInstance().RecordEvent("REXECUTE_ALL", eventProperties);

                        callCount = methodTestCandidates.size();
                        long savedCandidatesCount = methodTestCandidates.stream()
                                .filter(e -> e.getCandidateId() != null)
                                .count();
                        AtomicInteger componentCounter = new AtomicInteger(0);
                        AtomicInteger passingSavedCandidateCount = new AtomicInteger(0);
                        AtomicInteger passingCandidateCount = new AtomicInteger(0);

                        long batchTime = System.currentTimeMillis();
                        ReplayAllExecutionContext context = new ReplayAllExecutionContext(
                                ExecutionRequestSourceType.Bulk,
                                hasStoredCandidates);
                        executeCandidate(new ArrayList<>(methodTestCandidates), psiClass1, context,
                                (testCandidate, agentCommandResponse, diffResult) -> {
                                    int currentCount = componentCounter.incrementAndGet();
                                    if (diffResult.getDiffResultType().equals(DiffResultType.SAME)) {
//                                if (testCandidate.getCandidateId() != null) {
//                                    passingSavedCandidateCount.incrementAndGet();
//                                }
//                                passingCandidateCount.incrementAndGet();
                                    }
                                    logger.warn("component counter: " + currentCount);
                                    if (currentCount == callCount) {
                                        insidiousService.updateMethodHashForExecutedMethod(methodElement);
                                        insidiousService.triggerGutterIconReload();
                                        ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
                                        int passedCount;
                                        if (savedCandidatesCount > 0) {
                                            passedCount = passingSavedCandidateCount.get();
                                            if (passedCount == savedCandidatesCount) {
                                                InsidiousNotification.notifyMessage(
                                                        passedCount + " of " + savedCandidatesCount
                                                                + " saved atomic tests passed",
                                                        NotificationType.INFORMATION);
                                            } else {
                                                InsidiousNotification.notifyMessage(
                                                        passedCount + " of " + savedCandidatesCount
                                                                + " saved atomic tests passed",
                                                        NotificationType.WARNING);
                                            }
                                        } else {
                                            passedCount = passingCandidateCount.get();
                                            if (passedCount == callCount) {
                                                InsidiousNotification.notifyMessage(passedCount + " of " + callCount
                                                        + " atomic tests passed", NotificationType.INFORMATION);
                                            } else {
                                                InsidiousNotification.notifyMessage(passedCount + " of " + callCount
                                                        + " atomic tests passed", NotificationType.WARNING);
                                            }
                                        }
                                    }
                                });
                    });
        });

    }

    private void updateJunitButtonStatuses() {
//        try {
//            candidateComponentMap.values().forEach(
//                    TestCandidateListedItemComponent::refreshJunitButtonStatus);
//        } catch (Exception e) {
//            logger.info("Exception updating enable status of Junit buttons for candidates " + e);
//        }
    }

    public void refreshAndReloadCandidates(final MethodAdapter methodAdapter, List<StoredCandidate> candidates) {
        long start = new Date().getTime();
//        updateJunitButtonStatuses();

        if (methodElement == null || methodAdapter == null
                || methodAdapter.getPsiMethod() != methodElement.getPsiMethod()) {
            clearBoard();
        }
        methodElement = methodAdapter;
        if (methodElement == null) {
            return;
        }
        if (DumbService.getInstance(getProject()).isDumb()) {
            return;
        }
        methodUnderTest = MethodUnderTest.fromMethodAdapter(methodElement);
        methodInfo = insidiousService.getMethodInformation(methodUnderTest);

        TitledBorder topPanelTitledBorder = (TitledBorder) topPanel.getBorder();
        topPanelTitledBorder.setTitle(
                methodElement.getContainingClass().getName() + "." + methodElement.getName() + "()");


        if (candidates.size() == 0) {
            candidateCountLabel.setText(gridPanel.getComponents().length + " recorded method executions");
            insidiousService.getDirectInvokeTab().setCoveragePercent(0);
            return;
        }


        List<ArgumentNameValuePair> methodArgumentNameList = generateParameterList(methodElement.getParameters());
        candidates.stream()
                .filter(testCandidateMetadata -> {
                    TestCandidateListedItemComponent existingComponent = candidateComponentMap.get(
                            getKeyForCandidate(testCandidateMetadata));
                    if (existingComponent != null) {
                        if (existingComponent.getCandidateMetadata().getCandidateId() == null) {
                            existingComponent.setCandidate(testCandidateMetadata);
                        }
                    }
                    if (currentResponsePreviewComponent != null) {
                        StoredCandidate previewCandidate = currentResponsePreviewComponent.getTestCandidate();
                        if (Objects.equals(previewCandidate.getCandidateId(), testCandidateMetadata.getCandidateId()) ||
                                previewCandidate.getEntryProbeIndex() == testCandidateMetadata.getEntryProbeIndex()) {
                            currentResponsePreviewComponent.setTestCandidate(testCandidateMetadata);
                        }
                    }
                    return existingComponent == null;
                })
                .peek(testCandidateMetadata -> {
                    testCandidateMetadata.setMethod(methodUnderTest);
                })
                .map(e -> new TestCandidateListedItemComponent(e, methodArgumentNameList,
                        this, insidiousService, methodAdapter)
                )
                .forEach(e -> {
                    if (!candidateComponentMap.containsKey(getKeyForCandidate(e.getCandidateMetadata()))) {
                        candidateComponentMap.put(getKeyForCandidate(e.getCandidateMetadata()), e);
                        gridPanel.add(e.getComponent());
                    }
                });
        refreshCoverageData();
        executeAndShowDifferencesButton.setEnabled(true);

        gridPanel.revalidate();
        gridPanel.repaint();

        setListDimensions(calculatePanelHeight());

        executeAndShowDifferencesButton.revalidate();
        executeAndShowDifferencesButton.repaint();
        candidateCountLabel.setText(gridPanel.getComponents().length + " recorded method executions");

        rootContent.revalidate();
        rootContent.repaint();
        long end = new Date().getTime();
        logger.warn("load and refresh candidates in MEC for took " + (end - start) + " ms");
    }

    private String getKeyForCandidate(StoredCandidate testCandidateMetadata) {
        return testCandidateMetadata.calculateCandidateHash();
    }

    private void refreshCoverageData() {

        Map<Boolean, List<StoredCandidate>> coveredLinesMap = candidateComponentMap.values()
                .stream()
                .map(TestCandidateListedItemComponent::getCandidateMetadata)
                .collect(Collectors.groupingBy(e -> e.getCandidateId() != null, Collectors.toList()));

        List<StoredCandidate> savedCandidates = coveredLinesMap.get(true);
        List<StoredCandidate> unsavedCandidates = coveredLinesMap.get(false);

        Set<Integer> savedLineCovered = savedCandidates == null ? new HashSet<>() : savedCandidates.stream()
                .map(StoredCandidate::getLineNumbers)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Set<Integer> unsavedLineCovered = unsavedCandidates == null ? new HashSet<>() : unsavedCandidates.stream()
                .map(StoredCandidate::getLineNumbers)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        unsavedLineCovered.removeAll(savedLineCovered);

        int coveredSavedLineCount = savedLineCovered.size();
        int coveredUnsavedLineCount = unsavedLineCovered.size();
        int totalLineCount = methodInfo.getLineCount();

        insidiousService.getDirectInvokeTab().setCoveragePercent(coveredUnsavedLineCount);
        coveragePanel.setCoverageData(totalLineCount, coveredSavedLineCount, coveredUnsavedLineCount);

        Set<Integer> linesToHighlight = new HashSet<>();
        linesToHighlight.addAll(savedLineCovered);
        linesToHighlight.addAll(unsavedLineCovered);
        HighlightedRequest highlightedRequest = new HighlightedRequest(methodUnderTest, linesToHighlight);
        insidiousService.highlightLines(highlightedRequest);
    }

    public List<ArgumentNameValuePair> generateParameterList(ParameterAdapter[] parameters) {
        List<ArgumentNameValuePair> argumentList = new ArrayList<>();

        if (parameters != null) {
            for (ParameterAdapter methodParameter : parameters) {
                String argumentTypeCanonicalName = methodParameter.getType().getCanonicalText();
                argumentList.add(new ArgumentNameValuePair(methodParameter.getName(), argumentTypeCanonicalName, null));
            }
        }
        return argumentList;
    }


    private int calculatePanelHeight() {
        return Math.min(candidateComponentMap.values().stream()
                .map(e -> e.getComponent().getPreferredSize().getHeight())
                .mapToInt(Double::intValue)
                .sum() + 25, 300);
    }

    private void setListDimensions(int panelHeight) {
        panelHeight = Math.min(panelHeightMax, panelHeight);
        centerParent.setMaximumSize(new Dimension(-1, panelHeight));
        centerParent.setPreferredSize(new Dimension(-1, panelHeight));
        centerParent.setMinimumSize(new Dimension(-1, panelHeight));

        candidateScrollPanelContainer.revalidate();
        candidateScrollPanelContainer.repaint();
        centerParent.revalidate();
        centerParent.repaint();
    }

    public void clearBoard() {
        candidateComponentMap.clear();
        gridPanel.removeAll();
        scrollContainer.removeAll();
        centerParent.revalidate();
        centerParent.repaint();
    }

    public void clearBottomPanel() {
        scrollContainer.removeAll();
    }

    @Override
    public void executeCandidate(
            List<StoredCandidate> testCandidateList,
            ClassUnderTest classUnderTest,
            ReplayAllExecutionContext context,
            AgentCommandResponseListener<TestCandidateMetadata, String> agentCommandResponseListener
    ) {
        for (StoredCandidate testCandidate : testCandidateList) {
            executeSingleCandidate(testCandidate, classUnderTest, context, agentCommandResponseListener);
        }
    }

    private void executeSingleCandidate(
            StoredCandidate testCandidate,
            ClassUnderTest classUnderTest,
            ReplayAllExecutionContext context,
            AgentCommandResponseListener<TestCandidateMetadata, String> agentCommandResponseListener
    ) {
//        List<String> methodArgumentValues = testCandidate.getMethodArguments();
//        ArrayList<DeclaredMock> testCandidateStoredEnabledMockDefination = MockIntersection.enabledStoredMockDefination(
//                insidiousService, testCandidate.getMockIds(), insidiousService.getCurrentMethod());
//        AgentCommandRequest agentCommandRequest = MethodUtils.createExecuteRequestWithParameters(
//                methodElement, classUnderTest, methodArgumentValues, true, testCandidateStoredEnabledMockDefination);
//
//        TestCandidateListedItemComponent candidateComponent =
//                candidateComponentMap.get(getKeyForCandidate(testCandidate));
//
//        candidateComponent.setStatus("Executing");
//        insidiousService.executeMethodInRunningProcess(agentCommandRequest,
//                (request, agentCommandResponse) -> {
//                    candidateResponseMap.put(getKeyForCandidate(testCandidate), agentCommandResponse);
//
//                    DifferenceResult diffResult = DiffUtils.calculateDifferences(testCandidate, agentCommandResponse);
//                    if (context.getSource() == ExecutionRequestSourceType.Bulk) {
//                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_REPLAY);
//                        diffResult.setIndividualContext(false);
//                        String batchID = String.valueOf(new Date().getTime());
//                        diffResult.setBatchID(batchID);
//                    } else {
//                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_INDIVIDUAL);
//                        //check other statuses and add them for individual execution
//                        String status = getExecutionStatusFromCandidates(getKeyForCandidate(testCandidate),
//                                diffResult.getDiffResultType());
//                        String methodKey = agentCommandRequest.getClassName()
//                                + "#" + agentCommandRequest.getMethodName() + "#" + agentCommandRequest.getMethodSignature();
//                        insidiousService.getIndividualCandidateContextMap().put(methodKey, status);
//                        diffResult.setIndividualContext(true);
//                    }
//                    diffResult.setResponse(agentCommandResponse);
//                    diffResult.setCommand(agentCommandRequest);
//
//                    //skip considering non saved candidates for gutter state calc if
//                    //there are saved candidates.
//                    if (context.isSavedCandidateCentricFlow()) {
//                        if (testCandidate.getCandidateId() != null) {
//                            insidiousService.addDiffRecord(diffResult);
//                        }
//                    } else {
//                        insidiousService.addDiffRecord(diffResult);
//                    }
//
//                    StoredCandidateMetadata meta = testCandidate.getMetadata();
//                    if (meta == null) {
//                        meta = new StoredCandidateMetadata(HOSTNAME, HOSTNAME, agentCommandResponse.getTimestamp(),
//                                getStatusForState(diffResult.getDiffResultType()));
//                    }
//                    meta.setTimestamp(agentCommandResponse.getTimestamp());
//                    meta.setCandidateStatus(getStatusForState(diffResult.getDiffResultType()));
//                    if (testCandidate.getCandidateId() != null) {
//                        insidiousService.getAtomicRecordService().setCandidateStateForCandidate(
//                                testCandidate.getCandidateId(),
//                                agentCommandRequest.getClassName(),
//                                methodUnderTest.getMethodHashKey(),
//                                testCandidate.getMetadata().getCandidateStatus());
//                    }
//                    candidateComponent.setAndDisplayResponse(diffResult);
//                    agentCommandResponseListener.onSuccess(testCandidate, agentCommandResponse, diffResult);
//                });
    }


    private boolean showDifferentStatus(DiffResultType type) {
        switch (type) {
            case SAME:
                return false;
            default:
                return true;
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

    public String getExecutionStatusFromCandidates(String excludeKey, DiffResultType type) {
        List<TestCandidateListedItemComponent> savedCandidateComponents = candidateComponentMap.values().stream()
                .filter(e -> e.getCandidateMetadata().getCandidateId() != null).collect(Collectors.toList());
        boolean isKeySaved = savedCandidateComponents.stream()
                .anyMatch(e -> e.getCandidateMetadata().calculateCandidateHash().equals(excludeKey));
        if (isKeySaved) {
            if (showDifferentStatus(type)) {
                return "Diff";
            }
        }
        //show state for only saved candidates if they exist.
        if (savedCandidateComponents.size() > 0) {
            return getGutterStatusForComponents(savedCandidateComponents, excludeKey);
        } else {
            return getGutterStatusForComponents(candidateComponentMap.values().stream().collect(Collectors.toList()),
                    excludeKey);
        }
    }

    private String getGutterStatusForComponents(List<TestCandidateListedItemComponent> candidateListedItemComponents,
                                                String excludeKey) {
        boolean hasDiff = false;
        boolean hasNoRun = false;
        for (TestCandidateListedItemComponent component : candidateListedItemComponents) {
            if (Objects.equals(component.getCandidateMetadata().calculateCandidateHash(), excludeKey)) {
                continue;
            }
            String status = component.getExecutionStatus().trim();
            if (status.isEmpty() || status.isBlank()) {
                hasNoRun = true;
            }
            if (status.contains("Fail")) {
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

    public void displayResponse(Component component, boolean isExceptionFlow) {
        scrollContainer.removeAll();
        JBScrollPane scrollParent = new JBScrollPane(component);
        scrollParent.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        if (screenSize.getHeight() < 1000) {
            if (!isExceptionFlow) {
                component.setPreferredSize(new Dimension(-1, 180));
            }
            scrollContainer.setPreferredSize(new Dimension(-1, 60));
            scrollParent.setMinimumSize(new Dimension(-1, 200));
            scrollParent.setMaximumSize(new Dimension(-1, 200));
        } else {
            scrollParent.setMaximumSize(new Dimension(-1, 300));
            scrollParent.setMaximumSize(new Dimension(-1, 600));
        }

        scrollContainer.add(scrollParent);
        scrollContainer.revalidate();
        scrollContainer.repaint();
        logger.warn("diff component attached: " + component.getClass().getCanonicalName());
    }


    public JPanel getComponent() {
        return this.rootContent;
    }

    @Override
    public void onCandidateSelected(StoredCandidate testCandidateMetadata) {

        AgentCommandResponse<String> agentCommandResponse = candidateResponseMap.get(
                getKeyForCandidate(testCandidateMetadata));
        if (agentCommandResponse == null) {
            return;
        }
        currentResponsePreviewComponent = createTestCandidateChangeComponent(testCandidateMetadata,
                agentCommandResponse);
        boolean isExceptionFlow = testCandidateMetadata.isException()
                || (agentCommandResponse.getResponseType().equals(ResponseType.FAILED)
                || agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION));
        displayResponse(currentResponsePreviewComponent.get(), isExceptionFlow);
    }

    private ResponsePreviewComponent createTestCandidateChangeComponent(
            StoredCandidate testCandidateMetadata,
            AgentCommandResponse<String> agentCommandResponse
    ) {
//        if (testCandidateMetadata.isException()) {
//            return new AgentExceptionResponseComponent(
//                    testCandidateMetadata, agentCommandResponse, insidiousService, this);
//        }
//        if (agentCommandResponse.getResponseType() != null &&
//                (agentCommandResponse.getResponseType().equals(ResponseType.FAILED) ||
//                        agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION))) {
//            return new AgentExceptionResponseComponent(
//                    testCandidateMetadata, agentCommandResponse, insidiousService, this);
//        } else {
//            return new AgentResponseComponent(
//                    agentCommandResponse, testCandidateMetadata,
//                    insidiousService::generateCompareWindows, this
//            );
//        }
        return null;
    }

    private void actionPerformed(ActionEvent e) {
        executeAll();
    }

    @Override
    public void onSaved(StoredCandidate candidate) {
        TestCandidateListedItemComponent candidateItem = candidateComponentMap.get(getKeyForCandidate(candidate));
        if (candidate.getCandidateId() == null) {
            candidate.setCandidateId(UUID.randomUUID().toString());
            candidateComponentMap.put(getKeyForCandidate(candidate), candidateItem);
        }
        insidiousService.getProject().getService(AtomicRecordService.class)
                .saveCandidate(MethodUnderTest.fromMethodAdapter(methodElement), candidate);

//        insidiousService.getAtomicRecordService()
//                .saveCandidate(MethodUnderTest.fromMethodAdapter(methodElement), candidate);
        candidateItem.getComponent().setEnabled(true);
        candidateItem.setCandidate(candidate);
        triggerReExecute(candidate);
        candidateItem.getComponent().revalidate();
        gridPanel.revalidate();
        gridPanel.repaint();
        insidiousService.hideCandidateSaveForm(saveFormReference);
        saveFormReference = null;
        refreshCoverageData();
    }

    private void triggerReExecute(StoredCandidate candidate) {
//        TestCandidateListedItemComponent component = candidateComponentMap.get(
//                getKeyForCandidate(candidate));
//        insidiousService.chooseClassImplementation(methodElement.getContainingClass(), psiClass -> {
//            JSONObject eventProperties = new JSONObject();
//            eventProperties.put("className", psiClass.getQualifiedClassName());
//            eventProperties.put("methodName", methodElement.getName());
//            UsageInsightTracker.getInstance().RecordEvent("REXECUTE_SINGLE_UPDATE", eventProperties);
//            ReplayAllExecutionContext context = new ReplayAllExecutionContext(ExecutionRequestSourceType.Single, false);
//            executeCandidate(
//                    Collections.singletonList(candidate), psiClass, context,
//                    (candidateMetadata, agentCommandResponse, diffResult) -> {
//                        insidiousService.updateMethodHashForExecutedMethod(methodElement);
//                        component.setAndDisplayResponse(diffResult);
//                        onCandidateSelected(candidateMetadata);
//                        insidiousService.triggerGutterIconReload();
//                    }
//            );
//        });
    }

    @Override
    public void onSaveRequest(StoredCandidate storedCandidate, AgentCommandResponse<String> agentCommandResponse) {
        if (saveFormReference != null) {
            insidiousService.hideCandidateSaveForm(saveFormReference);
            saveFormReference = null;
        }
        if (storedCandidate.getCandidateId() == null) {
            // new test case
            storedCandidate.setName("test " + storedCandidate.getMethod().getName() + " returns expected value when");
            storedCandidate.setDescription("assert that the response value matches expected value");
        }
        saveFormReference = new SaveForm(storedCandidate, this);
        insidiousService.showCandidateSaveForm(saveFormReference);

    }

    @Override
    public void onDeleteRequest(StoredCandidate storedCandidate) {
        String candidateName = storedCandidate.getName();
        if (candidateName == null || candidateName.length() == 0) {
            candidateName = "no name";
        }
        int result = Messages.showYesNoDialog(
                "Are you sure you want to delete the stored test [" +
                        candidateName + "]",
                "Confirm Delete",
                UIUtils.NO_AGENT_HEADER
        );
        if (result == Messages.YES) {
            onDeleted(storedCandidate);
        }
    }

    @Override
    public void onDeleted(StoredCandidate storedCandidate) {
//        insidiousService.getAtomicRecordService().deleteStoredCandidate(
//                methodUnderTest.getClassName(), methodUnderTest.getMethodHashKey(), storedCandidate.getCandidateId());
        TestCandidateListedItemComponent testCandidateListedItemComponent = candidateComponentMap.get(
                getKeyForCandidate(storedCandidate));
        JPanel candidateComponent = testCandidateListedItemComponent.getComponent();

        candidateComponentMap.remove(getKeyForCandidate(storedCandidate));
        gridPanel.remove(candidateComponent);
        gridPanel.revalidate();
        gridPanel.repaint();
        scrollContainer.removeAll();
        candidateCountLabel.setText(gridPanel.getComponents().length + " recorded method executions");
        refreshCoverageData();

        if (candidateComponentMap.size() < 3) {
            setListDimensions(calculatePanelHeight());
            //calling this to ensure that we don't see an empty atomic window.
            if (candidateComponentMap.size() == 0) {
                onLastCandidateDeleted();
            }
        }
    }

    public void onLastCandidateDeleted() {
        //reload this view, to add
        refreshSearchAndLoad();
    }

    public void refreshSearchAndLoad() {

        CandidateSearchQuery query = insidiousService.createSearchQueryForMethod(methodElement, candidateFilterType,
                false);

        List<StoredCandidate> methodTestCandidates =
                ApplicationManager.getApplication().runReadAction((Computable<List<StoredCandidate>>) () ->
                        insidiousService.getStoredCandidatesFor(query));
        logger.warn("Candidates for [ " + query + "] in refreshSearchAndLoad => " + methodTestCandidates.size());

        refreshAndReloadCandidates(methodElement, methodTestCandidates);
    }

    @Override
    public void onUpdated(StoredCandidate storedCandidate) {
    }

    @Override
    public void onUpdateRequest(StoredCandidate storedCandidate) {
    }

    @Override
    public void onGenerateJunitTestCaseRequest(StoredCandidate testCandidate) {
        logger.warn("Create test case: " + testCandidate);

//        progressIndicator.setText("Generating JUnit Test case");
        TestCandidateMetadata loadedTestCandidate = insidiousService.getSessionInstance()
                .getTestCandidateById(testCandidate.getEntryProbeIndex(), true);
        if (loadedTestCandidate == null) {
            InsidiousNotification.notifyMessage("Saved cases need to be replayed once to generate test case",
                    NotificationType.ERROR);
            return;
        }

        String testMethodName =
                "testMethod" + ClassTypeUtils.upperInstanceName(testCandidate.getMethod().getName());
        TestCaseGenerationConfiguration testCaseGenerationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUnit5,
                MockFramework.Mockito,
                JsonFramework.Jackson,
                ResourceEmbedMode.IN_CODE
        );

        // mock all calls by default
        if (loadedTestCandidate != null) {
            testCaseGenerationConfiguration.getCallExpressionList().addAll(loadedTestCandidate.getCallsList());
        }

        testCaseGenerationConfiguration.setTestMethodName(testMethodName);

        testCaseGenerationConfiguration.getTestCandidateMetadataList().clear();
        if (loadedTestCandidate != null) {
            testCaseGenerationConfiguration.getTestCandidateMetadataList().add(loadedTestCandidate);
        }

        try {
            insidiousService.previewTestCase(methodElement, testCaseGenerationConfiguration, false);
        } catch (Exception ex) {
            InsidiousNotification.notifyMessage("Failed to generate test case: " + ex.getMessage(),
                    NotificationType.ERROR);
        }
    }

    @Override
    public boolean canGenerateUnitCase(StoredCandidate candidate) {
        TestCandidateMetadata loadedTestCandidate = insidiousService.getSessionInstance()
                .getTestCandidateById(candidate.getEntryProbeIndex(), true);
        if (loadedTestCandidate != null) {
            return true;
        }
        return false;
    }

    @Override
    public void onCancel() {
        if (saveFormReference != null) {
            insidiousService.hideCandidateSaveForm(saveFormReference);
            saveFormReference = null;
        }
    }

    @Override
    public Project getProject() {
        return insidiousService.getProject();
    }
}
