package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.util.ClassUtils;
import com.insidious.plugin.util.UIUtils;

import javax.swing.*;

public class MethodDisplayComponent {
    private MethodAdapter methodAdapter;
    private JPanel mainPanel;
    private JLabel methodNameLabel;
    private JLabel iconLabel;

    public MethodDisplayComponent() {
        iconLabel.setIcon(UIUtils.EXECUTE);
        methodNameLabel.setText("Select a method by clicking on the ");
    }

    public void setMethod(MethodAdapter methodAdapter) {
        iconLabel.setVisible(false);
        this.methodAdapter = methodAdapter;
        String simpleClassName = ClassUtils.getSimpleName(methodAdapter.getContainingClass().getQualifiedName());
        String labelText = simpleClassName + "." + methodAdapter.getName();
        if (labelText.length() > 50) {
            labelText = labelText.substring(0, 47) + "...";
        }
        methodNameLabel.setText("<html>" + labelText + "</html>");
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
