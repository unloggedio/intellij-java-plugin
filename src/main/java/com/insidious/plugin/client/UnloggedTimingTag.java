package com.insidious.plugin.client;

public class UnloggedTimingTag {
    int lineNumber;
    long nanoSecondTimestamp;

    public UnloggedTimingTag() {
    }

    public UnloggedTimingTag(int lineNumber, long nanoSecondTimestamp) {
        this.lineNumber = lineNumber;
        this.nanoSecondTimestamp = nanoSecondTimestamp;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public long getNanoSecondTimestamp() {
        return nanoSecondTimestamp;
    }

    public void setNanoSecondTimestamp(int nanoSecondTimestamp) {
        this.nanoSecondTimestamp = nanoSecondTimestamp;
    }
}
