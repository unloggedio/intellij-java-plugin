package com.insidious.plugin.ui.gutter;

import com.intellij.lang.jvm.JvmParameter;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ParameterInputComponent {
    private JPanel rootContent;
    private JTextField parameterValue;

    public ParameterInputComponent(JvmParameter parameter, String defaultParameterValue) {
        super();
        ((TitledBorder) rootContent.getBorder()).setTitle(parameter.getName());
        parameterValue.setText(defaultParameterValue);

    }

    public String getParameterValue() {
        return parameterValue.getText();
    }

    public JPanel getContent() {
        return rootContent;
    }
}
