package com.insidious.plugin.ui.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.client.ScanProgress;
import com.insidious.plugin.client.SessionScanEventListener;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.SemanticVersion;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.frameworks.JsonFramework;
import com.insidious.plugin.pojo.frameworks.MockFramework;
import com.insidious.plugin.pojo.frameworks.TestFramework;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.TestCaseGenerationConfiguration;
import com.insidious.plugin.ui.UnloggedOnboardingScreenV2;
import com.insidious.plugin.ui.UnloggedSDKOnboarding;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.insidious.plugin.ui.methodscope.MethodDirectInvokeComponent;
import com.insidious.plugin.ui.mocking.MockDefinitionEditor;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
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
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.impl.JavaPsiImplementationHelper;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.ui.GotItTooltip;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class StompComponent implements
        Consumer<List<TestCandidateMetadata>>,
        TestCandidateLifeListener,
        ComponentLifecycleListener<MethodDirectInvokeComponent>,
        Runnable, OnExpandListener, Disposable {
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
    private final AtomicRecordService atomicRecordService;
    private final Set<Long> pinnedItems = new HashSet<>();
    private final ActionToolbarImpl actionToolbar;
    private final UnloggedSDKOnboarding unloggedSDKOnboarding;
    private final Map<String, AtomicInteger> countByMethodName = new HashMap<>();
    BlockingQueue<TestCandidateMetadata> incomingQueue = new ArrayBlockingQueue<>(100);
    int totalAcceptedCount = 0;
    private JPanel mainPanel;
    private JPanel northPanelContainer;
    private JScrollPane historyStreamScrollPanel;
    private JPanel scrollContainer;
    //    private JLabel reloadButton;
    private JLabel filterButton;
    private JButton saveReplayButton;
    //    private JLabel replayButton;
//    private JLabel generateJUnitButton;
    private JPanel controlPanel;
    private JPanel infoPanel;
    private JLabel selectedCountLabel;
    private JLabel selectAllLabel;
    private JLabel clearSelectionLabel;
    private JPanel southPanel;
    private JLabel clearFilterLabel;
    private JLabel filterAppliedLabel;
    private JPanel actionToolbarContainer;
    private long lastEventId = 0;
    private MethodDirectInvokeComponent directInvokeComponent = null;
    private TestCandidateSaveForm saveFormReference;
    private boolean welcomePanelRemoved = false;
    private AtomicInteger candidateQueryLatch;
    private MethodUnderTest lastMethodFocussed;
    private boolean shownGotItNofiticaton = false;

    public StompComponent(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        Project project = insidiousService.getProject();
        atomicRecordService = project.getService(AtomicRecordService.class);
        configurationState = project.getService(InsidiousConfigurationState.class);

        filterAppliedLabel.setVisible(false);
        filterAppliedLabel.setForeground(JBColor.BLACK);
        filterModel = configurationState.getFilterModel();


        itemPanel = new JPanel();
        GridBagLayout mgr = new GridBagLayout();
        itemPanel.setLayout(mgr);
        itemPanel.setAlignmentY(0);
        itemPanel.setAlignmentX(0);

        itemPanel.add(new JPanel(), createGBCForFakeComponent());

        itemPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));


        historyStreamScrollPanel.setViewportView(itemPanel);
        historyStreamScrollPanel.setBorder(BorderFactory.createEmptyBorder());

        scrollContainer.setBorder(BorderFactory.createEmptyBorder());


        AnAction clearAction = new AnAction(() -> "Clear Timeline", UIUtils.DELETE_BIN_PARALLEL_RED) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                System.err.println("clear timeline");
                resetTimeline();
            }
        };


        AnAction generateJunitTestAction = new AnAction(() -> "Generate JUnit Test", UIUtils.TEST_TUBE_ICON) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                System.err.println("generate junit test");
                if (selectedCandidates.size() < 1) {
                    InsidiousNotification.notifyMessage("Select records to generate JUnit Test",
                            NotificationType.INFORMATION);
                    return;
                }

                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    onGenerateJunitTestCaseRequest(selectedCandidates);
                });
            }
        };

        AnAction replaySelectionAction = new AnAction(() -> "Replay Selected", UIUtils.FLASHLIGHT_BLUE) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                System.err.println("replay selected");
                if (selectedCandidates.size() < 1) {
                    InsidiousNotification.notifyMessage("Select records to replay", NotificationType.INFORMATION);
                    return;
                }
                InsidiousNotification.notifyMessage("Replayed " + selectedCandidates.size() + " records",
                        NotificationType.INFORMATION);
                for (TestCandidateMetadata selectedCandidate : selectedCandidates) {
                    executeSingleTestCandidate(selectedCandidate);
                }
            }
        };

        AnAction reloadAction = new AnAction(() -> "Reload All", UIUtils.REFRESH_TEAL) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                System.err.println("reload timeline");
                resetAndReload();
            }
        };


        List<AnAction> action11 = List.of(reloadAction, clearAction, generateJunitTestAction, replaySelectionAction);


        actionToolbar = new ActionToolbarImpl(
                "Live View", new DefaultActionGroup(action11), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);

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

//        selectAllLabel.setVisible(false);
        selectAllLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCandidates.clear();
                for (StompItem stompItem : stompItems) {
                    stompItem.setSelected(true);
                    selectedCandidates.add(stompItem.getTestCandidate());
                }
                updateControlPanel();
            }
        });


        clearFilterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        clearFilterLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
        clearFilterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearFilter();
                updateFilterLabel();
                resetAndReload();

            }

        });

        saveReplayButton.addActionListener(e -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                ApplicationManager.getApplication().runReadAction(this::saveSelected);
            });
        });


        filterButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterButton.addMouseListener(new MouseAdapter() {


            @Override
            public void mouseClicked(MouseEvent e) {

                FilterModel originalFilter = new FilterModel(filterModel);
                StompFilter stompFilter = new StompFilter(filterModel, lastMethodFocussed, project);
                JComponent component = stompFilter.getComponent();

                ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(component, null);

                JBPopup unloggedPreferencesPopup = gutterMethodComponentPopup
                        .setProject(project)
                        .setShowBorder(true)
                        .setShowShadow(true)
                        .setFocusable(true)
                        .setResizable(true)
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
                ComponentLifecycleListener<StompFilter> componentLifecycleListener = new ComponentLifecycleListener<StompFilter>() {
                    @Override
                    public void onClose(StompFilter component) {
                        unloggedPreferencesPopup.cancel();
                        if (originalFilter.equals(filterModel)) {
                            return;
                        }
                        if (filterModel.followEditor) {
                            lastMethodFocussed = null;
                            onMethodFocussed(insidiousService.getCurrentMethod());
                        }
                        updateFilterLabel();
                        resetAndReload();
                    }
                };

                stompFilter.setOnCloseListener(componentLifecycleListener);

                unloggedPreferencesPopup.showCenteredInCurrentWindow(project);


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
        stompStatusComponent = new StompStatusComponent(project
                .getService(InsidiousConfigurationState.class).getFilterModel());
        mainPanel.add(stompStatusComponent.getComponent(), BorderLayout.SOUTH);


        unloggedSDKOnboarding = new UnloggedSDKOnboarding(insidiousService);
        itemPanel.add(unloggedSDKOnboarding.getComponent(), createGBCForProcessStartedComponent());

        saveReplayButton.setEnabled(false);
        updateFilterLabel();
    }

    private void clearFilter() {
        filterModel.getIncludedMethodNames().clear();
        filterModel.getExcludedMethodNames().clear();
        filterModel.getIncludedClassNames().clear();
        filterModel.getExcludedClassNames().clear();
    }

    private void saveSelected() {
        if (DumbService.getInstance(insidiousService.getProject()).isDumb()) {
            InsidiousNotification.notifyMessage("Please wait for IDE indexing to complete",
                    NotificationType.INFORMATION);
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            SaveFormListener candidateLifeListener = new SaveFormListener(insidiousService);
            saveFormReference = new TestCandidateSaveForm(selectedCandidates, candidateLifeListener,
                    component -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            southPanel.removeAll();
                            scrollContainer.revalidate();
                            scrollContainer.repaint();
                        });
                    });
            ApplicationManager.getApplication().invokeLater(() -> {
                JPanel component = saveFormReference.getComponent();
                southPanel.removeAll();
                component.setMaximumSize(new Dimension(600, 800));
                southPanel.add(component, BorderLayout.SOUTH);
                southPanel.revalidate();
                southPanel.repaint();
                southPanel.getParent().revalidate();
                southPanel.getParent().repaint();
                scrollContainer.revalidate();
                scrollContainer.repaint();
            });


        });


    }

    private void resetAndReload() {
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

    private void executeSingleTestCandidate(TestCandidateMetadata selectedCandidate) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            PsiMethod methodPsiElement = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiMethod>) () -> ClassTypeUtils.getPsiMethod(selectedCandidate.getMainMethod(),
                            insidiousService.getProject()).getFirst());
            if (methodPsiElement == null) {
                InsidiousNotification.notifyMessage("Failed to identify method in source for " +
                        selectedCandidate.getMainMethod().getMethodName(), NotificationType.WARNING);
                return;
            }
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
        itemPanel.remove(unloggedSDKOnboarding.getComponent());

        totalAcceptedCount++;

        if (totalAcceptedCount > 10) {
            if (!insidiousService.getProject().isDisposed()) {
                new GotItTooltip("Unlogged.Stomp.Item.Filter",
                        "Filter items on the timeline by including and excluding classes so only relevant replays show up",
                        insidiousService.getProject())
                        .withHeader("Filter whats visible")
                        .withIcon(UIUtils.UNLOGGED_ICON_DARK_SVG)
                        .withLink("Enable Follow Method Filter", () -> {
                            filterModel.setFollowEditor(true);
                            InsidiousNotification.notifyMessage(
                                    "Filter will follow method focussed in editor", NotificationType.INFORMATION
                            );
                        })
                        .withPosition(Balloon.Position.atLeft)
                        .show(filterButton, GotItTooltip.LEFT_MIDDLE);
            }

        }

        for (TestCandidateMetadata testCandidateMetadata : testCandidateMetadataList) {
            if (isAcceptable(testCandidateMetadata)) {
                incomingQueue.offer(testCandidateMetadata);
            }
        }

    }

    private boolean isAcceptable(TestCandidateMetadata testCandidateMetadata) {
        if (testCandidateMetadata.getMainMethod().getMethodName().contains("$")) {
            // lambda function
            return false;
        }
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
        rowPanel.add(dateAndTimePanel, BorderLayout.EAST);


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
                            "Hover on and select by clicking the checkbox next to the pink icon<br>"
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
//        ApplicationManager.getApplication().invokeLater(() -> {
//            verticalScrollBar1.setValue(max);
//        });
//        logger.warn("Component count is - " +itemPanel.getComponentCount());

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
        lineContainer.setBorder(BorderFactory.createEmptyBorder(0, 17, 0, 4));
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
    public void executeCandidate(List<TestCandidateMetadata> metadata,
                                 ClassUnderTest classUnderTest, ExecutionRequestSourceType source,
                                 AgentCommandResponseListener<TestCandidateMetadata, String> responseListener) {

        if (source == ExecutionRequestSourceType.Single) {
            TestCandidateMetadata selectedCandidate = metadata.get(0);
            PsiMethod methodPsiElement = ApplicationManager.getApplication()
                    .runReadAction(
                            (Computable<PsiMethod>) () -> ClassTypeUtils.getPsiMethod(selectedCandidate.getMainMethod(),
                                    insidiousService.getProject()).getFirst());
            if (methodPsiElement == null) {
                InsidiousNotification.notifyMessage("Failed to identify method in source for " +
                        selectedCandidate.getMainMethod().getMethodName(), NotificationType.WARNING);
                return;
            }

            JavaMethodAdapter method = new JavaMethodAdapter(methodPsiElement);
            showDirectInvoke(method);
            try {
                directInvokeComponent.renderForMethod(method,
                        selectedCandidate.getMainMethod().getArguments()
                                .stream().map(e -> new String(e.getProb().getSerializedValue()))
                                .collect(Collectors.toList()));
                directInvokeComponent.triggerExecute();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
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

        selectedCountLabel.setForeground(JBColor.DARK_GRAY);
        selectedCountLabel.setText(selectedCandidates.size() + " selected");
        if (selectedCandidates.size() > 0 && !controlPanel.isEnabled()) {
            clearSelectionLabel.setVisible(true);
            saveReplayButton.setEnabled(true);

            new GotItTooltip("Unlogged.Stomp.ActionToolbar",
                    "Use the toolbar to replay/save or generate test case for multiple method replays at once",
                    insidiousService.getProject())
                    .withPosition(Balloon.Position.above)
                    .show(actionToolbar.getComponent(), GotItTooltip.TOP_MIDDLE);


        } else if (selectedCandidates.size() == 0 && controlPanel.isEnabled()) {
            selectedCountLabel.setText("0 selected");
            clearSelectionLabel.setVisible(false);
            saveReplayButton.setEnabled(false);
        }
    }

    @Override
    public void unSelected(TestCandidateMetadata storedCandidate) {
        this.selectedCandidates.remove(storedCandidate);
        updateControlPanel();
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
    public void onGenerateJunitTestCaseRequest(List<TestCandidateMetadata> storedCandidate) {
        DumbService instance = DumbService.getInstance(insidiousService.getProject());

        TestCaseGenerationConfiguration generationConfiguration = new TestCaseGenerationConfiguration(
                TestFramework.JUnit5, MockFramework.Mockito, JsonFramework.Jackson, ResourceEmbedMode.IN_CODE
        );
        TestCaseService testCaseService = insidiousService.getTestCaseService();
        if (testCaseService == null) {
            InsidiousNotification.notifyMessage("Please start the application with unlogged-sdk to generate JUnit " +
                    "test cases", NotificationType.WARNING);
            return;
        }

        ArrayList<TestCandidateMetadata> selectedCandidatesCopy = new ArrayList<>(storedCandidate);
        for (TestCandidateMetadata testCandidateShell : selectedCandidatesCopy) {

            try {

                CountDownLatch cdl = new CountDownLatch(1);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    instance.waitForSmartMode();
                    instance.suspendIndexingAndRun("Generating JUnit Test cases", () -> {
                        try {
                            gnerateTestCaseSingle(generationConfiguration, testCaseService, testCandidateShell);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            cdl.countDown();
                        }


                    });

                });

                cdl.await();
                instance.waitForSmartMode();

            } catch (Exception e) {
                logger.error("Failed to generate test case", e);
                InsidiousNotification.notifyMessage(
                        "Failed to generate test case for [" + testCandidateShell.getTestSubject()
                                .getType() + "] " + e.getMessage(), NotificationType.ERROR);
            }
        }
        logger.info("completed");


    }

    private boolean gnerateTestCaseSingle(TestCaseGenerationConfiguration generationConfiguration, TestCaseService testCaseService, TestCandidateMetadata testCandidateShell) throws Exception {
        TestCandidateMetadata loadedTestCandidate = ApplicationManager.getApplication().executeOnPooledThread(
                () -> insidiousService.getSessionInstance()
                        .getTestCandidateById(testCandidateShell.getEntryProbeIndex(), true)).get();
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
        TestCaseUnit testCaseUnit = testCaseService.buildTestCaseUnit(generationConfiguration);
        List<TestCaseUnit> testCaseUnit1 = new ArrayList<>();
        testCaseUnit1.add(testCaseUnit);
        TestSuite testSuite = new TestSuite(testCaseUnit1);
        insidiousService.getJUnitTestCaseWriter().saveTestSuite(testSuite);
        return false;
    }

    @Override
    public void onCandidateSelected(TestCandidateMetadata testCandidateMetadata, MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {


//            ContextMenuPopupHandler.Simple group = new ContextMenuPopupHandler.Simple("io.unlogged");

//            ActionGroup actionGroup = group.getActionGroup(e);

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


        GridBagConstraints gbcForFakeComponent = createGBCForFakeComponent();
        gbcForFakeComponent.gridy = itemPanel.getComponentCount();
        itemPanel.add(new JPanel(), gbcForFakeComponent, itemPanel.getComponentCount());
        itemPanel.add(unloggedSDKOnboarding.getComponent(), createGBCForProcessStartedComponent());

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
        if (candidateQueryLatch != null) {
            candidateQueryLatch.decrementAndGet();
            candidateQueryLatch = null;
        }
        stompStatusComponent.setDisconnected();
        scanEventListener.ended();
    }

    public void showDirectInvoke(MethodAdapter method) {
        if (directInvokeComponent == null) {
            directInvokeComponent = new MethodDirectInvokeComponent(insidiousService, this);
            JComponent content = directInvokeComponent.getContent();
            content.setMinimumSize(new Dimension(-1, 400));
            content.setMaximumSize(new Dimension(-1, 500));
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                directInvokeComponent.renderForMethod(method, null);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        ApplicationManager.getApplication().invokeLater(() -> {
            southPanel.removeAll();
            southPanel.add(directInvokeComponent.getContent(), BorderLayout.CENTER);

            southPanel.revalidate();
            southPanel.repaint();
            historyStreamScrollPanel.revalidate();
            historyStreamScrollPanel.repaint();
        });

    }

    public void showNewDeclaredMockCreator(JavaMethodAdapter javaMethodAdapter,
                                           PsiMethodCallExpression psiMethodCallExpression) {
        onMethodFocussed(javaMethodAdapter);
        MockDefinitionEditor mockEditor = new MockDefinitionEditor(MethodUnderTest.fromMethodAdapter(javaMethodAdapter),
                psiMethodCallExpression, insidiousService.getProject(), declaredMock -> {
            String newMockId = insidiousService.saveMockDefinition(declaredMock);
            InsidiousNotification.notifyMessage("Mock definition updated", NotificationType.INFORMATION);
            mainPanel.revalidate();
            mainPanel.repaint();
        }, component -> {
            southPanel.removeAll();
            scrollContainer.revalidate();
            scrollContainer.repaint();
        });
        JComponent content = mockEditor.getComponent();
        content.setMinimumSize(new Dimension(-1, 500));
        content.setMaximumSize(new Dimension(-1, 600));

        ApplicationManager.getApplication().invokeLater(() -> {
            southPanel.removeAll();
            southPanel.add(content, BorderLayout.CENTER);
            scrollContainer.revalidate();
            scrollContainer.repaint();
            historyStreamScrollPanel.revalidate();
            historyStreamScrollPanel.repaint();
            itemPanel.revalidate();
            itemPanel.repaint();
        });
    }


    public void removeDirectInvoke() {
        southPanel.remove(directInvokeComponent.getContent());
        directInvokeComponent = null;
        southPanel.revalidate();
        southPanel.repaint();
        scrollContainer.revalidate();
        scrollContainer.repaint();
    }

    public void setConnectedAndWaiting() {
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

    public void setConnected() {
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
//            while (true) {
            final TestCandidateMetadata testCandidateMetadata = incomingQueue.poll(1000, TimeUnit.MILLISECONDS);
            if (testCandidateMetadata == null) {
                return;
            }
            CountDownLatch pollerCDL = new CountDownLatch(1);
            ApplicationManager.getApplication().invokeLater(() -> {
                try {

                    List<TestCandidateMetadata> remainingItems = new ArrayList<>();
                    remainingItems.add(testCandidateMetadata);
                    incomingQueue.drainTo(remainingItems);
//                    while (remainingItems.size() > 100) {
//                        remainingItems.remove(0);
//                    }

                    for (TestCandidateMetadata remainingItem : remainingItems) {
                        acceptSingle(remainingItem);
                    }
                    while (itemPanel.getComponentCount() > 100) {
                        itemPanel.remove(0);
                    }


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

    private void acceptSingle(TestCandidateMetadata testCandidateMetadata) {
//        logger.warn("entr acceptSingle: " + testCandidateMetadata);
        String className = testCandidateMetadata.getFullyQualifiedClassname();
        String methodName = testCandidateMetadata.getMainMethod().getMethodName();
        String key = className + "." +
                methodName;
        AtomicInteger countAtomic = countByMethodName.get(key);
        if (countAtomic == null) {
            countAtomic = new AtomicInteger(0);
            countByMethodName.put(key, countAtomic);
        }

        int count = countAtomic.incrementAndGet();
        if (count > 40) {
            if (!filterModel.getIncludedClassNames().contains(className) ||
                    !filterModel.getIncludedMethodNames().contains(methodName)) {
                filterModel.getExcludedMethodNames().add(methodName);
                filterModel.getExcludedClassNames().add(className);
                InsidiousNotification.notifyMessage("Excluded [" + key + "] from live view. If you want to see them " +
                                "include the class [" + className + "] and method [" + methodName + "] in filters.",
                        NotificationType.INFORMATION);
                return;

            }
        }

        if (testCandidateMetadata.getExitProbeIndex() > lastEventId) {
            lastEventId = testCandidateMetadata.getExitProbeIndex();
        }
//        if (stompItems.size() > 0) {
//
////            StompItem last = stompItems.get(stompItems.size() - 1);
////            long gapStartIndex = last.getTestCandidate().getExitProbeIndex();
////            long gapEndIndex = testCandidateMetadata.getEntryProbeIndex();
////            int count = insidiousService.getMethodCallCountBetween(gapStartIndex, gapEndIndex);
////            if (count > 0) {
////                AFewCallsLater aFewCallsLater = new AFewCallsLater(gapStartIndex, gapEndIndex, count,
////                        this);
////
////                JScrollBar verticalScrollBar = historyStreamScrollPanel.getVerticalScrollBar();
////                int scrollPosition = verticalScrollBar.getValue();
////
////                JPanel labelPanel = aFewCallsLater.getComponent();
////
////                JPanel rowPanel = new JPanel(new BorderLayout());
////                rowPanel.add(labelPanel, BorderLayout.CENTER);
////                rowPanel.add(createLinePanel(createLineComponent()), BorderLayout.EAST);
////                GridBagConstraints gbcForLeftMainComponent = createGBCForLeftMainComponent(
////                        itemPanel.getComponentCount());
//////                makeSpace(0);
////                itemPanel.add(rowPanel, gbcForLeftMainComponent, 0);
////
////            }
//        }
        addCandidateToUi(testCandidateMetadata, itemPanel.getComponentCount());
        itemPanel.revalidate();
        scrollContainer.revalidate();
        scrollContainer.repaint();
//        logger.warn("exit acceptSingle: " + testCandidateMetadata);

    }

    public void setSession(ExecutionSession executionSession) {
        if (candidateQueryLatch != null) {
            candidateQueryLatch.decrementAndGet();
            candidateQueryLatch = null;
        }
    }

    public void onMethodFocussed(MethodAdapter method) {
        if (method == null) {
            lastMethodFocussed = null;
            return;
        }
        String newMethodName = method.getName();
        ClassAdapter containingClass = method.getContainingClass();
        List<String> newClassNameList = new ArrayList<>();
        newClassNameList.add(containingClass.getQualifiedName());
        for (ClassAdapter aSuper : containingClass.getSupers()) {
            newClassNameList.add(aSuper.getQualifiedName());
        }

        JavaPsiImplementationHelper jsp = JavaPsiImplementationHelper.getInstance(method.getProject());

        Collection<PsiClass> childClasses = ClassInheritorsSearch.search(
                ApplicationManager.getApplication().runReadAction(
                        (Computable<PsiClass>) () -> method.getPsiMethod().getContainingClass())).findAll();

        for (PsiClass childClass : childClasses) {
            newClassNameList.add(ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) childClass::getQualifiedName));
        }


        MethodUnderTest newMethodAdapter = ApplicationManager.getApplication().runReadAction(
                (Computable<MethodUnderTest>) () -> MethodUnderTest.fromMethodAdapter(method));
        if (lastMethodFocussed != null) {
            if (lastMethodFocussed.getMethodHashKey().equals(newMethodAdapter.getMethodHashKey())) {
                // same method focussed again
                return;
            }
        }

        lastMethodFocussed = newMethodAdapter;
        if (filterModel.isFollowEditor()) {

            if (filterModel.getIncludedMethodNames().size() == 1 && filterModel.getIncludedMethodNames()
                    .contains(newMethodName)) {
                if (filterModel.getIncludedClassNames().size() == 1 && filterModel.getIncludedClassNames()
                        .containsAll(newClassNameList)) {
                    if (filterModel.getExcludedClassNames().size() == 0 && filterModel.getExcludedMethodNames()
                            .size() == 0) {
                        // already set
                        return;
                    }
                }
            }

            clearFilter();
            filterModel.getIncludedMethodNames().add(newMethodName);
            filterModel.getIncludedClassNames().addAll(newClassNameList);
            updateFilterLabel();
            resetAndReload();
        }
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
                        " this plugin" + requiredVersion + ". Please update the unlogged-sdk version in your pom" +
                        ".xml/build.gradle",
                NotificationType.ERROR);
        notification.setImportant(true);
        notification.setSubtitle("Use unlogged-sdk:" + requiredVersion.toString());
        notification.setIcon(UIUtils.UNLOGGED_ICON_DARK_SVG);
        Notifications.Bus.notify(notification);
    }
}
