package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.model.ReplayData;

public class TestCandidate {
    private final MethodInfo methodInfo;
    private final ClassInfo classInfo;
    private final ExecutionSession executionSession;
    private final DataInfo methodEntryProbe;
    private final ReplayData probeReplayData;

    public TestCandidate(MethodInfo methodInfo,
                         ClassInfo classInfo,
                         ExecutionSession session,
                         DataInfo methodEntryProbe,
                         ReplayData probeReplayData) {

        this.methodInfo = methodInfo;
        this.classInfo = classInfo;
        this.executionSession = session;
        this.methodEntryProbe = methodEntryProbe;
        this.probeReplayData = probeReplayData;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
