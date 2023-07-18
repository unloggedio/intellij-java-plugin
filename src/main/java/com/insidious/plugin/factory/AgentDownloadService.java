package com.insidious.plugin.factory;

import com.insidious.plugin.Checksums;
import com.insidious.plugin.Constants;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class AgentDownloadService {
    private static final Logger logger = LoggerUtil.getInstance(AgentDownloadService.class);
    private final String id;
    private boolean isDownloading;
    private boolean downloadCompleted;

    public AgentDownloadService() {
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String toString() {
        return id;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public synchronized boolean downloadAgent(String url) {
        logger.warn("Download unlogged java agent from: " + url);
        if (downloadCompleted) {
            return true;
        }
        isDownloading = true;
        try {

            UsageInsightTracker.getInstance().RecordEvent("AgentDownloadStart", null);
            Path fileURiString = Constants.AGENT_PATH;
            String absolutePath = fileURiString.toAbsolutePath().toString();
            File agentInfoFile = Constants.AGENT_INFO_PATH.toFile();

            try {
                boolean agentInfoFileCreated = agentInfoFile.createNewFile();
                if (!agentInfoFileCreated) {
                    logger.warn("Agent file already exists, skipping");
                    // failed to create agent info file
                    // someone else did ?
                    return false;
                }
                FileUtil.writeToFile(agentInfoFile, Constants.AGENT_VERSION);

            } catch (IOException e) {
                // failed to create agent info file
                JSONObject prop = new JSONObject();
                prop.put("message", e.getMessage());
                UsageInsightTracker.getInstance().RecordEvent("AgentDownloadException1", prop);
                logger.error("Failed to create agent file", e);
                return false;
            }

            File agentFile = new File(absolutePath);

            try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
                 FileOutputStream fileOS = new FileOutputStream(absolutePath)) {
                byte[] data = new byte[1024];
                int byteContent;
                while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                    fileOS.write(data, 0, byteContent);
                }

                if (md5Check(agentFile)) {
                    InsidiousNotification.notifyMessage("Agent downloaded. Start your application with Unlogged Java " +
                                    "agent to start using AtomicRuns and DirectInvoke",
                            NotificationType.INFORMATION);
                    downloadCompleted = true;
                    return true;
                } else {
                    agentInfoFile.delete();
                    InsidiousNotification.notifyMessage(
                            "Agent md5 check failed."
                                    + "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                            NotificationType.ERROR);
                    UsageInsightTracker.getInstance().RecordEvent("MD5checkFailed", null);
                }
                UsageInsightTracker.getInstance().RecordEvent("AgentDownloadDone", null);
            } catch (Exception e) {
                e.printStackTrace();
                InsidiousNotification.notifyMessage(
                        "Failed to download agent."
                                + "\n Need help ? <br /><a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                        NotificationType.ERROR);

                JSONObject eventProperties = new JSONObject();
                eventProperties.put("exception", e.getMessage());
                UsageInsightTracker.getInstance().RecordEvent("AgentDownloadException", eventProperties);
            }
        } catch(Exception e) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("exception", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("AgentDownloadException2", eventProperties);
        } finally {
            isDownloading = false;
        }
        return false;
    }

    public boolean md5Check(File agent) {
        try {
            String checksum = md5SumForFile(agent);
            return Checksums.AGENT_MD5.equals(checksum);
        } catch (Exception e) {
            JSONObject properties = new JSONObject();
            properties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("FAILED_AGENT_HASH_CHECK", properties);
            logger.error("Failed to get checksum of downloaded file.", e);
        }
        return false;
    }

    @NotNull
    private String md5SumForFile(File agent) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(agent.getPath()));
        byte[] hash;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            hash = messageDigest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
        String checksum = new BigInteger(1, hash).toString(16);
        while (checksum.length() < 32) {
            checksum = "0" + checksum;
        }
        return checksum;
    }


}
