package com.insidious.plugin.pojo;

public class TestCaseUnit {


    private final String code;

    public TestCaseUnit(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return code;
    }
}
