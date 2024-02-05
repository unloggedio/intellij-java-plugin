package com.insidious.plugin.ui.library;

import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.stomp.LibraryFilterState;
import com.insidious.plugin.ui.stomp.StompComponent;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LibraryComponent {
    private final InsidiousService insidiousService;
    private final AtomicRecordService atomicRecordService;
    private final LibraryFilterState filterModel;
    private JPanel mainPanel;
    private JPanel northPanelContainer;
    private JPanel controlPanel;
    private JLabel reloadButton;
    private JLabel deleteButton;
    private JLabel filterButton;
    private JPanel scrollContainer;
    private JScrollPane itemScrollPanel;
    private JPanel infoPanel;
    private JLabel selectedCountLabel;
    private JLabel selectAllLabel;
    private JLabel clearSelectionLabel;
    private JLabel clearFilterLabel;
    private JLabel filterAppliedLabel;
    private JPanel southPanel;
    private JCheckBox mocksCheckBox;
    private JCheckBox testsCheckBox;

    public LibraryComponent(Project project) {
        insidiousService = project.getService(InsidiousService.class);
        atomicRecordService = project.getService(AtomicRecordService.class);

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

    }

    private boolean isAcceptable(DeclaredMock declaredMock) {
        return true;
    }

    private boolean isAcceptable(StoredCandidate storedCandidate) {
        return true;
    }

    private void reloadItems() {
        Map<String, List<DeclaredMock>> mocksByClass = atomicRecordService.getAllDeclaredMocks()
                .stream().collect(Collectors.groupingBy(DeclaredMock::getFieldTypeName));
        Set<String> classNamesList = mocksByClass.keySet();
        ArrayList<String> sortedClassNameList = new ArrayList<>(classNamesList);
        sortedClassNameList.sort(null);

        JPanel itemsContainer = new JPanel();
        itemsContainer.setLayout(new GridLayout(0, 1));
        itemScrollPanel.setViewportView(itemsContainer);

        ItemLifeCycleListener<DeclaredMock> mockItemLifeCycleListener = new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(DeclaredMock item) {

            }

            @Override
            public void onDelete(DeclaredMock item) {

            }

            @Override
            public void onEdit(DeclaredMock item) {

            }
        };

        for (String className : sortedClassNameList) {
            List<DeclaredMock> mocksForClass = new ArrayList<>(mocksByClass.get(className));
            mocksForClass.sort(null);
            for (DeclaredMock declaredMock : mocksForClass) {
                if (!isAcceptable(declaredMock)) {
                    continue;
                }
                DeclaredMockItemPanel mockPanel = new DeclaredMockItemPanel(declaredMock,
                        mockItemLifeCycleListener);
                JComponent component = mockPanel.getComponent();
                component.setMaximumSize(new Dimension(500, StompComponent.COMPONENT_HEIGHT));
                component.setMinimumSize(new Dimension(300, StompComponent.COMPONENT_HEIGHT));
                itemsContainer.add(component, new GridConstraints());
            }
        }


        List<StoredCandidate> testCandidates = atomicRecordService.getAllTestCandidates();

        Map<String, List<StoredCandidate>> candidatesByClassName = testCandidates.stream()
                .collect(Collectors.groupingBy(e -> e.getMethod().getClassName()));

        ArrayList<String> names = new ArrayList<>(candidatesByClassName.keySet());
        names.sort(null);

        ItemLifeCycleListener<StoredCandidate> storedCandidateItemLifeCycleListener = new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(StoredCandidate item) {

            }

            @Override
            public void onDelete(StoredCandidate item) {

            }

            @Override
            public void onEdit(StoredCandidate item) {

            }
        };


        for (String name : names) {
            List<StoredCandidate> candidates = candidatesByClassName.get(name);

            for (StoredCandidate candidate : candidates) {
                if (!isAcceptable(candidate)) {
                    continue;
                }

                SavedTestCandidateItemPanel candidatePanel = new SavedTestCandidateItemPanel(candidate,
                        storedCandidateItemLifeCycleListener);
                JComponent component = candidatePanel.getComponent();
                component.setMaximumSize(new Dimension(500, StompComponent.COMPONENT_HEIGHT));
                component.setMinimumSize(new Dimension(300, StompComponent.COMPONENT_HEIGHT));
                itemsContainer.add(component, new GridConstraints());

            }

        }


    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
