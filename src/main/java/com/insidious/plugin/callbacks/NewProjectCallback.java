package com.insidious.plugin.callbacks;

public interface NewProjectCallback {
    void error(String errorMessage);
    void success(String projectId);
}
