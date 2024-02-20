package com.insidious.plugin.ui.library;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.InsidiousUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.java.JavaBundle;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private JPanel mainPanel;
    private JPanel northPanelContainer;
    private JPanel controlPanel;
    private JLabel reloadButton;
    private JLabel deleteButton;
    private JLabel showOptionsButton;
    private JPanel scrollContainer;
    private JScrollPane itemScrollPanel;
    private JPanel infoPanel;
    private JLabel selectedCountLabel;
    private JLabel selectAllLabel;
    private JLabel clearSelectionLabel;
    private JLabel clearFilterLabel;
    private JLabel filterAppliedLabel;
    private JPanel southPanel;
    private JRadioButton includeMocksCheckBox;
    private JRadioButton includeTestsCheckBox;
    private JPanel topContainerPanel;
    private JRadioButton mockingEnableRadioButton;
    private JRadioButton mockingDisableRadioButton;
    private MethodAdapter lastFocussedMethod;
    private boolean currentMockInjectStatus = false;

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
        deletePromptLower.setBorder(new EmptyBorder(5,5,5,5));

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

    public LibraryComponent(Project project) {
        insidiousService = project.getService(InsidiousService.class);
        atomicRecordService = project.getService(AtomicRecordService.class);

        ActionListener mockStatusChangeActionListener = e -> {
            List<DeclaredMock> allDeclaredMocks = insidiousService.getAllDeclaredMocks();
            if (mockingEnableRadioButton.isSelected() && !currentMockInjectStatus) {
                currentMockInjectStatus = true;
                insidiousService.injectMocksInRunningProcess(allDeclaredMocks);
            } else {
                insidiousService.removeMocksInRunningProcess(allDeclaredMocks);
            }
        };
        mockingDisableRadioButton.addActionListener(mockStatusChangeActionListener);
        mockingEnableRadioButton.addActionListener(mockStatusChangeActionListener);


        clearSelectionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearSelectionLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                clearSelection();
                reloadItems();
            }
        });


        selectAllLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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


        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (filterModel.isShowMocks()) {
                    int selectedCount = selectedMocks.size();
                    if (selectedCount < 1) {
                        // shouldnt happen
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

                } else if (filterModel.isShowTests()) {
                    int selectedCount = selectedCandidates.size();
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
        });

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

                DialogWrapper dialogWrapper = new DialogWrapper(project) {
                    {
                        init();
                        setTitle(JavaBundle.message("dialog.title.configure.annotations"));
                    }

                    @Override
                    protected JComponent createCenterPanel() {
                        final JPanel panel = new JPanel(new GridBagLayout());
                        panel.add(new JTextField("this is a message"));
                        return panel;
                    }

                    @Override
                    protected void doOKAction() {
                        super.doOKAction();
                    }

                    @Override
                    protected @NotNull Action getOKAction() {
                        return new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {

                            }
                        };
                    }
                };
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

        reloadButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reloadButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                reloadItems();
            }
        });
        reloadItems();

        includeMocksCheckBox.addActionListener(e -> {
            boolean reload = false;
            if (includeMocksCheckBox.isSelected()) {
                if (filterModel.isShowTests()) {
                    filterModel.setShowMocks(true);
                    filterModel.setShowTests(false);
                    updateSelectionLabel();
                    reload = true;
                }
            }
            if (reload) {
                updateFilterLabel();
                reloadItems();
            }
        });

        includeTestsCheckBox.addActionListener(e -> {
            boolean reload = false;
            if (includeTestsCheckBox.isSelected()) {
                if (filterModel.isShowMocks()) {
                    filterModel.setShowMocks(false);
                    filterModel.setShowTests(true);
                    updateSelectionLabel();
                    reload = true;
                }
            }
            if (reload) {
                updateFilterLabel();
                reloadItems();
            }
        });

        if (filterModel.isShowMocks()) {
            includeMocksCheckBox.setSelected(true);
            includeTestsCheckBox.setSelected(false);
        } else {
            includeMocksCheckBox.setSelected(false);
            includeTestsCheckBox.setSelected(true);
        }


        selectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (StoredCandidateItemPanel storedCandidateItemPanel : listedCandidateItems) {
                    selectedCandidates.add(storedCandidateItemPanel.getStoredCandidate());
                }
                for (DeclaredMockItemPanel listedMockItem : listedMockItems) {
                    selectedMocks.add(listedMockItem.getDeclaredMock());
                }
                for (StoredCandidateItemPanel storedCandidateItemPanel : listedCandidateItems) {
                    storedCandidateItemPanel.setSelected(true);
                }
                for (DeclaredMockItemPanel listedMockItem : listedMockItems) {
                    listedMockItem.setSelected(true);
                }

            }
        });

        updateFilterLabel();

    }

    public void setMockStatus(boolean status) {
        if (status) {
            mockingEnableRadioButton.setSelected(true);
            mockingDisableRadioButton.setSelected(false);
        } else {
            mockingEnableRadioButton.setSelected(false);
            mockingDisableRadioButton.setSelected(true);
        }
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
        if (filterModel.isShowMocks()) {
            count = selectedMocks.size();
            selectedCountLabel.setText(count + " selected");
        } else {
            count = selectedCandidates.size();
            selectedCountLabel.setText(count + " selected");
        }
        deleteButton.setVisible(count > 0);
        clearSelectionLabel.setVisible(count > 0);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

        itemScrollPanel.setViewportView(itemsContainer);

        int count = 1;
        if (filterModel.isShowMocks()) {
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
                    DeclaredMockItemPanel mockPanel = new DeclaredMockItemPanel(declaredMock,
                            MOCK_ITEM_LIFE_CYCLE_LISTENER, insidiousService.getProject());
                    listedMockItems.add(mockPanel);
                    JComponent component = mockPanel.getComponent();
                    itemsContainer.add(component, createGBCForLeftMainComponent(count++));
                }
            }
        }

        if (filterModel.isShowTests()) {

            List<StoredCandidate> testCandidates = atomicRecordService.getAllTestCandidates();

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

    public JComponent getComponent() {
        return mainPanel;
    }


//    public void showMockCreator(MethodUnderTest method, PsiMethodCallExpression callExpression) {
//        MockDefinitionEditor mockEditor = new MockDefinitionEditor(method, callExpression,
//                insidiousService.getProject(), declaredMock -> {
//                    atomicRecordService.saveMockDefinition(declaredMock);
//                    InsidiousNotification.notifyMessage("Mock definition updated", NotificationType.INFORMATION);
//                    southPanel.removeAll();
//                    scrollContainer.revalidate();
//                    scrollContainer.repaint();
//                });
//        JComponent content = mockEditor.getComponent();
//        content.setMinimumSize(new Dimension(-1, 400));
//        content.setMaximumSize(new Dimension(-1, 400));
//        southPanel.removeAll();
//        southPanel.add(content, BorderLayout.CENTER);
//
//    }

    public void onMethodFocussed(MethodAdapter method) {
        this.lastFocussedMethod = method;
    }
}
