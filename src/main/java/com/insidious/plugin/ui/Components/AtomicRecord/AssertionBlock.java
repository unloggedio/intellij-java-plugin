package com.insidious.plugin.ui.Components.AtomicRecord;

import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.util.JsonTreeUtils;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AssertionBlock implements AssertionBlockManager {
    private final RuleAdditionPanel ruleAdditionPanel;
    private final List<AssertionElement> assertionElements = new ArrayList<>();
    private final AssertionBlockManager manager;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel contentPanel;
    private JPanel controlPanel;
//    private boolean controlPanelIsVisible = false;

    public AssertionBlock(AssertionBlockManager blockManager) {
        this.manager = blockManager;
        BoxLayout boxLayout = new BoxLayout(contentPanel, BoxLayout.Y_AXIS);
        contentPanel.setLayout(boxLayout);

        ruleAdditionPanel = new RuleAdditionPanel(this, false);
        ruleAdditionPanel.setNested(true);

//        GridLayout gridLayout = new GridLayout(1, 1);
        controlPanel.setLayout(new BorderLayout());
//        GridConstraints constraints = new GridConstraints();
//        constraints.setRow(0);

        JPanel ruleAdditionPanelContainer = ruleAdditionPanel.getMainPanel();
        controlPanel.add(ruleAdditionPanelContainer, BorderLayout.CENTER);


//        MouseAdapter controlPanelHoverAdapter = new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (controlPanelIsVisible) {
//                    controlPanelIsVisible = false;
//                    controlPanel.remove(ruleAdditionPanelContainer);
//
//                } else {
//                    controlPanelIsVisible = true;
//                    controlPanel.add(ruleAdditionPanelContainer, constraints);
//                }
//                controlPanel.revalidate();
//                controlPanel.repaint();
//            }
//
//
////            @Override
////            public void mouseEntered(MouseEvent e) {
////                controlPanel.add(ruleAdditionPanelContainer, constraints);
////                controlPanel.revalidate();
////                controlPanel.repaint();
////            }
////
////            @Override
////            public void mouseExited(MouseEvent e) {
////                controlPanel.remove(ruleAdditionPanelContainer);
////                controlPanel.revalidate();
////                controlPanel.repaint();
////            }
//        };

//        ruleAdditionPanelContainer.addMouseListener(controlPanelHoverAdapter);
//        mainPanel.addMouseListener(controlPanelHoverAdapter);

//        controlPanel.revalidate();

        AtomicAssertion payload = new AtomicAssertion();
        payload.setId(UUID.randomUUID().toString());
        String kv = getCurrentTreeKey();
        Map.Entry<String, String> entry = JsonTreeUtils.getKeyValuePair(kv);
        payload.setKey(entry.getKey());
        payload.setExpectedValue(entry.getValue());
        addRule(payload);
    }

    public JPanel getMainPanel() {
        return this.mainPanel;
    }

    private void addRule(AtomicAssertion payload) {
        AssertionRule rule = new AssertionRule(this, payload);
//        model.getElementList().add(rule);
        AssertionElement ruleElement = new AssertionElement(rule, null);
        assertionElements.add(ruleElement);
        rule.setAssertionElement(ruleElement);
        contentPanel.add(rule.getMainPanel());
        contentPanel.revalidate();
    }

    @Override
    public void addNewRule() {
        AtomicAssertion assertion = new AtomicAssertion();
        String kv = getCurrentTreeKey();
        Map.Entry<String, String> entry = JsonTreeUtils.getKeyValuePair(kv);
        assertion.setKey(entry.getKey());
        assertion.setExpectedValue(entry.getValue());
        assertion.setId(UUID.randomUUID().toString());
        addRule(assertion);
    }

    @Override
    public void addNewGroup() {
        AssertionBlock newBlock = new AssertionBlock(this);
//        if (assertionElements.size() == 0) {
//            InsidiousNotification.notifyMessage("Add a rule first", NotificationType.INFORMATION);
//            return;
//        }
        AssertionElement lastRule = assertionElements.get(assertionElements.size() - 1);
//        if (lastRule.getBlock() == null) {
        lastRule.setBlock(newBlock);
        contentPanel.add(newBlock.getMainPanel());
        contentPanel.revalidate();
//        } else {
//            System.out.println("Can't add to this rule.");
//        }
    }

    @Override
    public void removeAssertionElement(AssertionElement element) {
        if (assertionElements.contains(element)) {
            if (element.getBlock() != null) {
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
        for (AssertionElement element : assertionElements) {
            if (element.getBlock().equals(block)) {
                element.setBlock(null);
                contentPanel.remove(block.getMainPanel());
                contentPanel.revalidate();
            }
        }
    }

    private void removeSelf() {
        manager.removeAssertionBlock(this);
    }

    public List<AtomicAssertion> getAtomicAssertions() {
        List<AtomicAssertion> assertions = new ArrayList<>();
        for (AssertionElement element : assertionElements) {
            AtomicAssertion assertion = element.getRule().getAtomicAssertion();
            if (element.getBlock() != null) {
                List<AtomicAssertion> subAssertions = buildAssertionsFromBlock(element.getBlock());
                assertion.setSubAssertions(subAssertions);
            }
            assertions.add(assertion);
        }
        return assertions;
    }

    public List<AssertionElement> getAssertionElements() {
        return assertionElements;
    }

    private List<AtomicAssertion> buildAssertionsFromBlock(AssertionBlock block) {
        List<AtomicAssertion> subAssertions = new ArrayList<>();
        for (AssertionElement element : block.getAssertionElements()) {
            AtomicAssertion assertion = element.getRule().getAtomicAssertion();
            if (element.getBlock() != null) {
                List<AtomicAssertion> subs = buildAssertionsFromBlock(element.getBlock());
                assertion.setSubAssertions(subs);
            }
            subAssertions.add(assertion);
        }
        return subAssertions;
    }
}
