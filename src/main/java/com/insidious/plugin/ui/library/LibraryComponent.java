package com.insidious.plugin.ui.library;

import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.intellij.openapi.project.Project;
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

    public LibraryComponent(Project project) {
        insidiousService = project.getService(InsidiousService.class);
        atomicRecordService = project.getService(AtomicRecordService.class);

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
                atomicRecordService.deleteMockDefinition(item);
                reloadItems();
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
                atomicRecordService.deleteStoredCandidate(item.getMethod(), item.getCandidateId());
                reloadItems();
            }

            @Override
            public void onEdit(StoredCandidate item) {

            }
        };

        filterModel = project.getService(
                InsidiousConfigurationState.class).getLibraryFilterModel();

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

                    SavedTestCandidateItemPanel candidatePanel = new SavedTestCandidateItemPanel(candidate,
                            STORED_CANDIDATE_ITEM_LIFE_CYCLE_LISTENER);
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
