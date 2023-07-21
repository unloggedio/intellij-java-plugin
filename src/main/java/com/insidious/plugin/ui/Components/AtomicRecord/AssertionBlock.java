package com.insidious.plugin.ui.Components.AtomicRecord;

import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Random;

public class AssertionBlock {
    private JPanel mainPanel;
    private JPanel alignerPanel;
    private JPanel contentPanel;
    private JButton addNestedConditionButton;
    private AssertionBlockModel model;
    private AssertionRuleEditor ruleEditor;

    public AssertionBlock(AssertionBlockModel model, AssertionRuleEditor editor)
    {
        //mockCreate();
        this.model = model;
        ruleEditor = editor;
        BoxLayout boxLayout = new BoxLayout(contentPanel,BoxLayout.Y_AXIS);
        contentPanel.setLayout(boxLayout);

        if(this.getModel().getRuleDataList().size()==0) {
            RuleData payload = new RuleData("Where",
                    null, null, null,null);
            addRule(payload);
        }
        else
        {
            loadAssertionBlockForRules(model.getRuleDataList());
        }
        addNestedConditionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RuleData data = new RuleData("AND",null,null,null,null);
                addRule(data);
            }
        });
    }

    private void loadAssertionBlockForRules(List<RuleData> ruleDataList) {
        for(RuleData ruleData : ruleDataList)
        {
            addRule(ruleData);
        }
    }

    public JPanel getMainPanel()
    {
        return this.mainPanel;
    }

    private void mockCreate()
    {
        int count = new Random().nextInt(6);
        count = count>0 ? count : 1;
        GridLayout layout = new GridLayout(count,1);
        contentPanel.setLayout(layout);
        for(int i=0;i<count;i++)
        {
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            AssertionRule assertionRule = new AssertionRule(this);
            contentPanel.add(assertionRule.getMainPanel(),constraints);
        }
        contentPanel.revalidate();
    }

    private void addRule(RuleData payload)
    {
        AssertionRule rule = new AssertionRule(this,payload);
        model.getElementList().add(rule);
        contentPanel.add(rule.getMainPanel());
        contentPanel.revalidate();
    }

    public void removeRule(AssertionRule assertionRule) {
        model.getElementList().remove(assertionRule);
        contentPanel.remove(assertionRule.getMainPanel());
        contentPanel.revalidate();

        if(model.getElementList().size()==0)
        {
            //remove block.
            ruleEditor.removeAssertionBlock(this);
        }
        else
        {
            System.out.println("Has elements in block");
        }
    }

    public AssertionBlockModel getModel()
    {
        return this.model;
    }
}
