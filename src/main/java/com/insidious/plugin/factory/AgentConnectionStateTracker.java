package com.insidious.plugin.factory;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.agent.AgentConnectionStateNotifier;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AgentConnectionStateTracker implements AgentConnectionStateNotifier {


    private static final Logger logger = LoggerUtil.getInstance(AgentConnectionStateTracker.class);
    private final Project project;

    public AgentConnectionStateTracker(Project project) {
        this.project = project;
    }

    @Override
    public void onConnectedToAgentServer(ServerMetadata serverMetadata) {
        InsidiousService insidiousService = project.getService(InsidiousService.class);
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
        properties.put("project", project.getName());
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
                    (Computable<PsiPackage>) () -> JavaPsiFacade.getInstance(project)
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
        insidiousService.onAgentConnected(serverMetadata);


    }


    public void onDisconnectedFromAgentServer() {
        InsidiousService insidiousService = project.getService(InsidiousService.class);
        insidiousService.onAgentDisconnected();
    }
}
