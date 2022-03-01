package com.insidious.plugin.callbacks;

import com.insidious.plugin.network.pojo.ExecutionSession;

import java.util.List;

public interface GetProjectSessionsCallback {
    void error(String message);

    void success(List<ExecutionSession> executionSessionList);
}
