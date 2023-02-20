package com.insidious.plugin.ui.Components;

import com.insidious.plugin.client.ConstructorStrategy;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.ParameterDataChangeListener;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

public class ParameterEditorForm {
    private static final Logger logger = LoggerUtil.getInstance(ParameterEditorForm.class);
    private final Parameter parameter;
    private final List<ParameterDataChangeListener> listeners = new ArrayList<>();
    private final PsiParameter psiParameter;
    private JPanel mainContainer;
    private JLabel parameterTypeLabel;
    private JPanel parameterPanel;
    private JComboBox<ConstructorStrategy> creatorStrategySelector;
    private JPanel valueSelectorPanel;
    private ConstructorStrategy constructorStrategy;

    public ParameterEditorForm(Parameter parameter, PsiParameter psiParameter) {
        this.parameter = parameter;
        this.psiParameter = psiParameter;

        parameterTypeLabel.setText(parameter.getName());
        TitledBorder titledBorder = (TitledBorder) parameterPanel.getBorder();
        titledBorder.setTitle(psiParameter.getType().getPresentableText());


        creatorStrategySelector.addItem(ConstructorStrategy.CONSTRUCTOR);
        creatorStrategySelector.addItem(ConstructorStrategy.JSON_DESERIALIZE);
        creatorStrategySelector.addItem(ConstructorStrategy.MANUAL_CODE);

        creatorStrategySelector.addActionListener(e -> {
            logger.info("Creator strategy changed:  " + creatorStrategySelector.getSelectedItem());
            ConstructorStrategy selectedCreatorStrategy = (ConstructorStrategy) creatorStrategySelector.getSelectedItem();
            if (selectedCreatorStrategy == null) {
                return;
            }
            updateValueSelectorPanel();
            callListeners();
        });
        updateValueSelectorPanel();


    }

    private void updateValueSelectorPanel() {
        ConstructorStrategy selectedType = (ConstructorStrategy) this.creatorStrategySelector.getSelectedItem();
        if (this.constructorStrategy == selectedType) {
            return;
        }
        assert selectedType != null;

        valueSelectorPanel.removeAll();
        this.constructorStrategy = selectedType;

        switch (selectedType) {

            case CONSTRUCTOR:
                ComboBox<String> constructorSelector = new ComboBox<>();

                if (psiParameter.getType().getCanonicalText().equals("java.lang.String")) {
                } else if (psiParameter.getType() instanceof PsiClassReferenceType) {
                    PsiClassReferenceType classReferenceType = (PsiClassReferenceType) psiParameter.getType();
                    @NotNull PsiJavaCodeReferenceElement javaClassReference = classReferenceType.getReference();
                }

                valueSelectorPanel.add(constructorSelector, BorderLayout.CENTER);

                break;
            case JSON_DESERIALIZE:
                JTextField parameterValueTextField = new JTextField();

                if (psiParameter.getType().getCanonicalText().equals("java.lang.String")) {
                    parameterValueTextField.setText("Value for " + psiParameter.getName());
                    parameter.getProb()
                            .setSerializedValue(("\"" + parameterValueTextField.getText() + "\"").getBytes());
                } else if (psiParameter.getType() instanceof PsiClassReferenceType) {
                    PsiClassReferenceType classReferenceType = (PsiClassReferenceType) psiParameter.getType();
                    @NotNull PsiJavaCodeReferenceElement javaClassReference = classReferenceType.getReference();
                    parameterValueTextField.setText("{\"fieldName\": \"fieldValue\"}");
                    parameter.getProb().setSerializedValue(("{\"fieldName\": \"fieldValue\"}").getBytes());
                }
                parameterValueTextField.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {

                    }

                    @Override
                    public void focusLost(FocusEvent e) {
                        parameter.getProb()
                                .setSerializedValue(("\"" + parameterValueTextField.getText() + "\"").getBytes());
                        callListeners();
                    }
                });
                valueSelectorPanel.add(parameterValueTextField, BorderLayout.CENTER);
                break;
            case MANUAL_CODE:
                parameter.getProb().setSerializedValue(new byte[0]);
                break;
        }
        valueSelectorPanel.revalidate();
        valueSelectorPanel.repaint();
    }

    private void callListeners() {
        this.listeners.forEach(e -> {
            e.onParameterChange(parameter);
        });
    }

    public JPanel getContent() {
        return mainContainer;
    }

    public void addChangeListener(ParameterDataChangeListener parameterDataChangeListener) {
        this.listeners.add(parameterDataChangeListener);
    }
}
