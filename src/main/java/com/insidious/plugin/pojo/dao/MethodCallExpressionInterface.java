package com.insidious.plugin.pojo.dao;

import java.util.List;

public interface MethodCallExpressionInterface {
    long getId();

    void setId(long id);

    int getEntryProbeInfo_id();

    void setEntryProbeInfoId(int probeInfoId);

    long getSubject();

    void setSubject(long subjectId);

    List<Long> getArguments();

    void setArguments(String arguments);

    long getReturnValue_id();

    void setReturnValue_id(long returnValue_id);

    String getMethodName();

    void setMethodName(String methodName);

    void setIsStatic(boolean aStatic);

    boolean isStaticCall();

    void setStaticCall(boolean staticCall);

    long getEntryProbe_id();

    void setEntryProbeId(long eventId);

    int getCallStack();

    void setCallStack(int callStack);

    int getMethodAccess();

    void setMethodAccess(int methodAccess);

    List<Long> getArgumentProbes();

    void setArgumentProbes(String argumentProbes);

    long getReturnDataEvent();

    void setReturnDataEvent(long returnDataEvent);

    boolean getUsesFields();

    void setUsesFields(boolean usesFields);

    long getParentId();

    void setParentId(long parentId);

    void setMethodDefinitionId(int methodDefinitionId);

    int getMethodDefinitionId();

    int getThreadId();
    void setThreadId(int threadId);
}
