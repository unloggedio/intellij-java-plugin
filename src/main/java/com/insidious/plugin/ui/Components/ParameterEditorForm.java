package com.insidious.plugin.ui.Components;

import com.insidious.plugin.client.ConstructorStrategy;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.ui.ParameterDataChangeListener;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiParameter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

public class ParameterEditorForm {
    private static final Logger logger = LoggerUtil.getInstance(ParameterEditorForm.class);
    private final Parameter parameter;
    private final List<ParameterDataChangeListener> listeners = new ArrayList<>();
    private JPanel mainContainer;
    private JTextField parameterValueTextField;
    private JLabel parameterTypeLabel;
    private JPanel parameterPanel;
    private JComboBox<ConstructorStrategy> creatorStrategySelector;

    public ParameterEditorForm(Parameter parameter, PsiParameter psiParameter) {
        this.parameter = parameter;

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
            switch (selectedCreatorStrategy) {
                case CONSTRUCTOR:
                    break;
                case JSON_DESERIALIZE:
                    break;
                case MANUAL_CODE:
                    break;
            }
        });

        parameterValueTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                parameter.getProb().setSerializedValue(("\"" + parameterValueTextField.getText() + "\"").getBytes());
                callListeners();
            }
        });

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
