package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UnloggedSDKOnboarding {
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel infoPanel;
    private JLabel headingLabel;
    private JPanel mainContent;
    private JPanel dependencyContents;
    private JTextArea dependencyArea;
    private JButton copyCodeButton;
    private JButton discordButton;
    private InsidiousService insidiousService;

    public UnloggedSDKOnboarding(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        copyCodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyCode();
            }
        });
        discordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                routeToDiscord();
            }
        });
    }

    public void copyCode() {
        insidiousService.copyToClipboard(dependencyArea.getText());
    }

    public void routeToDiscord() {
        String link = "https://discord.gg/Hhwvay8uTa";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDiscord_GPT", null);
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }
}
