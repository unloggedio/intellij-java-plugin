package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.ParameterMatcher;
import com.insidious.plugin.mocking.ParameterMatcherType;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WhenParameterInputPanel {
    private final static Set<String> baseClassNames = new HashSet<>(Arrays.asList(
            "int",
            "short",
            "byte",
            "char",
            "boolean",
            "float",
            "double",
            "long"
    ));
    private final ParameterMatcher parameterMatcher;
    private final Project project;
    private final Color originalValueTextFieldColor;
    private JPanel mainPanel;
    private JTextField parameterNameTextField;
    private JTextField matcherValueTextField;
    private JComboBox<ParameterMatcherType> matcherTypeComboBox;

    public WhenParameterInputPanel(ParameterMatcher parameterMatcher, Project project) {
        this.project = project;
        this.parameterMatcher = parameterMatcher;
        originalValueTextFieldColor = matcherValueTextField.getBackground();
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
            public void keyReleased(KeyEvent e) {
                parameterMatcher.setValue(matcherValueTextField.getText());
                checkMatcherValueValid();
            }
        });
    }

    public void checkMatcherValueValid() {
        if (parameterMatcher.getType() == ParameterMatcherType.ANY) {
            String className = parameterMatcher.getValue();
            if (baseClassNames.contains(className)) {
                matcherValueTextField.setBackground(originalValueTextFieldColor);
                return;
            }
            PsiClass locatedClass = JavaPsiFacade.getInstance(
                    project).findClass(className, GlobalSearchScope.allScope(project));
            if (locatedClass == null) {
                matcherValueTextField.setBackground(UIUtils.WARNING_RED);
            } else {
                matcherValueTextField.setBackground(originalValueTextFieldColor);
            }
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
