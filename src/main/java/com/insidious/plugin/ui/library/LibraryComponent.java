package com.insidious.plugin.ui.library;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.intellij.notification.NotificationType;
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
    public final ItemLifeCycleListener<DeclaredMock> MOCK_ITEM_LIFE_CYCLE_LISTENER;
    public final ItemLifeCycleListener<StoredCandidate> STORED_CANDIDATE_ITEM_LIFE_CYCLE_LISTENER;
    private final InsidiousService insidiousService;
    private final AtomicRecordService atomicRecordService;
    private final LibraryFilterState filterModel;
    private final Set<DeclaredMock> selectedMocks = new HashSet<>();
    private final Set<StoredCandidate> selectedCandidates = new HashSet<>();
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
    private List<DeclaredMockItemPanel> listedMockItems = new ArrayList<>();
    private List<StoredCandidateItemPanel> storedCandidateItemPanels = new ArrayList<>();

    public LibraryComponent(Project project) {
        insidiousService = project.getService(InsidiousService.class);
        atomicRecordService = project.getService(AtomicRecordService.class);


        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
                    builder.addCancelAction();
                    builder.setTitle("Confirm Delete");
                    builder.setErrorText(
                            "Are you sure you want to delete " + selectedCount + " mock definition" + (selectedCount == 1 ? "s" : ""));
                    builder.setOkOperation(() -> {
                        for (DeclaredMock selectedMock : selectedMocks) {
                            atomicRecordService.deleteMockDefinition(selectedMock);
                        }
                        selectedMocks.clear();
                        InsidiousNotification.notifyMessage(
                                "Deleted " + selectedCount + " mock definition" + (selectedCount == 1 ? "s" : ""),
                                NotificationType.INFORMATION);
                    });

                } else if (filterModel.isShowTests()) {
                    int selectedCount = selectedCandidates.size();
                    DialogBuilder builder = new DialogBuilder(project);
                    builder.addCancelAction();
                    builder.setTitle("Confirm Delete");
                    builder.setErrorText(
                            "Are you sure you want to delete " + selectedCount + " relay test" + (selectedCount == 1 ? "s" : ""));
                    builder.setOkOperation(() -> {
                        for (StoredCandidate storedCandidate : selectedCandidates) {
                            atomicRecordService.deleteStoredCandidate(storedCandidate.getMethod(),
                                    storedCandidate.getCandidateId());
                        }
                        selectedMocks.clear();
                        InsidiousNotification.notifyMessage("Deleted " + selectedCount + " relay test"
                                        + (selectedCount == 1 ? "s" : ""),
                                NotificationType.INFORMATION);
                    });

                }

            }
        });

        MOCK_ITEM_LIFE_CYCLE_LISTENER = new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(DeclaredMock item) {
                selectedMocks.add(item);
            }

            @Override
            public void onUnSelect(DeclaredMock item) {
                selectedMocks.remove(item);
            }

            @Override
            public void onDelete(DeclaredMock item) {

                DialogBuilder builder = new DialogBuilder(project);
                builder.addCancelAction();
                builder.setTitle("Confirm Delete");
                builder.setErrorText("Are you sure you want to delete " + "mock definition");
                builder.setOkOperation(() -> {
                    atomicRecordService.deleteMockDefinition(item);
                    reloadItems();
                    InsidiousNotification.notifyMessage(
                            "Deleted " + "mock definition",
                            NotificationType.INFORMATION);
                });
            }

            @Override
            public void onEdit(DeclaredMock item) {

            }
        };
        STORED_CANDIDATE_ITEM_LIFE_CYCLE_LISTENER = new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(StoredCandidate item) {
                selectedCandidates.add(item);
            }

            @Override
            public void onUnSelect(StoredCandidate item) {
                selectedCandidates.remove(item);
            }

            @Override
            public void onDelete(StoredCandidate item) {

                DialogBuilder builder = new DialogBuilder(project);
                builder.addCancelAction();
                builder.setTitle("Confirm Delete");
                builder.setErrorText("Are you sure you want to delete " + "replay test");
                builder.setOkOperation(() -> {
                    atomicRecordService.deleteStoredCandidate(item.getMethod(), item.getCandidateId());
                    reloadItems();
                    InsidiousNotification.notifyMessage(
                            "Deleted " + "replay test",
                            NotificationType.INFORMATION);
                });

                reloadItems();
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
            if (includeMocksCheckBox.isSelected()) {
                filterModel.setShowMocks(true);
                filterModel.setShowTests(false);
            } else {
                filterModel.setShowMocks(false);
                filterModel.setShowTests(true);
            }
            updateFilterLabel();
            reloadItems();
        });

        includeTestsCheckBox.addActionListener(e -> {
            if (includeMocksCheckBox.isSelected()) {
                filterModel.setShowMocks(true);
                filterModel.setShowTests(false);
            } else {
                filterModel.setShowMocks(false);
                filterModel.setShowTests(true);
            }
            updateFilterLabel();
            reloadItems();
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
                for (StoredCandidateItemPanel storedCandidateItemPanel : storedCandidateItemPanels) {
                    selectedCandidates.add(storedCandidateItemPanel.getStoredCandidate());
                }
                for (DeclaredMockItemPanel listedMockItem : listedMockItems) {
                    selectedMocks.add(listedMockItem.getDeclaredMock());
                }
                for (StoredCandidateItemPanel storedCandidateItemPanel : storedCandidateItemPanels) {
                    storedCandidateItemPanel.setSelected(true);
                }
                for (DeclaredMockItemPanel listedMockItem : listedMockItems) {
                    listedMockItem.setSelected(true);
                }

            }
        });
        clearSelectionLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedCandidates.clear();
                selectedMocks.clear();
                for (StoredCandidateItemPanel storedCandidateItemPanel : storedCandidateItemPanels) {
                    storedCandidateItemPanel.setSelected(false);
                }
                for (DeclaredMockItemPanel listedMockItem : listedMockItems) {
                    listedMockItem.setSelected(false);
                }


            }
        });

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


    private void updateFilterLabel() {
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
        return true;
    }

    private boolean isAcceptable(StoredCandidate storedCandidate) {
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


    private void reloadItems() {

        JPanel itemsContainer = new JPanel();
        itemsContainer.setLayout(new GridLayout(0, 1));
        GridBagLayout mgr = new GridBagLayout();
        itemsContainer.setLayout(mgr);
        itemsContainer.setAlignmentY(0);
        itemsContainer.setAlignmentX(0);

        listedMockItems.clear();
        storedCandidateItemPanels.clear();

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
                            MOCK_ITEM_LIFE_CYCLE_LISTENER);
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
                            STORED_CANDIDATE_ITEM_LIFE_CYCLE_LISTENER);
                    storedCandidateItemPanels.add(candidatePanel);
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
