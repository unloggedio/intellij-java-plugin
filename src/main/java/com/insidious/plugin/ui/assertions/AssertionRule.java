package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.Expression;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.DocumentAdapter;


import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.*;

public class AssertionRule {
    private static final Logger logger = LoggerUtil.getInstance(AssertionRule.class);
    private final AssertionBlock manager;
    private final AtomicAssertion assertion;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JLabel nameSelector;
    private JLabel operationSelector;
    private JLabel valueField;
    private JButton trashButton;

    public AssertionRule(AssertionBlock assertionBlock, AtomicAssertion atomicAssertion) {
        this.assertion = atomicAssertion;
        this.manager = assertionBlock;

        this.nameSelector.setText(atomicAssertion.getKey() != null ? atomicAssertion.getKey() : "Nothing selected");

//        Color currentBackgroundColor = nameSelector.getBackground();
//        nameSelector.setEditable(false);
//        nameSelector.setBackground(currentBackgroundColor);
//        nameSelector.setBackground(JBColor.BLACK);
//        nameSelector.setOpaque(true);
//        nameSelector.repaint();
        nameSelector.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                e.consume();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                e.consume();
            }
        });

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
                        operationSelector.setText("is");
                        break;
                    case SIZE:
                        operationSelector.setText("size is");
                        break;
                    case LENGTH:
                        operationSelector.setText("length is");
                        break;
                }
                break;
            case EQUAL_IGNORE_CASE:
                operationSelector.setText("equals ignore case");
                break;
            case NOT_EQUAL:
                switch (atomicAssertion.getExpression()) {
                    case SELF:
                        operationSelector.setText("is not");
                        break;
                    case SIZE:
                        operationSelector.setText("size is not");
                        break;
                    case LENGTH:
                        operationSelector.setText("length is not");
                        break;
                }

                break;
            case FALSE:
                operationSelector.setText("is false");
                break;
            case MATCHES_REGEX:
                operationSelector.setText("matches regex");
                break;
            case NOT_MATCHES_REGEX:
                operationSelector.setText("not matches regex");
                break;
            case TRUE:
                operationSelector.setText("is true");
                break;
            case LESS_THAN:
                operationSelector.setText("<");
                break;
            case LESS_THAN_OR_EQUAL:
                operationSelector.setText("<=");
                break;
            case GREATER_THAN:
                operationSelector.setText(">");
                break;
            case GREATER_THAN_OR_EQUAL:
                operationSelector.setText(">=");
                break;
            case NOT_NULL:
                operationSelector.setText("is not null");
                break;
            case NULL:
                operationSelector.setText("is null");
                break;
            case EMPTY:
                operationSelector.setText("is empty");
                break;
            case NOT_EMPTY:
                operationSelector.setText("is not empty");
                break;
            case CONTAINS_KEY:
                operationSelector.setText("contains key in object");
                break;
            case CONTAINS_ITEM:
                operationSelector.setText("contains item in array");
                break;
            case NOT_CONTAINS_ITEM:
                operationSelector.setText("not contains item in array");
                break;
            case CONTAINS_STRING:
                operationSelector.setText("contains substring");
                break;
            case NOT_CONTAINS_KEY:
                operationSelector.setText("not contains key in object");
                break;
            case NOT_CONTAINS_STRING:
                operationSelector.setText("not contains substring");
                break;
        }

        operationSelector.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String selectedItem = (String) operationSelector.getText();
                logger.warn("Operator selected: " + selectedItem);
                switch (selectedItem) {
                    case "is":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.EQUAL);
                        break;
                    case "is not":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_EQUAL);
                        break;
                    case ">":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.GREATER_THAN);
                        break;
                    case "<":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.LESS_THAN);
                        break;
                    case "<=":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.LESS_THAN_OR_EQUAL);

                        break;
                    case ">=":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.GREATER_THAN_OR_EQUAL);
                        break;

                    case "is null":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NULL);
                        break;
                    case "is not null":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_NULL);
                        break;
                    case "is empty":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.EMPTY);
                        break;
                    case "is not empty":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_EMPTY);
                        break;

                    case "is true":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.TRUE);
                        break;
                    case "is false":
                        assertion.setExpression(Expression.SELF);
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
                    case "contains key in object":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.CONTAINS_KEY);
                        break;
                    case "contains item in array":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.CONTAINS_ITEM);
                        break;
                    case "not contains item in array":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_CONTAINS_ITEM);
                        break;
                    case "contains substring":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.CONTAINS_STRING);
                        break;
                    case "not contains key in object":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_CONTAINS_KEY);
                        break;
                    case "not contains substring":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_CONTAINS_STRING);
                        break;
                    case "matches regex":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.MATCHES_REGEX);
                        break;
                    case "not matches regex":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_MATCHES_REGEX);
                        break;
                    case "equals ignore case":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.EQUAL_IGNORE_CASE);
                        break;
                    case "starts with":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.STARTS_WITH);
                        break;
                    case "not starts with":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_STARTS_WITH);
                        break;
                    case "ends with":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.ENDS_WITH);
                        break;
                    case "not ends with":
                        assertion.setExpression(Expression.SELF);
                        assertion.setAssertionType(AssertionType.NOT_ENDS_WITH);
                        break;

                }
                updateResult();
            }
        });


//        valueField.setCaretPosition(0);
//        valueField.getDocument().addDocumentListener(new DocumentAdapter() {
//            @Override
//            protected void textChanged( DocumentEvent e) {
//                logger.warn("Value field updated: " + valueField.getText().trim());
//                assertion.setExpectedValue(valueField.getText().trim());
//                updateResult();
//            }
//        });

        trashButton.addActionListener(e -> deleteRule());
        updateResult();
    }

    public void updateResult() {
        AssertionResult thisResult = manager.executeAssertion(assertion);
        Boolean result = thisResult.getResults().get(assertion.getId());
        if (result) {
//            topAligner.setBorder(new LineBorder(AtomicAssertionConstants.PASSING_COLOR));
            topAligner.setBackground(UIUtils.ASSERTION_PASSING_COLOR);
//            mainPanel.setBackground(AtomicAssertionConstants.PASSING_COLOR);
//            leftAligner.setBackground(AtomicAssertionConstants.PASSING_COLOR);
        } else {
//            topAligner.setBorder(new LineBorder(AtomicAssertionConstants.FAILING_COLOR));
            topAligner.setBackground(UIUtils.ASSERTION_FAILING_COLOR);
//            leftAligner.setBackground(AtomicAssertionConstants.FAILING_COLOR);
        }
    }

    public AssertionBlock getBlock() {
        return manager;
    }

    private void setupOptions() {
//        operationSelector.addItem("is");
//        operationSelector.addItem("is not");
//        operationSelector.addItem(">");
//        operationSelector.addItem(">=");
//        operationSelector.addItem("<");
//        operationSelector.addItem("<=");
//
//
//        operationSelector.addItem("is null");
//        operationSelector.addItem("is empty");
//        operationSelector.addItem("is true");
//
//        operationSelector.addItem("is false");
//        operationSelector.addItem("is not empty");
//        operationSelector.addItem("is not null");
//
//        operationSelector.addItem("size is");
//        operationSelector.addItem("size is not");
//        operationSelector.addItem("length is");
//        operationSelector.addItem("length is not");
//
//        operationSelector.addItem("equals ignore case");
//
//        operationSelector.addItem("contains substring");
//        operationSelector.addItem("matches regex");
//        operationSelector.addItem("contains item in array");
//        operationSelector.addItem("contains key in object");
//        operationSelector.addItem("starts with");
//        operationSelector.addItem("ends with");
//
//        operationSelector.addItem("not contains substring");
//        operationSelector.addItem("not matches regex");
//        operationSelector.addItem("not contains item in array");
//        operationSelector.addItem("not contains key in object");
//        operationSelector.addItem("not starts with");
//        operationSelector.addItem("not ends with");


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
