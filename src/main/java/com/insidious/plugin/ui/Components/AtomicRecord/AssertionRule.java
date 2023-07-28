package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.Expression;
import com.insidious.plugin.ui.Components.AtomicAssertionConstants;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AssertionRule {
    private static final Logger logger = LoggerUtil.getInstance(AssertionRule.class);
    private final AssertionBlock manager;
    private final AtomicAssertion assertion;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JTextField nameSelector;
    private JComboBox<String> operationSelector;
    private JTextField valueField;
    private JLabel statusIconLabel;
    private JPanel leftAligner;
    private JButton trashButton;

    public AssertionRule(AssertionBlock assertionBlock, AtomicAssertion atomicAssertion) {
        this.assertion = atomicAssertion;
        this.manager = assertionBlock;

        this.nameSelector.setText(atomicAssertion.getKey() != null ? atomicAssertion.getKey() : "Nothing selected");

        this.valueField.setText(atomicAssertion.getExpectedValue() != null ? atomicAssertion.getExpectedValue() : "");

        setupOptions();
        if (atomicAssertion.getAssertionType() == null) {
            atomicAssertion.setAssertionType(AssertionType.EQUAL);
        }

        switch (atomicAssertion.getAssertionType()) {

            case ALLOF:
                // cant happen in rule
                break;
            case ANYOF:
                // cant happen in rule
                break;
            case NOTALLOF:
                // cant happen in rule
                break;
            case NOTANYOF:
                // cant happen in rule
                break;
            case EQUAL:
                switch (atomicAssertion.getExpression()) {
                    case SELF:
                        operationSelector.setSelectedItem("is");
                        break;
                    case SIZE:
                        operationSelector.setSelectedItem("size is");
                        break;
                    case LENGTH:
                        operationSelector.setSelectedItem("length is");
                        break;
                }
                break;
            case NOT_EQUAL:
                switch (atomicAssertion.getExpression()) {
                    case SELF:
                        operationSelector.setSelectedItem("is not");
                        break;
                    case SIZE:
                        operationSelector.setSelectedItem("size is not");
                        break;
                    case LENGTH:
                        operationSelector.setSelectedItem("length is not");
                        break;
                }

                break;
            case FALSE:
                operationSelector.setSelectedItem("is false");
                break;
            case TRUE:
                operationSelector.setSelectedItem("is true");
                break;
            case LESS_THAN:
                operationSelector.setSelectedItem("<");
                break;
            case LESS_THAN_OR_EQUAL:
                operationSelector.setSelectedItem("<=");
                break;
            case GREATER_THAN:
                operationSelector.setSelectedItem(">");
                break;
            case GREATER_THAN_OR_EQUAL:
                operationSelector.setSelectedItem(">=");
                break;
            case CONTAINS:
                operationSelector.setSelectedItem("contains");
                break;
            case NOT_CONTAINS:
                operationSelector.setSelectedItem("not contains");
                break;
            case NOT_NULL:
                operationSelector.setSelectedItem("is not null");
                break;
            case NULL:
                operationSelector.setSelectedItem("is null");
                break;
            case EMPTY:
                operationSelector.setSelectedItem("is empty");
                break;
            case NOT_EMPTY:
                operationSelector.setSelectedItem("is not empty");
                break;
        }

        operationSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) operationSelector.getSelectedItem();
                logger.warn("Operator selected: " + selectedItem);
                switch (selectedItem) {
                    case "is":
                        assertion.setAssertionType(AssertionType.EQUAL);
                        break;
                    case "is not":
                        assertion.setAssertionType(AssertionType.NOT_EQUAL);
                        break;
                    case ">":
                        assertion.setAssertionType(AssertionType.GREATER_THAN);
                        break;
                    case "<":
                        assertion.setAssertionType(AssertionType.LESS_THAN);
                        break;
                    case "<=":
                        assertion.setAssertionType(AssertionType.LESS_THAN_OR_EQUAL);

                        break;
                    case ">=":
                        assertion.setAssertionType(AssertionType.GREATER_THAN_OR_EQUAL);
                        break;

                    case "contains":
                        assertion.setAssertionType(AssertionType.CONTAINS);
                        break;
                    case "does not contains":
                        assertion.setAssertionType(AssertionType.NOT_CONTAINS);
                        break;

                    case "is null":
                        assertion.setAssertionType(AssertionType.NULL);
                        break;
                    case "is not null":
                        assertion.setAssertionType(AssertionType.NOT_NULL);
                        break;
                    case "is empty":
                        assertion.setAssertionType(AssertionType.EMPTY);
                        break;
                    case "is not empty":
                        assertion.setAssertionType(AssertionType.NOT_EMPTY);
                        break;

                    case "is true":
                        assertion.setAssertionType(AssertionType.TRUE);
                        break;
                    case "is false":
                        assertion.setAssertionType(AssertionType.FALSE);
                        break;

                    case "size is":
                        assertion.setExpression(Expression.SIZE);
                        assertion.setAssertionType(AssertionType.EQUAL);
                        break;
                    case "size is not":
                        assertion.setExpression(Expression.SIZE);
                        assertion.setAssertionType(AssertionType.NOT_EQUAL);
                        break;

                    case "length is":
                        assertion.setExpression(Expression.LENGTH);
                        assertion.setAssertionType(AssertionType.EQUAL);
                        break;
                    case "length is not":
                        assertion.setExpression(Expression.LENGTH);
                        assertion.setAssertionType(AssertionType.NOT_EQUAL);
                        break;
                }
                updateResult();
            }
        });


        valueField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                logger.warn("Value field updated: " + valueField.getText().trim());
                assertion.setExpectedValue(valueField.getText().trim());
                updateResult();
            }
        });

        trashButton.addActionListener(e -> deleteRule());
        updateResult();
    }

    public void updateResult() {
        AssertionResult thisResult = manager.executeAssertion(assertion);
        Boolean result = thisResult.getResults().get(assertion.getId());
        if (result) {
            topAligner.setBackground(AtomicAssertionConstants.PASSING_COLOR);
            mainPanel.setBackground(AtomicAssertionConstants.PASSING_COLOR);
            leftAligner.setBackground(AtomicAssertionConstants.PASSING_COLOR);
        } else {
            topAligner.setBackground(AtomicAssertionConstants.FAILING_COLOR);
            leftAligner.setBackground(AtomicAssertionConstants.FAILING_COLOR);
        }
    }

    public AssertionBlock getBlock() {
        return manager;
    }

    private void setupOptions() {
        operationSelector.addItem("is");
        operationSelector.addItem("is not");
        operationSelector.addItem(">");
        operationSelector.addItem(">=");
        operationSelector.addItem("<");
        operationSelector.addItem("<=");

//        operationSelector.addItem("contains");
//        operationSelector.addItem("does not contains");

        operationSelector.addItem("is null");
        operationSelector.addItem("is not null");
        operationSelector.addItem("is empty");
        operationSelector.addItem("is not empty");
        operationSelector.addItem("is true");
        operationSelector.addItem("is false");

        operationSelector.addItem("size is");
        operationSelector.addItem("size is not");
        operationSelector.addItem("length is");
        operationSelector.addItem("length is not");

    }


    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void deleteRule() {
        manager.deleteAssertionRule(this);
    }

    @Override
    public String toString() {
        return getAtomicAssertion().toString();
    }

    public AtomicAssertion getAtomicAssertion() {
        return assertion;
    }

}
