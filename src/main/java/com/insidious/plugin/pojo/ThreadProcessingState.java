package com.insidious.plugin.pojo;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ThreadProcessingState {
    private List<Long> valueStack = new ArrayList<>();
    private int threadId;
    private List<com.insidious.plugin.pojo.dao.MethodCallExpression> callStack = new ArrayList<>();
    private List<String> nextNewObjectType = new LinkedList<>();
    private List<com.insidious.plugin.pojo.dao.TestCandidateMetadata> testCandidateMetadataStack = new ArrayList<>();
    private com.insidious.plugin.pojo.dao.MethodCallExpression mostRecentReturnedCall;
    private boolean skipTillNextMethodExit;

    public ThreadProcessingState(int threadId) {
        this.threadId = threadId;
    }

    public List<Long> getValueStack() {
        return valueStack;
    }

    public void setValueStack(List<Long> valueStack) {
        this.valueStack = valueStack;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public List<com.insidious.plugin.pojo.dao.MethodCallExpression> getCallStack() {
        return callStack;
    }

    public void setCallStack(List<com.insidious.plugin.pojo.dao.MethodCallExpression> callStack) {
        this.callStack = callStack;
    }

    public com.insidious.plugin.pojo.dao.MethodCallExpression getTopCall() {
        return callStack.get(callStack.size() - 1);
    }

    public int getCallStackSize() {
        return callStack.size();
    }

    public void pushCall(com.insidious.plugin.pojo.dao.MethodCallExpression methodCall) {
        callStack.add(methodCall);
    }

    public com.insidious.plugin.pojo.dao.MethodCallExpression popCall() {
        return callStack.remove(callStack.size() - 1);
    }

    public List<String> getNextNewObjectTypeStack() {
        return nextNewObjectType;
    }

    public String popNextNewObjectType() {
        return nextNewObjectType.remove(nextNewObjectType.size() - 1);
    }

    public void setNextNewObjectType(List<String> nextNewObjectType) {
        this.nextNewObjectType = nextNewObjectType;
    }

    public void pushNextNewObjectType(String nextNewObjectType) {
        this.nextNewObjectType.add(nextNewObjectType);
    }

    public void pushValue(long eventValue) {
        valueStack.add(eventValue);
    }

    public Long popValue() {
        return valueStack.remove(valueStack.size() - 1);
    }

    public com.insidious.plugin.pojo.dao.TestCandidateMetadata getTopCandidate() {
        return testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1);
    }

    public com.insidious.plugin.pojo.dao.TestCandidateMetadata popTopCandidate() {
        return testCandidateMetadataStack.remove(testCandidateMetadataStack.size() - 1);
    }

    public void pushTopCandidate(com.insidious.plugin.pojo.dao.TestCandidateMetadata testCandidateMetadata) {
        testCandidateMetadataStack.add(testCandidateMetadata);
    }

    public int candidateSize() {
        return testCandidateMetadataStack.size();
    }

    public List<com.insidious.plugin.pojo.dao.TestCandidateMetadata> getCandidateStack() {
        return testCandidateMetadataStack;
    }

    public void setCandidateStack(List<com.insidious.plugin.pojo.dao.TestCandidateMetadata> candidateStack) {
        this.testCandidateMetadataStack = candidateStack;
    }

    public com.insidious.plugin.pojo.dao.MethodCallExpression getMostRecentReturnedCall() {
        return mostRecentReturnedCall;
    }

    public void setMostRecentReturnedCall(com.insidious.plugin.pojo.dao.MethodCallExpression mostRecentReturnedCall) {
        this.mostRecentReturnedCall = mostRecentReturnedCall;
    }

    public boolean isSkipTillNextMethodExit() {
        return skipTillNextMethodExit;
    }

    public void setSkipTillNextMethodExit(boolean skipTillNextMethodExit1) {
        this.skipTillNextMethodExit = skipTillNextMethodExit1;
    }
}
