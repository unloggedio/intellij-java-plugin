package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.TestCandidateMetadata;

public class TestCandidate {
    private final MethodInfo methodInfo;
    private final ClassInfo classInfo;
    private final ExecutionSession executionSession;
    private final DataInfo methodEntryProbe;
    private final ClassWeaveInfo classWeaveInfo;
    private TestCandidateMetadata metadata;

    public TestCandidate(MethodInfo methodInfo,
                         ClassInfo classInfo,
                         ExecutionSession session,
                         DataInfo methodEntryProbe,
                         ClassWeaveInfo classWeaveInfo) {

        this.methodInfo = methodInfo;
        this.classInfo = classInfo;
        this.executionSession = session;
        this.methodEntryProbe = methodEntryProbe;
        this.classWeaveInfo = classWeaveInfo;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public ExecutionSession getExecutionSession() {
        return executionSession;
    }

    public DataInfo getMethodEntryProbe() {
        return methodEntryProbe;
    }

    public ClassWeaveInfo getClassWeaveInfo() {
        return classWeaveInfo;
    }

    public void setMetadata(TestCandidateMetadata metadata) {
        this.metadata = metadata;
    }

    public TestCandidateMetadata getMetadata() {
        return metadata;
    }
}
