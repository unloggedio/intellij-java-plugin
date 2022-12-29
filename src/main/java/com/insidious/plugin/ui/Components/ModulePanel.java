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
    private JPanel labelParent;
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
        this.getMainPanel().addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent me) {
                hoverStateManager(me, true);
            }

            public void mouseExited(MouseEvent me) {
                hoverStateManager(me, false);
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
    private void hoverStateManager(MouseEvent me, boolean mouseEntered) {
        if (mouseEntered) {
            Color color = new Color(1, 204, 245,50);
            Border border = new LineBorder(UI_Utils.teal);
            mainPanel.setBorder(border);
            this.labelParent.setOpaque(true);
            Color transparent = new Color(1, 204, 245,1);
            this.labelParent.setBackground(transparent);
            this.mainPanel.setOpaque(true);
            this.mainPanel.setBackground(color);
        } else {
            Color color = new Color(187, 187, 187);
            Border border = new LineBorder(Color.gray);
            mainPanel.setBorder(border);
            this.labelParent.setOpaque(false);
            this.mainPanel.setOpaque(false);
        }
    }

}
