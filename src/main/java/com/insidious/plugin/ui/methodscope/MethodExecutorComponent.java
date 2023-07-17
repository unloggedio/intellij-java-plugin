package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.ui.Components.AtomicRecord.SaveForm;
import com.insidious.plugin.ui.MethodExecutionListener;
import com.insidious.plugin.util.*;
import com.intellij.codeInsight.hints.ParameterHintsPassFactory;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MethodExecutorComponent implements MethodExecutionListener, CandidateSelectedListener, CandidateLifeListener {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;

    private final Map<Long, AgentCommandResponse<String>> candidateResponseMap = new HashMap<>();
    private final JPanel gridPanel;
    private final Map<Long, TestCandidateListedItemComponent> candidateComponentMap = new HashMap<>();
    private final JScrollPane candidateScrollPanelContainer;
    private MethodAdapter methodElement;
    private JPanel rootContent;
    private JButton executeAndShowDifferencesButton;
    private JLabel candidateCountLabel;
    private JPanel diffContentPanel;
    private JPanel topPanel;
    //    private JPanel centerPanel;
    private JPanel centerParent;
    private JPanel topAligner;
    private JScrollPane scrollParent;
    private int callCount = 0;
    private SaveForm saveFormReference;

    public MethodExecutorComponent(InsidiousService insidiousService) {
//        System.out.println("In Constructor mec");
        this.insidiousService = insidiousService;
        executeAndShowDifferencesButton.addActionListener(this::actionPerformed);
        gridPanel = createCandidateScrollPanel();

        candidateScrollPanelContainer = new JBScrollPane(gridPanel);
        candidateScrollPanelContainer.setBorder(JBUI.Borders.empty());
        candidateScrollPanelContainer.setMaximumSize(new Dimension(-1, 40));


        centerParent.setMaximumSize(new Dimension(-1, Math.min(300, 40)));
        centerParent.setMinimumSize(new Dimension(-1, 300));

        centerParent.add(candidateScrollPanelContainer, BorderLayout.CENTER);
        centerParent.revalidate();
        centerParent.repaint();

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

    private List<StoredCandidate> getCandidatesFromComponents()
    {
        return candidateComponentMap.values().stream().
                map(value -> value.getCandidateMetadata()).collect(Collectors.toList());
    }

    public void executeAll() {
        clearBottomPanel();
        List<StoredCandidate> methodTestCandidates = getCandidatesFromComponents();
        if (methodTestCandidates.size() == 0) {
            InsidiousNotification.notifyMessage(
                    "Please use the agent to record values for replay. " +
                            "No candidates found for " + methodElement.getName(),
                    NotificationType.WARNING
            );
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            JSONObject eventProperties = new JSONObject();

            ClassUtils.chooseClassImplementation(methodElement.getContainingClass(), psiClass1 -> {
                eventProperties.put("className", psiClass1.getQualifiedName());
                eventProperties.put("methodName", methodElement.getName());
                UsageInsightTracker.getInstance().RecordEvent("REXECUTE_ALL", eventProperties);

                callCount = methodTestCandidates.size();
                long savedCandidatesCount = methodTestCandidates.stream()
                        .filter(e -> e.getCandidateId() != null)
                        .count();
                AtomicInteger componentCounter = new AtomicInteger(0);
                AtomicInteger passingSavedCandidateCount = new AtomicInteger(0);
                AtomicInteger passingCandidateCount = new AtomicInteger(0);

                long batchTime = System.currentTimeMillis();
                executeCandidate(new ArrayList<>(methodTestCandidates), psiClass1, "all-" + batchTime,
                        (testCandidate, agentCommandResponse, diffResult) -> {
                            int currentCount = componentCounter.incrementAndGet();
                            if (diffResult.getDiffResultType().equals(DiffResultType.SAME)) {
                                if (testCandidate.getCandidateId() != null) {
                                    passingSavedCandidateCount.incrementAndGet();
                                }
                                passingCandidateCount.incrementAndGet();
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
                                        InsidiousNotification.notifyMessage(passedCount + " of " + savedCandidatesCount
                                                + " saved atomic tests passed", NotificationType.INFORMATION);
                                    } else {
                                        InsidiousNotification.notifyMessage(passedCount + " of " + savedCandidatesCount
                                                + " saved atomic tests passed", NotificationType.WARNING);
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

    public void refreshAndReloadCandidates(final MethodAdapter method, List<StoredCandidate> candidates) {

        if (methodElement == null || method == null || method.getPsiMethod() != methodElement.getPsiMethod()) {
            clearBoard();
        }
        this.methodElement = method;
        if (this.methodElement == null) {
            return;
        }
        final String methodHash = methodElement.getText().hashCode() + "";
        final String methodName = methodElement.getName();
        final String methodJVMSignature = methodElement.getJVMSignature();
        final String classQualifiedName = methodElement.getContainingClass().getQualifiedName();

        candidates.stream()
                .filter(testCandidateMetadata -> !candidateComponentMap.containsKey(
                        testCandidateMetadata.getEntryProbeIndex()))
                .peek(testCandidateMetadata -> {
                    testCandidateMetadata.setMethodHash(methodHash);
                    testCandidateMetadata.setMethodName(methodName);
                    testCandidateMetadata.setMethodSignature(methodJVMSignature);
                    testCandidateMetadata.setClassName(classQualifiedName);
                })
                .map(e -> new TestCandidateListedItemComponent(e, this.methodElement, this,
                        MethodExecutorComponent.this))
                .forEach(e -> {
                    candidateComponentMap.put(e.getCandidateMetadata().getEntryProbeIndex(), e);
                    gridPanel.add(e.getComponent());
                });

        executeAndShowDifferencesButton.setEnabled(true);
        insidiousService.showNewTestCandidateGotIt();

        int panelHeight = candidateComponentMap.values().stream()
                .map(e -> e.getComponent().getPreferredSize().getHeight())
                .mapToInt(Double::intValue)
                .sum() + 40;

        gridPanel.revalidate();
        gridPanel.repaint();

        setListDimensions(panelHeight);

        candidateScrollPanelContainer.revalidate();
        candidateScrollPanelContainer.repaint();
        centerParent.revalidate();
        centerParent.repaint();


        executeAndShowDifferencesButton.revalidate();
        executeAndShowDifferencesButton.repaint();
        candidateCountLabel.setText(candidateComponentMap.size() + " Unique candidates");
        TitledBorder topPanelTitledBorder = (TitledBorder) topPanel.getBorder();
        topPanelTitledBorder.setTitle(
                methodElement.getContainingClass().getName() + "." + methodElement.getName() + "()");

        rootContent.revalidate();
        rootContent.repaint();
    }

    private void setListDimensions(int panelHeight)
    {
        if(candidateComponentMap.size()<3)
        {
            centerParent.setMaximumSize(new Dimension(-1, Math.min(300, panelHeight)));
            centerParent.setPreferredSize(new Dimension(-1, Math.min(300, panelHeight)));
            centerParent.setMinimumSize(new Dimension(-1, panelHeight));
        }
        else {
            centerParent.setMaximumSize(new Dimension(-1, Math.min(300, 30)));
            centerParent.setPreferredSize(new Dimension(-1, Math.min(300, 30)));
            centerParent.setMinimumSize(new Dimension(-1, 300));
        }
    }

    public void clearBoard() {
        candidateComponentMap.clear();
        gridPanel.removeAll();
        diffContentPanel.removeAll();
        diffContentPanel.revalidate();
        centerParent.revalidate();
        centerParent.repaint();
    }

    public void clearBottomPanel()
    {
        diffContentPanel.removeAll();
        diffContentPanel.revalidate();
    }

    @Override
    public void executeCandidate(
            List<StoredCandidate> testCandidateList,
            PsiClass psiClass,
            String source,
            AgentCommandResponseListener<String> agentCommandResponseListener
    ) {
        for (StoredCandidate testCandidate : testCandidateList) {
            executeSingleCandidate(testCandidate, psiClass, source, agentCommandResponseListener);
        }
    }

    private void executeSingleCandidate(
            StoredCandidate testCandidate,
            PsiClass psiClass,
            String source,
            AgentCommandResponseListener<String> agentCommandResponseListener
    ) {
        List<String> methodArgumentValues = testCandidate.getMethodArguments();
        AgentCommandRequest agentCommandRequest = MethodUtils.createRequestWithParameters(
                methodElement, psiClass, methodArgumentValues);

        insidiousService.executeMethodInRunningProcess(agentCommandRequest,
                (request, agentCommandResponse) -> {
                    candidateResponseMap.put(testCandidate.getEntryProbeIndex(), agentCommandResponse);
                    DifferenceResult diffResult = DiffUtils.calculateDifferences(testCandidate,
                            agentCommandResponse);
                    logger.info("Source [EXEC]: " + source);
                    if (source.startsWith("all")) {
                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_REPLAY);
                        diffResult.setIndividualContext(false);
                        String batchID = source.split("-")[1];
                        diffResult.setBatchID(batchID);
                    } else {
                        diffResult.setExecutionMode(DifferenceResult.EXECUTION_MODE.ATOMIC_RUN_INDIVIDUAL);
                        //check other statuses and add them for individual execution
                        String status = getExecutionStatusFromCandidates(testCandidate.getEntryProbeIndex(),
                                diffResult.getDiffResultType());
                        String methodKey = methodElement.getContainingClass().getQualifiedName()
                                + "#" + methodElement.getName() + "#" + methodElement.getJVMSignature();
                        if (status.equals("Diff") || status.equals("NoRun")) {
                            logger.info("Setting status multi run : " + status);
                            insidiousService.getIndividualCandidateContextMap().put(methodKey, status);
                        } else {
                            logger.info("Setting status multi run : Same");
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
                });
    }

    private boolean showDiffernetStatus(DiffResultType type) {
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

    //refactor pending - if there are stored candidates show only from stored, else others.
    public String getExecutionStatusFromCandidates(long excludeKey, DiffResultType type) {
        if (showDiffernetStatus(type)) {
            return "Diff";
        }
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


    public void displayResponse(Component component) {
        scrollParent.setMinimumSize(new Dimension(-1, 700));
        diffContentPanel.removeAll();
        diffContentPanel.setLayout(new GridLayout(1, 1));
        diffContentPanel.setMinimumSize(new Dimension(-1, 700));
        component.setMinimumSize(new Dimension(-1, 700));
        diffContentPanel.add(component);
        scrollParent.revalidate();
        scrollParent.repaint();
        diffContentPanel.revalidate();
        diffContentPanel.repaint();
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
        displayResponse(response.get());
    }

    @NotNull
    private Supplier<Component> createTestCandidateChangeComponent(
            StoredCandidate testCandidateMetadata,
            AgentCommandResponse<String> agentCommandResponse
    ) {
        if (testCandidateMetadata.isException()) {
            return new AgentExceptionResponseComponent(
                    testCandidateMetadata, agentCommandResponse, insidiousService, this);
        }
        if (agentCommandResponse.getResponseType() != null &&
                (agentCommandResponse.getResponseType().equals(ResponseType.FAILED) ||
                        agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION))) {
            return new AgentExceptionResponseComponent(
                    testCandidateMetadata, agentCommandResponse, insidiousService, this);
        } else {
            return new AgentResponseComponent(
                    agentCommandResponse, testCandidateMetadata,
                    true, insidiousService::generateCompareWindows,
                    this
            );
        }
    }

    private void actionPerformed(ActionEvent e) {
        executeAll();
    }

    @Override
    public void onSaved(StoredCandidate storedCandidate) {
        if (storedCandidate.getCandidateId() == null) {
            storedCandidate.setCandidateId(UUID.randomUUID().toString());
        }
        insidiousService.getAtomicRecordService().saveCandidate(
                methodElement.getContainingClass().getQualifiedName(),
                storedCandidate.getMethodName(),
                methodElement.getJVMSignature(),
                storedCandidate
        );
        TestCandidateListedItemComponent candidateItem = candidateComponentMap.get(
                storedCandidate.getEntryProbeIndex());
        candidateItem.setTitledBorder(storedCandidate.getName());
        candidateItem.getComponent().setEnabled(true);
        candidateItem.setCandidate(storedCandidate);
        triggerReExecute(storedCandidate);
        candidateItem.getComponent().revalidate();
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void triggerReExecute(StoredCandidate candidate)
    {
        TestCandidateListedItemComponent component = candidateComponentMap.get(
                candidate.getEntryProbeIndex());
        ClassUtils.chooseClassImplementation(methodElement.getContainingClass(), psiClass -> {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("className", psiClass.getQualifiedName());
            eventProperties.put("methodName", methodElement.getName());
            UsageInsightTracker.getInstance().RecordEvent("REXECUTE_SINGLE_UPDATE", eventProperties);
            executeCandidate(
                    Collections.singletonList(candidate), psiClass, "individual",
                    (candidateMetadata, agentCommandResponse, diffResult) -> {
                        insidiousService.updateMethodHashForExecutedMethod(methodElement);
                        component.setAndDisplayResponse(agentCommandResponse, diffResult);
                        onCandidateSelected(candidateMetadata);
                        insidiousService.triggerGutterIconReload();
                    }
            );
        });
    }

    @Override
    public void onSaveRequest(StoredCandidate storedCandidate, AgentCommandResponse<String> agentCommandResponse) {
        if (saveFormReference != null) {
            saveFormReference.dispose();
        }
        saveFormReference = new SaveForm(storedCandidate, agentCommandResponse, this);
        saveFormReference.setVisible(true);
    }

    @Override
    public void onDeleteRequest(StoredCandidate storedCandidate) {
        int result = Messages.showYesNoDialog(
                "Are you sure you want to delete the stored test [" + storedCandidate.getName() + "]",
                "Confirm delete",
                UIUtils.NO_AGENT_HEADER
        );
        if (result == Messages.YES) {
            onDeleted(storedCandidate);
        }
    }

    @Override
    public void onDeleted(StoredCandidate storedCandidate) {
        insidiousService.getAtomicRecordService().deleteStoredCandidate(
                methodElement.getContainingClass().getQualifiedName(),
                methodElement.getName() + "#" + methodElement.getJVMSignature(),
                storedCandidate.getCandidateId());
        TestCandidateListedItemComponent testCandidateListedItemComponent = candidateComponentMap.get(
                storedCandidate.getEntryProbeIndex());
        JPanel candidateComponent = testCandidateListedItemComponent.getComponent();

        candidateComponentMap.remove(storedCandidate.getEntryProbeIndex());
        candidateCountLabel.setText(candidateComponentMap.size() + " Unique candidates");
        gridPanel.remove(candidateComponent);
        gridPanel.revalidate();
        gridPanel.repaint();
        diffContentPanel.removeAll();

        //calling this to ensure that we don't see an empty atomic window.
        if(candidateComponentMap.size()==0)
        {
            onLastCandidateDeleted();
        }
    }

    public void onLastCandidateDeleted()
    {
        GutterState currentState = insidiousService.getGutterStateFor(methodElement);
        if (currentState.equals(GutterState.DATA_AVAILABLE)
                || currentState.equals(GutterState.NO_DIFF)
                || currentState.equals(GutterState.DIFF)
                || currentState.equals(GutterState.EXECUTE))
        {
            //reload this view, to add
            List<StoredCandidate> methodTestCandidates =
                    ApplicationManager.getApplication().runReadAction((Computable<List<StoredCandidate>>) () ->
                            insidiousService.getStoredCandidatesFor(methodElement));
            refreshAndReloadCandidates(methodElement,methodTestCandidates);
        }
        else
        {
            //load process running window
            insidiousService.loadSingleWindowForState(GutterState.PROCESS_RUNNING);
        }
    }

    @Override
    public void onUpdated(StoredCandidate storedCandidate) {

    }

    @Override
    public void onUpdateRequest(StoredCandidate storedCandidate) {

    }

    @Override
    public String getSaveLocation() {
        return insidiousService.getAtomicRecordService().getSaveLocation();
    }
}
