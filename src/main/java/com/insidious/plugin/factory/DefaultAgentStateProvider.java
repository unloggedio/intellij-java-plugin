package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.agent.AgentClient;
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

import java.util.Iterator;

public class DefaultAgentStateProvider implements AgentStateProvider {
    final private String javaAgentString = "-javaagent:\"" + Constants.AGENT_PATH + "=i=YOUR.PACKAGE.NAME\"";
    private Logger logger = LoggerUtil.getInstance(DefaultAgentStateProvider.class);
    private boolean isAgentServerRunning;
    private InsidiousService insidiousService;
    private AgentClient agentClient;

    public DefaultAgentStateProvider(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
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
    public void onConnectedToAgentServer() {
        logger.warn("connected to agent");
        // connected to agent
        this.isAgentServerRunning = true;
        insidiousService.triggerGutterIconReload();

        ServerMetadata serverMetadata = this.agentClient.getServerMetadata();
        InsidiousNotification.notifyMessage("New session identified "
                        + serverMetadata.getIncludePackageName()
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
        Iterator<Library> lib_iterator = libraryTable.getLibraryIterator();
        int count = 0;
        while (lib_iterator.hasNext()) {
            Library lib = lib_iterator.next();
            if (lib.getName().contains("jackson-databind:")) {
                version = fetchVersionFromLibName(lib.getName(), "jackson-databind");
            }
            count++;
        }
        ProjectTypeInfo projectTypeInfo = insidiousService.getProjectTypeInfo();
        if (count == 0) {
            //libs not ready
            return projectTypeInfo.DEFAULT_PREFERRED_JSON_MAPPER();

        } else {
            if (version == null) {
                return projectTypeInfo.DEFAULT_PREFERRED_JSON_MAPPER();
            } else {
                return "jackson-" + version;
            }
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
        return false;
    }
}
