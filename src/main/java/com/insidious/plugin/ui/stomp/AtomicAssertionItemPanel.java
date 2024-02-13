package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class AtomicAssertionItemPanel {
    private final AtomicAssertion atomicAssertion;
    private final ItemLifeCycleListener<AtomicAssertion> atomicAssertionLifeListener;
    private final Project project;
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JLabel valueName;
    private JLabel keyName;
    private JLabel operationName;

    public AtomicAssertionItemPanel(AtomicAssertion atomicAssertion, ItemLifeCycleListener<AtomicAssertion> atomicAssertionLifeListener, Project project) {

        this.atomicAssertion = atomicAssertion;
        this.atomicAssertionLifeListener = atomicAssertionLifeListener;
        this.project = project;

        titleLabel.setText(atomicAssertion.getKey());
        keyName.setText(String.valueOf(atomicAssertion.getExpression()));
        operationName.setText(String.valueOf(atomicAssertion.getAssertionType()));
        valueName.setText(atomicAssertion.getExpectedValue());
    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
