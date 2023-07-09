package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.ui.Components.AtomicRecord.SaveForm;
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

public class MethodExecutorComponent implements MethodExecutionListener, CandidateSelectedListener, CandidateLifeListener {
    private static final Logger logger = LoggerUtil.getInstance(MethodExecutorComponent.class);
    private final InsidiousService insidiousService;

    private final Map<Long, AgentCommandResponse<String>> candidateResponseMap = new HashMap<>();
    private final Set<StoredCandidate> methodTestCandidates = new HashSet<>();
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
        gridLayout.setVgap(8);
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

    public void executeAll() {
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


        this.methodTestCandidates.addAll(candidates);

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
        candidateScrollPanelContainer.setMaximumSize(new Dimension(-1, Math.min(300, panelHeight)));
        candidateScrollPanelContainer.setPreferredSize(new Dimension(-1, Math.min(300, panelHeight)));
        centerParent.setMaximumSize(new Dimension(-1, Math.min(300, 30)));
        centerParent.setPreferredSize(new Dimension(-1, Math.min(300, 30)));
        centerParent.setMinimumSize(new Dimension(-1, 300));


        candidateScrollPanelContainer.revalidate();
        candidateScrollPanelContainer.repaint();
        centerParent.revalidate();
        centerParent.repaint();


        executeAndShowDifferencesButton.revalidate();
        executeAndShowDifferencesButton.repaint();
        candidateCountLabel.setText(methodTestCandidates.size() + " Unique candidates");
        TitledBorder topPanelTitledBorder = (TitledBorder) topPanel.getBorder();
        topPanelTitledBorder.setTitle(
                methodElement.getContainingClass().getName() + "." + methodElement.getName() + "()");

        rootContent.revalidate();
        rootContent.repaint();
    }

    private void clearBoard() {
        methodTestCandidates.clear();
        candidateComponentMap.clear();
        gridPanel.removeAll();
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
                        String status = getExecutionStatusFromCandidates(testCandidate.getEntryProbeIndex());
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
        testCandidateMetadata.setMethodHash(methodElement.getText().hashCode() + "");
        testCandidateMetadata.setMethodName(methodElement.getName());
        testCandidateMetadata.setMethodSignature(methodElement.getJVMSignature());
        testCandidateMetadata.setClassName(methodElement.getContainingClass().getQualifiedName());
        Supplier<Component> response = createTestCandidateChangeComponent(testCandidateMetadata, agentCommandResponse);
        displayResponse(response);
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

//    public void setMethod(MethodAdapter lastSelection) {
////        System.out.println("In set method");
//        if (lastSelection != null) {
////            System.out.println("Set Method");
//            this.methodElement = lastSelection;
//        }
//    }

    private void actionPerformed(ActionEvent e) {
        executeAll();
    }

    @Override
    public void onSaved(StoredCandidate storedCandidate) {
        insidiousService.getAtomicRecordService().saveCandidate(
                methodElement.getContainingClass().getQualifiedName(),
                storedCandidate.getMethodName(),
                methodElement.getJVMSignature(),
                storedCandidate
        );
        candidateComponentMap.get(storedCandidate.getEntryProbeIndex()).setTitledBorder(storedCandidate.getName());
    }

    @Override
    public void onSaveRequest(StoredCandidate storedCandidate) {
        if (saveFormReference != null) {
            saveFormReference.dispose();
        }
        saveFormReference = new SaveForm(storedCandidate, this);
        saveFormReference.setVisible(true);
    }

    @Override
    public void onDeleteRequest(StoredCandidate storedCandidate) {
        onDeleted(storedCandidate);
    }

    @Override
    public void onDeleted(StoredCandidate storedCandidate) {
        insidiousService.getAtomicRecordService().deleteStoredCandidate(
                storedCandidate.getClassName(),
                storedCandidate.getMethodName() + "#" + storedCandidate.getMethodSignature(),
                storedCandidate.getCandidateId());
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
