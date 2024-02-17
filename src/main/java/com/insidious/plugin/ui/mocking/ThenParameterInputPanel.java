package com.insidious.plugin.ui.mocking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.mocking.MethodExitType;
import com.insidious.plugin.mocking.ThenParameter;
import com.insidious.plugin.ui.treeeditor.JsonTreeEditor;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import javax.swing.*;
import java.awt.*;
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
            "void",
            "long"
    ));

    private static final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private static final Logger logger = LoggerUtil.getInstance(ThenParameterInputPanel.class);
    private final ThenParameter thenParameter;
    private final Project project;
    private final Color originalBackgroundColor;
    private JPanel mainPanel;
    //    private JLabel returnTypeTextField;
//    private JLabel returnType;
    private JScrollPane valueScrollPanel;
    private JPanel textAreaScrollParent;

    public ThenParameterInputPanel(ThenParameter thenParameter, Project project) {
        this.project = project;
        this.thenParameter = thenParameter;
        this.originalBackgroundColor = valueScrollPanel.getBackground();
        String simpleClassName = thenParameter.getReturnParameter().getClassName();
        if (simpleClassName.contains(".")) {
            simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
        }
//        returnTypeTextField.setText(simpleClassName);
//        returnTypeTextField.setVisible(false);
        String thenParamValue = thenParameter.getReturnParameter().getValue();
        valueScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        try {
            thenParamValue = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(thenParamValue));
        } catch (JsonProcessingException e) {
            // no pretty print for this value
        }


        try {
            JsonTreeEditor editor = new JsonTreeEditor(objectMapper.readTree(thenParamValue),
                    simpleClassName);
            editor.setEditable(true);
            editor.addChangeListener(object -> thenParameter.getReturnParameter().setValue(object.toString()));
            valueScrollPanel.setViewportView(editor.getContent());


        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        updateVisibleControls();
        validateTypeValid();
    }


    public void updateVisibleControls() {
        if (thenParameter.getMethodExitType() == MethodExitType.NULL) {
//            returnTypeTextField.setEnabled(false);
            valueScrollPanel.setEnabled(false);
        } else {
//            returnTypeTextField.setEnabled(true);
            valueScrollPanel.setEnabled(true);
        }

    }

    public void validateTypeValid() {
        String className = thenParameter.getReturnParameter().getClassName();
        if (baseClassNames.contains(className)) {
//            returnTypeTextField.setBackground(originalBackgroundColor);
            return;
        }
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf("<"));
        }
        if (className.contains("[")) {
            className = className.substring(0, className.indexOf("["));
        }

        String finalClassName = className;
        PsiClass locatedClass = ApplicationManager.getApplication()
                .runReadAction((Computable<PsiClass>) () -> JavaPsiFacade.getInstance(project)
                        .findClass(finalClassName.replace("$", "."), GlobalSearchScope.allScope(project)));
        if (locatedClass == null) {
//            returnTypeTextField.setBackground(UIUtils.WARNING_RED);
        } else {
//            returnTypeTextField.setBackground(originalBackgroundColor);
        }

    }

    public void validateValueValid() {
        String value = thenParameter.getReturnParameter().getValue();
        try {
            JsonNode jsonNode = objectMapper.readTree(value);
            valueScrollPanel.setBackground(originalBackgroundColor);
        } catch (Exception e) {
            valueScrollPanel.setBackground(UIUtils.WARNING_RED);
        }
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void setExitType(MethodExitType normal) {
        thenParameter.setMethodExitType(normal);
    }
}
