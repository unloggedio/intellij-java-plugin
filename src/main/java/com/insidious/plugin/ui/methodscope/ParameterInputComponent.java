package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class ParameterInputComponent {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private JPanel rootContent;
    private JTextComponent parameterValue;

    public ParameterInputComponent(ParameterAdapter parameter, String defaultParameterValue,
                                   Class<? extends JTextComponent> inputTextComponent) {
        super();
        ((TitledBorder) rootContent.getBorder()).setTitle(
                parameter.getType().getPresentableText() + " " + parameter.getName());

        if (inputTextComponent.equals(JBTextField.class) || inputTextComponent.equals(JTextField.class)) {
            parameterValue = new JBTextField();
            rootContent.setMaximumSize(new Dimension(-1, 100));
        } else if (inputTextComponent.equals(JBTextArea.class) || inputTextComponent.equals(JTextArea.class)) {
            parameterValue = new JBTextArea();
            try {
                JsonNode jsonNode = objectMapper.readValue(defaultParameterValue, JsonNode.class);
                defaultParameterValue = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
            } catch (JsonProcessingException e) {
                // no pretty print
            }
        }

        parameterValue.setText(defaultParameterValue);
        // row="0" column="0" row-span="1" col-span="1" vsize-policy="0" hsize-policy="6" anchor="8" fill="1" indent="0"
//        rootContent.add(parameterValue,
//                new GridConstraints(0, 0, 1, 1, 8, 1, 6, 0, new Dimension(-1, 80)
//                        , new Dimension(-1, 120), new Dimension(-1, 400)));

        rootContent.add(parameterValue, BorderLayout.CENTER);

    }

    public String getParameterValue() {
        return parameterValue.getText();
    }

    public JPanel getContent() {
        return rootContent;
    }
}
