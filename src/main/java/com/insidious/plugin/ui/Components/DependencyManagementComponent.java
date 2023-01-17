package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.OnboardingService;
import com.insidious.plugin.ui.UI_Utils;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class DependencyManagementComponent {
    private JPanel mainPanel;
    private JPanel packagesSelectionPanel;
    private JPanel packageManagementBorderParent;
    private JPanel checksPanel;
    private JPanel bpp;
    private JPanel topPanel;
    private JPanel highlightPanel;
    private JLabel mpHighlightLabel;
    private JPanel bottomPanel;
    private JButton addToDependenciesButton;
    private JPanel missingDependenciesPanel;
    private JLabel body_label;
    private Map<String,String> missing_ = new TreeMap<>();
    private OnboardingService onboardingService;
    private InsidiousService insidiousService;
    private HashSet<String> selectedDependencies = new HashSet<>();

    public JPanel getComponent() {
        return mainPanel;
    }

    public DependencyManagementComponent(Map<String,String> missing, OnboardingService onboardingService, InsidiousService insidiousService)
    {
        this.missing_= missing;
        this.onboardingService = onboardingService;
        this.insidiousService = insidiousService;
        addToDependenciesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checksPanel.setBorder(new LineBorder(UI_Utils.yellow_alert));
        addToDependenciesButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onboardingService.postProcessDependencies(missing_, new HashSet<>(missing_.keySet()));
            }
        });
        updateDependenciesTab();
    }

    private void updateDependenciesTab()
    {
        this.missingDependenciesPanel.removeAll();
        this.missing_ = new TreeMap<>();
        Map<String,String> dep_Status = onboardingService.getDependencyStatus();
        for(String key : dep_Status.keySet())
        {
            if(dep_Status.get(key)==null &&
                !insidiousService.getProjectTypeInfo().
                        getDependencies_addedManually().contains(key))
            {
                this.missing_.put(key,dep_Status.get(key));
            }
        }
        System.out.println("Missing _ "+missing_);
        int GridRows = 16;
        if (missing_.size() > GridRows) {
            GridRows = missing_.size();
        }
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        Dimension d = new Dimension();
        d.setSize(-1, 30);
        JPanel gridPanel = new JPanel(gridLayout);
        int i = 0;
        for (String dependency : missing_.keySet()) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            constraints.setIndent(16);
            JLabel label = new JLabel();
            label.setText(dependency);
            label.setIcon(UI_Utils.ARROW_YELLOW_RIGHT);
            label.setBorder(new EmptyBorder(4,8,0,0));
            gridPanel.add(label, constraints);
            i++;
        }
        gridPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        EmptyBorder emptyBorder = new EmptyBorder(0, 0, 0, 0);
        scrollPane.setBorder(emptyBorder);
        missingDependenciesPanel.setPreferredSize(scrollPane.getSize());
        missingDependenciesPanel.add(scrollPane, BorderLayout.CENTER);
        if (missing_.size() <= 15) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        this.missingDependenciesPanel.revalidate();
    }
}
