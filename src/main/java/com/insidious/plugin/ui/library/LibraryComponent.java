package com.insidious.plugin.ui.library;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.ImagePanel;
import com.insidious.plugin.ui.InsidiousUtils;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.insidious.plugin.ui.mocking.MockDefinitionEditor;
import com.insidious.plugin.ui.mocking.OnSaveListener;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Computable;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class LibraryComponent {

    private static final Logger logger = LoggerUtil.getInstance(LibraryComponent.class);
    public final ItemLifeCycleListener<DeclaredMock> MOCK_ITEM_LIFE_CYCLE_LISTENER;
    public final ItemLifeCycleListener<StoredCandidate> STORED_CANDIDATE_ITEM_LIFE_CYCLE_LISTENER;
    private final InsidiousService insidiousService;
    private final AtomicRecordService atomicRecordService;
    private final LibraryFilterState filterModel;
    private final Set<DeclaredMock> selectedMocks = new HashSet<>();
    private final Set<StoredCandidate> selectedCandidates = new HashSet<>();
    private final List<DeclaredMockItemPanel> listedMockItems = new ArrayList<>();
    private final List<StoredCandidateItemPanel> listedCandidateItems = new ArrayList<>();
    private final Project project;
    private final ActionToolbarImpl actionToolbar;
    private final AnAction addAction;
    private final ActionToolbarImpl timelineActionToolbar;
    private JPanel mainPanel;
    private JPanel northPanelContainer;
    private JPanel controlPanel;
    //    private JLabel reloadButton;
//    private JLabel deleteButton;
//    private JLabel showOptionsButton;
    private JPanel scrollContainer;
    private JScrollPane itemScrollPanel;
    private JLabel selectedCountLabel;
    private JLabel clearSelectionLabel;
    private JLabel clearFilterLabel;
    private JLabel filterAppliedLabel;
    private JRadioButton includeMocksCheckBox;
    private JRadioButton includeTestsCheckBox;
    //    private JButton mockingEnableRadioButton;
//    private JButton mockingDisableRadioButton;
    private JPanel southPanel;
    private JPanel callMockingControlPanel;
    private JPanel topContainerPanel;
    private JPanel preferencesButtonContainer;
    private JPanel selectedMocksControlPanel;
    private JLabel mockDescriptionLabel;
    private JPanel infoPanel;
    private JPanel infoInsideScrollerPanel;
    private MethodUnderTest lastMethodFocussed;
    private boolean currentMockInjectStatus = false;

    public LibraryComponent(Project project) {
        this.project = project;
        insidiousService = project.getService(InsidiousService.class);
        atomicRecordService = project.getService(AtomicRecordService.class);

        mockDescriptionLabel.setIcon(AllIcons.General.Information);

        addAction = new AnAction(() -> "Add", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

            }
        };


        AnAction enableMocksAction = new AnAction(() -> "Mock", UIUtils.LINK_ICON) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (!insidiousService.isAgentConnected()) {
                    InsidiousNotification.notifyMessage(
                            "Please start the application with unlogged-sdk and open the unlogged tool window to use",
                            NotificationType.WARNING
                    );
                    currentMockInjectStatus = false;
                    return;
                }
                insidiousService.enableMock(selectedMocks);
            }



            @Override
            public boolean displayTextInToolbar() {
                return true;
            }

        };


        AnAction disableMocksAction = new AnAction(() -> "Un-Mock", UIUtils.UNLINK_ICON) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (!insidiousService.isAgentConnected()) {
                    InsidiousNotification.notifyMessage(
                            "Please start the application with unlogged-sdk and open the unlogged tool window to use",
                            NotificationType.WARNING
                    );
                    currentMockInjectStatus = false;
                    return;
                }
                insidiousService.disableMock(selectedMocks);
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };

        List<AnAction> action33 = new ArrayList<>();
        action33.add(enableMocksAction);
        action33.add(disableMocksAction);

        timelineActionToolbar = new ActionToolbarImpl(
                "Library View Timeline Toolbar", new DefaultActionGroup(action33), true);
        timelineActionToolbar.setMiniMode(false);
        timelineActionToolbar.setForceMinimumSize(true);
        timelineActionToolbar.setTargetComponent(mainPanel);

        selectedMocksControlPanel.add(timelineActionToolbar.getComponent(), BorderLayout.CENTER);


        clearSelectionLabel.setVisible(false);
        clearSelectionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearSelectionLabel.setForeground(new JBColor(new Color(84, 138, 247),
                new Color(84, 138, 247)));
        clearSelectionLabel.setFont(new Font("SF Pro Text", Font.PLAIN, 13));
        clearSelectionLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                clearSelection();
                reloadItems();
            }
        });


        clearFilterLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearFilterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                filterModel.getExcludedMethodNames().clear();
                filterModel.getExcludedClassNames().clear();
                filterModel.getIncludedMethodNames().clear();
                filterModel.getIncludedClassNames().clear();
                updateFilterLabel();
                reloadItems();
            }
        });

//        showOptionsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        MouseAdapter showOptionsMouseAdapter = new MouseAdapter() {
//
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                showOptionsWindowPopup();
//            }
//        };
//        showOptionsButton.addMouseListener(showOptionsMouseAdapter);


        MOCK_ITEM_LIFE_CYCLE_LISTENER = new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(DeclaredMock item) {
                selectedMocks.add(item);
                updateSelectionLabel();
            }

            @Override
            public void onClick(DeclaredMock item) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    InsidiousUtils.focusInEditor(item.getFieldTypeName(),
                            item.getMethodName(), project);
                });
            }

            @Override
            public void onUnSelect(DeclaredMock item) {
                selectedMocks.remove(item);
                updateSelectionLabel();
            }

            @Override
            public void onDelete(DeclaredMock item) {

//                DialogWrapper dialogWrapper = new DialogWrapper(project) {
//                    {
//                        init();
//                        setTitle(JavaBundle.message("dialog.title.configure.annotations"));
//                    }
//
//                    @Override
//                    protected JComponent createCenterPanel() {
//                        final JPanel panel = new JPanel(new GridBagLayout());
//                        panel.add(new JTextField("this is a message"));
//                        return panel;
//                    }
//
//                    @Override
//                    protected void doOKAction() {
//                        super.doOKAction();
//                    }
//
//                    @Override
//                    protected @NotNull Action getOKAction() {
//                        return new AbstractAction() {
//                            @Override
//                            public void actionPerformed(ActionEvent e) {
//
//                            }
//                        };
//                    }
//                };
                DialogBuilder builder = new DialogBuilder(project);
                builder.addOkAction();
                builder.addCancelAction();
                builder.setTitle("Confirm Delete");

                JPanel deletePanel = LibraryComponent.deletePromptPanelBuilder(
                        "Are you sure you want to delete mock definition"
                );
                builder.setCenterPanel(deletePanel);

                builder.setOkOperation(() -> {
                    atomicRecordService.deleteMockDefinition(item);
                    reloadItems();
                    InsidiousNotification.notifyMessage(
                            "Deleted " + "mock definition",
                            NotificationType.INFORMATION);
                    builder.getDialogWrapper().close(0);
                });
                builder.showModal(true);
            }

            @Override
            public void onEdit(DeclaredMock item) {
                showMockEditor(item, new OnSaveListener() {
                    @Override
                    public void onSaveDeclaredMock(DeclaredMock declaredMock) {
                        //
                    }
                });
            }
        };
        STORED_CANDIDATE_ITEM_LIFE_CYCLE_LISTENER = new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(StoredCandidate item) {
                selectedCandidates.add(item);
                updateSelectionLabel();
            }

            @Override
            public void onClick(StoredCandidate item) {

            }

            @Override
            public void onUnSelect(StoredCandidate item) {
                selectedCandidates.remove(item);
                updateSelectionLabel();
            }

            @Override
            public void onDelete(StoredCandidate item) {

                DialogBuilder builder = new DialogBuilder(project);
                builder.addOkAction();
                builder.addCancelAction();
                builder.setTitle("Confirm Delete");
                JPanel deletePanel = LibraryComponent.deletePromptPanelBuilder(
                        "Are you sure you want to delete replay test"
                );
                builder.setCenterPanel(deletePanel);

                builder.setOkOperation(() -> {
                    atomicRecordService.deleteStoredCandidate(item.getMethod(), item.getCandidateId());
                    InsidiousNotification.notifyMessage(
                            "Deleted " + "replay test", NotificationType.INFORMATION);
                    reloadItems();
                    builder.dispose();
                });
                builder.showModal(true);
            }

            @Override
            public void onEdit(StoredCandidate item) {

            }
        };

        filterModel = project.getService(InsidiousConfigurationState.class).getLibraryFilterModel();

        atomicRecordService.checkPreRequisites();

        reloadItems();

        includeMocksCheckBox.addActionListener(e -> {
            boolean reload = false;
            if (includeMocksCheckBox.isSelected()) {
                if (filterModel.getItemFilterType().equals(ItemFilterType.SavedReplay)) {
                    filterModel.setItemFilterType(ItemFilterType.SavedMocks);
                    updateSelectionLabel();
                    reload = true;
                }
            }
            if (reload) {
                JSONObject eventProperties = new JSONObject();
                eventProperties.put("filter", filterModel.toString());
                UsageInsightTracker.getInstance().RecordEvent("TCSF_CONFIRM", eventProperties);
                updateFilterLabel();
                reloadItems();
            }
        });

        includeTestsCheckBox.addActionListener(e -> {
            boolean reload = false;
            if (includeTestsCheckBox.isSelected()) {
                if (filterModel.getItemFilterType().equals(ItemFilterType.SavedMocks)) {
                    filterModel.setItemFilterType(ItemFilterType.SavedReplay);
                    updateSelectionLabel();
                    reload = true;
                }
            }
            if (reload) {
                JSONObject eventProperties = new JSONObject();
                eventProperties.put("filter", filterModel.toString());
                UsageInsightTracker.getInstance().RecordEvent("TCSF_CONFIRM", eventProperties);

                updateFilterLabel();
                reloadItems();
            }
        });

        updateItemTypeFilter();

        updateFilterLabel();

        AnAction reloadAction = new AnAction(() -> "Reload", AllIcons.Actions.Refresh) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                reloadItems();
            }
        };

        AnAction deleteAction = new AnAction(() -> "Delete", AllIcons.Actions.GC) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                deleteSelectedItem();
            }
        };

        AnAction selectAllAction = new AnAction(() -> "Select All", AllIcons.Actions.Selectall) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                selectAll();
            }
        };

        List<AnAction> action11 = new ArrayList<>();
        action11.add(reloadAction);
        action11.add(deleteAction);
        action11.add(selectAllAction);
        actionToolbar = new ActionToolbarImpl(
                "Live View", new DefaultActionGroup(action11), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);

        controlPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);


        AnAction filterAction = new AnAction(() -> "Filter", AllIcons.General.Filter) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                JSONObject eventProperties = new JSONObject();
                eventProperties.put("count", selectedCandidates.size());
                UsageInsightTracker.getInstance().RecordEvent("ACTION_FILTER", eventProperties);
                showOptionsWindowPopup();
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        };


        List<AnAction> action22 = List.of(
                filterAction
        );


        ActionToolbarImpl actionToolbar2 = new ActionToolbarImpl(
                "Library Control Panel", new DefaultActionGroup(action22), true);
        actionToolbar2.setMiniMode(false);
        actionToolbar2.setForceMinimumSize(true);
        actionToolbar2.setTargetComponent(mainPanel);


        preferencesButtonContainer.add(actionToolbar2.getComponent(), BorderLayout.CENTER);

    }

    static JPanel deletePromptPanelBuilder(String deletePrompt) {
        // deletePrompt
        // deletePanelLeft
        JLabel deletePanelLeft = new JLabel();
        deletePanelLeft.setIcon(UIUtils.TRASH_PROMPT);

        // deletePromptUpper
        JLabel deletePromptUpper = new JLabel();
        deletePromptUpper.setText(deletePrompt);

        // deletePromptLower
        JLabel deletePromptLower = new JLabel("<html> Use git to track and restore later. </html>");
        deletePromptLower.setForeground(Color.GRAY);
        String defaultFont = UIManager.getFont("Label.font").getFontName();
        deletePromptLower.setFont(new Font("", Font.PLAIN, 12));
        deletePromptLower.setBorder(new EmptyBorder(5, 5, 5, 5));

        // deletePanelRight
        JPanel deletePanelRight = new JPanel();
        deletePanelRight.setLayout(new BoxLayout(deletePanelRight, BoxLayout.Y_AXIS));
        deletePanelRight.add(deletePromptUpper);
        deletePanelRight.add(deletePromptLower);

        // configure
        JPanel deletePanel = new JPanel();
        deletePanel.setLayout(new BoxLayout(deletePanel, BoxLayout.X_AXIS));
        deletePanel.add(deletePanelLeft);
        deletePanel.add(deletePanelRight);

        return deletePanel;
    }

    private void selectAll() {
        for (DeclaredMockItemPanel listedMockItem : listedMockItems) {
            selectedMocks.add(listedMockItem.getDeclaredMock());
            listedMockItem.setSelected(true);
        }
        for (StoredCandidateItemPanel listedMockItem : listedCandidateItems) {
            selectedCandidates.add(listedMockItem.getStoredCandidate());
            listedMockItem.setSelected(true);
        }
        updateSelectionLabel();
    }

    private void showOptionsWindowPopup() {
        LibraryFilter libraryFilter = new LibraryFilter(filterModel, lastMethodFocussed);
        JComponent component = libraryFilter.getComponent();
        ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(component, null);
        JBPopup unloggedPreferencesPopup = gutterMethodComponentPopup
                .setProject(project)
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(false)
                .setCancelOnOtherWindowOpen(false)
                .setCancelKeyEnabled(false)
                .setBelongsToGlobalPopupStack(false)
                .setTitle("Unlogged Preferences")
                .setTitleIcon(new ActiveIcon(UIUtils.UNLOGGED_ICON_DARK))
                .createPopup();
        component.setMaximumSize(new Dimension(500, 800));
        ComponentLifecycleListener<LibraryFilter> componentLifecycleListener = new ComponentLifecycleListener<LibraryFilter>() {
            @Override
            public void onClose(LibraryFilter component) {
                unloggedPreferencesPopup.cancel();
                updateFilterLabel();
                reloadItems();
            }
        };
        libraryFilter.setOnCloseListener(componentLifecycleListener);
        unloggedPreferencesPopup.showCenteredInCurrentWindow(project);
    }

    private void deleteSelectedItem() {
        if (filterModel.getItemFilterType().equals(ItemFilterType.SavedMocks)) {
            int selectedCount = selectedMocks.size();
            if (selectedCount < 1) {
                InsidiousNotification.notifyMessage("Nothing selected to delete", NotificationType.INFORMATION);
                return;
            }
            DialogBuilder builder = new DialogBuilder(project);
            builder.okActionEnabled(true);
            builder.setTitle("Confirm Delete");

            JPanel deletePanel = LibraryComponent.deletePromptPanelBuilder(
                    "Are you sure you want to delete " + selectedCount + " mock definition" + (selectedCount == 1 ? "s" : "")
            );
            builder.setCenterPanel(deletePanel);

            builder.setOkOperation(() -> {
                for (DeclaredMock selectedMock : selectedMocks) {
                    atomicRecordService.deleteMockDefinition(selectedMock);
                }
                clearSelection();
                InsidiousNotification.notifyMessage(
                        "Deleted " + selectedCount + " mock definition" + (selectedCount == 1 ? "s" : ""),
                        NotificationType.INFORMATION);
                builder.getDialogWrapper().close(0);
                builder.dispose();
                reloadItems();

            });
            builder.showModal(true);

        } else if (filterModel.getItemFilterType().equals(ItemFilterType.SavedReplay)) {
            int selectedCount = selectedCandidates.size();
            if (selectedCount < 1) {
                InsidiousNotification.notifyMessage("Nothing selected to delete", NotificationType.INFORMATION);
                return;
            }
            DialogBuilder builder = new DialogBuilder(project);
            builder.addOkAction();
            builder.addCancelAction();
            builder.setTitle("Confirm Delete");

            JPanel deletePanel = LibraryComponent.deletePromptPanelBuilder(
                    "Are you sure you want to delete " + selectedCount + " replay test" + (selectedCount == 1 ? "s" : "") + "?"
            );
            builder.setCenterPanel(deletePanel);

            builder.setOkOperation(() -> {
                for (StoredCandidate storedCandidate : selectedCandidates) {
                    atomicRecordService.deleteStoredCandidate(storedCandidate.getMethod(),
                            storedCandidate.getCandidateId());
                }
                selectedMocks.clear();
                InsidiousNotification.notifyMessage("Deleted " + selectedCount + " relay test"
                                + (selectedCount == 1 ? "s" : ""),
                        NotificationType.INFORMATION);
                builder.getDialogWrapper().close(0);
                builder.dispose();
                reloadItems();
            });
            builder.showModal(true);

        }
    }

    private void updateItemTypeFilter() {
        if (filterModel.getItemFilterType().equals(ItemFilterType.SavedMocks)) {
            includeMocksCheckBox.setSelected(true);
            includeTestsCheckBox.setSelected(false);
        } else {
            includeMocksCheckBox.setSelected(false);
            includeTestsCheckBox.setSelected(true);
        }
    }

    public void setLibraryFilterState(LibraryFilterState lfs) {
        clearFilter();
        filterModel.getIncludedClassNames().addAll(lfs.getIncludedClassNames());
        filterModel.getIncludedMethodNames().addAll(lfs.getIncludedMethodNames());
        filterModel.getExcludedClassNames().addAll(lfs.getExcludedClassNames());
        filterModel.getExcludedMethodNames().addAll(lfs.getExcludedMethodNames());
        filterModel.setItemFilterType(lfs.selectedItemType());
        filterModel.setFollowEditor(lfs.isFollowEditor());

        updateItemTypeFilter();
        updateFilterLabel();
        reloadItems();
    }

    private void clearSelection() {
        selectedMocks.clear();
        selectedCandidates.clear();

        for (DeclaredMockItemPanel listedMockItem : listedMockItems) {
            listedMockItem.setSelected(false);
        }
        for (StoredCandidateItemPanel listedMockItem : listedCandidateItems) {
            listedMockItem.setSelected(false);
        }
        updateSelectionLabel();
    }

    private void updateSelectionLabel() {
        int count;
        if (filterModel.getItemFilterType().equals(ItemFilterType.SavedMocks)) {
            count = selectedMocks.size();
            selectedCountLabel.setText(count + " selected");
        } else {
            count = selectedCandidates.size();
            selectedCountLabel.setText(count + " selected");
        }
        clearSelectionLabel.setVisible(count > 0);
    }

    private GridBagConstraints createGBCForFakeComponent(int yIndex) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = yIndex;
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


    public void updateFilterLabel() {
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

        if (filterModel.getItemFilterType().equals(ItemFilterType.SavedReplay)) {
            callMockingControlPanel.setVisible(false);
        } else {
            callMockingControlPanel.setVisible(true);
        }

    }

    private boolean isAcceptable(DeclaredMock declaredMock) {
        String className = declaredMock.getFieldTypeName();
        if (filterModel.getIncludedClassNames().size() > 0) {
            if (!filterModel.getIncludedClassNames().contains(className)) {
                return false;
            }
        }
        String methodName = declaredMock.getMethodName();
        if (filterModel.getIncludedMethodNames().size() > 0) {
            if (!filterModel.getIncludedMethodNames().contains(methodName)) {
                return false;
            }
        }
        if (filterModel.getExcludedClassNames().size() > 0) {
            if (filterModel.getExcludedClassNames().contains(className)) {
                return false;
            }
        }
        if (filterModel.getExcludedMethodNames().size() > 0) {
            if (filterModel.getExcludedMethodNames().contains(methodName)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAcceptable(StoredCandidate testCandidateMetadata) {
        String className = testCandidateMetadata.getMethod().getClassName();
        if (filterModel.getIncludedClassNames().size() > 0) {
            if (!filterModel.getIncludedClassNames().contains(className)) {
                return false;
            }
        }
        String methodName = testCandidateMetadata.getMethod().getName();
        if (filterModel.getIncludedMethodNames().size() > 0) {
            if (!filterModel.getIncludedMethodNames().contains(methodName)) {
                return false;
            }
        }
        if (filterModel.getExcludedClassNames().size() > 0) {
            if (filterModel.getExcludedClassNames().contains(className)) {
                return false;
            }
        }
        if (filterModel.getExcludedMethodNames().size() > 0) {
            if (filterModel.getExcludedMethodNames().contains(methodName)) {
                return false;
            }
        }
        return true;
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


    public void reloadItems() {

        JPanel itemsContainer = new JPanel();
        itemsContainer.setLayout(new GridLayout(0, 1));
        GridBagLayout mgr = new GridBagLayout();
        itemsContainer.setLayout(mgr);
        itemsContainer.setAlignmentY(0);
        itemsContainer.setAlignmentX(0);

        listedMockItems.clear();
        listedCandidateItems.clear();
        clearSelection();

        itemsContainer.setDoubleBuffered(true);
        itemScrollPanel.setViewportView(itemsContainer);
        JScrollBar vscroll = itemScrollPanel.getVerticalScrollBar();
        vscroll.setUnitIncrement(16);
        atomicRecordService.checkPreRequisites();
        int count = 1;
        if (filterModel.getItemFilterType().equals(ItemFilterType.SavedMocks)) {
            int countMocks = 0;
            Map<String, List<DeclaredMock>> mocksByClass = atomicRecordService.getAllDeclaredMocks()
                    .stream().collect(Collectors.groupingBy(DeclaredMock::getFieldTypeName));
            Set<String> classNamesList = mocksByClass.keySet();
            ArrayList<String> sortedClassNameList = new ArrayList<>(classNamesList);
            sortedClassNameList.sort(null);

            for (String className : sortedClassNameList) {
                List<DeclaredMock> mocksForClass = new ArrayList<>(mocksByClass.get(className));
                mocksForClass.sort(null);
                for (DeclaredMock declaredMock : mocksForClass) {
                    if (!isAcceptable(declaredMock)) {
                        continue;
                    }
                    countMocks++;
                    DeclaredMockItemPanel mockPanel = new DeclaredMockItemPanel(declaredMock,
                            MOCK_ITEM_LIFE_CYCLE_LISTENER, insidiousService);
                    listedMockItems.add(mockPanel);
                    JComponent component = mockPanel.getComponent();
                    itemsContainer.add(component, createGBCForLeftMainComponent(count++));
                }
            }
            if (countMocks == 0) {
                try {

                    JPanel picLabel1 = getImageLabel("images/mocks-commentary-image-1-scaled.png", 131);
                    itemsContainer.add(picLabel1, createGBCForLeftMainComponent(count++));
                    itemsContainer.add(new JSeparator(), createGBCForLeftMainComponent(count++));

                    JPanel picLabel2 = getImageLabel("images/mocks-commentary-image-2-scaled.png", 211);
                    itemsContainer.add(picLabel2, createGBCForLeftMainComponent(count++));
                    itemsContainer.add(new JSeparator(), createGBCForLeftMainComponent(count++));

                    JPanel picLabel3 = getImageLabel("images/mocks-commentary-image-3-scaled.png", 158);
                    itemsContainer.add(picLabel3, createGBCForLeftMainComponent(count++));


                } catch (IOException e) {
                    //
                }
            }
        }

        if (filterModel.selectedItemType().equals(ItemFilterType.SavedReplay)) {

            List<StoredCandidate> testCandidates = atomicRecordService.getAllTestCandidates();

            int countReplay = 0;
            Map<String, List<StoredCandidate>> candidatesByClassName = testCandidates.stream()
                    .collect(Collectors.groupingBy(e -> e.getMethod().getClassName()));

            ArrayList<String> names = new ArrayList<>(candidatesByClassName.keySet());
            names.sort(null);


            for (String name : names) {
                List<StoredCandidate> candidates = candidatesByClassName.get(name);

                for (StoredCandidate candidate : candidates) {
                    if (!isAcceptable(candidate)) {
                        continue;
                    }

                    StoredCandidateItemPanel candidatePanel = new StoredCandidateItemPanel(candidate,
                            STORED_CANDIDATE_ITEM_LIFE_CYCLE_LISTENER, insidiousService.getProject());
                    listedCandidateItems.add(candidatePanel);
                    JComponent component = candidatePanel.getComponent();
                    itemsContainer.add(component, createGBCForLeftMainComponent(count++));
                    countReplay++;

                }

            }

            if (countReplay == 0) {
                try {

                    JPanel picLabel1 = getImageLabel("images/tests-commentary-image-1-scaled.png", 131);
                    itemsContainer.add(picLabel1, createGBCForLeftMainComponent(count++));
                    itemsContainer.add(new JSeparator(), createGBCForLeftMainComponent(count++));

                    JPanel picLabel2 = getImageLabel("images/tests-commentary-image-2-scaled.png", 131);
                    itemsContainer.add(picLabel2, createGBCForLeftMainComponent(count++));
                    itemsContainer.add(new JSeparator(), createGBCForLeftMainComponent(count++));

                    JPanel picLabel3 = getImageLabel("images/tests-commentary-image-3-scaled.png", 208);
                    itemsContainer.add(picLabel3, createGBCForLeftMainComponent(count++));

                    JPanel picLabel4 = getImageLabel("images/tests-commentary-image-4-scaled.png", 218);
                    itemsContainer.add(picLabel4, createGBCForLeftMainComponent(count++));

                } catch (IOException e) {
                    //
                }

            }

        }

        itemsContainer.add(new JPanel(), createGBCForFakeComponent(count++));

        ApplicationManager.getApplication().invokeLater(() -> {
            itemScrollPanel.revalidate();
            itemScrollPanel.repaint();
            itemScrollPanel.getParent().revalidate();
            itemScrollPanel.getParent().repaint();
        });


    }

    @NotNull
    private JPanel getImageLabel(String name, int height) throws IOException {
        BufferedImage myPicture = ImageIO.read(
                this.getClass().getClassLoader().getResourceAsStream(
                        name
                )
        );
        JLabel picLabel1 = new JLabel(new ImageIcon(myPicture));
        Dimension maximumSize = new Dimension(400, height);
        picLabel1.setMaximumSize(maximumSize);
        picLabel1.setMinimumSize(maximumSize);
        picLabel1.setPreferredSize(maximumSize);
        @NotNull JPanel jpanel = new JPanel();
        jpanel.setBorder(
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
        );
        jpanel.setLayout(new GridBagLayout());
        new JPanel();
        jpanel.add(picLabel1, new GridBagConstraints());
        return jpanel;
    }

    public JComponent getComponent() {
        return mainPanel;
    }


    public void showMockEditor(DeclaredMock declaredMock, OnSaveListener onSaveListener) {
        MethodUnderTest methodUnderTest = new MethodUnderTest(
                declaredMock.getMethodName(), declaredMock.getMethodHashKey().split("#")[2], 0,
                declaredMock.getFieldTypeName()
        );
        MockDefinitionEditor mockEditor = new MockDefinitionEditor(methodUnderTest, declaredMock,
                insidiousService.getProject(), declaredMockUpdated -> {
            atomicRecordService.saveMockDefinition(declaredMockUpdated);
            InsidiousNotification.notifyMessage("Mock definition updated", NotificationType.INFORMATION);
            southPanel.removeAll();
            scrollContainer.revalidate();
            scrollContainer.repaint();
            onSaveListener.onSaveDeclaredMock(declaredMockUpdated);
        }, component -> {
            southPanel.removeAll();
            scrollContainer.revalidate();
            scrollContainer.repaint();
        });
        JComponent content = mockEditor.getComponent();
        GraphicsDevice gd = MouseInfo.getPointerInfo().getDevice();
        int height = gd.getDisplayMode().getHeight();
        content.setMinimumSize(new Dimension(-1, height / 3));
        content.setPreferredSize(new Dimension(-1, height / 2));
        content.setMaximumSize(new Dimension(-1, height / 2));
        southPanel.removeAll();
        southPanel.add(content, BorderLayout.CENTER);
        ApplicationManager.getApplication().invokeLater(() -> {
            southPanel.revalidate();
            southPanel.repaint();
            southPanel.getParent().revalidate();
            southPanel.getParent().repaint();
        });

    }


    public void onMethodFocussed(MethodAdapter method) {
        String newMethodName = method.getName();
        String newClassName = method.getContainingClass().getQualifiedName();
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
                        .contains(newClassName)) {
                    if (filterModel.getExcludedClassNames().size() == 0 && filterModel.getExcludedMethodNames()
                            .size() == 0) {
                        // already set
                        return;
                    }
                }
            }
            clearFilter();
            filterModel.getIncludedMethodNames().add(newMethodName);
            filterModel.getIncludedClassNames().add(newClassName);
            updateFilterLabel();
            reloadItems();
        }
    }

    private void clearFilter() {
        filterModel.getIncludedMethodNames().clear();
        filterModel.getExcludedMethodNames().clear();
        filterModel.getIncludedClassNames().clear();
        filterModel.getExcludedClassNames().clear();
    }

    public void setMockStatus(boolean status) {

    }
}
