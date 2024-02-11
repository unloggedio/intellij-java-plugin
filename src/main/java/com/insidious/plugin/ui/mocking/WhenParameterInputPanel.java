package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.ParameterMatcher;
import com.insidious.plugin.mocking.ParameterMatcherType;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
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
    private JLabel parameterNameTextField;
    private JLabel matcherValueTextField;
    private JLabel matcherTypeComboBox;

    public WhenParameterInputPanel(ParameterMatcher parameterMatcher, Project project) {
        this.project = project;
        this.parameterMatcher = parameterMatcher;
        originalValueTextFieldColor = matcherValueTextField.getBackground();

        setParameterName(parameterMatcher.getName());
        setParameterValue(parameterMatcher.getValue());
        setParameterType(ParameterMatcherType.ANY_OF_TYPE.toString());
//        matcherTypeComboBox.setModel(new DefaultComboBoxModel<>(ParameterMatcherType.values()));
//        matcherTypeComboBox.setSelectedItem(parameterMatcher.getType());
//        matcherTypeComboBox.addActionListener(e -> {
//            ParameterMatcherType newSelection = (ParameterMatcherType) matcherTypeComboBox.getSelectedItem();
//            parameterMatcher.setType(newSelection);
//            checkMatcherValueValid();
//        });
        matcherValueTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parameterMatcher.setValue(matcherValueTextField.getText());
                checkMatcherValueValid();
            }
        });
        checkMatcherValueValid();
    }

    private void setParameterName(String name) {
        if (name.length() > 12) {
            name = name.substring(0, 12) + "...";
        }
        parameterNameTextField.setText(name);
    }

    private void setParameterType(String typeName) {
        matcherTypeComboBox.setText("<html><u>" + typeName + "</u></html>");
    }

    private void setParameterValue(String value) {
        matcherValueTextField.setText("<html><u>" + value + "</u></html>");
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

            String finalClassName = className;
            PsiClass locatedClass = ApplicationManager.getApplication()
                    .runReadAction((Computable<PsiClass>) () -> JavaPsiFacade.getInstance(
                            project).findClass(finalClassName, GlobalSearchScope.allScope(project)));
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
