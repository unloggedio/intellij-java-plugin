package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.ui.assertions.AssertionBlock;
import com.insidious.plugin.ui.assertions.AssertionBlockManager;
import com.insidious.plugin.ui.assertions.AssertionRule;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class AtomicAssertionItemPanel {
    private final AtomicAssertion atomicAssertion;
    private final ItemLifeCycleListener<AtomicAssertion> atomicAssertionLifeListener;
    private final Project project;
    private final AssertionBlock assertionBlock;
    private JPanel mainPanel;
    private JScrollPane assertionPanel;

    public AtomicAssertionItemPanel(AtomicAssertion atomicAssertion, ItemLifeCycleListener<AtomicAssertion> atomicAssertionLifeListener, Project project) {


        assertionBlock = new AssertionBlock(atomicAssertion, new AssertionBlockManager() {
            @Override
            public void addNewRule() {

            }

            @Override
            public void addNewGroup() {

            }

            @Override
            public AssertionResult executeAssertion(AtomicAssertion atomicAssertion) {
                return null;
            }

            @Override
            public void deleteAssertionRule(AssertionRule assertionRule) {

            }

            @Override
            public void removeAssertionGroup() {

            }

            @Override
            public KeyValue getCurrentTreeKey() {
                return null;
            }

            @Override
            public void removeAssertionGroup(AssertionBlock block) {

            }
        }, true);

        this.atomicAssertion = atomicAssertion;
        this.atomicAssertionLifeListener = atomicAssertionLifeListener;
        this.project = project;

        assertionPanel.setViewportView(assertionBlock.getContent());
        assertionPanel.setBorder(BorderFactory.createEmptyBorder());
    }

    public void setTitle(String text) {
        ((TitledBorder) mainPanel.getBorder()).setTitle(text);
    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
