package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.adapter.ParameterAdapter;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;

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
        rootContent.add(parameterValue, BorderLayout.CENTER);

    }

    public String getParameterValue() {
        return parameterValue.getText();
    }

    public JPanel getContent() {
        return rootContent;
    }
}
