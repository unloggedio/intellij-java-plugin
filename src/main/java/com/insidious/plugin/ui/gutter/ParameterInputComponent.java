package com.insidious.plugin.ui.gutter;

import com.intellij.lang.jvm.JvmParameter;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class ParameterInputComponent {
    private JPanel rootContent;
    private JTextField parameterValue;

    public ParameterInputComponent(JvmParameter parameter) {
        super();
        ((TitledBorder) rootContent.getBorder()).setTitle(parameter.getName());
    }

    public String getParameterValue() {
        return parameterValue.getText();
    }

    public JPanel getContent() {
        return rootContent;
    }
}
