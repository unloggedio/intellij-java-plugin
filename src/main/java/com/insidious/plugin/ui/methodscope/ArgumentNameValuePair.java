package com.insidious.plugin.ui.methodscope;

public class ArgumentNameValuePair {
    String name;
    String type;
    String value;

    public ArgumentNameValuePair(String name, String type, String value) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
