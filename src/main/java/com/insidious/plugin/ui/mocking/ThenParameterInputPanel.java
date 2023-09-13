package com.insidious.plugin.ui.mocking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.mocking.MethodExitType;
import com.insidious.plugin.mocking.ThenParameter;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ThenParameterInputPanel {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ThenParameter thenParameter;
    private JPanel mainPanel;
    private JTextField returnTypeTextField;
    private JTextArea returnValueTextArea;
    private JComboBox<MethodExitType> returnType;
    private JScrollPane textAreaScrollPanel;
    private JPanel textAreaScrollParent;

    public ThenParameterInputPanel(ThenParameter thenParameter) {
        this.thenParameter = thenParameter;
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
            public void keyPressed(KeyEvent e) {
                thenParameter.getReturnParameter().setValue(returnValueTextArea.getText());
            }
        });

        returnTypeTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String text = returnTypeTextField.getText();
//                PsiClass classFound = JavaPsiFacade.getInstance(project)
//                        .findClass(text, GlobalSearchScope.projectScope(project));
//                if (classFound == null) {
//                    returnTypeTextField.setBackground(JBColor.RED);
//                }
                thenParameter.getReturnParameter().setClassName(text);
            }
        });

//        if (thenParamValue.split("\\n").length > 7) {
//            int height = 500;
//            textAreaScrollPanel.setPreferredSize(new Dimension(-1, height));
//            textAreaScrollPanel.setSize(new Dimension(-1, height));
//            textAreaScrollPanel.setMaximumSize(new Dimension(-1, height));
//            textAreaScrollParent.setPreferredSize(new Dimension(-1, height));
//            textAreaScrollParent.setSize(new Dimension(-1, height));
//            textAreaScrollParent.setMaximumSize(new Dimension(-1, height));
//            textAreaScrollPanel.revalidate();
//            textAreaScrollPanel.repaint();
//            textAreaScrollParent.revalidate();
//            textAreaScrollParent.repaint();
//        }
    }

    public ThenParameter getThenParameter() {
        return thenParameter;
    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
