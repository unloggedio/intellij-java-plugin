package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.ParameterMatcher;
import com.insidious.plugin.mocking.ParameterMatcherType;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
    private JPanel valueContainer;
    private JPanel typeContainer;
    private JPanel nameContainer;
    private JLabel editIconLabel;
    private JPanel editIconContainer;

    public WhenParameterInputPanel(ParameterMatcher parameterMatcher, Project project) {
        this.project = project;
        this.parameterMatcher = parameterMatcher;
        originalValueTextFieldColor = matcherValueTextField.getBackground();

        setParameterName(parameterMatcher.getName());
        setParameterValue(parameterMatcher.getValue());
        setParameterType(parameterMatcher.getType());

        matcherTypeComboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        matcherTypeComboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                matcherTypeComboBox.setVisible(false);
                ComboBoxModel<ParameterMatcherType> model = new DefaultComboBoxModel<>(ParameterMatcherType.values());
                model.setSelectedItem(parameterMatcher.getType());
                final ComboBox<ParameterMatcherType> valueField = new ComboBox<>(model);
                typeContainer.add(valueField, new GridConstraints());

                valueField.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        typeContainer.remove(valueField);
                        matcherTypeComboBox.setVisible(true);
                    }
                });
                valueField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            typeContainer.remove(valueField);
                            matcherTypeComboBox.setVisible(true);
                        }
                    }
                });
                valueField.addActionListener(e1 -> {
                    ParameterMatcherType selected = (ParameterMatcherType) valueField.getSelectedItem();
                    parameterMatcher.setType(selected);
                    setParameterType(parameterMatcher.getType());
                    typeContainer.remove(valueField);
                    matcherTypeComboBox.setVisible(true);

                });

                ApplicationManager.getApplication().invokeLater(() -> {
                    valueField.requestFocus();
                    valueField.setPopupVisible(true);
                });

            }
        });


        matcherValueTextField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        matcherValueTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                matcherValueTextField.setVisible(false);
                final JTextField valueField = new JTextField(parameterMatcher.getValue());
                Dimension newDim = valueField.getMinimumSize();

                valueField.setMinimumSize(new Dimension(100, (int) newDim.getHeight()));
                valueContainer.add(valueField, new GridConstraints());
                valueField.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusLost(FocusEvent e) {
                        valueContainer.remove(valueField);
                        matcherValueTextField.setVisible(true);
                    }
                });
                valueField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            parameterMatcher.setValue(valueField.getText().trim());
                            setParameterValue(parameterMatcher.getValue());
                            valueContainer.remove(valueField);
                            matcherValueTextField.setVisible(true);
                        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                            valueContainer.remove(valueField);
                            matcherValueTextField.setVisible(true);
                        }
                    }
                });

                ApplicationManager.getApplication().invokeLater(() -> {
                    valueField.requestFocus();
                    valueField.select(0, valueField.getText().length());

                });
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

    private void setParameterType(ParameterMatcherType typeValue) {
        matcherTypeComboBox.setText("<html><u>" + typeValue.toString() + "</u></html>");
    }

    private void setParameterValue(String value) {
        if (parameterMatcher.getType() == ParameterMatcherType.ANY_OF_TYPE) {
            if (value.contains(".")) {
                // show simple class name
                value = value.substring(value.lastIndexOf(".") + 1);
            }
        }
        if (value.length() > 30) {
            value = value.substring(0, 27) + "...";
        }
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
