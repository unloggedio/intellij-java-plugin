package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.util.JsonTreeUtils;

import javax.swing.*;
import java.awt.*;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AssertionBlock implements AssertionBlockManager {
    private final AssertionBlockControlPanel assertionBlockControlPanel;
    private final List<AssertionRule> assertionRules = new ArrayList<>();
    private final List<AssertionBlock> assertionGroups = new ArrayList<>();
    private final AssertionBlockManager manager;
    private final AtomicAssertion atomicAssertion;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel contentPanel;
    private JPanel controlPanel;
//    private boolean controlPanelIsVisible = false;

    public AssertionBlock(AtomicAssertion atomicAssertion, AssertionBlockManager blockManager) {
        this.manager = blockManager;
        this.atomicAssertion = atomicAssertion;
        BoxLayout boxLayout = new BoxLayout(contentPanel, BoxLayout.Y_AXIS);
        contentPanel.setLayout(boxLayout);

        assertionBlockControlPanel = new AssertionBlockControlPanel(this, atomicAssertion);

        controlPanel.setLayout(new BorderLayout());


        JPanel ruleAdditionPanelContainer = assertionBlockControlPanel.getMainPanel();
        controlPanel.add(ruleAdditionPanelContainer, BorderLayout.CENTER);

        for (AtomicAssertion subAssertion : atomicAssertion.getSubAssertions()) {
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
//        Map.Entry<String, String> entry = JsonTreeUtils.getKeyValuePair(selectedKeyValue);
        AtomicAssertion assertion = new AtomicAssertion(AssertionType.EQUAL, selectedKeyValue.getKey(),
                selectedKeyValue.getValue().toString());
        atomicAssertion.getSubAssertions().add(assertion);

        addRule(assertion);
    }

    @Override
    public void addNewGroup() {
        AtomicAssertion newSubGroup = new AtomicAssertion();
        AssertionBlock newBlock = new AssertionBlock(newSubGroup, this);

        assertionGroups.add(newBlock);
        atomicAssertion.getSubAssertions().add(newSubGroup);

        contentPanel.add(newBlock.getMainPanel());
        contentPanel.revalidate();
    }

    @Override
    public void deleteAssertionRule(AssertionRule element) {
        if (assertionRules.contains(element)) {
            assertionRules.remove(element);
            atomicAssertion.getSubAssertions().remove(element.getAtomicAssertion());
            contentPanel.remove(element.getMainPanel());
            contentPanel.revalidate();
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
                atomicAssertion.getSubAssertions().remove(block.getAtomicAssertion());
                contentPanel.remove(block.getMainPanel());
                contentPanel.revalidate();
                break;
            }
        }
    }


    public AtomicAssertion getAtomicAssertion() {
        return atomicAssertion;
    }


}
