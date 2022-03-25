package com.insidious.plugin.pojo;

public class TracePoint {

    private final long recordedAt;
    long classId, linenum, dataId, threadId, value;
    String executionSessionId;
    String filename;
    String classname;
    String exceptionClass;
    private long nanoTime;

    public TracePoint(long classId, long linenum,
                      long dataId,
                      long threadId,
                      long value,
                      String executionSessionId,
                      String filename,
                      String classname,
                      String exceptionClass,
                      long recordedAt) {
        this.classId = classId;
        this.linenum = linenum;
        this.dataId = dataId;
        this.threadId = threadId;
        this.value = value;
        this.executionSessionId = executionSessionId;
        this.filename = filename;
        this.classname = classname;
        this.exceptionClass = exceptionClass;
        this.recordedAt = recordedAt;
        this.nanoTime = recordedAt;
    }

    public long getClassId() {
        return classId;
    }

    public long getLinenum() {
        return linenum;
    }

    public long getDataId() {
        return dataId;
    }

    public long getThreadId() {
        return threadId;
    }

    public long getValue() {
        return value;
    }

    public String getExecutionSessionId() {
        return executionSessionId;
    }

    public void setExecutionSessionId(String executionSessionId) {
        this.executionSessionId = executionSessionId;
    }

    public String getFilename() {
        return filename;
    }

    public String getClassname() {
        return classname;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public long getRecordedAt() {
        return recordedAt;
    }

    public long getNanoTime() {
        return nanoTime;
    }

    public void setNanoTime(long nanoTime) {
        this.nanoTime = nanoTime;
    }
}
