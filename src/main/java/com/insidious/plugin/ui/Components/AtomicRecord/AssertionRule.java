package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AssertionRule {
    private JPanel mainPanel;
    private JPanel alignerParent;
    private JTextField nameSelector;
    private JComboBox<AssertionType> operationSelector;
    private JTextField valueField;
    private JLabel closeLabel;
    private JLabel statusIconLabel;
    private JPanel leftAligner;
    private AssertionBlockManager parentBlock;
    private AssertionElement referenceElement;
    private AtomicAssertion assertion;

    public AssertionRule(AssertionBlockManager assertionBlock, AtomicAssertion payload)
    {
        setupOptions();
        this.assertion = payload;
        this.parentBlock = assertionBlock;
//        this.statusIconLabel.setText(payload.getstat());
        this.nameSelector.setText(payload.getKey()!=null ? payload.getKey() : "Nothing selected");
//        this.operationSelector.setSelectedItem(payload.getAssertionType()!=null ? payload.getAssertionType() : operationSelector.getItemAt(0));
        this.valueField.setText(payload.getExpectedValue()!=null ? payload.getExpectedValue() : "");
        setupListeners();
    }

    private void setupOptions()
    {
        for (AssertionType type : AssertionType.values()) {
            operationSelector.addItem(type);
        }
    }

    public void setupListeners()
    {
            closeLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    deleteRule();
                }
            });
    }

    private void toggleOperation() {
        if(!statusIconLabel.getText().equals("Where"))
        {
            if (statusIconLabel.getText().equals("AND"))
            {
                statusIconLabel.setText("OR");
            }
            else
            {
                statusIconLabel.setText("AND");
            }
        }
    }

    public JPanel getMainPanel()
    {
        return mainPanel;
    }

    public void deleteRule()
    {
        this.parentBlock.removeAssertionElement(referenceElement);
    }

    @Override
    public String toString() {
        return getAtomicAssertion().toString();
    }

    public AtomicAssertion getAtomicAssertion()
    {
        assertion.setAssertionType((AssertionType) this.operationSelector.getSelectedItem());
        assertion.setExpectedValue(this.valueField.getText().trim());
        return assertion;
    }

    public void setAssertionElement(AssertionElement ruleElement) {
        this.referenceElement = ruleElement;
    }
}
