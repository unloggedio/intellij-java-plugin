package com.insidious.plugin.factory;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.agent.ConnectionStateListener;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultAgentStateProvider implements ConnectionStateListener, AgentStateProvider {
    final private Logger logger = LoggerUtil.getInstance(DefaultAgentStateProvider.class);
    final private InsidiousService insidiousService;
    private boolean isAgentServerRunning;

    public DefaultAgentStateProvider(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
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
        properties.put("agentVersion", serverMetadata.getAgentVersion());
        properties.put("package", serverMetadata.getIncludePackageName());
        properties.put("project", insidiousService.getProject().getName());
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
            PsiPackage locatedPackage = ApplicationManager.getApplication().runReadAction(
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

        InsidiousNotification.notifyMessage("New session identified tracking package " +
                        "[" + finalIncludedPackageName + "]",
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
            insidiousService.onDisconnectedFromAgentServer();
            insidiousService.removeCurrentActiveHighlights();
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
    public boolean isAgentRunning() {
        return isAgentServerRunning;
    }
}
