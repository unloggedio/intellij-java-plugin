package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.ui.Components.AtomicAssertionConstants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssertionBlock implements AssertionBlockManager {
    //    private boolean controlPanelIsVisible = false;
    private static final List<AssertionType> GROUP_CONDITIONS = Arrays.asList(
            AssertionType.ANYOF,
            AssertionType.NOTANYOF,
            AssertionType.ALLOF,
            AssertionType.NOTALLOF
    );
    private final AssertionBlockControlPanel assertionBlockControlPanel;
    private final List<AssertionRule> assertionRules = new ArrayList<>();
    private final List<AssertionBlock> assertionGroups = new ArrayList<>();
    private final AssertionBlockManager manager;
    private final AtomicAssertion assertion;
    private final boolean isRootCondition;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel contentPanel;
    private JPanel controlPanel;

    public AssertionBlock(AtomicAssertion assertion, AssertionBlockManager blockManager, boolean isRootCondition) {
        this.isRootCondition = isRootCondition;
        this.manager = blockManager;
        this.assertion = assertion;
        BoxLayout boxLayout = new BoxLayout(contentPanel, BoxLayout.Y_AXIS);
        contentPanel.setLayout(boxLayout);

        if (!isRootCondition) {
            topAligner.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        }

        assertionBlockControlPanel = new AssertionBlockControlPanel(this, assertion, isRootCondition);


        controlPanel.setLayout(new BorderLayout());


        JPanel ruleAdditionPanelContainer = assertionBlockControlPanel.getMainPanel();
        controlPanel.add(ruleAdditionPanelContainer, BorderLayout.CENTER);

        if (assertion.getSubAssertions() != null) {
            for (AtomicAssertion subAssertion : assertion.getSubAssertions()) {
                if (GROUP_CONDITIONS.contains(subAssertion.getAssertionType())) {
                    addGroup(subAssertion);
                } else {
                    addRule(subAssertion);
                }
            }
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

        addGroup(newSubGroup);
    }

    private void addGroup(AtomicAssertion newSubGroup) {
        AssertionBlock newBlock = new AssertionBlock(newSubGroup, this, false);
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
//            topAligner.setBorder(new LineBorder(AtomicAssertionConstants.PASSING_COLOR));
//            mainPanel.setBackground(AtomicAssertionConstants.PASSING_COLOR);
        } else {
//            topAligner.setBorder(new LineBorder(AtomicAssertionConstants.FAILING_COLOR));
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

    public List<AssertionRule> getAssertionRules() {
        return assertionRules;
    }

    public List<AssertionBlock> getAssertionGroups() {
        return assertionGroups;
    }
}
