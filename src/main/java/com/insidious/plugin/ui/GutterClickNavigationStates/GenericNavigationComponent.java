package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.Checksums;
import com.insidious.plugin.Constants;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Objects;

public class GenericNavigationComponent {
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel topTextPanel;
    private JLabel iconLabel;
    private JTextArea topStatusText;
    private JButton actionButton;
    private JTextArea mainContentText;
    private GutterState currentState;
    private JEditorPane imagePane;
    private JButton discordButton;
    private JPanel supportPanel;
    private InsidiousService insidiousService;

    public GenericNavigationComponent(GutterState state, InsidiousService insidiousService) {
        this.currentState = state;
        this.insidiousService = insidiousService;
        setTextAndIcons();
        if (state.equals(GutterState.NO_AGENT)) {
            //display button
            this.actionButton.setVisible(true);
            this.actionButton.setText("Download Agent");
            actionButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    triggerAgentDownload();
                }
            });
            actionButton.setIcon(UIUtils.DOWNLOAD_WHITE);
        } else if (state.equals(GutterState.PROCESS_RUNNING)) {
            this.actionButton.setVisible(true);
            this.actionButton.setText("Execute method");
            loadimageForCurrentState();
            actionButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    insidiousService.openDirectExecuteWindow();
                }
            });
            actionButton.setIcon(UIUtils.GENERATE_ICON);
        } else {
            actionButton.setVisible(false);
        }
        //loadimageForCurrentState();
        discordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });
    }

    public void loadimageForCurrentState() {
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

    public void triggerAgentDownload() {
        if (this.currentState.equals(GutterState.NO_AGENT)) {
            System.out.println("Download Agent triggered");
            String agentVersion = insidiousService.suggestAgentVersion();
            System.out.println("Agent suggested : " + agentVersion);
            downloadAgentinBackground(agentVersion);
        }
    }

    public void downloadAgentinBackground(String version) {
        Task.Backgroundable dl_task =
                new Task.Backgroundable(insidiousService.getProject(), "Unlogged, Inc.", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        checkProgressIndicator("Downloading Unlogged agent", null);
                        downloadAgent(version);
                    }
                };
        ProgressManager.getInstance()
                .run(dl_task);
    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText(text1);
            }
        }
    }

    private void downloadAgent(String version) {
//        agentDownloadInitiated = true;
        String host = "https://builds.bug.video/unlogged-java-agent-1.10.3-SNAPSHOT-";
        String type = version;
        String extention = ".jar";

        checkProgressIndicator("Downloading Unlogged agent", "version : " + type);
        String url = (host + type + extention).trim();
//        logger.info("[Downloading from] " + url);
        InsidiousNotification.notifyMessage(
                "Downloading agent for dependency : " + type,
                NotificationType.INFORMATION);
        downloadAgent(url, true);
    }

    private void downloadAgent(String url, boolean overwrite) {
//        logger.info("[Starting agent download]");
        UsageInsightTracker.getInstance()
                .RecordEvent("AgentDownloadStart", null);
        Path fileURiString = Constants.AGENT_PATH;
        String absolutePath = fileURiString.toAbsolutePath()
                .toString();

        File agentFile = new File(absolutePath);
        if (agentFile.exists() && !overwrite) {
            return;
        }
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOS = new FileOutputStream(absolutePath)) {
            byte[] data = new byte[1024];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                fileOS.write(data, 0, byteContent);
            }
//            logger.info("[Agent download complete]");
            if (md5Check(fetchVersionFromUrl(url), agentFile)) {
                InsidiousNotification.notifyMessage("Agent downloaded.",
                        NotificationType.INFORMATION);

            } else {
                InsidiousNotification.notifyMessage(
                        "Agent md5 check failed."
                                + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                        NotificationType.ERROR);
//                logger.info("Agent MD5 check failed, checksums are different.");
                UsageInsightTracker.getInstance()
                        .RecordEvent("MD5checkFailed", null);
            }
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("agent_version", fetchVersionFromUrl(url));
            UsageInsightTracker.getInstance()
                    .RecordEvent("AgentDownloadDone", eventProperties);
        } catch (Exception e) {
//            logger.info("[Agent download failed]");
            InsidiousNotification.notifyMessage(
                    "Failed to download agent."
                            + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                    NotificationType.ERROR);

            JSONObject eventProperties = new JSONObject();
            eventProperties.put("exception", e.getMessage());
            UsageInsightTracker.getInstance()
                    .RecordEvent("AgentDownloadException", eventProperties);
        }
    }

    public String fetchVersionFromUrl(String url) {
        if (url.contains("gson")) {
            return "gson";
        } else {
            return url.substring(url.indexOf("-jackson-") + 1, url.indexOf(".jar"));
        }
    }

    public boolean md5Check(String agentVersion, File agent) {
        checkProgressIndicator("Checking md5 checksum", null);
        try {
            byte[] data = Files.readAllBytes(Paths.get(agent.getPath()));
            byte[] hash = MessageDigest.getInstance("MD5")
                    .digest(data);
            String checksum = new BigInteger(1, hash).toString(16);
            while (checksum.length() < 32) {
                checksum = "0" + checksum;
            }
            switch (agentVersion) {
                case "gson":
                    if (checksum.equals(Checksums.AGENT_GSON)) {
                        return true;
                    }
                    break;
                case "jackson-2.8":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_8)) {
                        return true;
                    }
                    break;
                case "jackson-2.9":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_9)) {
                        return true;
                    }
                    break;
                case "jackson-2.10":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_10)) {
                        return true;
                    }
                    break;
                case "jackson-2.11":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_11)) {
                        return true;
                    }
                    break;
                case "jackson-2.12":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_12)) {
                        return true;
                    }
                    break;
                case "jackson-2.13":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_13)) {
                        return true;
                    }
                    break;
                case "jackson-2.14":
                    if (checksum.equals(Checksums.AGENT_JACKSON_2_14)) {
                        return true;
                    }
                    break;
            }
        } catch (Exception e) {
//            logger.info("Failed to get checksum of downloaded file.");
        }
        return false;
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
                header = "Unlogged Agent is not found.";
                break;
        }
        return header;
    }

    private void routeToDiscord() {
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
                "routeToDiscord_EXE", null);
    }

    public void setButtonText(String text)
    {
        this.actionButton.setText(text);
    }
}
