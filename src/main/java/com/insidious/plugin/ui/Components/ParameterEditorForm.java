package com.insidious.plugin.ui.Components;

import com.insidious.plugin.client.ConstructorStrategy;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiParameter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class ParameterEditorForm {
    private static final Logger logger = LoggerUtil.getInstance(ParameterEditorForm.class);
    private JPanel mainContainer;
    private JTextField parameterValueTextField;
    private JLabel parameterTypeLabel;
    private JPanel parameterPanel;
    private JComboBox<ConstructorStrategy> creatorStrategySelector;

    public ParameterEditorForm(PsiParameter parameter) {
        parameterTypeLabel.setText(parameter.getName());
        parameterTypeLabel.setSize(new Dimension(100, -1));
        parameterTypeLabel.setPreferredSize(new Dimension(100, -1));
        parameterTypeLabel.setMaximumSize(new Dimension(100, -1));
        TitledBorder titledBorder = (TitledBorder) parameterPanel.getBorder();
        titledBorder.setTitle(parameter.getType().getPresentableText());


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

    }

    public JPanel getContent() {
        return mainContainer;
    }
}
