package com.insidious.plugin.ui.Components.AtomicRecord;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RuleAdditionPanel {
    private final AssertionBlockManager parentBlock;
    private final Color defaultColor;
    private JPanel mainPanel;
    private JPanel labelHolder;
    private JLabel andLabel;
    private JLabel orLabel;
    private JCheckBox notCheckBox;
    private JPanel buttonParent;
    private JButton ruleButton;
    private JButton groupButton;
    private JButton deleteButton;
    private JLabel iconLabel;
    private JPanel andPanel;
    private JPanel orPanel;
    private boolean isNested;
    private String operation = "AND";

    public RuleAdditionPanel(AssertionBlockManager parent, boolean showInitialFlow) {
        defaultColor = andLabel.getBackground();
        parentBlock = parent;

        setNested(false);
        setCondition("AND");

        ruleButton.addActionListener(e -> addNewRule());
        groupButton.addActionListener(e -> addNewGroup());
        deleteButton.addActionListener(e -> deleteGroup());
        MouseAdapter selectAnd = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setCondition("AND");
            }
        };
        MouseAdapter selectOr = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setCondition("OR");
            }
        };
        andLabel.addMouseListener(selectAnd);
        andPanel.addMouseListener(selectAnd);
        orLabel.addMouseListener(selectOr);
        orPanel.addMouseListener(selectOr);
    }

    public JPanel getMainPanel() {
        return this.mainPanel;
    }

    private void setCondition(String condition) {
        this.operation = condition;
        if (condition.equalsIgnoreCase("and")) {
            andPanel.setBackground(JBColor.BLUE);
            orPanel.setBackground(defaultColor);
        } else {
            orPanel.setBackground(JBColor.BLUE);
            andPanel.setBackground(defaultColor);
        }
        andLabel.repaint();
        orLabel.repaint();
    }


    public void addNewRule() {
        //send add new rule trigger
        parentBlock.addNewRule();
    }

    public void addNewGroup() {
        //send add new assertion trigger
        parentBlock.addNewGroup();
    }

    public void setNested(boolean b) {
        this.isNested = b;
        if (isNested) {
            iconLabel.setVisible(true);
            deleteButton.setVisible(true);
        } else {
            deleteButton.setVisible(false);
            iconLabel.setVisible(false);
        }
    }

    private void deleteGroup() {
        parentBlock.removeAssertionGroup();
    }

}
