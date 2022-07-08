package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.MethodInfo;

public class TestCaseUnit {

    MethodInfo methodInfo;


    private final String code;

    public TestCaseUnit(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
