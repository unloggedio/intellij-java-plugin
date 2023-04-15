package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.ParameterAdapter;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ParameterInputComponent {
    private JPanel rootContent;
    private JTextField parameterValue;
    private JLabel parameterName;

    public ParameterInputComponent(ParameterAdapter parameter, String defaultParameterValue) {
        super();
        ((TitledBorder) rootContent.getBorder()).setTitle(parameter.getType().getPresentableText());
        parameterName.setText(parameter.getName());
        parameterValue.setText(defaultParameterValue);

    }

    public String getParameterValue() {
        return parameterValue.getText();
    }

    public JPanel getContent() {
        return rootContent;
    }
}
