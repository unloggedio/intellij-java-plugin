package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.ParameterMatcher;
import com.insidious.plugin.mocking.ParameterMatcherType;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class WhenParameterInputPanel {
    private JPanel mainPanel;
    private JTextField parameterNameTextField;
    private JTextField matcherValueTextField;
    private JComboBox<ParameterMatcherType> matcherTypeComboBox;

    public WhenParameterInputPanel(ParameterMatcher parameterMatcher) {
        matcherTypeComboBox.setModel(new DefaultComboBoxModel<>(ParameterMatcherType.values()));
        parameterNameTextField.setText(parameterMatcher.getName());
        matcherTypeComboBox.setSelectedItem(parameterMatcher.getType());
        matcherValueTextField.setText(parameterMatcher.getValue());

        matcherTypeComboBox.addActionListener(e -> {
            ParameterMatcherType newSelection = (ParameterMatcherType) matcherTypeComboBox.getSelectedItem();
            parameterMatcher.setType(newSelection);
        });
        matcherValueTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                parameterMatcher.setValue(matcherValueTextField.getText());
            }
        });
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
