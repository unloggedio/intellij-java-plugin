package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AssertionBlockControlPanel {
    public static final JBColor SELECTED_LOGICAL_OPERATOR_COLOR = new JBColor(new Color(38, 117, 191),
            new Color(38, 117, 191));
    private final static Logger logger = LoggerUtil.getInstance(AssertionBlockControlPanel.class);
    private final AssertionBlockManager parentBlock;
    //    private final Color defaultColor;
    private final AtomicAssertion atomicAssertion;
    private JPanel mainPanel;
    private JPanel labelHolder;
    //    private JLabel andLabel;
//    private JLabel orLabel;
    private JCheckBox notCheckBox;
    private JPanel buttonParent;
    private JButton ruleButton;
    private JButton groupButton;
    private JButton deleteButton;
    private JLabel iconLabel;
    private JPanel logicalConditionSelectPanel;
    private JComboBox<String> logicalControlComboBox;
//    private JPanel andPanel;
//    private JPanel orPanel;

    public AssertionBlockControlPanel(AssertionBlockManager blockManager, AtomicAssertion atomicAssertion, boolean isRootCondition) {

        this.atomicAssertion = atomicAssertion;
//        RoundedLineBorder logicalOperatorRoundedBorder = new RoundedLineBorder(
//                new JBColor(new Color(0, 0, 0, 39), new Color(0, 0, 0, 39)), 5);

        if (isRootCondition) {
            labelHolder.remove(iconLabel);
            buttonParent.remove(deleteButton);
        }

        logicalControlComboBox.addItem("AND");
        logicalControlComboBox.addItem("OR");


//        andPanel.setBorder(logicalOperatorRoundedBorder);
//        orPanel.setBorder(logicalOperatorRoundedBorder);

//        defaultColor = andLabel.getBackground();
        parentBlock = blockManager;


        switch (atomicAssertion.getAssertionType()) {
            case ALLOF:
                logicalControlComboBox.setSelectedItem("AND");
                break;
            case NOTALLOF:
                logicalControlComboBox.setSelectedItem("AND");
                notCheckBox.setSelected(true);
                break;
            case ANYOF:
                logicalControlComboBox.setSelectedItem("OR");
                break;
            case NOTANYOF:
                logicalControlComboBox.setSelectedItem("OR");
                notCheckBox.setSelected(true);
                break;
        }

        notCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logger.warn("Not check box triggered: " + notCheckBox.isSelected());

                if (atomicAssertion.getAssertionType() == AssertionType.ALLOF ||
                        atomicAssertion.getAssertionType() == AssertionType.NOTALLOF) {
                    setCondition("AND");
                } else {
                    setCondition("OR");
                }
            }
        });
        ;


        ruleButton.addActionListener(e -> addNewRule());
        groupButton.addActionListener(e -> addNewGroup());
        deleteButton.addActionListener(e -> deleteGroup());

        logicalControlComboBox.addActionListener(e -> {
            String selectedLogicalOperator = (String) logicalControlComboBox.getSelectedItem();
            if (selectedLogicalOperator == null) {
                selectedLogicalOperator = "AND";
            }
            setCondition(selectedLogicalOperator);
        });

//        MouseAdapter selectAnd = new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                setCondition("AND");
//            }
//        };
//        MouseAdapter selectOr = new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                setCondition("OR");
//            }
//        };

//        andLabel.addMouseListener(selectAnd);
//        orLabel.addMouseListener(selectOr);
//        andPanel.addMouseListener(selectAnd);
//        orPanel.addMouseListener(selectOr);
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
//            andPanel.setBackground(SELECTED_LOGICAL_OPERATOR_COLOR);
//            orPanel.setBackground(defaultColor);
        } else {
            if (notCheckBox.isSelected()) {
                atomicAssertion.setAssertionType(AssertionType.NOTANYOF);
            } else {
                atomicAssertion.setAssertionType(AssertionType.ANYOF);
            }
//            orPanel.setBackground(SELECTED_LOGICAL_OPERATOR_COLOR);
//            andPanel.setBackground(defaultColor);
        }

        parentBlock.executeAssertion(atomicAssertion);

//        andLabel.repaint();
//        orLabel.repaint();
    }


    public void addNewRule() {
        parentBlock.addNewRule();
    }

    public void addNewGroup() {
        parentBlock.addNewGroup();
    }

    private void deleteGroup() {
        parentBlock.removeAssertionGroup();
    }

}
