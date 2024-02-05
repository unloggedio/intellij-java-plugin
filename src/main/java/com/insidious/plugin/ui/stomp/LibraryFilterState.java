package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.ui.methodscope.CandidateFilterType;

import java.util.HashSet;
import java.util.Set;

public class LibraryFilterState {
    private final Set<String> includedClassNames = new HashSet<>();
    private final Set<String> excludedClassNames = new HashSet<>();
    private final Set<String> includedMethodNames = new HashSet<>();
    private final Set<String> excludedMethodNames = new HashSet<>();
    boolean followEditor;
    CandidateFilterType candidateFilterType;
    boolean showTests;
    boolean showMocks;

    public CandidateFilterType getCandidateFilterType() {
        return candidateFilterType;
    }

    public void setCandidateFilterType(CandidateFilterType candidateFilterType) {
        this.candidateFilterType = candidateFilterType;
    }

    public boolean isShowTests() {
        return showTests;
    }

    public void setShowTests(boolean showTests) {
        this.showTests = showTests;
    }

    public boolean isShowMocks() {
        return showMocks;
    }

    public void setShowMocks(boolean showMocks) {
        this.showMocks = showMocks;
    }

    public boolean isFollowEditor() {
        return followEditor;
    }

    public void setFollowEditor(boolean followEditor) {
        this.followEditor = followEditor;
    }

    public Set<String> getIncludedClassNames() {
        return includedClassNames;
    }

    public Set<String> getExcludedClassNames() {
        return excludedClassNames;
    }

    public Set<String> getIncludedMethodNames() {
        return includedMethodNames;
    }

    public Set<String> getExcludedMethodNames() {
        return excludedMethodNames;
    }
}
