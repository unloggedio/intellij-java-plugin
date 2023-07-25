package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.util.JsonTreeUtils;
import com.intellij.notification.NotificationType;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AssertionRuleEditorImpl implements AssertionRuleEditor, AssertionBlockManager {
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel assertionsContainerPanel;
    private JPanel controlPanel;
    private String currentKey;
    private RuleAdditionPanel ruleAdditionPanel;
    private List<AssertionElement> ruleElements = new ArrayList<>();

    public AssertionRuleEditorImpl()
    {
        BoxLayout layout = new BoxLayout(assertionsContainerPanel,BoxLayout.Y_AXIS);
        assertionsContainerPanel.setLayout(layout);

        ruleAdditionPanel = new RuleAdditionPanel(this,true);
        ruleAdditionPanel.setNested(false);
        GridLayout controlLayout = new GridLayout(1,1);
        controlPanel.setLayout(controlLayout);

        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        controlPanel.add(ruleAdditionPanel.getMainPanel(),constraints);
        controlPanel.revalidate();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public List<AtomicAssertion> getAtomicAssertions()
    {
        List<AtomicAssertion> assertions = new ArrayList<>();
        for (AssertionElement element : ruleElements)
        {
            AtomicAssertion assertion = element.getRule().getAtomicAssertion();
            if(element.getBlock()!=null)
            {
                List<AtomicAssertion> subAssertions = buildAssertionsFromBlock(element.getBlock(),
                        new ArrayList<>());
                assertion.setSubAssertions(subAssertions);
            }
            assertions.add(assertion);
        }
        return assertions;
    }

    private List<AtomicAssertion> buildAssertionsFromBlock(AssertionBlock block,
                                                           List<AtomicAssertion> subAssertions)
    {
        List<AssertionElement> elements = block.getAssertionElements();
        for (AssertionElement element : elements)
        {
            AtomicAssertion assertion = element.getRule().getAtomicAssertion();
            if(element.getBlock()!=null)
            {
                List<AtomicAssertion> subs = buildAssertionsFromBlock(element.getBlock(),
                        new ArrayList<>());
                assertion.setSubAssertions(subs);
            }
            subAssertions.add(assertion);
        }
        return subAssertions;
    }

    @Override
    public void setCurrentKey(String key) {
        this.currentKey = key;
    }

    @Override
    public void addFirstRule() {
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

        AssertionRule rule = new AssertionRule(this, assertion);
        AssertionElement ruleElement = new AssertionElement(rule);
        ruleElements.add(ruleElement);
        rule.setAssertionElement(ruleElement);
        assertionsContainerPanel.add(rule.getMainPanel());
        assertionsContainerPanel.revalidate();
    }

    @Override
    public void addNewGroup() {
        System.out.println("Adding new group");
        AssertionBlock block = new AssertionBlock(this);
        if(ruleElements.size()==0)
        {
            InsidiousNotification.notifyMessage("Add a rule first", NotificationType.INFORMATION);
            return;
        }
        AssertionElement lastRule = ruleElements.get(ruleElements.size()-1);
        if(lastRule.getBlock()==null)
        {
            lastRule.setBlock(block);
            assertionsContainerPanel.add(block.getMainPanel());
            assertionsContainerPanel.revalidate();
        }
        else
        {
            System.out.println("Can't add to this rule.");
        }
    }

    @Override
    public void removeAssertionElement(AssertionElement element) {
        if(ruleElements.contains(element))
        {
            if(element.getBlock()!=null)
            {
                removeAssertionBlock(element.getBlock());
            }
            ruleElements.remove(element);
            assertionsContainerPanel.remove(element.getRule().getMainPanel());
            assertionsContainerPanel.revalidate();
        }
    }

    @Override
    public void removeAssertionGroup() {
        //can't remove this, reload flow
    }

    @Override
    public String getCurrentTreeKey() {
        return currentKey;
    }

    @Override
    public void removeAssertionBlock(AssertionBlock block) {
        for(AssertionElement element : ruleElements)
        {
            if (element.getBlock()!=null &&
                    element.getBlock().equals(block))
            {
                element.setBlock(null);
                assertionsContainerPanel.remove(block.getMainPanel());
                assertionsContainerPanel.revalidate();
            }
        }
    }
}
