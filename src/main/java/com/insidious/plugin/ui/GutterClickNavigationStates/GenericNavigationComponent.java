package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class GenericNavigationComponent {
    private final GutterState currentState;
    private final InsidiousService insidiousService;
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel topTextPanel;
    private JLabel iconLabel;
    private JTextArea topStatusText;
    private JButton actionButton;
    private JTextArea mainContentText;
    private JEditorPane imagePane;
    private JButton discordButton;
    private JPanel supportPanel;

    public GenericNavigationComponent(GutterState state, InsidiousService insidiousService) {
        this.currentState = state;
        this.insidiousService = insidiousService;
        setTextAndIcons();
        if (state.equals(GutterState.NO_AGENT)) {
            //display button
            actionButton.setVisible(true);
            actionButton.setText("Download Agent");
            actionButton.addActionListener((e) -> insidiousService.getAgentStateProvider().triggerAgentDownload());
            actionButton.setIcon(UIUtils.DOWNLOAD_WHITE);
        } else if (state.equals(GutterState.PROCESS_RUNNING)) {
            actionButton.setVisible(true);
            actionButton.setText("Execute method");
            loadImageForCurrentState();
            actionButton.addActionListener((e) -> insidiousService.openDirectExecuteWindow());
            actionButton.setIcon(UIUtils.GENERATE_ICON);
        } else {
            actionButton.setVisible(false);
        }
        //loadimageForCurrentState();
        discordButton.addActionListener(e -> routeToDiscord());
        discordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });
    }

    public void loadImageForCurrentState() {
        this.imagePane.setVisible(true);
        String gif = "postman_gif.gif";
        loadHintGif(gif);
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void setTextAndIcons() {
        this.iconLabel.setText(getHeaderTextForCurrentState());
        this.mainContentText.setText(getBodyText());
        Icon icon = null;
        switch (currentState) {
//            case DATA_AVAILABLE:
//                icon= UIUtils.DATA_AVAILABLE_HEADER;
//                iconLabel.setForeground(UIUtils.yellow_alert);
//                break;
            case PROCESS_RUNNING:
                icon = UIUtils.PROCESS_RUNNING_HEADER;
                break;
            case NO_AGENT:
                icon = UIUtils.NO_AGENT_HEADER;
                iconLabel.setForeground(UIUtils.red);
                break;
        }
        this.iconLabel.setIcon(icon);
    }

    public void loadHintGif(String gif) {
        imagePane.setContentType("text/html");
        imagePane.setOpaque(false);
        String htmlString = "<html><body>" +
                "<div align=\"left\"><img src=\"" + Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("icons/gif/" + gif)) + "\" /></div></body></html>";
        imagePane.setText(htmlString);
        imagePane.revalidate();

    }

    private String getBodyText() {
        String header = "";
        switch (currentState) {
//            case DATA_AVAILABLE:
//                header="You can make code changes and run these inputs to check before and after";
//                break;
            case PROCESS_RUNNING:
                header = "Call your application/relevant APIs using Postman, Swagger or UI. The Unlogged agent will record input/output for each method accessed.";
                break;
            case NO_AGENT:
                header = "The agent byte code instruments your code to record input/output values of each method in your code.\n" +
                        "Read more about bytecode instrumenting here.";
                break;
        }
        return header;
    }

    private String getHeaderTextForCurrentState() {
        String header = "";
        switch (currentState) {
//            case DATA_AVAILABLE:
//                header="Unlogged agent has successfully recorded input/output this method";
//                break;
            case PROCESS_RUNNING:
                header = "Application is running but no recordings found for this method";
                break;
            case NO_AGENT:
                header = "Unlogged Java Agent not found";
                break;
        }
        return header;
    }

    private void routeToDiscord() {
        String link = "https://discord.gg/Hhwvay8uTa";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "ROUTE_TO_DISCORD", null);
    }

}
