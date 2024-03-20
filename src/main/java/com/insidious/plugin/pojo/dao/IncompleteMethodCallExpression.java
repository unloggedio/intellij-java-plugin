package com.insidious.plugin.pojo.dao;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.util.StringUtils;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@DatabaseTable(tableName = "incomplete_method_call")
public class IncompleteMethodCallExpression implements MethodCallExpressionInterface {

    @DatabaseField(id = true)
    private long id;

    @DatabaseField
    private String arguments;
    @DatabaseField
    private String methodName;
    @DatabaseField
    private boolean isStaticCall;

    @DatabaseField
    private boolean usesFields;
    @DatabaseField
    private long subject_id;

    @DatabaseField
    private int entryProbeInfo_id;
    @DatabaseField
    private long returnValue_id;
    @DatabaseField
    private long entryProbe_id;
    @DatabaseField
    private int callStack;
    @DatabaseField(index = true)
    private int methodAccess;
    @DatabaseField
    private String argumentProbes;
    @DatabaseField
    private long returnDataEvent;
    @DatabaseField(index = true)
    private long parentId;
    @DatabaseField
    private int methodDefinitionId;
    @DatabaseField(index = true)
    private int threadId;
    @DatabaseField
    private long returnNanoTime;
    @DatabaseField
    private long enterNanoTime;



    public IncompleteMethodCallExpression() {
    }

    public IncompleteMethodCallExpression(
            String methodName,
            long subject,
            String arguments,
            long returnValue_id
    ) {
        this.methodName = methodName;
        this.subject_id = subject;
        this.arguments = arguments;
        this.returnValue_id = returnValue_id;
    }

    public long getReturnNanoTime() {
        return returnNanoTime;
    }

    public void setReturnNanoTime(long returnNanoTime) {
        this.returnNanoTime = returnNanoTime;
    }

    public long getEnterNanoTime() {
        return enterNanoTime;
    }

    public void setEnterNanoTime(long enterNanoTime) {
        this.enterNanoTime = enterNanoTime;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int getEntryProbeInfo_id() {
        return entryProbeInfo_id;
    }

    @Override
    public void setEntryProbeInfoId(int probeInfoId) {
        this.entryProbeInfo_id = probeInfoId;
    }

    @Override
    public long getSubject() {
        return subject_id;
    }

    @Override
    public void setSubject(long testSubjectId) {
        this.subject_id = testSubjectId;
    }

    @Override
    public List<Long> getArguments() {
        if (arguments == null || arguments.length() < 1) {
            return List.of();
        }
        List<Long> argumentList = new LinkedList<>();
        String[] args = arguments.split(",");
        for (String arg : args) {
            argumentList.add(Long.valueOf(arg));
        }
        return argumentList;
    }

    @Override
    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    @Override
    public long getReturnValue_id() {
        return returnValue_id;
    }

    @Override
    public void setReturnValue_id(long returnValue_id) {
        this.returnValue_id = returnValue_id;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public void setIsStatic(boolean aStatic) {
        this.isStaticCall = aStatic;
    }

    @Override
    public boolean isStaticCall() {
        return isStaticCall;
    }

    @Override
    public void setStaticCall(boolean staticCall) {
        isStaticCall = staticCall;
    }

    @Override
    public long getEntryProbe_id() {
        return entryProbe_id;
    }

    @Override
    public void setEntryProbeId(long eventId) {
        this.entryProbe_id = eventId;
    }

    @Override
    public int getCallStack() {
        return callStack;
    }

    @Override
    public void setCallStack(int callStack) {
        this.callStack = callStack;
    }

    @Override
    public int getMethodAccess() {
        return methodAccess;
    }

    @Override
    public void setMethodAccess(int methodAccess) {
        this.methodAccess = methodAccess;
    }

    @Override
    public List<Long> getArgumentProbes() {
        if (argumentProbes == null || argumentProbes.length() < 1) {
            return List.of();
        }
        List<Long> argsProbeList = new LinkedList<>();
        String[] probeIds = argumentProbes.split(",");
        for (String probeId : probeIds) {
            argsProbeList.add(Long.valueOf(probeId));
        }
        return argsProbeList;
    }

    @Override
    public void setArgumentProbes(String argumentProbes) {
        this.argumentProbes = argumentProbes;
    }

    public String getArgumentProbesString() {
        return argumentProbes;
    }

    @Override
    public long getReturnDataEvent() {
        return returnDataEvent;
    }

    @Override
    public void setReturnDataEvent(long returnDataEvent) {
        this.returnDataEvent = returnDataEvent;
    }

    @Override
    public boolean getUsesFields() {
        return usesFields;
    }

    @Override
    public void setUsesFields(boolean usesFields) {
        this.usesFields = usesFields;
    }

    @Override
    public int getMethodDefinitionId() {
        return methodDefinitionId;
    }

    @Override
    public void setMethodDefinitionId(int methodDefinitionId) {
        this.methodDefinitionId = methodDefinitionId;
    }

    @Override
    public long getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }
}
