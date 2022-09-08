package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.factory.candidate.TestCandidateMetadata;

public class TestCandidate {
    private final MethodInfo methodInfo;
    private final ClassInfo classInfo;
    private final long methodEntryProbeIndex;
    private final ClassWeaveInfo classWeaveInfo;
    private TestCandidateMetadata metadata;

    public TestCandidate(MethodInfo methodInfo,
                         ClassInfo classInfo,
                         long methodEntryProbeIndex,
                         ClassWeaveInfo classWeaveInfo) {

        this.methodInfo = methodInfo;
        this.classInfo = classInfo;
        this.methodEntryProbeIndex = methodEntryProbeIndex;
        this.classWeaveInfo = classWeaveInfo;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public long getMethodEntryProbeIndex() {
        return methodEntryProbeIndex;
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
