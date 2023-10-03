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
    private static final Set<ParameterMatcherType> NEED_VALUE = new HashSet<>(
            Arrays.asList(
                    ParameterMatcherType.EQUAL,
                    ParameterMatcherType.ANY_OF_TYPE,
                    ParameterMatcherType.STARTS_WITH,
                    ParameterMatcherType.ENDS_WITH,
                    ParameterMatcherType.MATCHES_REGEX
            )
    );
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
            checkMatcherValueValid();
        });
        matcherValueTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parameterMatcher.setValue(matcherValueTextField.getText());
                checkMatcherValueValid();
            }
        });
        checkMatcherValueValid();
    }

    public void checkMatcherValueValid() {
        matcherValueTextField.setEnabled(NEED_VALUE.contains(parameterMatcher.getType()));

        if (parameterMatcher.getType() == ParameterMatcherType.ANY_OF_TYPE) {
            String className = parameterMatcher.getValue();
            if (baseClassNames.contains(className)) {
                matcherValueTextField.setBackground(originalValueTextFieldColor);
                return;
            }
            if (className.contains("<")) {
                className = className.substring(0, className.indexOf("<"));
            }

            PsiClass locatedClass = JavaPsiFacade.getInstance(
                    project).findClass(className, GlobalSearchScope.allScope(project));
            if (locatedClass == null) {
                matcherValueTextField.setBackground(UIUtils.WARNING_RED);
            } else {
                matcherValueTextField.setBackground(originalValueTextFieldColor);
            }
        } else {
            matcherValueTextField.setBackground(originalValueTextFieldColor);
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
