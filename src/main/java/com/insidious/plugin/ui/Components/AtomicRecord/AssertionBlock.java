package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.ui.Components.AtomicAssertionConstants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AssertionBlock implements AssertionBlockManager {
    private final AssertionBlockControlPanel assertionBlockControlPanel;
    private final List<AssertionRule> assertionRules = new ArrayList<>();
    private final List<AssertionBlock> assertionGroups = new ArrayList<>();
    private final AssertionBlockManager manager;
    private final AtomicAssertion assertion;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel contentPanel;
    private JPanel controlPanel;
//    private boolean controlPanelIsVisible = false;

    public AssertionBlock(AtomicAssertion assertion, AssertionBlockManager blockManager) {
        this.manager = blockManager;
        this.assertion = assertion;
        BoxLayout boxLayout = new BoxLayout(contentPanel, BoxLayout.Y_AXIS);
        contentPanel.setLayout(boxLayout);

        assertionBlockControlPanel = new AssertionBlockControlPanel(this, assertion);

        controlPanel.setLayout(new BorderLayout());


        JPanel ruleAdditionPanelContainer = assertionBlockControlPanel.getMainPanel();
        controlPanel.add(ruleAdditionPanelContainer, BorderLayout.CENTER);

        for (AtomicAssertion subAssertion : assertion.getSubAssertions()) {
            addRule(subAssertion);
        }

    }

    public JPanel getMainPanel() {
        return this.mainPanel;
    }

    private void addRule(AtomicAssertion payload) {
        AssertionRule rule = new AssertionRule(this, payload);
        assertionRules.add(rule);
        contentPanel.add(rule.getMainPanel());
        contentPanel.revalidate();
    }

    @Override
    public void addNewRule() {

        KeyValue selectedKeyValue = getCurrentTreeKey();
        AtomicAssertion assertion = new AtomicAssertion(AssertionType.EQUAL, selectedKeyValue.getKey(),
                selectedKeyValue.getValue().toString());
        this.assertion.getSubAssertions().add(assertion);

        addRule(assertion);
    }

    @Override
    public void addNewGroup() {
        KeyValue selectedKeyValue = getCurrentTreeKey();
        AtomicAssertion assertion = new AtomicAssertion(AssertionType.EQUAL, selectedKeyValue.getKey(),
                selectedKeyValue.getValue().toString());

        AtomicAssertion newSubGroup = new AtomicAssertion();
        newSubGroup.setAssertionType(AssertionType.ALLOF);
        newSubGroup.getSubAssertions().add(assertion);

        this.assertion.getSubAssertions().add(newSubGroup);
        AssertionBlock newBlock = new AssertionBlock(newSubGroup, this);

        assertionGroups.add(newBlock);

        contentPanel.add(newBlock.getMainPanel());
        contentPanel.revalidate();
    }

    @Override
    public AssertionResult executeAssertion(AtomicAssertion subAssertion) {
        AssertionResult thisResult = manager.executeAssertion(assertion);
        Boolean result = thisResult.getResults().get(assertion.getId());

        if (result) {
            topAligner.setBackground(AtomicAssertionConstants.PASSING_COLOR);
//            mainPanel.setBackground(AtomicAssertionConstants.PASSING_COLOR);
        } else {
            topAligner.setBackground(AtomicAssertionConstants.FAILING_COLOR);
//            mainPanel.setBackground(AtomicAssertionConstants.FAILING_COLOR);
        }

        return thisResult;
    }

    @Override
    public void deleteAssertionRule(AssertionRule element) {
        if (assertionRules.contains(element)) {
            assertionRules.remove(element);
            assertion.getSubAssertions().remove(element.getAtomicAssertion());
            contentPanel.remove(element.getMainPanel());
            contentPanel.revalidate();
            executeAssertion(assertion);
        }
        if (assertion.getSubAssertions().size() == 0) {
            removeAssertionGroup();
        }
    }

    @Override
    public void removeAssertionGroup() {
        manager.removeAssertionGroup(this);
    }

    @Override
    public KeyValue getCurrentTreeKey() {
        return manager.getCurrentTreeKey();
    }

    @Override
    public void removeAssertionGroup(AssertionBlock block) {
        for (AssertionBlock assertionBlock : assertionGroups) {
            if (assertionBlock.equals(block)) {
                assertionGroups.remove(block);
                assertion.getSubAssertions().remove(block.getAssertion());
                contentPanel.remove(block.getMainPanel());
                contentPanel.revalidate();
                executeAssertion(assertion);
                break;
            }
        }
    }


    public AtomicAssertion getAssertion() {
        return assertion;
    }


}
