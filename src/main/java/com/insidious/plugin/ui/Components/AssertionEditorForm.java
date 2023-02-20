package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.testcase.candidate.TestAssertion;
import com.insidious.plugin.ui.AssertionType;
import com.intellij.ui.CollectionComboBoxModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssertionEditorForm {
    private final List<TestAssertionChangeListener> changeListeners = new ArrayList<>();
    private final List<TestAssertionRemovedListener> deleteListeners = new ArrayList<>();
    private final TestAssertion testAssertion;
    private JPanel mainContainer;
    private JComboBox<AssertionType> assertionTypeComboBox;
    private JButton deleteAssertionButton;

    public AssertionEditorForm(TestAssertion testAssertion) {
        CollectionComboBoxModel<AssertionType> aModel =
                new CollectionComboBoxModel<AssertionType>(Arrays.asList(AssertionType.values()));

        assertionTypeComboBox.setModel(aModel);
        this.testAssertion = testAssertion;
        deleteAssertionButton.addActionListener(e -> notifyDeleteListeners());
        assertionTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testAssertion.setAssertionType((AssertionType) assertionTypeComboBox.getSelectedItem());
                notifyChangeListeners();
            }
        });
    }

    private void notifyDeleteListeners() {
        for (TestAssertionRemovedListener deleteListener : this.deleteListeners) {
            deleteListener.onDelete(this);
        }
    }

    public TestAssertion getTestAssertion() {
        return testAssertion;
    }

    private void notifyChangeListeners() {
        for (TestAssertionChangeListener changeListener : this.changeListeners) {
            changeListener.onChange(testAssertion);
        }
    }

    public JComponent getContent() {
        return mainContainer;
    }

    public void onChange(TestAssertionChangeListener testAssertionChangeListener) {
        this.changeListeners.add(testAssertionChangeListener);
    }

    public void onDelete(TestAssertionRemovedListener testAssertionChangeListener) {
        this.deleteListeners.add(testAssertionChangeListener);
    }
}
