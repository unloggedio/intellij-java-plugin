package com.insidious.plugin.ui.methodscope;

import java.util.List;
import java.util.Map;
import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.agent.AgentCommand;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandResponse;

public class DifferenceResult {
    private final List<DifferenceInstance> differenceInstanceList;
    private DiffResultType diffResultType;
    private String gutterStatus;
    private final Map<String, Object> leftOnly;
    private final Map<String, Object> rightOnly;
    private MethodAdapter methodAdapter;
    private AgentCommandRequest command;
    private AgentCommandResponse response;
    public enum EXECUTION_MODE {DIRECT_INVOKE, ATOMIC_RUN_INDIVIDUAL, ATOMIC_RUN_REPLAY}
    private EXECUTION_MODE executionMode;

    public Map<String, Object> getLeftOnly() {
        return leftOnly;
    }

    public Map<String, Object> getRightOnly() {
        return rightOnly;
    }

    public DifferenceResult(List<DifferenceInstance> differenceInstanceList,
                            DiffResultType diffResultType,
                            Map<String, Object> leftOnly,
                            Map<String, Object> rightOnly) {
        this.differenceInstanceList = differenceInstanceList;
        this.diffResultType = diffResultType;
        this.leftOnly = leftOnly;
        this.rightOnly = rightOnly;
    }

    public List<DifferenceInstance> getDifferenceInstanceList() {
        return differenceInstanceList;
    }

    public DiffResultType getDiffResultType() {
        return diffResultType;
    }

    public void setDiffResultType(DiffResultType type)
    {
        this.diffResultType = type;
    }

    public MethodAdapter getMethodAdapter() {
        return methodAdapter;
    }

    public void setMethodAdapter(MethodAdapter methodAdapter) {
        this.methodAdapter = methodAdapter;
    }

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
                ", leftOnly=" + leftOnly +
                ", rightOnly=" + rightOnly +
                ", methodAdapter=" + methodAdapter +
                ", command=" + command +
                ", response=" + response +
                ", executionMode=" + executionMode +
                '}';
    }

    public String getGutterStatus() {
        return gutterStatus;
    }

    public void setGutterStatus(String gutterStatus) {
        this.gutterStatus = gutterStatus;
    }
}
