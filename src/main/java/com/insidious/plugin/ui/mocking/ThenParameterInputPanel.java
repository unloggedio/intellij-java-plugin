package com.insidious.plugin.ui.mocking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.mocking.MethodExitType;
import com.insidious.plugin.mocking.ThenParameter;
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

public class ThenParameterInputPanel {

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

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ThenParameter thenParameter;
    private final Project project;
    private final Color originalBackgroundColor;
    private JPanel mainPanel;
    private JTextField returnTypeTextField;
    private JTextArea returnValueTextArea;
    private JComboBox<MethodExitType> returnType;
    private JScrollPane textAreaScrollPanel;
    private JPanel textAreaScrollParent;

    public ThenParameterInputPanel(ThenParameter thenParameter, Project project) {
        this.project = project;
        this.thenParameter = thenParameter;
        this.originalBackgroundColor = returnValueTextArea.getBackground();
        returnType.setModel(new DefaultComboBoxModel<>(MethodExitType.values()));
        returnTypeTextField.setText(thenParameter.getReturnParameter().getClassName());
        String thenParamValue = thenParameter.getReturnParameter().getValue();
        textAreaScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        try {
            thenParamValue = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(thenParamValue));
        } catch (JsonProcessingException e) {
            // no pretty print for this value
        }

        returnValueTextArea.setText(thenParamValue);

        returnValueTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                thenParameter.getReturnParameter().setValue(returnValueTextArea.getText());
                validateValueValid();
            }
        });

        returnTypeTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = returnTypeTextField.getText();
                thenParameter.getReturnParameter().setClassName(text);
                validateTypeValid();
            }
        });

    }

    public void validateTypeValid() {
        String className = thenParameter.getReturnParameter().getClassName();
        if (baseClassNames.contains(className)) {
            returnTypeTextField.setBackground(originalBackgroundColor);
            return;
        }
        PsiClass locatedClass = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.allScope(project));
        if (locatedClass == null) {
            returnTypeTextField.setBackground(UIUtils.WARNING_RED);
        } else {
            returnTypeTextField.setBackground(originalBackgroundColor);
        }

    }

    public void validateValueValid() {
        String value = thenParameter.getReturnParameter().getValue();
        try {
            JsonNode jsonNode = objectMapper.readTree(value);
            returnValueTextArea.setBackground(originalBackgroundColor);
        } catch (Exception e) {
            returnValueTextArea.setBackground(UIUtils.WARNING_RED);
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
