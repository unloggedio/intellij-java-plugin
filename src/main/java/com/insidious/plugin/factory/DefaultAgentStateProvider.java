package com.insidious.plugin.factory;

import com.insidious.plugin.Checksums;
import com.insidious.plugin.Constants;
import com.insidious.plugin.agent.ConnectionStateListener;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DefaultAgentStateProvider implements ConnectionStateListener, AgentStateProvider {
    final private String javaAgentString = "-javaagent:\"" + Constants.AGENT_PATH + "=i=YOUR.PACKAGE.NAME\"";
    final private Logger logger = LoggerUtil.getInstance(DefaultAgentStateProvider.class);
    final private InsidiousService insidiousService;
    final private ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(1);
    private boolean isAgentServerRunning;
    private boolean agentJarExists;

    public DefaultAgentStateProvider(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        threadPoolExecutor.submit(() -> {
            while (true) {
                File agentFile = Constants.AGENT_PATH.toFile();
                if (agentFile.exists()) {
                    logger.warn("Found agent jar at: " + Constants.AGENT_PATH);
                    System.out.println("Found agent jar.");
                    agentJarExists = true;
                    insidiousService.triggerGutterIconReload();
                    if (this.isAgentServerRunning) {
                        insidiousService.promoteState(GutterState.PROCESS_RUNNING);
                    } else {
                        insidiousService.promoteState(GutterState.PROCESS_NOT_RUNNING);
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    @Override
    public String getJavaAgentString() {
        return javaAgentString;
    }


    @Override
    public void onConnectedToAgentServer(ServerMetadata serverMetadata) {
        // connected to agent
        String includedPackageName = serverMetadata.getIncludePackageName().replace('/', '.');
        if (includedPackageName.startsWith("[")) {
            includedPackageName = includedPackageName.substring(1);
            if (includedPackageName.endsWith("]")) {
                includedPackageName = includedPackageName.substring(0,
                        includedPackageName.length() - 1);
            }
            includedPackageName = includedPackageName.split(",")[0];
        }
        logger.warn("connected to agent: " + serverMetadata);
        JSONObject properties = new JSONObject();
        properties.put("version", serverMetadata.getAgentVersion());
        properties.put("package", serverMetadata.getIncludePackageName());
        UsageInsightTracker.getInstance().RecordEvent("AGENT_CONNECTED", properties);
        if ("?".equals(includedPackageName)) {
            InsidiousNotification.notifyMessage("A process running with unlogged agent was discovered, but the " +
                    "package to be captured is set to [?]. This means that no class is being captured. Please fix the" +
                    " i= parameter in the JVM parameters and specify the correct package to be captured and restart " +
                    "the process", NotificationType.WARNING);
            return;
        }

        String finalIncludedPackageName = includedPackageName;
        @Nullable PsiPackage locatedPackage = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiPackage>) () -> JavaPsiFacade.getInstance(insidiousService.getProject())
                        .findPackage(finalIncludedPackageName));
        if (locatedPackage == null) {
            logger.warn("Package for agent [" + includedPackageName + "] not found in current project");
            return;
        } else {

            ApplicationManager.getApplication().runReadAction(() -> {
                List<String> classNameList = Arrays.stream(locatedPackage.getDirectories()).map(PsiDirectory::getName)
                        .collect(Collectors.toList());
                logger.info("Package [" + finalIncludedPackageName + "] found in [" + locatedPackage.getProject()
                        .getName() + "] -> " + classNameList);
            });
        }
        this.isAgentServerRunning = true;

        InsidiousNotification.notifyMessage("New session identified tracking package [" + finalIncludedPackageName
                        + "] connected, agent version: " + serverMetadata.getAgentVersion(),
                NotificationType.INFORMATION);


        if (agentJarExists) {
            insidiousService.triggerGutterIconReload();
            insidiousService.promoteState(GutterState.PROCESS_RUNNING);
            insidiousService.focusDirectInvokeTab();
        }


    }

    @Override
    public void onDisconnectedFromAgentServer() {
        logger.warn("disconnected from agent");
        // disconnected from agent
        this.isAgentServerRunning = false;
        ApplicationManager.getApplication().invokeLater(() -> {
            insidiousService.triggerGutterIconReload();
            if (agentJarExists) {
                insidiousService.promoteState(GutterState.PROCESS_NOT_RUNNING);
            } else {
                insidiousService.promoteState(GutterState.NO_AGENT);
            }
        });

    }

    @Override
    public String fetchVersionFromLibName(String name, String lib) {
        String[] parts = name.split(lib + ":");
        return trimVersion(parts[parts.length - 1].trim());
    }

    public String trimVersion(String version) {
        String[] versionParts = version.split("\\.");
        if (versionParts.length > 2) {
            return versionParts[0] + "." + versionParts[1];
        }
        return version;
    }


    @Override
    public boolean doesAgentExist() {
        return agentJarExists;
    }

    @Override
    public boolean isAgentRunning() {
        return isAgentServerRunning;
    }

    @Override
    public void triggerAgentDownload() {
        logger.info("Download Agent triggered");
        downloadAgentInBackground();
    }

    /**
     *
     */
    public void downloadAgentInBackground() {
        Task.Backgroundable downloadTask =
                new Task.Backgroundable(insidiousService.getProject(), "Unlogged Inc.", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        checkProgressIndicator("Downloading Unlogged agent", null);
                        downloadAgent();
                    }
                };
        ProgressManager.getInstance().run(downloadTask);
    }

    private void downloadAgent() {
//        agentDownloadInitiated = true;
        // deprecating downloads from s3 bucket and switching to maven repository
//        String host = "https://builds.bug.video/unlogged-java-agent-"+ Constants.AGENT_VERSION +"-";
//        String host = "https://s01.oss.sonatype.org/service/local/repositories/releases/content/video/bug/unlogged-java-agent/" + Constants.AGENT_VERSION + "/unlogged-java-agent-" + Constants.AGENT_VERSION;
        String host = "https://repo1.maven.org/maven2/video/bug/unlogged-java-agent/" + Constants.AGENT_VERSION + "/unlogged-java-agent-" + Constants.AGENT_VERSION;
        String extension = ".jar";

        checkProgressIndicator("Downloading Unlogged Java Agent", "Version: " + Constants.AGENT_VERSION);
        String url = (host + extension).trim();

        InsidiousNotification.notifyMessage(
                "Downloading Unlogged Java agent to $HOME/.unlogged/unlogged-java-agent.jar",
                NotificationType.INFORMATION);
        downloadAgent(url);
    }

    private void downloadAgent(String url) {
        UsageInsightTracker.getInstance().RecordEvent("AgentDownloadStart", null);
        Path fileURiString = Constants.AGENT_PATH;
        String absolutePath = fileURiString.toAbsolutePath().toString();

        File agentFile = new File(absolutePath);
        if (agentFile.exists()) {
            return;
        }
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

            } else {
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
    }


    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText(text1);
            }
        }
    }

    public boolean md5Check(File agent) {
        checkProgressIndicator("Checking md5 checksum", null);
        try {
            byte[] data = Files.readAllBytes(Paths.get(agent.getPath()));
            byte[] hash = MessageDigest.getInstance("MD5").digest(data);
            String checksum = new BigInteger(1, hash).toString(16);
            while (checksum.length() < 32) {
                checksum = "0" + checksum;
            }
            return Checksums.AGENT_JACKSON_2_13.equals(checksum);
        } catch (Exception e) {
            JSONObject properties = new JSONObject();
            properties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("FAILED_AGNET_HASH_CHECK", properties);
            logger.error("Failed to get checksum of downloaded file.", e);
        }
        return false;
    }

}
