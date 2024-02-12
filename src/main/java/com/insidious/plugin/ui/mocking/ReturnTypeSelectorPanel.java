package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.ReturnValueType;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ReturnTypeSelectorPanel {
    public static final JBColor HOVER_COLER = new JBColor(
            new Color(150, 188, 199),
            new Color(150, 188, 199));
    private JPanel mainPanel;
    private JPanel returnNullPanel;
    private JRadioButton returnNullRadioButton;
    private JPanel returnInstancePanel;
    private JPanel throwInstancePanel;
    private JLabel returnNullLabel;
    private JLabel returnInstanceLabel;
    private JLabel throwExceptionLabel;
    private JPanel returnNullcheckBoxContainer;
    private JPanel returnNullLabelContainer;

    public ReturnTypeSelectorPanel(OnSelectListener<ReturnValueType> onSelectListener) {

        Color originalBackgrounColor = mainPanel.getBackground();

        MouseAdapter backgroundColorAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                returnNullLabel.setForeground(JBColor.WHITE);

                mainPanel.setBackground(HOVER_COLER);
                returnNullPanel.setBackground(HOVER_COLER);
                returnNullRadioButton.setBackground(HOVER_COLER);
                returnNullLabel.setBackground(HOVER_COLER);
                returnNullcheckBoxContainer.setBackground(HOVER_COLER);
                returnNullLabelContainer.setBackground(HOVER_COLER);

            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                returnNullLabel.setForeground(JBColor.BLACK);

                mainPanel.setBackground(originalBackgrounColor);
                returnNullPanel.setBackground(null);
                returnNullRadioButton.setBackground(null);
                returnNullLabel.setBackground(null);
                returnNullcheckBoxContainer.setBackground(null);
                returnNullLabelContainer.setBackground(null);
            }
        };
        returnNullPanel.addMouseListener(backgroundColorAdapter);
        returnNullRadioButton.addMouseListener(backgroundColorAdapter);
        returnNullLabel.addMouseListener(backgroundColorAdapter);
        returnNullcheckBoxContainer.addMouseListener(backgroundColorAdapter);
        returnNullLabelContainer.addMouseListener(backgroundColorAdapter);

        returnNullLabelContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        returnNullRadioButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        returnNullPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        returnNullLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        returnNullcheckBoxContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));


    }

    public JComponent getContent() {
        return mainPanel;
    }
}
