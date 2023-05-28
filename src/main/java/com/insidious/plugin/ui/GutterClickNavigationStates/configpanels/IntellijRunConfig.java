package com.insidious.plugin.ui.GutterClickNavigationStates.configpanels;

import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.ui.GutterClickNavigationStates.AddToRunConfigListener;
import com.insidious.plugin.ui.GutterClickNavigationStates.CopyToClipboardListener;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;

public class IntellijRunConfig {
    private JPanel vmparamsSection;
    private JTextArea vmparamsArea;
    private JButton copyToClipboardButton;
    private JButton addToCurrentRunConfigButton;
    private JPanel content;

    public IntellijRunConfig(String configText,
                             CopyToClipboardListener copyToClipboardListener,
                             AddToRunConfigListener addToRunConfigListener) {
        vmparamsArea.setText(configText);
        copyToClipboardButton.addActionListener((e) -> {
            copyToClipboardListener.onClick();
        });
        addToCurrentRunConfigButton.addActionListener(
                e -> {
                    addToRunConfigListener.onClick();
                });

    }

    public Component getComponent() {
        return content;
    }
}
