package com.insidious.plugin.ui.Components.AtomicRecord;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RuleAdditionPanel {
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel defaultControls;
    private JPanel emptyControls;
    private JButton addAssertionButton;
    private JPanel labelHolder;
    private JLabel andLabel;
    private JLabel orLabel;
    private JCheckBox notCheckBox;
    private JPanel buttonParent;
    private JButton ruleButton;
    private JButton groupButton;
    private JButton deleteButton;
    private JLabel iconLabel;
    private AssertionBlockManager parentBlock;
    private boolean isNested;
    private String operation = "AND";
    private Color defaultColor;

    public JPanel getMainPanel()
    {
        return this.mainPanel;
    }

    public RuleAdditionPanel(AssertionBlockManager parent, boolean showInitialFlow)
    {
        defaultColor = andLabel.getBackground();
        parentBlock = parent;
        if(showInitialFlow)
        {
            this.defaultControls.setVisible(false);
            this.emptyControls.setVisible(true);
        }
        else
        {
            this.defaultControls.setVisible(true);
            this.emptyControls.setVisible(false);
        }
        setNested(false);
        setCondition("AND");

        addAssertionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFirstCondition();
            }
        });
        ruleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewRule();
            }
        });
        groupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewGroup();
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteGroup();
            }
        });
        andLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setCondition("AND");
            }
        });
        orLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setCondition("OR");
            }
        });
    }

    private void setCondition(String condition) {
        this.operation = condition;
        if(condition.equalsIgnoreCase("and"))
        {
            andLabel.setBackground(Color.BLUE);
            orLabel.setBackground(defaultColor);
        }
        else
        {
            orLabel.setBackground(Color.BLUE);
            andLabel.setBackground(defaultColor);
        }
    }

    public void addFirstCondition()
    {
        this.emptyControls.setVisible(false);
        this.defaultControls.setVisible(true);
        //send updward call to add rule
        parentBlock.addFirstRule();
    }

    public void addNewRule()
    {
        //send add new rule trigger
        parentBlock.addNewRule();
    }

    public void addNewGroup()
    {
        //send add new assertion trigger
        parentBlock.addNewGroup();
    }

    public void setNested(boolean b) {
        this.isNested = b;
        if(isNested)
        {
            iconLabel.setVisible(true);
            deleteButton.setVisible(true);
        }
        else
        {
            deleteButton.setVisible(false);
            iconLabel.setVisible(false);
        }
    }

    private void deleteGroup()
    {
        parentBlock.removeAssertionGroup();
    }

    public String getSelectedCondition()
    {
        return notCheckBox.isSelected() ? "NOT "+operation : operation;
    }
}
