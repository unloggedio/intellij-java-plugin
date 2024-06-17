package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.client.NetworkClient;
import com.insidious.plugin.client.UnloggedClientInterface;
import com.insidious.plugin.client.UnloggedLocalClient;
import com.insidious.plugin.constants.ExecutionSessionSourceMode;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.upload.ExecutionSessionSource;
import com.intellij.openapi.project.Project;

public class UnloggedClientFactory {
    public static UnloggedClientInterface createClient(ExecutionSessionSource executionSessionSource) {
        UnloggedClientInterface client;
        if (executionSessionSource.getSessionMode() == ExecutionSessionSourceMode.REMOTE) {
            client = new NetworkClient(executionSessionSource);
        } else {
            client = new UnloggedLocalClient(InsidiousService.PATH_TO_SESSIONS);
        }
        return client;
    }
}
