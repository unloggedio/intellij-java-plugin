package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.pojo.atomic.MethodUnderTest;

import java.util.List;

public class TestCandidateBareBone {
    long id;
    MethodUnderTest methodUnderTest;
    long exitProbeIndex;
    List<Integer> lineNumbers;
    long createdAt;
    private long timeSpentNano;

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public MethodUnderTest getMethodUnderTest() {
        return methodUnderTest;
    }

    public void setMethodUnderTest(MethodUnderTest methodUnderTest) {
        this.methodUnderTest = methodUnderTest;
    }

    public long getExitProbeIndex() {
        return exitProbeIndex;
    }

    public void setExitProbeIndex(long exitProbeIndex) {
        this.exitProbeIndex = exitProbeIndex;
    }

    public List<Integer> getLineNumbers() {
        return lineNumbers;
    }

    public void setLineNumbers(List<Integer> lineNumbers) {
        this.lineNumbers = lineNumbers;
    }

    public long getTimeSpentNano() {
        return timeSpentNano;
    }

    public void setTimeSpentNano(long timeSpentMs) {
        this.timeSpentNano = timeSpentMs;
    }
}
