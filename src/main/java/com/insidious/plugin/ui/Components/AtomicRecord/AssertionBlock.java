package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.util.JsonTreeUtils;
import com.intellij.notification.NotificationType;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class AssertionBlock implements AssertionBlockManager {
    private JPanel mainPanel;
    private JPanel alignerPanel;
    private JPanel contentPanel;
    private JButton addNestedConditionButton;
    private JPanel controllerComponent;
    private RuleAdditionPanel ruleAdditionPanel;
    private List<AssertionElement> assertionElements = new ArrayList<>();
    private AssertionElement referenceElement;

    private AssertionBlockManager manager;
    public AssertionBlock(AssertionBlockManager blockManager)
    {
        this.manager = blockManager;
        BoxLayout boxLayout = new BoxLayout(contentPanel,BoxLayout.Y_AXIS);
        contentPanel.setLayout(boxLayout);

        ruleAdditionPanel = new RuleAdditionPanel(this, false);
        ruleAdditionPanel.setNested(true);
        GridLayout gridLayout = new GridLayout(1,1);
        controllerComponent.setLayout(gridLayout);
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        controllerComponent.add(ruleAdditionPanel.getMainPanel(),constraints);
        controllerComponent.revalidate();

        AtomicAssertion payload = new AtomicAssertion();
        payload.setId(UUID.randomUUID().toString());
        String kv = getCurrentTreeKey();
        Map.Entry<String,String> entry = JsonTreeUtils.getKeyValuePair(kv);
        payload.setKey(entry.getKey());
        payload.setExpectedValue(entry.getValue());
        addRule(payload);
    }

    public JPanel getMainPanel()
    {
        return this.mainPanel;
    }

    private void addRule(AtomicAssertion payload)
    {
        AssertionRule rule = new AssertionRule(this, payload);
        //model.getElementList().add(rule);
        AssertionElement ruleElement = new AssertionElement(rule);
        assertionElements.add(ruleElement);
        rule.setAssertionElement(ruleElement);
        contentPanel.add(rule.getMainPanel());
        contentPanel.revalidate();
    }

    @Override
    public void addFirstRule()
    {
        addNewRule();
    }

    @Override
    public void addNewRule() {
        AtomicAssertion assertion = new AtomicAssertion();
        String kv = getCurrentTreeKey();
        Map.Entry<String,String> entry = JsonTreeUtils.getKeyValuePair(kv);
        assertion.setKey(entry.getKey());
        assertion.setExpectedValue(entry.getValue());
        assertion.setId(UUID.randomUUID().toString());
        addRule(assertion);
    }

    @Override
    public void addNewGroup() {
        AssertionBlock newBlock = new AssertionBlock(this);
        if(assertionElements.size()==0)
        {
            InsidiousNotification.notifyMessage("Add a rule first", NotificationType.INFORMATION);
            return;
        }
        AssertionElement lastRule = assertionElements.get(assertionElements.size()-1);
        if(lastRule.getBlock()==null)
        {
            lastRule.setBlock(newBlock);
            contentPanel.add(newBlock.getMainPanel());
            contentPanel.revalidate();
        }
        else
        {
            System.out.println("Can't add to this rule.");
        }
    }

    @Override
    public void removeAssertionElement(AssertionElement element) {
        if(assertionElements.contains(element))
        {
            if(element.getBlock()!=null)
            {
                removeAssertionBlock(element.getBlock());
            }
            assertionElements.remove(element);
            contentPanel.remove(element.getRule().getMainPanel());
            contentPanel.revalidate();
        }
    }

    @Override
    public void removeAssertionGroup() {
        removeSelf();
    }

    @Override
    public String getCurrentTreeKey() {
        return manager.getCurrentTreeKey();
    }

    @Override
    public void removeAssertionBlock(AssertionBlock block) {
        for(AssertionElement element : assertionElements)
        {
            if (element.getBlock().equals(block))
            {
                element.setBlock(null);
                contentPanel.remove(block.getMainPanel());
                contentPanel.revalidate();
            }
        }
    }

    private void removeSelf()
    {
        manager.removeAssertionBlock(this);
    }

    public List<AssertionElement> getAssertionElements()
    {
        return this.assertionElements;
    }
}
