package com.insidious.plugin.callbacks;

import com.insidious.plugin.videobugclient.pojo.ExecutionSession;

import java.io.IOException;
import java.util.List;

public interface GetProjectSessionsCallback {
    void error(String message);

    void success(List<ExecutionSession> executionSessionList) throws IOException;
}
