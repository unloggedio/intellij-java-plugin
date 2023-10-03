package com.insidious.plugin.pojo;

public enum ResourceEmbedMode {
    IN_FILE, IN_CODE;

    @Override
    public String toString() {
        switch (this) {
            case IN_FILE:
                return "In file";
            case IN_CODE:
                return "In code";
        }
        return "n/a";
    }
}
