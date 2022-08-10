package com.insidious.plugin.client;

public class SELogFileMetadata {
    Long firstEventId;
    Long lastEventId;
    Integer threadId;

    public Long getFirstEventId() {
        return firstEventId;
    }

    public Long getLastEventId() {
        return lastEventId;
    }

    public Integer getThreadId() {
        return threadId;
    }

    public SELogFileMetadata(Long firstEventId, Long lastEventId, Integer threadId) {
        this.firstEventId = firstEventId;
        this.lastEventId = lastEventId;
        this.threadId = threadId;
    }
}
