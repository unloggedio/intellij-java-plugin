package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.UsageInsightTracker;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UnloggedGPTNavigationBar {
    private JPanel mainPanel;
    private JPanel centerPanel;
    private JButton findBugsButton;
    private JButton optimizeButton;
    private JButton refactorButton;
    private JButton explainButton;
    private JPanel elementsParent;
    private JPanel selectedClassDetailsPanel;
    private JLabel selectedMethodNameLabel;

    private UnloggedGptListener listener;

    public UnloggedGPTNavigationBar(UnloggedGptListener listener) {
        this.listener=listener;
        findBugsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendUpdateRequest(findBugsButton.getText());
            }
        });
        findBugsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        optimizeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendUpdateRequest(optimizeButton.getText());
            }
        });
        optimizeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refactorButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendUpdateRequest(refactorButton.getText());
            }
        });
        refactorButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        explainButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendUpdateRequest(explainButton.getText());
            }
        });
        explainButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public JPanel getComponent()
    {
        return mainPanel;
    }

    public void sendUpdateRequest(String type)
    {
        listener.triggerCallOfType(type);
    }

    public void updateSelection(String s) {
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("option_type", s);
        UsageInsightTracker.getInstance().RecordEvent(
                "GPTOptionClicked",eventProperties);
        this.selectedMethodNameLabel.setText(s);
    }
}
