package com.insidious.plugin.UITests.wrapper;

public class DirectInvokeTreeLine {
    private int index;
    private String value;

    public DirectInvokeTreeLine(int index, String value) {
        if (index < 1) index = 1;
        this.index = index;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
