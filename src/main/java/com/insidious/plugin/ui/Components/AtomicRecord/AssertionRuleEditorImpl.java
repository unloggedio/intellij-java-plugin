package com.insidious.plugin.ui.Components.AtomicRecord;

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

public class AssertionRuleEditorImpl implements AssertionRuleEditor{
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel assertionsContainerPanel;
    private JButton addAssertionButton;
    private int counter = 0;
    private List<AssertionBlockElement> assertionElements = new ArrayList<>();

    public AssertionRuleEditorImpl()
    {
        BoxLayout layout = new BoxLayout(assertionsContainerPanel,BoxLayout.Y_AXIS);
        assertionsContainerPanel.setLayout(layout);

        addAssertionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addAssertion();
            }
        });
    }

    private void addAssertion()
    {
        System.out.println("Counter : "+counter);
        if(counter>0)
        {
            //add bridgeCondition
            createNewBridgeCondidtion(null);
            counter++;
        }
        createNewAssertionBlock(null);
        assertionsContainerPanel.revalidate();
        counter++;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public String getRuleSet()
    {
        return this.assertionElements.toString();
    }

    @Override
    public void removeAssertionBlock(AssertionBlock assertionBlock) {
        AssertionBlockElement element = getBlockElementByPanel(assertionBlock.getMainPanel());
        System.out.println("INDEX OF BLOCK TO DELETE : "+assertionElements.indexOf(element));
        if(assertionElements.indexOf(element)>0)
        {
            //remove above condition
            AssertionBlockElement connector = assertionElements.
                    get(assertionElements.indexOf(element)-1);

            assertionsContainerPanel.remove(element.getPanel());
            assertionsContainerPanel.remove(connector.getPanel());

            assertionElements.remove(element);
            assertionElements.remove(connector);
            counter--;
            counter--;
        }
        else
        {
            if(assertionElements.size()>2)
            {
                AssertionBlockElement connector = assertionElements.
                        get(1);
                assertionsContainerPanel.remove(connector.getPanel());
                assertionElements.remove(connector);
                counter--;
            }
            assertionsContainerPanel.remove(assertionBlock.getMainPanel());
            assertionElements.remove(element);
            counter--;
        }
        assertionsContainerPanel.revalidate();
    }

    private AssertionBlockElement getBlockElementByPanel(JPanel panel) {
        AssertionBlockElement element = null;
        for (AssertionBlockElement blockElement : assertionElements)
        {
            if(blockElement.getPanel().equals(panel))
            {
                //same panel
                element = blockElement;
            }
        }
        return element;
    }

    public void openSavedRules(List<AssertionBlockElement> blockElements)
    {
        System.out.println("Opening saved rules");
        this.assertionElements.clear();
        constructForRules(blockElements);
    }

    private void constructForRules(List<AssertionBlockElement> blockElements)
    {
        assertionsContainerPanel.removeAll();
        counter = 0;
        for(AssertionBlockElement element : blockElements)
        {
            if(element.getElementType().equals(AssertionBlockElement.AssertionBlockElementType.RULE))
            {
                createNewAssertionBlock(element.getAssertionBlockModel().getRuleDataList());
            }
            else
            {
                createNewBridgeCondidtion(element.getConnector());
            }
            counter++;
        }
        assertionsContainerPanel.revalidate();
    }

    private JPanel createNewBridgeCondidtion(String condition)
    {
        if(condition == null)
        {
            condition = "AND";
        }

        JPanel connector = new JPanel();
        connector.setLayout(new GridLayout(1,1));
        JLabel label = new JLabel(condition, SwingConstants.LEFT);
        label.setBorder(new EmptyBorder(8,8,8,0));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String newLabel = label.getText();
                if(newLabel.equals("AND"))
                {
                    newLabel = "OR";
                }
                else
                {
                    newLabel = "AND";
                }
                label.setText(newLabel);
                AssertionBlockElement element = getBlockElementByPanel(connector);
                element.setConnector(newLabel);
            }
        });
        connector.add(label);
        connector.setBorder(new LineBorder(Color.BLUE));
        assertionElements.add(
                new AssertionBlockElement(
                        AssertionBlockElement.AssertionBlockElementType.CONNECTOR,
                        label.getText(),
                        null,
                        connector));
        assertionsContainerPanel.add(connector);
        return connector;
    }

    private void createNewAssertionBlock(List<RuleData> ruleData)
    {
        if(ruleData==null)
        {
            ruleData = new ArrayList<>();
        }
        AssertionBlockModel model = new AssertionBlockModel(counter,ruleData);
        JPanel panel = new AssertionBlock(model,this).getMainPanel();
        panel.setBorder(new LineBorder(Color.ORANGE));

        assertionElements.add(
                new AssertionBlockElement(
                        AssertionBlockElement.AssertionBlockElementType.RULE,
                        null,
                        model,
                        panel));
        assertionsContainerPanel.add(panel);
    }
}
