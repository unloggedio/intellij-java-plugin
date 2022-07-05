package com.insidious.plugin.pojo;

import com.insidious.common.weaver.MethodInfo;

public class TestCandidate {
    private final MethodInfo methodInfo;

    public TestCandidate(MethodInfo methodInfo) {
        this.methodInfo = methodInfo;
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }
}
