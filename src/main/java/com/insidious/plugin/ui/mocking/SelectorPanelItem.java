package com.insidious.plugin.ui.mocking;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.insidious.plugin.ui.mocking.SelectorPanel.HOVER_COLER;

public class SelectorPanelItem<T> {
    private JPanel radioBoxContainer;
    private JRadioButton radioButton;
    private JPanel labelContainer;
    private JLabel mainLabel;
    private JPanel mainPanel;

    public SelectorPanelItem(T item, OnSelectListener<T> onSelectListener) {
        mainLabel.setText(item.toString());
        MouseAdapter l = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSelectListener.onSelect(item);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                mainLabel.setForeground(JBColor.WHITE);

                mainPanel.setBackground(HOVER_COLER);
                radioButton.setBackground(HOVER_COLER);
                radioButton.setBackground(HOVER_COLER);
                labelContainer.setBackground(HOVER_COLER);
                radioBoxContainer.setBackground(HOVER_COLER);

            }


            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                mainLabel.setForeground(JBColor.BLACK);

                mainPanel.setBackground(null);
                radioButton.setBackground(null);
                radioButton.setBackground(null);
                labelContainer.setBackground(null);
                radioBoxContainer.setBackground(null);
            }
        };
        mainLabel.addMouseListener(l);
        labelContainer.addMouseListener(l);
        radioButton.addMouseListener(l);
        radioBoxContainer.addMouseListener(l);

        Cursor predefinedCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        radioBoxContainer.setCursor(predefinedCursor);
        labelContainer.setCursor(predefinedCursor);
        radioButton.setCursor(predefinedCursor);
        mainLabel.setCursor(predefinedCursor);
    }

    public Component getContent() {
        return mainPanel;
    }
}
