package com.insidious.plugin.ui.Components;

import com.insidious.plugin.ui.UIUtils;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicBorders;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ListCard_OBV3 {
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel topContent;
    private JLabel headingText;
    private JLabel descriptionText;
    private JSeparator seperator_1;
    private JPanel centerContent;
    private JPanel contentContainer;
    private JPanel bottomPanel;
    private JButton refreshButton;
    private Set<String> selections = new TreeSet<>();
    private DependencyCardInformation dependencyCardInformation;
    private CardSelectionActionListener listener;

    public ListCard_OBV3(DependencyCardInformation dependencyCardInformation, CardSelectionActionListener listener) {

        this.listener = listener;
        this.dependencyCardInformation = dependencyCardInformation;
        this.headingText.setText(dependencyCardInformation.getHeading());
        this.descriptionText.setText(dependencyCardInformation.getDescription());
        this.refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                refreshDependencies();
            }
        });
        loadDependencies(dependencyCardInformation.getDependencies());
    }

    private void loadDependencies(List<String> missing_) {
        this.contentContainer.removeAll();
        if (missing_ == null || (missing_ != null && missing_.size() == 0)) {
            this.headingText.setIcon(UIUtils.NO_MISSING_DEPENDENCIES_ICON);
            return;
        }

        this.headingText.setIcon(UIUtils.MISSING_DEPENDENCIES_ICON);
        int GridRows = 16;
        if (missing_.size() > GridRows) {
            GridRows = missing_.size();
        }
        LayoutManager gridLayout = new VerticalFlowLayout(GridRows, 1);
//        Dimension d = new Dimension();
//        d.setSize(-1, 30);
        JPanel gridPanel = new JPanel(gridLayout);
        int i = 0;
        for (String dependency : missing_) {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            constraints.setIndent(16);
            JCheckBox label = new JCheckBox();
            label.setBorder(new BasicBorders.MarginBorder());
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (label.isSelected()) {
                        if (!selections.contains(dependency)) {
                            selections.add(dependency);
                        }
                    } else {
                        if (selections.contains(dependency)) {
                            selections.remove(dependency);
                        }
                    }
                    listener.setSelectionsForDependencyAddition(selections);
                }
            });
            label.setText(getDisplayTextForDependency(dependency));
            Font font = new Font("JetBrains Mono", 0, 15);
            label.setFont(font);
            gridPanel.add(label, constraints);
            i++;
        }
        gridPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        JScrollPane scrollPane = new JBScrollPane(gridPanel);
        EmptyBorder emptyBorder = JBUI.Borders.empty();
        scrollPane.setBorder(emptyBorder);
        contentContainer.setPreferredSize(scrollPane.getSize());
        contentContainer.add(scrollPane, BorderLayout.CENTER);
        if (missing_.size() <= 15) {
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        }
        this.contentContainer.revalidate();
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    public void refreshDependencies() {
        listener.refreshDependencies();
    }

    public String getDisplayTextForDependency(String dependency) {
        switch (dependency) {
            case "jackson-datatype-jsr310":
                return dependency + " (Check if you are using Java date/time types such as Instant, LocalDateTime, etc)";
            case "jackson-datatype-joda":
                return dependency + " (Check if you are using Joda data types)";
            case "jackson-datatype-hibernate5":
                return dependency + " (Check if you are using Hibernate specific datatypes and properties such as lazy-loading aspects)";
            case "jackson-datatype-jdk8":
                return dependency + " (Check if you are using new Java 8 datatypes such as Optional, OptionalLong, OptionalDouble)";
            default:
                return dependency;

        }
    }
}
