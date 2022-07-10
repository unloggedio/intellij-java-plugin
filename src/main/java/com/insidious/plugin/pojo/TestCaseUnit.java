package com.insidious.plugin.pojo;

public class TestCaseUnit {


    private final String code;
    private final String packageName;
    private final String className;

    public TestCaseUnit(String code, String packageName, String className) {
        this.code = code;
        this.packageName = packageName;
        this.className = className;
    }

    @Override
    public String toString() {
        return code;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getCode() {
        return code;
    }

    public String getClassName() {
        return className;
    }
}
