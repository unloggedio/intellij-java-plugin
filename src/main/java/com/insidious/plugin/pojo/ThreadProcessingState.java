package com.insidious.plugin.pojo;


import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;

import java.util.LinkedList;
import java.util.List;

public class ThreadProcessingState {
    private List<Long> valueStack = new LinkedList<>();
    private int threadId;
    private List<MethodCallExpression> callStack = new LinkedList<>();
    private List<String> nextNewObjectType = new LinkedList<>();
    private List<TestCandidateMetadata> testCandidateMetadataStack = new LinkedList<>();
    private MethodCallExpression mostRecentReturnedCall;

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

    public List<MethodCallExpression> getCallStack() {
        return callStack;
    }

    public void setCallStack(List<MethodCallExpression> callStack) {
        this.callStack = callStack;
    }

    public MethodCallExpression getTopCall() {
        return callStack.get(callStack.size() - 1);
    }

    public int getCallStackSize() {
        return callStack.size();
    }

    public void pushCall(MethodCallExpression methodCall) {
        callStack.add(methodCall);
    }

    public MethodCallExpression popCall() {
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

    public TestCandidateMetadata getTopCandidate() {
        return testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1);
    }

    public TestCandidateMetadata popTopCandidate() {
        return testCandidateMetadataStack.remove(testCandidateMetadataStack.size() - 1);
    }

    public void pushTopCandidate(TestCandidateMetadata testCandidateMetadata) {
        testCandidateMetadataStack.add(testCandidateMetadata);
    }

    public int candidateSize() {
        return testCandidateMetadataStack.size();
    }

    public List<TestCandidateMetadata> getCandidateStack() {
        return testCandidateMetadataStack;
    }

    public void setCandidateStack(List<TestCandidateMetadata> candidateStack) {
        this.testCandidateMetadataStack = candidateStack;
    }

    public void setMostRecentReturnedCall(MethodCallExpression mostRecentReturnedCall) {
        this.mostRecentReturnedCall = mostRecentReturnedCall;
    }

    public MethodCallExpression getMostRecentReturnedCall() {
        return mostRecentReturnedCall;
    }
}
