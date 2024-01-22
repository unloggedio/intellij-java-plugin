package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.ui.methodscope.CandidateFilterType;

import java.util.List;

public class FilterModel {
    boolean followEditor;
    CandidateFilterType candidateFilterType;
    List<String> includedClassNames;
    List<String> excludedClassNames;
    List<String> includedMethodNames;
    List<String> excludedMethodNames;

    public boolean isFollowEditor() {
        return followEditor;
    }

    public void setFollowEditor(boolean followEditor) {
        this.followEditor = followEditor;
    }

    public List<String> getIncludedClassNames() {
        return includedClassNames;
    }

    public List<String> getExcludedClassNames() {
        return excludedClassNames;
    }

    public List<String> getIncludedMethodNames() {
        return includedMethodNames;
    }

    public List<String> getExcludedMethodNames() {
        return excludedMethodNames;
    }
}
