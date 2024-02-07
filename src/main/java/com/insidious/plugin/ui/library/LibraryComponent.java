package com.insidious.plugin.ui.library;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
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

    public LibraryComponent(Project project) {
        insidiousService = project.getService(InsidiousService.class);
        atomicRecordService = project.getService(AtomicRecordService.class);


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

                    builder.setCenterPanel(new JLabel(
                            "Are you sure you want to delete " + selectedCount + " mock definition" + (selectedCount == 1 ? "s" : "")));

                    builder.setOkOperation(() -> {
                        for (DeclaredMock selectedMock : selectedMocks) {
                            atomicRecordService.deleteMockDefinition(selectedMock);
                        }
                        selectedMocks.clear();
                        InsidiousNotification.notifyMessage(
                                "Deleted " + selectedCount + " mock definition" + (selectedCount == 1 ? "s" : ""),
                                NotificationType.INFORMATION);
                        builder.dispose();

                    });
                    builder.showModal(true);

                } else if (filterModel.isShowTests()) {
                    int selectedCount = selectedCandidates.size();
                    DialogBuilder builder = new DialogBuilder(project);
                    builder.addOkAction();
                    builder.addCancelAction();
                    builder.setTitle("Confirm Delete");

                    builder.setCenterPanel(new JLabel(
                            "Are you sure you want to delete " + selectedCount + " relay test" + (selectedCount == 1 ? "s" : "")));

                    builder.setOkOperation(() -> {
                        for (StoredCandidate storedCandidate : selectedCandidates) {
                            atomicRecordService.deleteStoredCandidate(storedCandidate.getMethod(),
                                    storedCandidate.getCandidateId());
                        }
                        selectedMocks.clear();
                        InsidiousNotification.notifyMessage("Deleted " + selectedCount + " relay test"
                                        + (selectedCount == 1 ? "s" : ""),
                                NotificationType.INFORMATION);
                        builder.dispose();
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
            public void onUnSelect(DeclaredMock item) {
                selectedMocks.remove(item);
                updateSelectionLabel();
            }

            @Override
            public void onDelete(DeclaredMock item) {

                DialogBuilder builder = new DialogBuilder(project);
                builder.addOkAction();
                builder.addCancelAction();
                builder.setTitle("Confirm Delete");
                builder.setCenterPanel(new JLabel("Are you sure you want to delete " + "mock definition"));

                builder.setOkOperation(() -> {
                    atomicRecordService.deleteMockDefinition(item);
                    reloadItems();
                    InsidiousNotification.notifyMessage(
                            "Deleted " + "mock definition",
                            NotificationType.INFORMATION);
                    builder.dispose();
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
                builder.setCenterPanel(new JLabel("Are you sure you want to delete " + "replay test"));
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

        itemScrollPanel.revalidate();
        itemScrollPanel.repaint();


    }

    public JComponent getComponent() {
        return mainPanel;
    }


}
