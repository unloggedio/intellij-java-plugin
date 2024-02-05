package com.insidious.plugin.ui.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.client.ScanProgress;
import com.insidious.plugin.client.SessionScanEventListener;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.*;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.ui.assertions.SaveForm;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.ui.methodscope.MethodDirectInvokeComponent;
import com.insidious.plugin.ui.methodscope.OnCloseListener;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.lang.jvm.JvmMethod;
import com.intellij.lang.jvm.JvmParameter;
import com.intellij.lang.jvm.types.JvmType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class StompComponent implements
        Consumer<List<TestCandidateMetadata>>,
        TestCandidateLifeListener,
        OnCloseListener,
        Runnable,
        OnExpandListener {
    public static final int COMPONENT_HEIGHT = 93;
    private static final Logger logger = LoggerUtil.getInstance(StompComponent.class);
    private final InsidiousService insidiousService;
    private final JPanel itemPanel;
    private final StompStatusComponent stompStatusComponent;
    private final List<TestCandidateMetadata> selectedCandidates = new ArrayList<>();
    private final List<StompItem> stompItems = new ArrayList<>(500);
    private final SessionScanEventListener scanEventListener;
    private final SimpleDateFormat dateFormat;
    private final Map<TestCandidateMetadata, Component> candidateMetadataStompItemMap = new HashMap<>();
    private final InsidiousConfigurationState configurationState;
    private final FilterModel filterModel;
    BlockingQueue<TestCandidateMetadata> incomingQueue = new ArrayBlockingQueue<>(100);
    private JPanel mainPanel;
    private JPanel northPanelContainer;
    private JScrollPane historyStreamScrollPanel;
    private JPanel scrollContainer;
    private JLabel reloadButton;
    private JLabel filterButton;
    private JButton saveReplayButton;
    private JLabel replayButton;
    private JLabel saveAsMockButton;
    private JLabel generateJUnitButton;
    private JPanel controlPanel;
    private JPanel infoPanel;
    private JLabel selectedCountLabel;
    private JLabel selectAllLabel;
    private JLabel clearSelectionLabel;
    private JPanel southPanel;
    private JLabel clearFilterLabel;
    private JLabel filterAppliedLabel;
    private JLabel clearTimelineLabel;
    private long lastEventId = 0;
    private MethodDirectInvokeComponent directInvokeComponent = null;
    private SaveForm saveFormReference;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
    private boolean welcomePanelRemoved = false;
    private AtomicInteger candidateQueryLatch;

    public StompComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        configurationState = insidiousService.getProject()
                .getService(InsidiousConfigurationState.class);

        filterAppliedLabel.setVisible(false);
        filterModel = configurationState.getFilterModel();


        itemPanel = new JPanel();
        GridBagLayout mgr = new GridBagLayout();
        itemPanel.setLayout(mgr);
        itemPanel.setAlignmentY(0);
        itemPanel.setAlignmentX(0);
        clearTimelineLabel.setVisible(false);

        itemPanel.add(new JPanel(), createGBCForFakeComponent());

        itemPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));


        historyStreamScrollPanel.setViewportView(itemPanel);
        historyStreamScrollPanel.setBorder(BorderFactory.createEmptyBorder());

        scrollContainer.setBorder(BorderFactory.createEmptyBorder());


        clearTimelineLabel.setToolTipText("Clear the timeline");
        clearTimelineLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearTimelineLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                resetTimeline();
            }
        });


        clearSelectionLabel.setVisible(false);
        clearSelectionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        clearSelectionLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCandidates.clear();
                for (StompItem stompItem : stompItems) {
                    stompItem.setSelected(false);
                }
            }
        });

        selectAllLabel.setVisible(false);
        selectAllLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (StompItem stompItem : stompItems) {
                    stompItem.setSelected(true);
                    selectedCandidates.add(stompItem.getTestCandidate());
                }
            }
        });

        clearFilterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearFilterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                filterModel.getIncludedMethodNames().clear();
                filterModel.getExcludedMethodNames().clear();
                filterModel.getIncludedClassNames().clear();
                filterModel.getExcludedClassNames().clear();

                updateFilterLabel();

            }
        });

        replayButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveAsMockButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        generateJUnitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reloadButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        generateJUnitButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (TestCandidateMetadata selectedCandidate : selectedCandidates) {
                    onGenerateJunitTestCaseRequest(selectedCandidate);
                }

            }
        });

        saveAsMockButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                for (TestCandidateMetadata selectedCandidate : selectedCandidates) {
                    onSaveAsMockRequest(selectedCandidate);
                }
                InsidiousNotification.notifyMessage(selectedCandidates.size() + " new mock definitions saved are " +
                        "available now", NotificationType.INFORMATION);

            }
        });


        reloadButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                {
                    resetTimeline();
                    if (candidateQueryLatch != null) {
                        candidateQueryLatch.decrementAndGet();
                    }
                    candidateQueryLatch = null;
                    loadNewCandidates();
                    itemPanel.revalidate();
                    itemPanel.repaint();
                    historyStreamScrollPanel.revalidate();
                    historyStreamScrollPanel.repaint();
                }
            }
        });

        saveReplayButton.addActionListener(e -> {
            if (saveFormReference != null) {
                insidiousService.hideCandidateSaveForm(saveFormReference);
                saveFormReference = null;
            }
            AtomicRecordService atomicRecordService = insidiousService.getProject()
                    .getService(AtomicRecordService.class);
            StoredCandidate storedCandidate = new StoredCandidate(selectedCandidates.get(0));

            for (TestCandidateMetadata testCandidate : selectedCandidates) {
                MethodUnderTest methodUnderTest = null;
                try {
                    methodUnderTest = ApplicationManager.getApplication().executeOnPooledThread(
                                    () -> ApplicationManager.getApplication().runReadAction(
                                            (Computable<MethodUnderTest>) () -> MethodUnderTest.fromMethodAdapter(
                                                    new JavaMethodAdapter(getPsiMethod(testCandidate,
                                                            insidiousService.getProject())))))
                            .get();
                } catch (InterruptedException | ExecutionException ex) {
                    continue;
//                        throw new RuntimeException(ex);
                }
                StoredCandidate candidate = atomicRecordService
                        .getStoredCandidateFor(methodUnderTest, testCandidate);
                if (candidate.getCandidateId() == null) {
                    candidate.setCandidateId(UUID.randomUUID().toString());
                }
                candidate.setName("saved on " + simpleDateFormat.format(new Date()));
                atomicRecordService.saveCandidate(methodUnderTest, candidate);

            }

            InsidiousNotification.notifyMessage("Saved " + selectedCandidates.size() + " tests",
                    NotificationType.INFORMATION);


            if (storedCandidate.getCandidateId() == null) {
                // new test case
                storedCandidate.setName(
                        "test " + storedCandidate.getMethod().getName() + " returns expected value when");
                storedCandidate.setDescription("assert that the response value matches expected value");
            }
            try {
                saveFormReference = new SaveForm(storedCandidate, new CandidateLifeListener() {
                    @Override
                    public void executeCandidate(List<StoredCandidate> metadata, ClassUnderTest classUnderTest, ReplayAllExecutionContext context, AgentCommandResponseListener<TestCandidateMetadata, String> stringAgentCommandResponseListener) {

                    }

                    @Override
                    public void displayResponse(Component responseComponent, boolean isExceptionFlow) {

                    }

                    @Override
                    public void onSaved(StoredCandidate storedCandidate) {
                        for (TestCandidateMetadata testCandidate : selectedCandidates) {
                            MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(
                                    new JavaMethodAdapter(getPsiMethod(testCandidate,
                                            StompComponent.this.insidiousService.getProject())));
                            StoredCandidate candidate = atomicRecordService.getStoredCandidateFor(methodUnderTest,
                                    testCandidate);
                            if (candidate.getCandidateId() == null) {
                                candidate.setCandidateId(UUID.randomUUID().toString());
                            }
                            candidate.setTestAssertions(storedCandidate.getTestAssertions());
                            simpleDateFormat = new SimpleDateFormat();
                            if (candidate.getName() == null) {
                                candidate.setName("saved on " + simpleDateFormat.format(new Date()));
                            }
                            atomicRecordService.saveCandidate(methodUnderTest, candidate);

                        }

                        insidiousService.hideCandidateSaveForm(saveFormReference);
                        saveFormReference = null;


                    }

                    @Override
                    public void onSaveRequest(StoredCandidate storedCandidate, AgentCommandResponse<String> agentCommandResponse) {

                    }

                    @Override
                    public void onDeleteRequest(StoredCandidate storedCandidate) {

                    }

                    @Override
                    public void onDeleted(StoredCandidate storedCandidate) {

                    }

                    @Override
                    public void onUpdated(StoredCandidate storedCandidate) {

                    }

                    @Override
                    public void onUpdateRequest(StoredCandidate storedCandidate) {

                    }

                    @Override
                    public void onGenerateJunitTestCaseRequest(StoredCandidate storedCandidate) {

                    }

                    @Override
                    public void onCandidateSelected(StoredCandidate testCandidateMetadata) {

                    }

                    @Override
                    public boolean canGenerateUnitCase(StoredCandidate candidate) {
                        return false;
                    }

                    @Override
                    public void onCancel() {
                        insidiousService.hideCandidateSaveForm(saveFormReference);
                        saveFormReference = null;
                    }

                    @Override
                    public Project getProject() {
                        return insidiousService.getProject();
                    }
                });
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }

            insidiousService.showCandidateSaveForm(saveFormReference);
        });


//        filterButton.setSize(new Dimension(98, 32));
//        filterButton.setPreferredSize(new Dimension(98, 32));
//        filterButton.setMinimumSize(new Dimension(98, 32));
//        filterButton.setMaximumSize(new Dimension(98, 32));
//        filterButton.setBorder(new RoundBtnBorder(15));
//        Font font = filterButton.getFont();
//        Font boldFont = font.deriveFont(Font.BOLD);
//        filterButton.setFont(boldFont);


        filterButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                StompFilter stompFilter = new StompFilter(filterModel);
                ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(stompFilter.getComponent(), null);

                gutterMethodComponentPopup
                        .setProject(insidiousService.getProject())
                        .setShowBorder(true)
                        .setShowShadow(true)
                        .setFocusable(true)
                        .setRequestFocus(true)
                        .setCancelOnClickOutside(true)
                        .setCancelOnOtherWindowOpen(true)
                        .setCancelKeyEnabled(true)
                        .setBelongsToGlobalPopupStack(false)
                        .setTitle("Filter")
                        .setTitleIcon(new ActiveIcon(UIUtils.FILTER_LINE))
                        .createPopup()
                        .show(new RelativePoint(e));


            }
        });


        ConnectedAndWaiting connectedAndWaiting = new ConnectedAndWaiting();
        JPanel component = (JPanel) connectedAndWaiting.getComponent();
        component.setAlignmentY(1.0f);

        scanEventListener = new SessionScanEventListener() {
            @Override
            public void started() {
                lastEventId = 0;
                stompStatusComponent.addRightStatus("last-updated", "Last updated at " + simpleTime(new Date()));
            }

            @Override
            public void waiting() {
                stompStatusComponent.addRightStatus("last-updated", "Last updated at " + simpleTime(new Date()));
                stompStatusComponent.addRightStatus("scan-progress", "Waiting");

            }

            @Override
            public void paused() {
                stompStatusComponent.addRightStatus("last-updated", "Last updated at " + simpleTime(new Date()));
                stompStatusComponent.removeRightStatus("scan-progress");
            }

            @Override
            public void ended() {
                stompStatusComponent.removeRightStatus("last-updated");
                stompStatusComponent.removeRightStatus("scan-progress");
            }

            @Override
            public void progress(ScanProgress scanProgress) {
                stompStatusComponent.addRightStatus("last-updated", "Last updated at " + simpleTime(new Date()));
                stompStatusComponent.addRightStatus("scan-progress", String.format(
                        "Scanning %d of %d", scanProgress.getCount(), scanProgress.getTotal()
                ));
            }
        };

        dateFormat = new SimpleDateFormat("HH:mm:ss");
        stompStatusComponent = new StompStatusComponent(insidiousService.getProject()
                .getService(InsidiousConfigurationState.class).getFilterModel());
        mainPanel.add(stompStatusComponent.getComponent(), BorderLayout.SOUTH);


        saveReplayButton.setEnabled(false);
        replayButton.setEnabled(false);
        replayButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (DumbService.getInstance(insidiousService.getProject()).isDumb()) {
                    InsidiousNotification.notifyMessage("Please try after indexing is complete",
                            NotificationType.WARNING);
                    return;
                }

                for (TestCandidateMetadata selectedCandidate : selectedCandidates) {
                    executeSingleTestCandidate(selectedCandidate);
                }
                InsidiousNotification.notifyMessage("Re-executed " + selectedCandidates.size() + " records",
                        NotificationType.INFORMATION);
            }
        });

        updateFilterLabel();
    }

    @Nullable
    public static PsiMethod getPsiMethod(TestCandidateMetadata selectedCandidate, Project project) {
        PsiClass classPsiElement = JavaPsiFacade
                .getInstance(project)
                .findClass(selectedCandidate.getFullyQualifiedClassname(),
                        GlobalSearchScope.allScope(project));
        MethodCallExpression mainMethod = selectedCandidate.getMainMethod();

        JvmMethod[] methodsByName = classPsiElement.findMethodsByName(mainMethod.getMethodName());

        for (JvmMethod jvmMethod : methodsByName) {

            List<Parameter> expectedArguments = mainMethod.getArguments();
            JvmParameter[] actualArguments = jvmMethod.getParameters();
            if (expectedArguments.size() == actualArguments.length) {

                boolean mismatch = false;
                for (int i = 0; i < expectedArguments.size(); i++) {
                    Parameter expectedArgument = expectedArguments.get(i);
                    JvmParameter actualArgument = actualArguments[i];
                    JvmType type = actualArgument.getType();
                    if (type instanceof PsiType) {
                        if (!((PsiType) type).getCanonicalText().contains(expectedArgument.getType())) {
                            mismatch = true;
                            break;
                        }
                    }

                }
                if (mismatch) {
                    continue;
                }

                return (PsiMethod) jvmMethod.getSourceElement();


            }

        }
        return null;
    }

    private void executeSingleTestCandidate(TestCandidateMetadata selectedCandidate) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            PsiMethod methodPsiElement = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiMethod>) () -> getPsiMethod(selectedCandidate, insidiousService.getProject()));
            long batchTime = System.currentTimeMillis();

            insidiousService.executeSingleCandidate(
                    new StoredCandidate(selectedCandidate),
                    new ClassUnderTest(selectedCandidate.getFullyQualifiedClassname()),
                    ExecutionRequestSourceType.Bulk,
                    (testCandidate, agentCommandResponse, diffResult) -> {
                        if (agentCommandResponse.getResponseType() == ResponseType.FAILED) {
                            InsidiousNotification.notifyMessage(agentCommandResponse.getMessage(),
                                    NotificationType.WARNING);
                        }
                    },
                    new JavaMethodAdapter(methodPsiElement)
            );
        });
    }

    public String simpleTime(Date currentDate) {
        return dateFormat.format(currentDate);
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    synchronized public void accept(final List<TestCandidateMetadata> testCandidateMetadataList) {


        if (!welcomePanelRemoved) {
            historyStreamScrollPanel.setVisible(true);
            welcomePanelRemoved = true;
        }

        for (TestCandidateMetadata testCandidateMetadata : testCandidateMetadataList) {
            if (isAcceptable(testCandidateMetadata)) {
                incomingQueue.offer(testCandidateMetadata);
            }
        }

    }

    private boolean isAcceptable(TestCandidateMetadata testCandidateMetadata) {
        if (filterModel.getIncludedClassNames().size() > 0) {
            if (!filterModel.getIncludedClassNames().contains(testCandidateMetadata.getFullyQualifiedClassname())) {
                return false;
            }
        }
        if (filterModel.getIncludedMethodNames().size() > 0) {
            if (!filterModel.getIncludedMethodNames().contains(testCandidateMetadata.getMainMethod().getMethodName())) {
                return false;
            }
        }
        if (filterModel.getExcludedClassNames().size() > 0) {
            if (filterModel.getExcludedClassNames().contains(testCandidateMetadata.getFullyQualifiedClassname())) {
                return false;
            }
        }
        if (filterModel.getExcludedMethodNames().size() > 0) {
            if (filterModel.getExcludedMethodNames().contains(testCandidateMetadata.getMainMethod().getMethodName())) {
                return false;
            }
        }
        return true;
    }

    private synchronized void addCandidateToUi(TestCandidateMetadata testCandidateMetadata, int index) {
        StompItem stompItem = new StompItem(testCandidateMetadata, this, insidiousService);

        JScrollBar verticalScrollBar = historyStreamScrollPanel.getVerticalScrollBar();
        int scrollPosition = verticalScrollBar.getValue();

        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BorderLayout());

        stompItems.add(stompItem);

        final JPanel component = stompItem.getComponent();

        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                logger.warn("stomp item mouse click -> " + e.getButton());
                super.mouseClicked(e);
            }
        });
        candidateMetadataStompItemMap.put(testCandidateMetadata, component);
        component.setMaximumSize(new Dimension(500, COMPONENT_HEIGHT));
        component.setMinimumSize(new Dimension(300, COMPONENT_HEIGHT));


        rowPanel.add(component, BorderLayout.CENTER);

        JPanel dateAndTimePanel = createDateAndTimePanel(createTimeLineComponent(),
                Date.from(Instant.ofEpochMilli(testCandidateMetadata.getCreatedAt())));
        rowPanel.add(dateAndTimePanel, BorderLayout.EAST);


        makeSpace(index);
        GridBagConstraints gbcForLeftMainComponent = createGBCForLeftMainComponent();
        gbcForLeftMainComponent.gridy = index;
        itemPanel.add(rowPanel, gbcForLeftMainComponent);
        itemPanel.revalidate();
        itemPanel.repaint();
        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();

        if (!clearTimelineLabel.isVisible()) {
            clearTimelineLabel.setVisible(true);
        }

        // Restore the scroll position after adding the component
//        double newPanelSize = component.getSize().getHeight();
//        int newScrollPosition = (int) (scrollPosition + newPanelSize);
//        logger.warn("setting scroll position to [1]" + scrollPosition + "+" + newPanelSize
//                + " => " + newScrollPosition);
//        verticalScrollBar.setValue(newScrollPosition);
    }

    private GridBagConstraints createGBCForLeftMainComponent() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = JBUI.insetsBottom(0);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        return gbc;
    }


    private GridBagConstraints createGBCForProcessStartedComponent() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = JBUI.insetsBottom(0);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        return gbc;
    }

    private GridBagConstraints createGBCForFakeComponent() {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;

        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = JBUI.insetsBottom(8);
        gbc.ipadx = 0;
        gbc.ipady = 0;
        return gbc;
    }

//    private GridBagConstraints createGBCForDateAndTimePanel() {
//        GridBagConstraints gbc1 = new GridBagConstraints();
//
//        gbc1.gridx = 1;
//        gbc1.gridy = GridBagConstraints.RELATIVE;
//        gbc1.gridwidth = 1;
//        gbc1.gridheight = 1;
//
//        gbc1.weightx = 0.1;
//        gbc1.weighty = 0.3;
//        gbc1.anchor = GridBagConstraints.LINE_START;
//        gbc1.fill = GridBagConstraints.BOTH;
//
//        gbc1.insets = JBUI.emptyInsets();
//        gbc1.ipadx = 0;
//        gbc1.ipady = 0;
//        return gbc1;
//    }

//    private GridBagConstraints createGBCForLinePanel() {
//        GridBagConstraints gbc1 = new GridBagConstraints();
//
//        gbc1.gridx = 1;
//        gbc1.gridy = GridBagConstraints.RELATIVE;
//        gbc1.gridwidth = 1;
//        gbc1.gridheight = 1;
//
//        gbc1.weightx = 0.1;
//        gbc1.weighty = 0;
//        gbc1.anchor = GridBagConstraints.LINE_START;
//        gbc1.fill = GridBagConstraints.VERTICAL;
//
//        gbc1.insets = JBUI.insets(0, 16, 0, 0);
//        gbc1.ipadx = 0;
//        gbc1.ipady = 0;
//        return gbc1;
//    }

    private JPanel createDateAndTimePanel(JPanel lineContainer, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        JPanel comp = new JPanel();
        BorderLayout mgr = new BorderLayout();
        comp.setLayout(mgr);
        JLabel hello = new JLabel(String.format("          %s", sdf.format(date)));
        Font font = hello.getFont();
        hello.setFont(font.deriveFont(10.0f));
        hello.setForeground(Color.decode("#8C8C8C"));
        hello.setUI(new VerticalLabelUI(true));
        comp.add(hello, BorderLayout.CENTER);


        comp.add(lineContainer, BorderLayout.WEST);
        comp.setSize(new Dimension(50, -1));
        comp.setPreferredSize(new Dimension(50, -1));
        return comp;
    }

    private JPanel createLinePanel(JPanel lineContainer) {
        JPanel comp = new JPanel();
        BorderLayout mgr = new BorderLayout();
        comp.setLayout(mgr);
        JLabel comp1 = new JLabel("      ");
        comp1.setUI(new VerticalLabelUI(true));

//        comp.add(comp1, BorderLayout.CENTER);
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 4));
        comp.add(lineContainer, BorderLayout.WEST);
        comp.setSize(new Dimension(50, -1));
        comp.setPreferredSize(new Dimension(50, -1));

        return comp;
    }

    private JPanel createLineComponent() {
        JPanel lineContainer = new JPanel();
        lineContainer.setLayout(new GridBagLayout());
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.fill = GridBagConstraints.BOTH;
        lineContainer.add(createSeparator(), constraints);

        return lineContainer;
    }

    private JPanel createTimeLineComponent() {
        JPanel lineContainer = new JPanel();
        lineContainer.setLayout(new GridBagLayout());
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));

        JLabel comp1 = new JLabel();
        comp1.setIcon(UIUtils.CIRCLE_EMPTY);
        comp1.setMaximumSize(new Dimension(16, 16));
        comp1.setMinimumSize(new Dimension(16, 16));
        comp1.setPreferredSize(new Dimension(16, 16));

        JPanel comp2 = new JPanel();
        comp2.add(comp1);

        comp2.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.fill = GridBagConstraints.VERTICAL;

        lineContainer.add(createSeparator(), constraints);
        GridBagConstraints constraints1 = new GridBagConstraints();
        constraints1.gridx = 0;
        constraints1.gridy = 1;
        constraints1.weightx = 0;
        constraints1.weighty = 0;
        constraints1.anchor = GridBagConstraints.CENTER;
        constraints1.fill = GridBagConstraints.NONE;

        lineContainer.add(comp2, constraints1);
        GridBagConstraints constraints2 = new GridBagConstraints();
        constraints2.gridx = 0;
        constraints2.gridy = 2;
        constraints2.weightx = 0;
        constraints2.weighty = 1;
        constraints2.anchor = GridBagConstraints.SOUTH;
        constraints2.fill = GridBagConstraints.VERTICAL;
        lineContainer.add(createSeparator(), constraints2);
        return lineContainer;
    }

    @NotNull
    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        Dimension size = new Dimension(2, (COMPONENT_HEIGHT / 4) - 2); // Set preferred size for the separator
        separator.setForeground(Color.decode("#D9D9D9"));
        separator.setPreferredSize(size);
        separator.setMaximumSize(size);
        separator.setMinimumSize(size);
        return separator;
    }

    @Override
    public void executeCandidate(List<TestCandidateMetadata> metadata,
                                 ClassUnderTest classUnderTest, ExecutionRequestSourceType source,
                                 AgentCommandResponseListener<TestCandidateMetadata, String> responseListener) {

        if (source == ExecutionRequestSourceType.Single) {
            TestCandidateMetadata selectedCandidate = metadata.get(0);
            PsiMethod methodPsiElement = ApplicationManager.getApplication()
                    .runReadAction((Computable<PsiMethod>) () -> getPsiMethod(selectedCandidate,
                            insidiousService.getProject()));
            showDirectInvoke(new JavaMethodAdapter(methodPsiElement));
            directInvokeComponent.triggerExecute();
        }

    }

    @Override
    public void displayResponse(Component responseComponent, boolean isExceptionFlow) {

    }

    @Override
    public void onSaved(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onSelected(TestCandidateMetadata storedCandidate) {
        this.selectedCandidates.add(storedCandidate);
        updateControlPanel();
    }

    private void updateControlPanel() {

        selectedCountLabel.setText(selectedCandidates.size() + " selected");
        if (selectedCandidates.size() > 0 && !controlPanel.isEnabled()) {
//            reloadButton.setEnabled(true);

            clearSelectionLabel.setVisible(true);
            selectAllLabel.setVisible(true);


            generateJUnitButton.setEnabled(true);
            saveAsMockButton.setEnabled(true);
            replayButton.setEnabled(true);
            saveReplayButton.setEnabled(true);
            controlPanel.setEnabled(true);
//            setLabelsVisible(true);
        } else if (selectedCandidates.size() == 0 && controlPanel.isEnabled()) {
            selectedCountLabel.setText("None selected");
            clearSelectionLabel.setVisible(false);
            selectAllLabel.setVisible(false);


//            reloadButton.setEnabled(false);
            generateJUnitButton.setEnabled(false);
            saveAsMockButton.setEnabled(false);
            replayButton.setEnabled(false);
            saveReplayButton.setEnabled(false);
            controlPanel.setEnabled(false);
//            setLabelsVisible(false);
        }
    }

    @Override
    public void unSelected(TestCandidateMetadata storedCandidate) {
        this.selectedCandidates.remove(storedCandidate);
        updateControlPanel();
    }

    @Override
    public void onSaveAsTestRequest(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onSaveAsMockRequest(TestCandidateMetadata testCandidateMetadata) {

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            PsiMethod targetMethodPsi = getPsiMethod(testCandidateMetadata, insidiousService.getProject());
            MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(new JavaMethodAdapter(targetMethodPsi));
            DeclaredMock declaredMock = new DeclaredMock();
            declaredMock.setMethodName(methodUnderTest.getName());
            declaredMock.setName("mock recorded on " + testCandidateMetadata.getCreatedAt());
            declaredMock.setFieldName("*");
            declaredMock.setId(UUID.randomUUID().toString());
            declaredMock.setFieldTypeName(methodUnderTest.getClassName());
            declaredMock.setSourceClassName("*");

            List<ParameterMatcher> whenParameter = new ArrayList<>();
            for (Parameter argument : testCandidateMetadata.getMainMethod().getArguments()) {
                ParameterMatcher parameterMatcher = new ParameterMatcher();
                parameterMatcher.setType(ParameterMatcherType.EQUAL);
                parameterMatcher.setName(argument.getName());
                parameterMatcher.setValue(argument.getStringValue());
                whenParameter.add(parameterMatcher);
            }


            declaredMock.setWhenParameter(whenParameter);
            ThenParameter thenParameter = new ThenParameter();

            Parameter returnValue = testCandidateMetadata.getMainMethod().getReturnValue();
            if (returnValue.isException()) {
                thenParameter.setMethodExitType(MethodExitType.EXCEPTION);
            } else if (returnValue.getValue() == 0) {
                thenParameter.setMethodExitType(MethodExitType.NULL);
            } else {
                thenParameter.setMethodExitType(MethodExitType.NORMAL);
            }
            ReturnValue returnParameter = new ReturnValue();
            returnParameter.setClassName(returnValue.getType());
            returnParameter.setReturnValueType(ReturnValueType.REAL);
            returnParameter.setValue(returnValue.getStringValue());
            thenParameter.setReturnParameter(returnParameter);


            List<ThenParameter> thenParameter1 = new ArrayList<>();
            thenParameter1.add(thenParameter);
            declaredMock.setThenParameter(thenParameter1);
            insidiousService.saveMockDefinition(declaredMock, methodUnderTest);
        });

    }

    @Override
    public void onDeleteRequest(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onDeleted(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onUpdated(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onUpdateRequest(TestCandidateMetadata storedCandidate) {

    }

    @Override
    public void onGenerateJunitTestCaseRequest(TestCandidateMetadata storedCandidate) {

        TestCaseGenerationConfiguration generationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUnit5, MockFramework.Mockito, JsonFramework.Gson, ResourceEmbedMode.IN_FILE
        );
        TestCaseService testCaseService = insidiousService.getTestCaseService();

        for (TestCandidateMetadata testCandidateShell : selectedCandidates) {

            try {

                TestCandidateMetadata loadedTestCandidate = insidiousService.getSessionInstance()
                        .getTestCandidateById(testCandidateShell.getEntryProbeIndex(), true);
                Parameter testSubject = loadedTestCandidate.getTestSubject();
                if (testSubject.isException()) {
                    continue;
                }
                MethodCallExpression callExpression = loadedTestCandidate.getMainMethod();
                logger.warn(
                        "Generating test case: " + testSubject.getType() + "." + callExpression.getMethodName() + "()");
                generationConfiguration.getTestCandidateMetadataList().clear();
                generationConfiguration.getTestCandidateMetadataList().add(loadedTestCandidate);

                generationConfiguration.getCallExpressionList().clear();
                generationConfiguration.getCallExpressionList().addAll(loadedTestCandidate.getCallsList());

                TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
                List<TestCaseUnit> testCaseUnit1 = new ArrayList<>();
                testCaseUnit1.add(testCaseUnit);
                TestSuite testSuite = new TestSuite(testCaseUnit1);
                insidiousService.getJUnitTestCaseWriter().saveTestSuite(testSuite);

            } catch (Exception e) {
                logger.error("Failed to generate test case", e);
                InsidiousNotification.notifyMessage(
                        "Failed to generate test case for [" + testCandidateShell.getTestSubject()
                                .getType() + "] " + e.getMessage(), NotificationType.ERROR);
            }
        }


    }

    @Override
    public void onCandidateSelected(TestCandidateMetadata testCandidateMetadata, MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {

            DefaultTableModel dm = new DefaultTableModel();
            dm.addColumn("col 1");
            dm.addColumn("col 2");
            dm.addRow(new Object[]{"row 1-1", "row 1-2"});
            dm.addRow(new Object[]{"row 2-1", "row 2-2"});
            JTable table = new JBTable(dm);
            String fullyQualifiedClassname = testCandidateMetadata.getFullyQualifiedClassname();
            String methodName = testCandidateMetadata.getMainMethod().getMethodName();
            IPopupChooserBuilder<String> chooserPopUp = JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(Arrays.asList(
                            "Include class " + fullyQualifiedClassname,
                            "Exclude class " + fullyQualifiedClassname,
                            "Include method " + methodName,
                            "Exclude method " + methodName
                    ));

            chooserPopUp
                    .setRequestFocus(true)
                    .setItemChosenCallback(selection -> {

                        logger.warn("selected filter [" + selection + "]");
                        String[] parts = selection.split(" ");
                        switch (parts[0]) {
                            case "Include":

                                switch (parts[1]) {
                                    case "class":
                                        filterModel.getIncludedClassNames().add(fullyQualifiedClassname);
                                        filterModel.getExcludedClassNames().remove(fullyQualifiedClassname);
                                        break;
                                    case "method":
                                        filterModel.getIncludedMethodNames().add(methodName);
                                        filterModel.getExcludedMethodNames().remove(methodName);
                                        break;
                                }


                                break;
                            case "Exclude":

                                switch (parts[1]) {
                                    case "class":
                                        filterModel.getIncludedClassNames().remove(fullyQualifiedClassname);
                                        filterModel.getExcludedClassNames().add(fullyQualifiedClassname);
                                        break;
                                    case "method":
                                        filterModel.getIncludedMethodNames().remove(methodName);
                                        filterModel.getExcludedMethodNames().add(methodName);
                                        break;
                                }


                                break;
                        }
                        updateFilterLabel();
                        resetTimeline();
                        loadNewCandidates();

                    })
                    .setCancelOnClickOutside(true)
//                    .setCancelKeyEnabled(true)
                    .createPopup()
                    .show(new RelativePoint(e));

        }
    }

    private void updateFilterLabel() {
        StringBuilder tooltipText = new StringBuilder();
        int total = filterModel.getIncludedClassNames().size()
                + filterModel.getIncludedMethodNames().size()
                + filterModel.getExcludedClassNames().size()
                + filterModel.getExcludedMethodNames().size();
        if (total == 0) {
            clearFilterLabel.setVisible(false);
            filterAppliedLabel.setVisible(false);
        } else {
            clearFilterLabel.setVisible(true);
            filterAppliedLabel.setVisible(true);
            filterAppliedLabel.setText(total + (total == 1 ? " filter" : " filters"));
        }

        itemPanel.revalidate();
        itemPanel.repaint();
        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();


    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onExpandChildren(TestCandidateMetadata candidateMetadata) {
        try {
            List<TestCandidateMetadata> childCandidates =
                    insidiousService.getTestCandidateBetween(
                            candidateMetadata.getMainMethod().getEntryProbe().getEventId(),
                            candidateMetadata.getMainMethod().getReturnDataEvent().getEventId());

            Component component = candidateMetadataStompItemMap.get(candidateMetadata);
            int parentIndex = itemPanel.getComponentZOrder(component);

            for (TestCandidateMetadata childCandidate : childCandidates) {
                addCandidateToUi(childCandidate, parentIndex);
            }


        } catch (SQLException e) {
            InsidiousNotification.notifyMessage("Failed to load child calls: " + e.getMessage(),
                    NotificationType.ERROR);
            throw new RuntimeException(e);
        }
    }

    // removes the visible items on the timeline
    public void clear() {
        List<Component> itemsToNotDelete = new ArrayList<>();
        List<StompItem> pinnedStomps = new ArrayList<>();
        for (StompItem stompItem : stompItems) {
            if (stompItem.isPinned()) {
                pinnedStomps.add(stompItem);
                stompItem.setSelected(false);
                itemsToNotDelete.add(stompItem.getComponent().getParent());
            }
        }
        List<Component> allComponents = new ArrayList<>(Arrays.asList(itemPanel.getComponents()));
        allComponents.removeAll(itemsToNotDelete);
        for (Component component : allComponents) {
            itemPanel.remove(component);
        }
        normalizeItemPanelComponents();


        GridBagConstraints gbcForFakeComponent = createGBCForFakeComponent();
        gbcForFakeComponent.gridy = itemPanel.getComponentCount();
        itemPanel.add(new JPanel(), gbcForFakeComponent, itemPanel.getComponentCount());

        selectedCandidates.clear();
        updateControlPanel();
        stompItems.clear();
        stompItems.addAll(pinnedStomps);

        itemPanel.revalidate();
        itemPanel.repaint();
        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();

    }


    public void resetTimeline() {
        lastEventId = 0;

        List<Component> itemsToNotDelete = new ArrayList<>();
        List<StompItem> pinnedStomps = new ArrayList<>();
        for (StompItem stompItem : stompItems) {
            if (stompItem.isPinned()) {
                pinnedStomps.add(stompItem);
                stompItem.setSelected(false);
                itemsToNotDelete.add(stompItem.getComponent().getParent());
            }
        }
        List<Component> allComponents = new ArrayList<>(Arrays.asList(itemPanel.getComponents()));
        allComponents.removeAll(itemsToNotDelete);
        for (Component component : allComponents) {
            itemPanel.remove(component);
        }
        normalizeItemPanelComponents();


        GridBagConstraints gbcForFakeComponent = createGBCForFakeComponent();
        gbcForFakeComponent.gridy = itemPanel.getComponentCount();
        itemPanel.add(new JPanel(), gbcForFakeComponent, itemPanel.getComponentCount());

        selectedCandidates.clear();
        updateControlPanel();
        stompItems.clear();
        stompItems.addAll(pinnedStomps);

        itemPanel.revalidate();
        itemPanel.repaint();
        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();

    }

    private void normalizeItemPanelComponents() {
        Component[] components = itemPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            GridBagConstraints gbc = ((GridBagLayout) itemPanel.getLayout()).getConstraints(component);
            gbc.gridy = i;
            itemPanel.add(component, gbc);
        }
    }

    public void disconnected() {
        candidateQueryLatch.decrementAndGet();
        candidateQueryLatch = null;
        stompStatusComponent.setDisconnected();
        scanEventListener.ended();
    }

    public void showDirectInvoke(MethodAdapter method) {
        if (directInvokeComponent == null) {
            directInvokeComponent = new MethodDirectInvokeComponent(insidiousService, this);
            JComponent content = directInvokeComponent.getContent();
            content.setMinimumSize(new Dimension(-1, 400));
            content.setMaximumSize(new Dimension(-1, 400));
//            content.setPreferredSize(new Dimension(-1, 600));
            southPanel.add(content, BorderLayout.CENTER);
//            mainPanel.revalidate();
//            mainPanel.repaint();
        }
        try {
            directInvokeComponent.renderForMethod(method, null);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();
    }

    void makeSpace(int position) {
        Component[] components = itemPanel.getComponents();
        for (Component component : components) {
            GridBagConstraints gbc = ((GridBagLayout) itemPanel.getLayout()).getConstraints(component);
            if (gbc.gridy < position) {
                continue;
            }
            gbc.gridy += 1;
            itemPanel.add(component, gbc);
        }
    }

    public void removeDirectInvoke() {
        southPanel.remove(directInvokeComponent.getContent());
        directInvokeComponent = null;
        southPanel.revalidate();
        southPanel.repaint();
    }

    public void setConnectedAndWaiting() {
        if (!welcomePanelRemoved) {
            historyStreamScrollPanel.setVisible(true);
            welcomePanelRemoved = true;
        }

        JLabel process_started = new JLabel("Process started");
        process_started.setIcon(UIUtils.LINK);
        JPanel startedPanel = new JPanel();
        startedPanel.setLayout(new BorderLayout());
        process_started.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new JBColor(
                                new Color(35, 103, 30),
                                new Color(35, 103, 30)), 2, true),
                        BorderFactory.createEmptyBorder(6, 6, 6, 6)
                )
        );
        startedPanel.add(process_started, BorderLayout.EAST);

        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BorderLayout());


        rowPanel.add(startedPanel, BorderLayout.CENTER);
        rowPanel.add(createLinePanel(createLineComponent()), BorderLayout.EAST);
        makeSpace(0);
        rowPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        itemPanel.add(rowPanel, createGBCForProcessStartedComponent());

        stompStatusComponent.setConnected();
    }

    public synchronized void loadNewCandidates() {
        if (candidateQueryLatch != null) {
            return;
        }
        candidateQueryLatch = insidiousService.getSessionInstance().getTestCandidates(this, lastEventId);
    }

    public SessionScanEventListener getScanEventListener() {
        return scanEventListener;
    }

    @Override
    public void onClose(MethodDirectInvokeComponent methodDirectInvokeComponent) {
        removeDirectInvoke();

    }

    @Override
    public void onExpand(AFewCallsLater aFewCallsLater) {
        List<MethodCallExpression> calls = insidiousService.getMethodCallsBetween(
                aFewCallsLater.getGapStartIndex(),
                aFewCallsLater.getGapEndIndex());

        Container parent = aFewCallsLater.getComponent().getParent();
        GridBagConstraints gbc = ((GridBagLayout) itemPanel.getLayout()).getConstraints(parent);
        int position = gbc.gridy;


        for (com.insidious.plugin.pojo.MethodCallExpression call : calls) {
            TestCandidateMetadata testCandidateMetadata = new TestCandidateMetadata();
            testCandidateMetadata.setMainMethod(call);
            testCandidateMetadata.setTestSubject(call.getSubject());
            addCandidateToUi(testCandidateMetadata, position);
        }
        itemPanel.remove(parent);

    }

    @Override
    public void run() {
        try {
            while (true) {
                final TestCandidateMetadata testCandidateMetadata = incomingQueue.take();
                CountDownLatch pollerCDL = new CountDownLatch(1);
                ApplicationManager.getApplication().invokeLater(() -> {
                    acceptSingle(testCandidateMetadata);
                    pollerCDL.countDown();
                });

                pollerCDL.await();
            }


        } catch (InterruptedException e) {
            // just end
        } finally {
        }

    }

    private void acceptSingle(TestCandidateMetadata testCandidateMetadata) {
        if (testCandidateMetadata.getExitProbeIndex() > lastEventId) {
            lastEventId = testCandidateMetadata.getExitProbeIndex();
        }
        if (stompItems.size() > 0) {

            StompItem last = stompItems.get(stompItems.size() - 1);
            long gapStartIndex = last.getTestCandidate().getExitProbeIndex();
            long gapEndIndex = testCandidateMetadata.getEntryProbeIndex();
            int count = insidiousService.getMethodCallCountBetween(gapStartIndex, gapEndIndex);
            if (count > 0) {
                AFewCallsLater aFewCallsLater = new AFewCallsLater(gapStartIndex, gapEndIndex, count,
                        this);

                JScrollBar verticalScrollBar = historyStreamScrollPanel.getVerticalScrollBar();
                int scrollPosition = verticalScrollBar.getValue();

                JPanel labelPanel = aFewCallsLater.getComponent();

                JPanel rowPanel = new JPanel(new BorderLayout());
                rowPanel.add(labelPanel, BorderLayout.CENTER);
                rowPanel.add(createLinePanel(createLineComponent()), BorderLayout.EAST);
                GridBagConstraints gbcForLeftMainComponent = createGBCForLeftMainComponent();
                makeSpace(0);
                itemPanel.add(rowPanel, gbcForLeftMainComponent, 0);

            }
        }
        addCandidateToUi(testCandidateMetadata, 0);
        itemPanel.revalidate();
    }

    public void setSession(ExecutionSession executionSession) {
        if (candidateQueryLatch != null) {
            candidateQueryLatch.decrementAndGet();
            candidateQueryLatch = null;
        }
    }
}
