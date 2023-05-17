package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.agent.ConnectionStateListener;
import com.insidious.plugin.agent.ServerMetadata;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
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
                    insidiousService.promoteState();
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
    public String getVideoBugAgentPath() {
        return Constants.AGENT_PATH.toAbsolutePath().toString();
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

        insidiousService.triggerGutterIconReload();
        insidiousService.promoteState();
        InsidiousNotification.notifyMessage("New session identified " + finalIncludedPackageName
                        + ", connected, agent version: " + serverMetadata.getAgentVersion(),
                NotificationType.INFORMATION);
        insidiousService.focusDirectInvokeTab();

    }

    @Override
    public void onDisconnectedFromAgentServer() {
        logger.warn("disconnected from agent");
        // disconnected from agent
        this.isAgentServerRunning = false;
        ApplicationManager.getApplication().invokeLater(() -> {
            insidiousService.triggerGutterIconReload();
            insidiousService.demoteState();
        });

    }


    @Override
    public String suggestAgentVersion() {
        String version = null;
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(insidiousService.getProject());
        Iterator<Library> libraryTableIterator = libraryTable.getLibraryIterator();
        int count = libraryTable.getLibraries().length;
        ProjectTypeInfo projectTypeInfo = insidiousService.getProjectTypeInfo();
        if (count == 0) {
            return projectTypeInfo.DEFAULT_PREFERRED_JSON_MAPPER();
        }
        while (libraryTableIterator.hasNext()) {
            Library lib = libraryTableIterator.next();
            if (lib.getName().contains("jackson-databind:")) {
                version = fetchVersionFromLibName(lib.getName(), "jackson-databind");
            }
        }
        if (version == null) {
            return projectTypeInfo.DEFAULT_PREFERRED_JSON_MAPPER();
        } else {
            return "jackson-" + version;
        }
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
}
