package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.ui.methodscope.CandidateFilterType;

import java.util.HashSet;
import java.util.Set;

public class FilterModel {
    private final Set<String> includedClassNames = new HashSet<>();
    private final Set<String> excludedClassNames = new HashSet<>();
    private final Set<String> includedMethodNames = new HashSet<>();
    private final Set<String> excludedMethodNames = new HashSet<>();
    boolean followEditor;
    CandidateFilterType candidateFilterType;

    public FilterModel(FilterModel filterModel) {
        this.includedMethodNames.addAll(filterModel.includedMethodNames);
        this.includedClassNames.addAll(filterModel.includedClassNames);
        this.excludedMethodNames.addAll(filterModel.excludedMethodNames);
        this.excludedClassNames.addAll(filterModel.excludedClassNames);
        this.followEditor = filterModel.followEditor;
        this.candidateFilterType = filterModel.candidateFilterType;
    }

    public FilterModel() {
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

    public void setFrom(FilterModel filterModel) {
        this.includedMethodNames.clear();
        this.includedClassNames.clear();

        this.excludedMethodNames.clear();
        this.excludedClassNames.clear();


        this.includedMethodNames.addAll(filterModel.includedMethodNames);
        this.includedClassNames.addAll(filterModel.includedClassNames);
        this.excludedMethodNames.addAll(filterModel.excludedMethodNames);
        this.excludedClassNames.addAll(filterModel.excludedClassNames);
        this.followEditor = filterModel.followEditor;
        this.candidateFilterType = filterModel.candidateFilterType;

    }
}
