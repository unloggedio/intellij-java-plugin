package com.insidious.plugin.pojo.dao;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;

import java.util.List;

public interface MethodCallExpressionInterface {
    long getId();

    void setId(long id);

    int getEntryProbeInfo_id();

    void setEntryProbeInfo_id(DataInfo entryProbeInfo_id);

    void setEntryProbeInfo(ProbeInfo entryProbeInfo);

    long getSubject();

    void setSubject(Parameter testSubject);

    List<Long> getArguments();

    void setArguments(String arguments);

    long getReturnValue_id();

    void setReturnValue_id(Parameter returnValue_id);

    String getMethodName();

    void setMethodName(String methodName);

    void setIsStatic(boolean aStatic);

    boolean isStaticCall();

    void setStaticCall(boolean staticCall);

    long getEntryProbe_id();

    void setEntryProbe_id(DataEventWithSessionId entryProbe_id);

    int getCallStack();

    void setCallStack(int callStack);

    int getMethodAccess();

    void setMethodAccess(int methodAccess);

    List<Long> getArgumentProbes();

    void setArgumentProbes(List<Long> argumentProbes);

    long getReturnDataEvent();

    void setReturnDataEvent(long returnDataEvent);

    boolean getUsesFields();

    void setUsesFields(boolean usesFields);

    long getParentId();

    void setParentId(long parentId);

    void setMethodDefinitionId(int methodDefinitionId);

    int getMethodDefinitionId();
}
