package com.insidious.plugin.ui.Components;

import com.insidious.plugin.ui.ModuleSelectionListener;
import com.insidious.plugin.ui.UI_Utils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModulePanel {
    private JPanel mainPanel;
    private JLabel moduleNameText;
    private JLabel arrowLabel;
    private ModuleSelectionListener listener;

    public ModulePanel(String moduleName, ModuleSelectionListener listener)
    {
        Border border = new LineBorder(UI_Utils.teal);
        mainPanel.setBorder(border);
        this.listener=listener;
        setText(moduleName);
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                listener.onSelect(moduleName);
            }
        });
    }

    public void setText(String text)
    {
        this.moduleNameText.setText(text);
    }

    public JPanel getMainPanel()
    {
        return this.mainPanel;
    }
}
