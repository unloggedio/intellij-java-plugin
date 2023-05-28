package com.insidious.plugin.ui.GutterClickNavigationStates.configpanels;

import com.insidious.plugin.ui.GutterClickNavigationStates.CopyToClipboardListener;

import javax.swing.*;
import java.awt.*;

public class GradleConfigPanel {
    private JPanel vmparamsSection;
    private JTextArea vmparamsArea;
    private JButton copyToClipboardButton;
    private JPanel content;

    public GradleConfigPanel(String configText, CopyToClipboardListener copyToClipboardListener) {
        vmparamsArea.setText(configText);
        copyToClipboardButton.addActionListener((e) -> {
            copyToClipboardListener.onClick();
        });
    }
    public Component getComponent() {
        return content;
    }

}
