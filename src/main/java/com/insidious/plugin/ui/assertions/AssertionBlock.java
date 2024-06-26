package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

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
    private final Project project;
    private final boolean isRootCondition;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel contentPanel;
    private JPanel controlPanel;

    public AssertionBlock(AtomicAssertion assertion, AssertionBlockManager blockManager, boolean isRootCondition,
                          Project project) {
        this.isRootCondition = isRootCondition;
        this.manager = blockManager;
        this.assertion = assertion;
        this.project = project;
        BoxLayout boxLayout = new BoxLayout(contentPanel, BoxLayout.Y_AXIS);
        contentPanel.setLayout(boxLayout);
        mainPanel.setBackground(JBColor.WHITE);
        topAligner.setBackground(JBColor.WHITE);
        contentPanel.setBackground(JBColor.WHITE);

//        if (!isRootCondition) {
//            topAligner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
//        }

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
        controlPanel.setVisible(false);

    }

    public JPanel getContent() {
        return this.mainPanel;
    }

    private void addRule(AtomicAssertion payload) {
        AssertionRule rule = new AssertionRule(this, payload, project);
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
        AssertionBlock newBlock = new AssertionBlock(newSubGroup, this, false, project);
        assertionGroups.add(newBlock);
        contentPanel.add(newBlock.getContent());
        contentPanel.revalidate();
    }

    @Override
    public AssertionResult executeAssertion(AtomicAssertion subAssertion) {
        return null;
//        AssertionResult thisResult = manager.executeAssertion(assertion);
//        Boolean result = thisResult.getResults().get(assertion.getId());
//
//        if (result) {
//            topAligner.setBackground(UIUtils.ASSERTION_PASSING_COLOR);
//        } else {
//            topAligner.setBackground(UIUtils.ASSERTION_FAILING_COLOR);
//        }
//
//        return thisResult;
    }

    @Override
    public void deleteAssertionRule(AssertionRule element) {
        if (assertionRules.contains(element)) {
            assertionRules.remove(element);
            List<AtomicAssertion> existingSubAssertions = new ArrayList<>(assertion.getSubAssertions());
            existingSubAssertions.remove(element.getAtomicAssertion());
            assertion.setSubAssertions(existingSubAssertions);
            contentPanel.remove(element.getMainPanel());
            contentPanel.revalidate();
            executeAssertion(assertion);
        }
        if (assertion.getSubAssertions().isEmpty()) {
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
                contentPanel.remove(block.getContent());
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

    public void showEditForm(Supplier<List<KeyValue>> keyValueSupplier) {
        for (AssertionBlock assertionGroup : assertionGroups) {
            assertionGroup.showEditForm(keyValueSupplier);
        }
        for (AssertionRule assertionRule : assertionRules) {
            assertionRule.showEditForm(keyValueSupplier);
        }
    }

    public void hideEditForm() {
        for (AssertionBlock assertionGroup : assertionGroups) {
            assertionGroup.hideEditForm();
        }
        for (AssertionRule assertionRule : assertionRules) {
            assertionRule.hideEditForm();
        }

    }

    public void saveEdit() {
        for (AssertionBlock assertionGroup : assertionGroups) {
            assertionGroup.saveEdit();
        }
        for (AssertionRule assertionRule : assertionRules) {
            assertionRule.saveEdit();
        }

    }
}
