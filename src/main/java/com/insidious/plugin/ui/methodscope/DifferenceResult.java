package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.databind.JsonNode;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;

import java.util.List;

public class DifferenceResult {
    private final List<DifferenceInstance> differenceInstanceList;
    private final JsonNode leftOnly;
    private final JsonNode rightOnly;
    private DiffResultType diffResultType;
    //    private MethodAdapter methodAdapter;
    private AgentCommandRequest command;
    private AgentCommandResponse<?> response;
    private boolean useIndividualContext = false;
    private String batchID = null;
    private EXECUTION_MODE executionMode;

    public DifferenceResult(List<DifferenceInstance> differenceInstanceList,
                            DiffResultType diffResultType,
                            JsonNode leftOnly,
                            JsonNode rightOnly) {
        this.differenceInstanceList = differenceInstanceList;
        this.diffResultType = diffResultType;
        this.leftOnly = leftOnly;
        this.rightOnly = rightOnly;
    }

    public void setIndividualContext(boolean useIndividualContext) {
        this.useIndividualContext = useIndividualContext;
    }

    public boolean isUseIndividualContext() {
        return useIndividualContext;
    }

    public JsonNode getLeftOnly() {
        return leftOnly;
    }

    public JsonNode getRightOnly() {
        return rightOnly;
    }

    public List<DifferenceInstance> getDifferenceInstanceList() {
        return differenceInstanceList;
    }

    public DiffResultType getDiffResultType() {
        return diffResultType;
    }

    public void setDiffResultType(DiffResultType type) {
        this.diffResultType = type;
    }

//    public MethodAdapter getMethodAdapter() {
//        return methodAdapter;
//    }

//    public void setMethodAdapter(MethodAdapter methodAdapter) {
//        this.methodAdapter = methodAdapter;
//    }

    public AgentCommandRequest getCommand() {
        return command;
    }

    public void setCommand(AgentCommandRequest command) {
        this.command = command;
    }

    public AgentCommandResponse getResponse() {
        return response;
    }

    public void setResponse(AgentCommandResponse response) {
        this.response = response;
    }

    public EXECUTION_MODE getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(EXECUTION_MODE executionMode) {
        this.executionMode = executionMode;
    }

    @Override
    public String toString() {
        return "DifferenceResult{" +
                "differenceInstanceList=" + differenceInstanceList +
                ", diffResultType=" + diffResultType +
                ", useIndividualContext=" + useIndividualContext +
                ", leftOnly=" + leftOnly +
                ", rightOnly=" + rightOnly +
                ", command=" + command +
                ", response=" + response +
                ", executionMode=" + executionMode +
                '}';
    }

    public String getBatchID() {
        return batchID;
    }

    public void setBatchID(String batchID) {
        this.batchID = batchID;
    }

    public enum EXECUTION_MODE {DIRECT_INVOKE, ATOMIC_RUN_INDIVIDUAL, ATOMIC_RUN_REPLAY}
}
