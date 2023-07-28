package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AssertionBlockControlPanel {
    public static final JBColor SELECTED_LOGICAL_OPERATOR_COLOR = new JBColor(new Color(38, 117, 191),
            new Color(38, 117, 191));
    private final AssertionBlockManager parentBlock;
    private final Color defaultColor;
    private final AtomicAssertion atomicAssertion;
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

    public AssertionBlockControlPanel(AssertionBlockManager parent, AtomicAssertion atomicAssertion) {

        this.atomicAssertion = atomicAssertion;
        RoundedLineBorder logicalOperatorRoundedBorder = new RoundedLineBorder(
                new JBColor(new Color(0, 0, 0, 39), new Color(0, 0, 0, 39)),
                5);
        andPanel.setBorder(logicalOperatorRoundedBorder);
        orPanel.setBorder(logicalOperatorRoundedBorder);

        defaultColor = andLabel.getBackground();
        parentBlock = parent;


        notCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                if (atomicAssertion.getAssertionType() == AssertionType.ALLOF
                        || atomicAssertion.getAssertionType() == AssertionType.NOTALLOF) {
                    setCondition("AND");
                } else {
                    setCondition("OR");
                }
            }
        });

        switch (atomicAssertion.getAssertionType()) {
            case ALLOF:
                andPanel.setBackground(SELECTED_LOGICAL_OPERATOR_COLOR);
                orPanel.setBackground(defaultColor);
                break;
            case NOTALLOF:
                andPanel.setBackground(SELECTED_LOGICAL_OPERATOR_COLOR);
                orPanel.setBackground(defaultColor);
                notCheckBox.setSelected(true);
                break;
            case ANYOF:
                orPanel.setBackground(SELECTED_LOGICAL_OPERATOR_COLOR);
                andPanel.setBackground(defaultColor);
                break;
            case NOTANYOF:
                orPanel.setBackground(SELECTED_LOGICAL_OPERATOR_COLOR);
                andPanel.setBackground(defaultColor);
                notCheckBox.setSelected(true);
                break;
        }

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
        if (condition.equalsIgnoreCase("and")) {
            if (notCheckBox.isSelected()) {
                atomicAssertion.setAssertionType(AssertionType.NOTALLOF);
            } else {
                atomicAssertion.setAssertionType(AssertionType.ALLOF);
            }
            andPanel.setBackground(SELECTED_LOGICAL_OPERATOR_COLOR);
            orPanel.setBackground(defaultColor);
        } else {
            if (notCheckBox.isSelected()) {
                atomicAssertion.setAssertionType(AssertionType.NOTANYOF);
            } else {
                atomicAssertion.setAssertionType(AssertionType.ANYOF);
            }
            orPanel.setBackground(SELECTED_LOGICAL_OPERATOR_COLOR);
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

    private void deleteGroup() {
        parentBlock.removeAssertionGroup();
    }

}
