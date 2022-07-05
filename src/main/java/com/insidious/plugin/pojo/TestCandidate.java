package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.pojo.ExecutionSession;

public class TestCandidate {
    private final MethodInfo methodInfo;
    private final ClassInfo classInfo;
    private final ExecutionSession executionSession;

    public TestCandidate(MethodInfo methodInfo, ClassInfo classInfo, ExecutionSession session) {

        this.methodInfo = methodInfo;
        this.classInfo = classInfo;
        this.executionSession = session;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
