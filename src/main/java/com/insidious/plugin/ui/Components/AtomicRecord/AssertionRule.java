package com.insidious.plugin.ui.Components.AtomicRecord;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AssertionRule {
    private JPanel mainPanel;
    private JPanel alignerParent;
    private JComboBox<String> nameSelector;
    private JComboBox<String> operationSelector;
    private JTextField valueField;
    private JLabel closeLabel;
    private JLabel statusLabel;
    private JLabel contextLabel;
    private JPanel leftAligner;
    private AssertionBlock parentBlock;
    public AssertionRule(AssertionBlock assertionBlock)
    {
        this.parentBlock = assertionBlock;
        setupListeners();
    }

    public AssertionRule(AssertionBlock assertionBlock, RuleData payload)
    {
        this.parentBlock = assertionBlock;
        this.contextLabel.setText(payload.getContext());
        this.nameSelector.setSelectedItem(payload.getKey()!=null ? payload.getKey() : nameSelector.getItemAt(0));
        this.operationSelector.setSelectedItem(payload.getOperation()!=null ? payload.getOperation() : operationSelector.getItemAt(0));
        this.valueField.setText(payload.getExpectedValue()!=null ? payload.getExpectedValue() : "");
        setupListeners();
    }

    public void setupListeners()
    {
            closeLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    deleteRule();
                }
            });
            contextLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleOperation();
                }
            });
    }

    private void toggleOperation() {
        if(!contextLabel.getText().equals("Where"))
        {
            if (contextLabel.getText().equals("AND"))
            {
                contextLabel.setText("OR");
            }
            else
            {
                contextLabel.setText("AND");
            }
        }
    }

    public JPanel getMainPanel()
    {
        return mainPanel;
    }

    public void deleteRule()
    {
        this.parentBlock.removeRule(this);
    }

    @Override
    public String toString() {
        return getRuleData().toString();
    }

    public RuleData getRuleData()
    {
        return new RuleData(this.contextLabel.getText(),
                this.nameSelector.getSelectedItem().toString(),
                this.operationSelector.getSelectedItem().toString(),
                this.valueField.getText().trim(),
                null);
    }
}
