package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.client.ScanProgress;
import com.insidious.plugin.client.SessionInstanceInterface;
import com.insidious.plugin.client.SessionScanEventListener;
import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.SemanticVersion;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.ui.UnloggedOnboardingScreenV2;
import com.insidious.plugin.ui.UnloggedSDKOnboarding;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.insidious.plugin.ui.methodscope.MethodDirectInvokeComponent;
import com.insidious.plugin.ui.mocking.MockDefinitionEditor;
import com.insidious.plugin.ui.mocking.OnSaveListener;
import com.insidious.plugin.upload.ExecutionSessionSource;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.ui.GotItTooltip;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class StompComponent implements
        Consumer<List<TestCandidateBareBone>>,
        TestCandidateLifeListener,
        ComponentLifecycleListener<MethodDirectInvokeComponent>,
        Runnable, Disposable {
    public static final int COMPONENT_HEIGHT = 93;
    public static final int MAX_ITEM_TO_DISPLAY = 50;
    private static final Logger logger = LoggerUtil.getInstance(StompComponent.class);
    private final InsidiousService insidiousService;
    private final JPanel itemPanel;
    private final StompStatusComponent stompStatusComponent;
    private final List<TestCandidateBareBone> selectedCandidates = new ArrayList<>();
    private final List<StompItem> stompItems = new ArrayList<>(500);
    private final SessionScanEventListener scanEventListener;
    private final SimpleDateFormat dateFormat;
    private final Map<TestCandidateBareBone, Component> candidateMetadataStompItemMap = new HashMap<>();
    private final InsidiousConfigurationState configurationState;
    private final StompFilterModel stompFilterModel;
    private final Set<Long> pinnedItems = new HashSet<>();
    private final ActionToolbarImpl actionToolbar;
    private final UnloggedSDKOnboarding unloggedSDKOnboarding;
    private final Map<String, AtomicInteger> countByMethodName = new HashMap<>();
    private final AnAction filterAction;
    private final Project project;
    BlockingQueue<TestCandidateBareBone> incomingQueue = new ArrayBlockingQueue<>(100);
    int totalAcceptedCount = 0;
    private JPanel mainPanel;
    private JPanel northPanelContainer;
    private JScrollPane historyStreamScrollPanel;
    private JPanel scrollContainer;
    //    private JLabel reloadButton;
//    private JButton saveReplayButton;
    //    private JLabel replayButton;
//    private JLabel generateJUnitButton;
    private JPanel controlPanel;
    private JPanel infoPanel;
    private JLabel selectedCountLabel;
    private JLabel clearSelectionLabel;
    private JPanel southPanel;
    private JLabel clearFilterLabel;
    private JLabel filterAppliedLabel;
    private JPanel actionToolbarContainer;
    private JPanel timelineControlPanel;
    private JPanel topContainerPanel;
    private JSplitPane splitPane;
    private JLabel sourceLabelFilter;
    private long lastEventId = 0;
    private MethodDirectInvokeComponent directInvokeComponent = null;
    private TestCandidateSaveForm saveFormReference;
    private boolean welcomePanelRemoved = false;
    private AtomicInteger candidateQueryLatch;
    private MethodUnderTest lastMethodFocussed;
    private boolean shownGotItNofiticaton = false;
    private SessionInstanceInterface sessionInstance;

    public StompComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.project = insidiousService.getProject();
        configurationState = project.getService(InsidiousConfigurationState.class);

//        filterAppliedLabel.setVisible(false);
        stompFilterModel = configurationState.getFilterModel();


        itemPanel = new JPanel();
        GridBagLayout mgr = new GridBagLayout();
        itemPanel.setLayout(mgr);
        itemPanel.setAlignmentY(0);
        itemPanel.setAlignmentX(0);

        itemPanel.add(new JPanel(), createGBCForFakeComponent());

        itemPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));


        historyStreamScrollPanel.setViewportView(itemPanel);
        itemPanel.setDoubleBuffered(true);
        historyStreamScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        JScrollBar verticalScrollBar = historyStreamScrollPanel.getVerticalScrollBar();
        verticalScrollBar.setUnitIncrement(16); // Adjust as needed

        scrollContainer.setBorder(BorderFactory.createEmptyBorder());

        AnAction saveAction = new AnAction(() -> "Save", AllIcons.Actions.MenuSaveall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                DumbService.getInstance(project).runWhenSmart(() -> {
                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("count", selectedCandidates.size());
                    UsageInsightTracker.getInstance().RecordEvent("ACTION_SAVE", eventProperties);
                    saveSelected();
                });
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };


        AnAction generateJunitTestAction = new AnAction(() -> "JUnit", AllIcons.Scope.Tests) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                createJunitFromSelected();
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }


        };

//        AnAction replaySelectionAction = new AnAction(() -> "Replay", AllIcons.Actions.RestartFrame) {
//            @Override
//            public void actionPerformed(@NotNull AnActionEvent e) {
//                System.err.println("replay selected");
//                if (selectedCandidates.size() < 1) {
//                    InsidiousNotification.notifyMessage("Select records to replay", NotificationType.INFORMATION);
//                    return;
//                }
//                JSONObject eventProperties = new JSONObject();
//                eventProperties.put("count", selectedCandidates.size());
//                UsageInsightTracker.getInstance().RecordEvent("ACTION_REPLAY_SELECT", eventProperties);
//
//                InsidiousNotification.notifyMessage("Replayed " + selectedCandidates.size() + " records",
//                        NotificationType.INFORMATION);
//                for (TestCandidateBareBone selectedCandidate : selectedCandidates) {
//                    executeSingleTestCandidate(selectedCandidate);
//                }
//            }
//
//            @Override
//            public boolean displayTextInToolbar() {
//                return true;
//            }
//        };


        sourceLabelFilter.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showFiltersComponentPopup(project, insidiousService, 1);
            }
        });

        filterAction = new AnAction(() -> "Filter", AllIcons.General.Filter) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                JSONObject eventProperties = new JSONObject();
                eventProperties.put("count", selectedCandidates.size());
                UsageInsightTracker.getInstance().RecordEvent("ACTION_FILTER", eventProperties);

                showFiltersComponentPopup(project, insidiousService, 0);
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };


        List<AnAction> action11 = List.of(
                filterAction,
//                replaySelectionAction,
                generateJunitTestAction,
                saveAction
        );


        actionToolbar = new ActionToolbarImpl(
                "Live View", new DefaultActionGroup(action11), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);


        AnAction reloadAction = new AnAction(() -> "Reload", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                System.err.println("reload timeline");
                resetAndReload();
            }
        };

        AnAction clearAction = new AnAction(() -> "Clear", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                UsageInsightTracker.getInstance().RecordEvent("RESET_TIMELINE", null);
                System.err.println("clear timeline");
                resetTimeline();
            }
        };

        AnAction selectAllAction = new AnAction(() -> "Select All", AllIcons.Actions.Selectall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                selectAll();
            }
        };


        List<AnAction> action22 = List.of(
                reloadAction,
                clearAction,
                selectAllAction
        );


        ActionToolbarImpl timelineToolbar = new ActionToolbarImpl(
                "Timeline", new DefaultActionGroup(action22), true);
        timelineToolbar.setMiniMode(false);
        timelineToolbar.setForceMinimumSize(true);
        timelineToolbar.setTargetComponent(mainPanel);

        timelineControlPanel.add(timelineToolbar.getComponent(), BorderLayout.CENTER);


        actionToolbarContainer.add(actionToolbar.getComponent(), BorderLayout.CENTER);

        clearSelectionLabel.setVisible(false);
        clearSelectionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        clearSelectionLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        clearSelectionLabel.setForeground(new Color(84, 138, 247));
        clearSelectionLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCandidates.clear();
                for (StompItem stompItem : stompItems) {
                    stompItem.setSelected(false);
                }
                updateControlPanel();
            }
        });


        clearFilterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearFilterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JSONObject eventProperties = new JSONObject();
                eventProperties.put("filter", stompFilterModel.toString());
                UsageInsightTracker.getInstance().RecordEvent("CLEAR_FILTER", eventProperties);

                clearFilter();
                updateFilterLabel();
                resetAndReload();

            }

        });


//        ConnectedAndWaiting connectedAndWaiting = new ConnectedAndWaiting();
//        JPanel component = (JPanel) connectedAndWaiting.getComponent();
//        component.setAlignmentY(1.0f);

        scanEventListener = new SessionScanEventListener() {
            @Override
            public void started() {
                logger.warn("setConnectedAndWaiting: " + StompComponent.this.hashCode());
                lastEventId = 0;
                setConnectedAndWaiting();
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
            public synchronized void ended() {
                disconnected();
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
        stompStatusComponent = new StompStatusComponent(project
                .getService(InsidiousConfigurationState.class).getFilterModel());
        mainPanel.add(stompStatusComponent.getComponent(), BorderLayout.SOUTH);


        unloggedSDKOnboarding = new UnloggedSDKOnboarding(insidiousService);
        itemPanel.removeAll();
        itemPanel.add(unloggedSDKOnboarding.getComponent(), createGBCForProcessStartedComponent());
        itemPanel.add(new JPanel(), createGBCForFakeComponent(), itemPanel.getComponentCount());

//        saveReplayButton.setEnabled(false);
        updateFilterLabel();
        directInvokeComponent = new MethodDirectInvokeComponent(insidiousService, this);
        JComponent content = directInvokeComponent.getContent();
        content.setMinimumSize(new Dimension(-1, 400));
        content.setMaximumSize(new Dimension(-1, 500));

    }

    private void selectAll() {
        if (selectedCandidates.size() != stompItems.size()) {
            selectedCandidates.clear();
            for (StompItem stompItem : stompItems) {
                stompItem.setSelected(true);
                selectedCandidates.add(stompItem.getTestCandidate());
            }
        } else {
            selectedCandidates.clear();
            for (StompItem stompItem : stompItems) {
                stompItem.setSelected(false);
            }
        }
        updateControlPanel();
    }

    private void createJunitFromSelected() {
        System.err.println("generate junit test");
        if (selectedCandidates.isEmpty()) {
            InsidiousNotification.notifyMessage("Select records to generate JUnit Test",
                    NotificationType.INFORMATION);
            return;
        }

        if (DumbService.getInstance(project).isDumb()) {
            InsidiousNotification.notifyMessage("Please try after indexing is finished",
                    NotificationType.INFORMATION);
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("count", selectedCandidates.size());
            UsageInsightTracker.getInstance().RecordEvent("ACTION_GENERATE_JUNIT", eventProperties);


            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ProgressIndicator progressIndicator = ProgressManager.getInstance()
                        .getProgressIndicator();//(project, "New project...");
                progressIndicator.setIndeterminate(false);
                progressIndicator.setFraction(0);
                onGenerateJunitTestCaseRequest(selectedCandidates, new TestCaseGenerationConfiguration(
                        TestFramework.JUnit5, MockFramework.Mockito, JsonFramework.Jackson,
                        ResourceEmbedMode.IN_CODE
                ));
            }, "Generate JUnit Tests", true, project);


        });
    }

    private void showFiltersComponentPopup(Project project, InsidiousService insidiousService, int selectedTabIndex) {
        StompFilterModel originalFilter = new StompFilterModel(stompFilterModel);
        StompFilter stompFilter = new StompFilter(insidiousService, stompFilterModel, lastMethodFocussed,
                selectedTabIndex);
        JComponent component = stompFilter.getComponent();

        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(component, null);

        JBPopup unloggedPreferencesPopup = gutterMethodComponentPopup
                .setProject(project)
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(false)
                .setCancelOnOtherWindowOpen(false)
                .setCancelKeyEnabled(false)
                .setBelongsToGlobalPopupStack(false)
                .setTitle("Unlogged Preferences")
//                        .setCancelCallback(new Computable<Boolean>() {
//                            @Override
//                            public Boolean compute() {
//                                return null;
//                            }
//                        })
                .setTitleIcon(new ActiveIcon(UIUtils.UNLOGGED_ICON_DARK))
                .createPopup();

        component.setMaximumSize(new Dimension(500, 800));
        ComponentLifecycleListener<StompFilter> componentLifecycleListener = () -> {
            unloggedPreferencesPopup.cancel();
            if (originalFilter.equals(stompFilterModel)) {
                return;
            }
            if (stompFilterModel.followEditor) {
                lastMethodFocussed = null;
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    insidiousService.populateFromEditors(null);
                });
            }
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("filter", stompFilterModel.toString());
            UsageInsightTracker.getInstance().RecordEvent("FILTER_UPDATED", eventProperties);

            updateFilterLabel();
            resetAndReload();
        };

        stompFilter.setOnCloseListener(componentLifecycleListener);

        unloggedPreferencesPopup.showCenteredInCurrentWindow(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            stompFilter.setSelectedTab(selectedTabIndex);
        });
    }

    private void clearFilter() {
        stompFilterModel.getIncludedMethodNames().clear();
        stompFilterModel.getExcludedMethodNames().clear();
        stompFilterModel.getIncludedClassNames().clear();
        stompFilterModel.getExcludedClassNames().clear();
    }

    private void saveSelected() {

        if (selectedCandidates.isEmpty()) {
            InsidiousNotification.notifyMessage("Select items on the timeline to save",
                    NotificationType.INFORMATION);
            return;

        }

        ProgressManager.getInstance().
                runProcessWithProgressSynchronously(() -> {
                    ProgressIndicator progressIndicator = ProgressManager.getInstance()
                            .getProgressIndicator();//(project, "New project...");
                    progressIndicator.setIndeterminate(false);
                    progressIndicator.setFraction(0);
                    CountDownLatch cdl = new CountDownLatch(1);
//                    progressIndicator.setIndeterminate(false);
//                    progressIndicator.setFraction(0.25);
                    progressIndicator.setText(
                            "Gathering type information for " + selectedCandidates.size() + " replays");
//                            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    SaveFormListener candidateLifeListener = new SaveFormListener(insidiousService);

                    ArrayList<TestCandidateBareBone> sourceCandidates = new ArrayList<>();
                    int size = selectedCandidates.size();
                    for (int i = 0; i < size; i++) {
                        TestCandidateBareBone selectedCandidate = selectedCandidates.get(i);
                        sourceCandidates.add(selectedCandidate);
                    }


                    saveFormReference = new TestCandidateSaveForm(sourceCandidates, candidateLifeListener,
                            () -> {
                                ApplicationManager.getApplication().invokeLater(() -> {
                                    hideBottomSplit();
                                    scrollContainer.revalidate();
                                    scrollContainer.repaint();
                                });
                            }, progressIndicator);
                    cdl.countDown();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        JPanel saveFormComponent = saveFormReference.getComponent();
                        southPanel.removeAll();
                        splitPane.setDividerLocation(100);
                        saveFormComponent.setMaximumSize(new Dimension(600, 800));
                        southPanel.add(saveFormComponent, BorderLayout.CENTER);
                        southPanel.revalidate();
                        southPanel.repaint();
                        southPanel.getParent().revalidate();
                        southPanel.getParent().repaint();
                        scrollContainer.revalidate();
                        scrollContainer.repaint();
                    });


//                            });


                }, "Save Replays", true, insidiousService.getProject());


    }

    public void resetAndReload() {
        ApplicationManager.getApplication().invokeLater(() -> {
            updateFilterLabel();
            if (sessionInstance == null) {
                return;
            }
            resetTimeline();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                if (sessionInstance.isScanEnable() && sessionInstance.isConnected()) {
                    ApplicationManager.getApplication().invokeLater(stompStatusComponent::setConnected);
                } else {
                    ApplicationManager.getApplication().invokeLater(stompStatusComponent::setDisconnected);
                }
            });

            if (candidateQueryLatch != null) {
                candidateQueryLatch.decrementAndGet();
            }
            loadNewCandidates();
            itemPanel.revalidate();
            itemPanel.repaint();
            historyStreamScrollPanel.revalidate();
            historyStreamScrollPanel.repaint();
        });
    }

    private void executeSingleTestCandidate(TestCandidateBareBone candidateBareBone) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            TestCandidateMetadata loadedCandidate = ApplicationManager.getApplication()
                    .runReadAction((Computable<TestCandidateMetadata>) () -> {
                        return insidiousService.getTestCandidateById(candidateBareBone.getId(), false);
                    });
            MethodCallExpression mainMethod = loadedCandidate.getMainMethod();
            Pair<PsiMethod, PsiSubstitutor> psiMethod = ClassTypeUtils.getPsiMethod(mainMethod,
                    insidiousService.getProject());
            PsiMethod methodPsiElement = psiMethod == null ? null : psiMethod.getFirst();
            if (methodPsiElement == null) {
                InsidiousNotification.notifyMessage("Failed to identify method in source for " +
                        mainMethod.getMethodName(), NotificationType.WARNING);
                return;
            }
            long batchTime = System.currentTimeMillis();

            insidiousService.executeSingleCandidate(
                    new StoredCandidate(loadedCandidate),
                    new ClassUnderTest(loadedCandidate.getFullyQualifiedClassname()),
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
    synchronized public void accept(final List<TestCandidateBareBone> testCandidateMetadataList) {


        if (!welcomePanelRemoved) {
            historyStreamScrollPanel.setVisible(true);
            welcomePanelRemoved = true;
        }
        ApplicationManager.getApplication().invokeLater(() -> itemPanel.remove(unloggedSDKOnboarding.getComponent()));

        totalAcceptedCount++;

        if (totalAcceptedCount > 10) {
            if (!insidiousService.getProject().isDisposed()) {
                new GotItTooltip("Unlogged.Stomp.Item.Filter",
                        "Filter items on the timeline by including and excluding classes so only relevant replays show up",
                        this)
                        .withHeader("Filter whats visible")
                        .withIcon(UIUtils.UNLOGGED_ICON_DARK_SVG)
                        .withLink("Enable Follow Method Filter", () -> {
                            stompFilterModel.setFollowEditor(true);
                            InsidiousNotification.notifyMessage(
                                    "Filter will follow method focussed in editor", NotificationType.INFORMATION
                            );
                        })
                        .withPosition(Balloon.Position.atLeft)
                        .show(actionToolbarContainer, GotItTooltip.LEFT_MIDDLE);
            }

        }

        for (TestCandidateBareBone testCandidateMetadata : testCandidateMetadataList) {
            if (isAcceptable(testCandidateMetadata)) {
                incomingQueue.offer(testCandidateMetadata);
            }
        }

    }

    private boolean isAcceptable(TestCandidateBareBone testCandidateMetadata) {
        if (testCandidateMetadata.getMethodUnderTest().getName().contains("$")) {
            // lambda function
            return false;
        }
        String className = testCandidateMetadata.getMethodUnderTest().getClassName();
        String methodName = testCandidateMetadata.getMethodUnderTest().getName();
        if (!stompFilterModel.getIncludedClassNames().isEmpty()) {
            if (!stompFilterModel.getIncludedClassNames().contains(className)) {
                return false;
            }
        }
        if (!stompFilterModel.getIncludedMethodNames().isEmpty()) {
            if (!stompFilterModel.getIncludedMethodNames().contains(methodName)) {
                return false;
            }
        }
        if (!stompFilterModel.getExcludedClassNames().isEmpty()) {
            if (stompFilterModel.getExcludedClassNames().contains(className)) {
                return false;
            }
        }
        if (!stompFilterModel.getExcludedMethodNames().isEmpty()) {
            if (stompFilterModel.getExcludedMethodNames().contains(methodName)) {
                return false;
            }
        }
        return true;
    }

    private synchronized void addCandidateToUi(TestCandidateBareBone testCandidateMetadata, int index) {
        JCheckBox comp1 = new JCheckBox();
        StompItem stompItem = new StompItem(testCandidateMetadata, this, insidiousService, comp1);


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

        rowPanel.add(component, BorderLayout.CENTER);

        JPanel dateAndTimePanel = createDateAndTimePanel(createTimeLineComponent(comp1),
                Date.from(Instant.ofEpochMilli(testCandidateMetadata.getCreatedAt())));
        rowPanel.add(dateAndTimePanel, BorderLayout.WEST);
        stompItem.setStompRowItem(rowPanel);


//        makeSpace(index);
        GridBagConstraints gbcForLeftMainComponent = createGBCForLeftMainComponent(index);
        itemPanel.add(rowPanel, gbcForLeftMainComponent);
        itemPanel.revalidate();
        itemPanel.repaint();
        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();

        if (itemPanel.getComponentCount() > 5 && !shownGotItNofiticaton) {
            shownGotItNofiticaton = true;
            new GotItTooltip("Unlogged.Stomp.Item.Show",
                    "<html>Each method execution shows up here. <br>" +
                            "Hover on and select by clicking the checkbox<br>"
                            + "Right click to include/exclude </html>",
                    insidiousService.getProject())
                    .withPosition(Balloon.Position.below)
                    .show(component, GotItTooltip.BOTTOM_MIDDLE);

            new GotItTooltip("Unlogged.Stomp.Item.Checkbox",
                    "<html>Hover and click the checkbox to select a replay record.<br>Quick replay using the pink play " +
                            "button</html>",
                    insidiousService.getProject())
                    .withPosition(Balloon.Position.below)
                    .show(component, GotItTooltip.BOTTOM_LEFT);

        }

    }

    private GridBagConstraints createGBCForLeftMainComponent(int yIndex) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = yIndex;
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

//    private GridBagConstraints createGBCForDateAndTimePanel() {
//        GridBagConstraints gbc1 = new GridBagConstraints();
//
//        gbc1.gridx = 1;
//        gbc1.gridy = GridBagConstraints.RELATIVE;
//        gbc1.gridwidth = 1;
//        gbc1.gridheight = 1;
//
//        gbc1.weightx = 0.1;
//        gbc1.weighty = 0." ";
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

    private JPanel createDateAndTimePanel(JPanel lineContainer, Date date) {
//        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        JPanel comp = new JPanel();
        BorderLayout mgr = new BorderLayout();
        comp.setLayout(mgr);
//        JLabel timeLabel = new JLabel(String.format("          %s", sdf.format(date)));
//        Font font = timeLabel.getFont();
//        timeLabel.setFont(font.deriveFont(10.0f));
//        timeLabel.setForeground(Color.decode("#8C8C8C"));
//        timeLabel.setUI(new VerticalLabelUI(true));
//        comp.add(timeLabel, BorderLayout.CENTER);


        comp.add(lineContainer, BorderLayout.WEST);
        comp.setSize(new Dimension(40, -1));
        comp.setPreferredSize(new Dimension(40, -1));
        return comp;
    }

    private JPanel createLinePanel(JPanel lineContainer) {
        JPanel comp = new JPanel();
        BorderLayout mgr = new BorderLayout();
        comp.setLayout(mgr);
        JLabel comp1 = new JLabel("      ");
        comp1.setUI(new VerticalLabelUI(true));

//        comp.add(comp1, BorderLayout.CENTER);
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 4));
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

    private JPanel createTimeLineComponent(JCheckBox comp1) {
        JPanel lineContainer = new JPanel();
        lineContainer.setLayout(new GridBagLayout());
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        comp1.setMaximumSize(new Dimension(18, 18));
        comp1.setMinimumSize(new Dimension(18, 18));
        comp1.setPreferredSize(new Dimension(18, 18));

        JPanel comp2 = new JPanel();
        comp2.add(comp1);

        comp2.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 0));

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
    public void executeCandidate(List<TestCandidateBareBone> metadata,
                                 ClassUnderTest classUnderTest, ExecutionRequestSourceType source,
                                 AgentCommandResponseListener<TestCandidateBareBone, String> responseListener) {

        if (source == ExecutionRequestSourceType.Single) {
            TestCandidateBareBone candidateBareBone = metadata.get(0);
            TestCandidateMetadata selectedCandidate = ApplicationManager.getApplication().runReadAction(
                    (Computable<TestCandidateMetadata>) () -> insidiousService.getTestCandidateById(
                            candidateBareBone.getId(), true));
            PsiMethod methodPsiElement = ClassTypeUtils.getPsiMethod(selectedCandidate.getMainMethod(),
                    insidiousService.getProject()).getFirst();
            if (methodPsiElement == null) {
                InsidiousNotification.notifyMessage("Failed to identify method in source for " +
                        selectedCandidate.getMainMethod().getMethodName(), NotificationType.WARNING);
                return;
            }

            JavaMethodAdapter method = new JavaMethodAdapter(methodPsiElement);
            showDirectInvoke(method);
            directInvokeComponent.renderForMethod(method,
                    selectedCandidate.getMainMethod().getArguments()
                            .stream().map(e -> new String(e.getProb().getSerializedValue()))
                            .collect(Collectors.toList()));
            directInvokeComponent.triggerExecute();
        }

    }

    @Override
    public void displayResponse(Component responseComponent, boolean isExceptionFlow) {

    }

    @Override
    public void onSaved(TestCandidateBareBone storedCandidate) {

    }

    @Override
    public void onSelected(TestCandidateBareBone storedCandidate) {
        this.selectedCandidates.add(storedCandidate);
        updateControlPanel();
    }

    private void updateControlPanel() {

        selectedCountLabel.setForeground(JBColor.DARK_GRAY);
        selectedCountLabel.setText(selectedCandidates.size() + " selected");
        if (!selectedCandidates.isEmpty()) {
            clearSelectionLabel.setVisible(true);
            new GotItTooltip("Unlogged.Stomp.ActionToolbar",
                    "Use the toolbar to replay/save or generate test case for multiple method replays at once",
                    this)
                    .withPosition(Balloon.Position.above)
                    .show(actionToolbar.getComponent(), GotItTooltip.TOP_MIDDLE);


        } else {
            selectedCountLabel.setText("0 selected");
            clearSelectionLabel.setVisible(false);
        }
    }

    @Override
    public void unSelected(TestCandidateBareBone storedCandidate) {
        this.selectedCandidates.remove(storedCandidate);
        updateControlPanel();
    }

    @Override
    public void onDeleteRequest(TestCandidateBareBone storedCandidate) {

    }

    @Override
    public void onDeleted(TestCandidateBareBone storedCandidate) {

    }

    @Override
    public void onUpdated(TestCandidateBareBone storedCandidate) {

    }

    @Override
    public void onUpdateRequest(TestCandidateBareBone storedCandidate) {

    }

    @Override
    public void onGenerateJunitTestCaseRequest(List<TestCandidateBareBone> storedCandidate, TestCaseGenerationConfiguration generationConfiguration) {

        TestCaseService testCaseService = insidiousService.getTestCaseService();
        if (testCaseService == null) {
            InsidiousNotification.notifyMessage("Please start the application with unlogged-sdk to generate JUnit " +
                    "test cases", NotificationType.WARNING);
            return;
        }

        ArrayList<TestCandidateBareBone> selectedCandidatesCopy = new ArrayList<>(storedCandidate);
        for (TestCandidateBareBone testCandidateShell : selectedCandidatesCopy) {

            try {

                generateTestCaseSingle(generationConfiguration, testCaseService, testCandidateShell);

            } catch (ProcessCanceledException pce) {
                //
            } catch (Exception e) {
                logger.error("Failed to generate test case", e);
                InsidiousNotification.notifyMessage(
                        "Failed to generate test case for [" + testCandidateShell.getMethodUnderTest()
                                .getClassName() + "] " + e.getMessage(),
                        NotificationType.ERROR);
            }
        }
        logger.info("completed");


    }

    private boolean generateTestCaseSingle(
            TestCaseGenerationConfiguration generationConfiguration,
            TestCaseService testCaseService,
            TestCandidateBareBone testCandidateShell) throws Exception {
        TestCandidateMetadata loadedTestCandidate = insidiousService.getTestCandidateById(testCandidateShell.getId(),
                true);
        Parameter testSubject = loadedTestCandidate.getTestSubject();
        if (testSubject.isException()) {
            return true;
        }
        MethodCallExpression callExpression = loadedTestCandidate.getMainMethod();
        String methodName = callExpression.getMethodName();
        logger.warn(
                "Generating test case: " + testSubject.getType() + "." + methodName + "()");
        generationConfiguration.getTestCandidateMetadataList().clear();
        generationConfiguration.getTestCandidateMetadataList().add(loadedTestCandidate);

        generationConfiguration.getCallExpressionList().clear();
        generationConfiguration.getCallExpressionList().addAll(loadedTestCandidate.getCallsList());

        generationConfiguration.setTestMethodName(
                "test" + methodName.substring(0, 1).toUpperCase(Locale.ROOT) + methodName.substring(1));
        TestCaseUnit testCaseUnit = DumbService.getInstance(project)
                .tryRunReadActionInSmartMode(() -> {
                    try {
                        return testCaseService.buildTestCaseUnit(generationConfiguration);
                    } catch (ProcessCanceledException pce) {
                        return null;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, "Building test");
        if (testCaseUnit == null) {
            return false;
        }
        List<TestCaseUnit> testCaseUnit1 = new ArrayList<>();
        testCaseUnit1.add(testCaseUnit);
        TestSuite testSuite = new TestSuite(testCaseUnit1);
        insidiousService.getJUnitTestCaseWriter().saveTestSuite(testSuite);
        return false;
    }

    @Override
    public void onCandidateSelected(TestCandidateBareBone testCandidateMetadata, MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {


//            ContextMenuPopupHandler.Simple group = new ContextMenuPopupHandler.Simple("io.unlogged");

//            ActionGroup actionGroup = group.getActionGroup(e);

            String fullyQualifiedClassname = testCandidateMetadata.getMethodUnderTest().getClassName();
            String methodName = testCandidateMetadata.getMethodUnderTest().getName();
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
                                        stompFilterModel.getIncludedClassNames().add(fullyQualifiedClassname);
                                        stompFilterModel.getExcludedClassNames().remove(fullyQualifiedClassname);
                                        break;
                                    case "method":
                                        stompFilterModel.getIncludedMethodNames().add(methodName);
                                        stompFilterModel.getExcludedMethodNames().remove(methodName);
                                        break;
                                }


                                break;
                            case "Exclude":

                                switch (parts[1]) {
                                    case "class":
                                        stompFilterModel.getIncludedClassNames().remove(fullyQualifiedClassname);
                                        stompFilterModel.getExcludedClassNames().add(fullyQualifiedClassname);
                                        break;
                                    case "method":
                                        stompFilterModel.getIncludedMethodNames().remove(methodName);
                                        stompFilterModel.getExcludedMethodNames().add(methodName);
                                        break;
                                }


                                break;
                        }
                        updateFilterLabel();
                        resetAndReload();

                    })
                    .setCancelOnClickOutside(true)
//                    .setCancelKeyEnabled(true)
                    .createPopup()
                    .show(new RelativePoint(e));

        }
    }

    private void updateFilterLabel() {
        StringBuilder tooltipText = new StringBuilder();
        int total = stompFilterModel.getIncludedClassNames().size()
                + stompFilterModel.getIncludedMethodNames().size()
                + stompFilterModel.getExcludedClassNames().size()
                + stompFilterModel.getExcludedMethodNames().size();
        if (total == 0) {
            clearFilterLabel.setVisible(false);
//            filterAppliedLabel.setVisible(false);
        } else {
            clearFilterLabel.setVisible(true);
        }

        String text = total + (total == 1 ? " filter" : " filters");
        ExecutionSessionSource source = configurationState.getExecutionSessionSource();
        sourceLabelFilter.setText("<html><small>[<font color=blue><u>" + source.getSessionMode() + "</u></font>] " + "</font></small></html>");;
        filterAppliedLabel.setText("<html><small>" + "</font></small></html>");

        itemPanel.revalidate();
        itemPanel.repaint();
        historyStreamScrollPanel.revalidate();
        historyStreamScrollPanel.repaint();


    }

    @Override
    public void onCancel() {

    }


    // removes the visible items on the timeline
    public void clear() {
        List<Component> itemsToNotDelete = new ArrayList<>();
        List<StompItem> pinnedStomps = new ArrayList<>();
        countByMethodName.clear();
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


        itemPanel.add(unloggedSDKOnboarding.getComponent(),
                createGBCForLeftMainComponent(itemPanel.getComponentCount()));
        itemPanel.add(new JPanel(), createGBCForFakeComponent(), itemPanel.getComponentCount());

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
        logger.warn("setDisconnected: " + stompStatusComponent.hashCode());
        stompStatusComponent.setDisconnected();
    }

    public void showDirectInvoke(MethodAdapter method) {

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            directInvokeComponent.setMethod(method);
            ApplicationManager.getApplication().invokeLater(() -> {
                splitPane.setDividerLocation(200);
                southPanel.removeAll();
                southPanel.add(directInvokeComponent.getContent(), BorderLayout.CENTER);
                historyStreamScrollPanel.revalidate();
                historyStreamScrollPanel.repaint();
            });

        });

    }

    public void showNewDeclaredMockCreator(JavaMethodAdapter javaMethodAdapter,
                                           PsiMethodCallExpression psiMethodCallExpression, OnSaveListener onSaveListener) {
        onMethodFocussed(javaMethodAdapter);
        MockDefinitionEditor mockEditor = new MockDefinitionEditor(MethodUnderTest.fromMethodAdapter(javaMethodAdapter),
                psiMethodCallExpression, insidiousService.getProject(), declaredMock -> {
            String newMockId = insidiousService.saveMockDefinition(declaredMock);
            InsidiousNotification.notifyMessage("Mock definition updated", NotificationType.INFORMATION);
            onSaveListener.onSaveDeclaredMock(declaredMock);
            mainPanel.revalidate();
            mainPanel.repaint();
        }, () -> {
            insidiousService.showRouterForMethod(javaMethodAdapter);
            scrollContainer.revalidate();
            scrollContainer.repaint();
        });
        JComponent mockEditorComponent = mockEditor.getComponent();
        mockEditorComponent.setMinimumSize(new Dimension(-1, 500));
        mockEditorComponent.setMaximumSize(new Dimension(-1, 600));

        ApplicationManager.getApplication().invokeLater(() -> {
            splitPane.setDividerLocation(200);
            southPanel.removeAll();
            southPanel.add(mockEditorComponent, BorderLayout.CENTER);
            scrollContainer.revalidate();
            scrollContainer.repaint();
            historyStreamScrollPanel.revalidate();
            historyStreamScrollPanel.repaint();
            itemPanel.revalidate();
            itemPanel.repaint();
        });
    }


    public void removeDirectInvoke() {
        hideBottomSplit();
    }

    private void setConnectedAndWaiting() {
        if (!welcomePanelRemoved) {
            historyStreamScrollPanel.setVisible(true);
            welcomePanelRemoved = true;
        }

        JLabel process_started = new JLabel("<html><font color='#548AF7'><b> Process started... </b></font></html>");
//        process_started.setIcon(UIUtils.LIBRARY_ICON);
        JPanel startedPanel = new JPanel();
        startedPanel.setLayout(new BorderLayout());
        process_started.setBorder(
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        );
//        process_started.setForeground(new Color(84, 138, 247));
//        startedPanel.add(process_started, BorderLayout.EAST);

        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BorderLayout());


        rowPanel.add(startedPanel, BorderLayout.CENTER);
        rowPanel.add(createLinePanel(createLineComponent()), BorderLayout.EAST);
//        makeSpace(0);
        rowPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        itemPanel.add(rowPanel, createGBCForProcessStartedComponent());

        setConnected();
    }

    private void setConnected() {
        logger.warn("setConnected: " + stompStatusComponent.hashCode());
        stompStatusComponent.setConnected();
    }

    public synchronized void loadNewCandidates() {
        if (sessionInstance == null) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (sessionInstance.isConnected() && sessionInstance.isScanEnable()) {
                setConnected();
            } else {
                disconnected();
            }
        });

        countByMethodName.clear();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            candidateQueryLatch = new AtomicInteger(1);
            sessionInstance
                    .getTestCandidates(this, lastEventId, stompFilterModel, candidateQueryLatch);
        });
    }

    public SessionScanEventListener getScanEventListener() {
        return scanEventListener;
    }

    @Override
    public void onClose() {
        removeDirectInvoke();

    }

    @Override
    public void run() {
        try {
//            while (true) {
            final TestCandidateBareBone testCandidateMetadata = incomingQueue.poll(1000, TimeUnit.MILLISECONDS);
            if (testCandidateMetadata == null) {
                return;
            }
            CountDownLatch pollerCDL = new CountDownLatch(1);
            ApplicationManager.getApplication().invokeLater(() -> {
                try {

                    List<TestCandidateBareBone> remainingItems = new ArrayList<>();
                    remainingItems.add(testCandidateMetadata);
                    incomingQueue.drainTo(remainingItems);
                    while (remainingItems.size() > MAX_ITEM_TO_DISPLAY) {
                        remainingItems.remove(0);
                    }

                    for (TestCandidateBareBone remainingItem : remainingItems) {
                        acceptSingle(remainingItem);
                    }
                    while (stompItems.size() > MAX_ITEM_TO_DISPLAY) {
                        StompItem first = stompItems.remove(0);
                        itemPanel.remove(first.getStompRowItem());
                    }
                    itemPanel.revalidate();
                    itemPanel.repaint();
                    insidiousService.forceRedrawInlayHints();


                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    pollerCDL.countDown();
                }
            });

            pollerCDL.await();
//            }


        } catch (Exception e) {
            // who killed this
//            e.printStackTrace();
            // just end
        } finally {
        }

    }

    private void acceptSingle(TestCandidateBareBone testCandidateMetadata) {
//        logger.warn("entr acceptSingle: " + testCandidateMetadata);
        String className = testCandidateMetadata.getMethodUnderTest().getClassName();
        String methodName = testCandidateMetadata.getMethodUnderTest().getName();
        String key = className + "." +
                methodName;
        AtomicInteger countAtomic = countByMethodName.get(key);
        if (countAtomic == null) {
            countAtomic = new AtomicInteger(0);
            countByMethodName.put(key, countAtomic);
        }

        int count = countAtomic.incrementAndGet();
        if (count > 40) {
            if (!stompFilterModel.getIncludedClassNames().contains(className) ||
                    !stompFilterModel.getIncludedMethodNames().contains(methodName)) {
                stompFilterModel.getExcludedMethodNames().add(methodName);
                stompFilterModel.getExcludedClassNames().add(className);
                InsidiousNotification.notifyMessage("Excluded [" + key + "] from live view. If you want to see them " +
                                "include the class [" + className + "] and method [" + methodName + "] in filters.",
                        NotificationType.INFORMATION);
                return;

            }
        }

        if (testCandidateMetadata.getExitProbeIndex() > lastEventId) {
            lastEventId = testCandidateMetadata.getExitProbeIndex();
        }
        addCandidateToUi(testCandidateMetadata, itemPanel.getComponentCount());
        itemPanel.revalidate();
        scrollContainer.revalidate();
        scrollContainer.repaint();
//        logger.warn("exit acceptSingle: " + testCandidateMetadata);

    }

    public void setSession(SessionInstanceInterface sessionInstance) {
        if (candidateQueryLatch != null) {
            candidateQueryLatch.decrementAndGet();
        }
        this.sessionInstance = sessionInstance;
        if (sessionInstance != null) {
            sessionInstance.addSessionScanEventListener(getScanEventListener());
        }
        resetAndReload();
    }

    public void onMethodFocussed(MethodAdapter method) {
        if (method == null) {
            lastMethodFocussed = null;
            return;
        }
//        ClassAdapter containingClass = method.getContainingClass();
//        List<String> newClassNameList = new ArrayList<>();
//        newClassNameList.add(containingClass.getQualifiedName());
//        for (ClassAdapter aSuper : containingClass.getSupers()) {
//            newClassNameList.add(aSuper.getQualifiedName());
//        }


//        Collection<PsiClass> childClasses = ClassInheritorsSearch.search(
//                ApplicationManager.getApplication().runReadAction(
//                        (Computable<PsiClass>) () -> method.getPsiMethod().getContainingClass())).findAll();
//
//        for (PsiClass childClass : childClasses) {
//            ApplicationManager.getApplication().runReadAction(
//                    (Computable<String>) () -> {
//                        String name = childClass.getQualifiedName();
//                        newClassNameList.add(name);
//                        for (PsiClass anInterface : childClass.getInterfaces()) {
//                            newClassNameList.add(anInterface.getQualifiedName());
//                        }
//                        return name;
//                    });
//        }


        MethodUnderTest newMethodAdapter = ApplicationManager.getApplication().runReadAction(
                (Computable<MethodUnderTest>) () -> MethodUnderTest.fromMethodAdapter(method));
        if (lastMethodFocussed != null) {
            if (lastMethodFocussed.getMethodHashKey().equals(newMethodAdapter.getMethodHashKey())) {
                // same method focussed again
                return;
            }
        }

        lastMethodFocussed = newMethodAdapter;
    }

    public void showOnboardingScreen(UnloggedOnboardingScreenV2 screen) {
        mainPanel.removeAll();
        mainPanel.add(screen.getComponent(), BorderLayout.CENTER);
    }

    public void dispose() {
        if (candidateQueryLatch != null) {
            candidateQueryLatch.decrementAndGet();
        }
    }

    public void showVersionBadge(SemanticVersion currentVersion, SemanticVersion requiredVersion) {
        Notification notification = new Notification(InsidiousNotification.DISPLAY_ID, "Update unlogged-sdk Version",
                "You are using version " + currentVersion.toString() + " which is older than recommended version for" +
                        " this plugin " + requiredVersion + ". Please update the unlogged-sdk version in your pom" +
                        ".xml/build.gradle",
                NotificationType.WARNING);
        notification.setImportant(true);
        notification.setSubtitle("Use unlogged-sdk:" + requiredVersion.toString());
        notification.setIcon(UIUtils.UNLOGGED_ICON_DARK_SVG);
        Notifications.Bus.notify(notification);
    }

    public void hideBottomSplit() {
        southPanel.removeAll();
        splitPane.setDividerLocation(splitPane.getHeight());
    }

    public void createJunitFromSelectedReplay() {

    }

    public int selectVisibleCandidates() {
        selectedCandidates.clear();
        selectAll();

        return selectedCandidates.size();
    }

    public List<TestCandidateBareBone> getSelectedCandidates() {
        return selectedCandidates;
    }

    public void showRouterForMethod(MethodAdapter methodAdapter) {
        if (directInvokeComponent != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                directInvokeComponent.setMethod(methodAdapter);
            });
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            splitPane.setDividerLocation(50);
            southPanel.removeAll();
            southPanel.add(directInvokeComponent.getContent(), BorderLayout.CENTER);
            directInvokeComponent.showRouter();
            southPanel.revalidate();
            southPanel.repaint();
            southPanel.getParent().revalidate();
            southPanel.getParent().repaint();
        });

    }
}
