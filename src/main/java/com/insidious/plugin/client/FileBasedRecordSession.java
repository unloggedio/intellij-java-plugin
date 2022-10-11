package com.insidious.plugin.client;

import com.insidious.plugin.client.pojo.ExecutionSession;

public class FileBasedRecordSession implements RecordSession {

    private final ExecutionSession session;

    public FileBasedRecordSession(ExecutionSession session) {
        this.session = session;
    }

    @Override
    public String getSessionId() {
        return session.getSessionId();
    }

    @Override
    public ExecutionSession getExecutionSession() {
        return session;
    }
}
