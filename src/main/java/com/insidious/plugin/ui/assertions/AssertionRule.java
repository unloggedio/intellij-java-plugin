package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

public class AssertionRule {
    private static final Logger logger = LoggerUtil.getInstance(AssertionRule.class);
    private final AssertionBlock manager;
    private final AtomicAssertion assertion;
    private final Project project;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JLabel nameSelector;
    private JLabel trashButton;
    private JPanel nameSelectorContainerPanel;
    private AssertionRuleEditPanel editPanel;

    public AssertionRule(AssertionBlock assertionBlock, AtomicAssertion atomicAssertion1, Project project) {
        this.assertion = atomicAssertion1;
        this.project = project;
        if (assertion.getAssertionType() == null) {
            assertion.setAssertionType(AssertionType.EQUAL);
        }
        nameSelectorContainerPanel.setBackground(JBColor.WHITE);
        topAligner.setBackground(JBColor.WHITE);
        mainPanel.setBackground(JBColor.WHITE);


        this.manager = assertionBlock;


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


        setupOptions();


        nameSelector.setBackground(JBColor.WHITE);
        updateLabel();
//        this.valueField.setText("<html><pre>" + text + "</pre></html>");


//        operationSelector.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                String selectedItem = (String) operationSelector.getText();
//                logger.warn("Operator selected: " + selectedItem);
//                switch (selectedItem) {
//                    case "is":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.EQUAL);
//                        break;
//                    case "is not":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_EQUAL);
//                        break;
//                    case ">":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.GREATER_THAN);
//                        break;
//                    case "<":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.LESS_THAN);
//                        break;
//                    case "<=":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.LESS_THAN_OR_EQUAL);
//
//                        break;
//                    case ">=":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.GREATER_THAN_OR_EQUAL);
//                        break;
//
//                    case "is null":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NULL);
//                        break;
//                    case "is not null":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_NULL);
//                        break;
//                    case "is empty":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.EMPTY);
//                        break;
//                    case "is not empty":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_EMPTY);
//                        break;
//
//                    case "is true":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.TRUE);
//                        break;
//                    case "is false":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.FALSE);
//                        break;
//
//                    case "size is":
//                        assertion.setExpression(Expression.SIZE);
//                        assertion.setAssertionType(AssertionType.EQUAL);
//                        break;
//                    case "size is not":
//                        assertion.setExpression(Expression.SIZE);
//                        assertion.setAssertionType(AssertionType.NOT_EQUAL);
//                        break;
//
//                    case "length is":
//                        assertion.setExpression(Expression.LENGTH);
//                        assertion.setAssertionType(AssertionType.EQUAL);
//                        break;
//                    case "length is not":
//                        assertion.setExpression(Expression.LENGTH);
//                        assertion.setAssertionType(AssertionType.NOT_EQUAL);
//                        break;
//                    case "contains key in object":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.CONTAINS_KEY);
//                        break;
//                    case "contains item in array":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.CONTAINS_ITEM);
//                        break;
//                    case "not contains item in array":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_CONTAINS_ITEM);
//                        break;
//                    case "contains substring":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.CONTAINS_STRING);
//                        break;
//                    case "not contains key in object":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_CONTAINS_KEY);
//                        break;
//                    case "not contains substring":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_CONTAINS_STRING);
//                        break;
//                    case "matches regex":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.MATCHES_REGEX);
//                        break;
//                    case "not matches regex":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_MATCHES_REGEX);
//                        break;
//                    case "equals ignore case":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.EQUAL_IGNORE_CASE);
//                        break;
//                    case "starts with":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.STARTS_WITH);
//                        break;
//                    case "not starts with":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_STARTS_WITH);
//                        break;
//                    case "ends with":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.ENDS_WITH);
//                        break;
//                    case "not ends with":
//                        assertion.setExpression(Expression.SELF);
//                        assertion.setAssertionType(AssertionType.NOT_ENDS_WITH);
//                        break;
//
//                }
////                updateResult();
//            }
//        });

        trashButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        trashButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                deleteRule();
            }
        });
//        updateResult();
    }

    @NotNull
    private static String trimValue(String expectedValue) {
        String text = expectedValue != null ? expectedValue : "";
        if (text.length() > 40) {
            text = text.substring(0, 37) + "...";
        }
        return text;
    }

    private void updateLabel() {
        nameSelector.setText(
                "<html><pre>" + assertion.getKey() + " " + getOperationText(assertion) + " " + trimValue(
                        assertion.getExpectedValue()) + "</pre></html>");
    }

    public void showEditForm(Supplier<List<KeyValue>> keyValueSupplier) {
        nameSelectorContainerPanel.removeAll();
        editPanel = getEditPanel(keyValueSupplier);

        nameSelectorContainerPanel.add(editPanel.getComponent(), new GridConstraints());
        nameSelectorContainerPanel.revalidate();
        nameSelectorContainerPanel.repaint();
    }

    @NotNull
    private AssertionRuleEditPanel getEditPanel(Supplier<List<KeyValue>> keyValueSupplier) {
        return new AssertionRuleEditPanel(assertion, new ItemLifeCycleListener<>() {
            @Override
            public void onSelect(AtomicAssertion item) {

            }

            @Override
            public void onClick(AtomicAssertion item) {

            }

            @Override
            public void onUnSelect(AtomicAssertion item) {
                hideEditForm();
            }

            @Override
            public void onDelete(AtomicAssertion item) {

            }

            @Override
            public void onEdit(AtomicAssertion item) {
                hideEditForm();
            }
        }, keyValueSupplier, project);
    }

    public void hideEditForm() {
        nameSelectorContainerPanel.removeAll();
        updateLabel();
        nameSelectorContainerPanel.add(nameSelector, new GridConstraints());
        nameSelectorContainerPanel.revalidate();
        nameSelectorContainerPanel.repaint();
    }

    private String getOperationText(AtomicAssertion atomicAssertion) {
        String text;
        text = "";
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
                        String operatorText = "is";
                        text = getOperatorText(operatorText);
                        break;
                    case SIZE:
                        text = getOperatorText("size is");
                        break;
                    case LENGTH:
                        text = getOperatorText("length is");
                        break;
                }
                break;
            case EQUAL_IGNORE_CASE:
                text = getOperatorText("equals ignore case");
                break;
            case NOT_EQUAL:
                switch (atomicAssertion.getExpression()) {
                    case SELF:
                        text = getOperatorText("is not");
                        break;
                    case SIZE:
                        text = getOperatorText("size is not");
                        break;
                    case LENGTH:
                        text = getOperatorText("length is not");
                        break;
                }

                break;
            case FALSE:
                text = getOperatorText("is false");
                break;
            case MATCHES_REGEX:
                text = getOperatorText("matches regex");
                break;
            case NOT_MATCHES_REGEX:
                text = getOperatorText("not matches regex");
                break;
            case TRUE:
                text = getOperatorText("is true");
                break;
            case LESS_THAN:
                text = getOperatorText("<");
                break;
            case LESS_THAN_OR_EQUAL:
                text = getOperatorText("<=");
                break;
            case GREATER_THAN:
                text = getOperatorText(">");
                break;
            case GREATER_THAN_OR_EQUAL:
                text = getOperatorText(">=");
                break;
            case NOT_NULL:
                text = getOperatorText("is not null");
                break;
            case NULL:
                text = getOperatorText("is null");
                break;
            case EMPTY:
                text = getOperatorText("is empty");
                break;
            case NOT_EMPTY:
                text = getOperatorText("is not empty");
                break;
            case CONTAINS_KEY:
                text = getOperatorText("contains key in object");
                break;
            case CONTAINS_ITEM:
                text = getOperatorText("contains item in array");
                break;
            case NOT_CONTAINS_ITEM:
                text = getOperatorText("not contains item in array");
                break;
            case CONTAINS_STRING:
                text = getOperatorText("contains substring");
                break;
            case NOT_CONTAINS_KEY:
                text = getOperatorText("not contains key in object");
                break;
            case NOT_CONTAINS_STRING:
                text = getOperatorText("not contains substring");
                break;
        }
        return text;
    }

    private String getOperatorText(String operatorText) {
        return operatorText;
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

    public void saveEdit() {
        if (editPanel != null) {
            AtomicAssertion assertionRule = editPanel.getUpdatedValue();
            assertion.setAssertionType(assertionRule.getAssertionType());
            assertion.setKey(assertionRule.getKey());
            assertion.setExpectedValue(assertionRule.getExpectedValue());
            hideEditForm();
            editPanel = null;
        }
    }
}
