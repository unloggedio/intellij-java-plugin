package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.agent.ConnectionStateListener;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.InsidiousNotification;
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

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultAgentStateProvider implements ConnectionStateListener, AgentStateProvider {
    final private String javaAgentString = "-javaagent:\"" + Constants.AGENT_PATH + "=i=YOUR.PACKAGE.NAME\"";
    final private Logger logger = LoggerUtil.getInstance(DefaultAgentStateProvider.class);
    final private InsidiousService insidiousService;
    private final AgentDownloadService agentDownloadService;
    private boolean isAgentServerRunning;
    private boolean agentJarExists = true;
    private boolean downloadFailed;

    public DefaultAgentStateProvider(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.agentDownloadService = ApplicationManager.getApplication().getService(AgentDownloadService.class);
        logger.info("Agent download service: " + agentDownloadService.toString());
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
        String finalIncludedPackageName = "*";
        if (!"null".equals(includedPackageName)) {

            if ("?".equals(includedPackageName)) {
                InsidiousNotification.notifyMessage("A process running with unlogged agent was discovered, but the " +
                        "package to be captured is set to [?]. This means that no class is being captured. Please fix the" +
                        " i= parameter in the JVM parameters and specify the correct package to be captured and restart " +
                        "the process", NotificationType.WARNING);
                return;
            }

            finalIncludedPackageName = includedPackageName;
            String finalIncludedPackageName1 = finalIncludedPackageName;
            @Nullable PsiPackage locatedPackage = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiPackage>) () -> JavaPsiFacade.getInstance(insidiousService.getProject())
                            .findPackage(finalIncludedPackageName1));
            if (locatedPackage == null) {
                logger.warn("Package for agent [" + includedPackageName + "] not found in current project");
                return;
            } else {

                ApplicationManager.getApplication().runReadAction(() -> {
                    List<String> classNameList = Arrays.stream(locatedPackage.getDirectories())
                            .map(PsiDirectory::getName)
                            .collect(Collectors.toList());
                    logger.info("Package [" + finalIncludedPackageName1 + "] found in [" + locatedPackage.getProject()
                            .getName() + "] -> " + classNameList);
                });
            }
        }
        this.isAgentServerRunning = true;
        //reset atomicRecordMap
        insidiousService.initAtomicRecordService();

        InsidiousNotification.notifyMessage("New session identified tracking package [" + finalIncludedPackageName
                        + "] connected, agent version: " + serverMetadata.getAgentVersion(),
                NotificationType.INFORMATION);


        insidiousService.triggerGutterIconReload();
        insidiousService.setAgentProcessState(GutterState.PROCESS_RUNNING);
        insidiousService.focusDirectInvokeTab();

    }

    @Override
    public void onDisconnectedFromAgentServer() {
        logger.warn("disconnected from agent");
        // disconnected from agent
        this.isAgentServerRunning = false;
        if (insidiousService.getAtomicRecordService() != null) {
            insidiousService.getAtomicRecordService().writeAll();
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            insidiousService.triggerGutterIconReload();
            insidiousService.setAgentProcessState(GutterState.PROCESS_NOT_RUNNING);
            insidiousService.clearAtomicBoard();
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
        Task.Backgroundable downloadTask =
                new Task.Backgroundable(insidiousService.getProject(), "Unlogged Inc.", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        checkProgressIndicator("Downloading Unlogged agent", null);
                        if (!agentDownloadService.isDownloading()) {
                            Constants.AGENT_INFO_PATH.toFile().delete();
                            Constants.AGENT_PATH.toFile().delete();
                        }
                        downloadFailed = false;
                        downloadAgent();
                    }
                };
        ProgressManager.getInstance().run(downloadTask);
    }

    private boolean downloadAgent() {
//        agentDownloadInitiated = true;
        // deprecating downloads from s3 bucket and switching to maven repository
//        String host = "https://builds.bug.video/unlogged-java-agent-"+ Constants.AGENT_VERSION +"-";
//        String host = "https://s01.oss.sonatype.org/service/local/repositories/releases/content/video/bug/unlogged-java-agent/" + Constants.AGENT_VERSION + "/unlogged-java-agent-" + Constants.AGENT_VERSION;
        if (downloadFailed) {
            return false;
        }
        String host = "https://repo1.maven.org/maven2/video/bug/unlogged-java-agent/" + Constants.AGENT_VERSION + "/unlogged-java-agent-" + Constants.AGENT_VERSION;
        String extension = ".jar";

        checkProgressIndicator("Downloading Unlogged Java Agent", "Version: " + Constants.AGENT_VERSION);
        String url = (host + extension).trim();

        InsidiousNotification.notifyMessage(
                "Downloading Unlogged Java agent to $HOME/.unlogged/unlogged-java-agent.jar",
                NotificationType.INFORMATION);
        return agentDownloadService.downloadAgent(url);
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

//    @Override
//    public void run() {
//        long agentFileLastCheck = -1;
//        while (true) {
//
//            File agentInfoFile = Constants.AGENT_INFO_PATH.toFile();
//            File agentFile = Constants.AGENT_PATH.toFile();
//            if (agentInfoFile.exists()) {
//                try {
//                    String agentVersion = FileUtil.loadFile(agentInfoFile);
//                    if (!Constants.AGENT_VERSION.equals(agentVersion)) {
//                        logger.warn("Unexpected agent version on disk: " + agentVersion
//                                + ", expected: " + Constants.AGENT_VERSION);
//                        // unexpected agent version found
//                        // what to do ? let's download new version ?
//                        if (!agentDownloadService.isDownloading()) {
//                            agentInfoFile.delete();
//                            if (agentFile.exists()) {
//                                agentFile.delete();
//                            }
//                            if (downloadAgent()) {
//                                jarFound();
//                                break;
//                            } else {
//                                downloadFailed = true;
//                            }
//                        }
//                    } else {
//                        String existingAgentMd5 = "";
//                        if (!agentFile.exists() && !agentDownloadService.isDownloading()) {
//                            agentInfoFile.delete();
//                            continue;
//                        }
//
//                        long agentFileLastModified = agentFile.lastModified();
//                        if (agentFileLastModified > agentFileLastCheck) {
//                            existingAgentMd5 = md5SumForFile(agentFile);
//                            agentFileLastCheck = agentFileLastModified;
//                            if (Checksums.AGENT_MD5.equals(existingAgentMd5)) {
//                                // everything is all right
//                                jarFound();
//                                break;
//                            } else {
//                                // agent jar exists, and version file points to correct one
//                                // but the hash failed
//                                // maybe it is being downloaded ?
//                                logger.warn("Agent jar exists expected version [" + agentVersion + "] but hash check " +
//                                        "failed. Expected [" + Checksums.AGENT_MD5 + "] vs actual [" + existingAgentMd5 + "]");
//                            }
//                        }
//
//                    }
//                } catch (IOException e) {
//                    // failed to read agent info file ?
//                    logger.error("Failed to read agent info file: ", e);
//                }
//            } else {
//                // agent file doesn't exist
//                if (agentFile.exists()) {
//                    boolean oldAgentFileDeleted = agentFile.delete();
//                    if (!oldAgentFileDeleted) {
//                        // failed to delete old agent file which has no agent info file
//                        logger.warn("Failed to delete old agent jar");
//                    }
//                }
//                if (downloadAgent()) {
//                    jarFound();
//                    break;
//                } else {
//                    downloadFailed = true;
//                }
//            }
//
//            try {
//                Thread.sleep(1500);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

//    private void jarFound() {
//        agentJarExists = true;
//        insidiousService.triggerGutterIconReload();
//        if (this.isAgentServerRunning) {
//            insidiousService.setAgentProcessState(GutterState.PROCESS_RUNNING);
//        } else {
//            insidiousService.setAgentProcessState(GutterState.PROCESS_NOT_RUNNING);
//        }
//    }
}
