package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ClassInfo;

public class TestCaseScript {
    private final String code;

    public TestCaseScript(String code, ClassInfo classInfo) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
