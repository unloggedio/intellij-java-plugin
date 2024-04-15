package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.ui.methodscope.CandidateFilterType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StompFilterModel {
    private final Set<String> includedClassNames = new HashSet<>();
    private final Set<String> excludedClassNames = new HashSet<>();
    private final Set<String> includedMethodNames = new HashSet<>();
    private final Set<String> excludedMethodNames = new HashSet<>();
    boolean followEditor;
    CandidateFilterType candidateFilterType = CandidateFilterType.ALL;
    public boolean isEmpty() {
        return includedClassNames.isEmpty()
                && includedMethodNames.isEmpty()
                && excludedClassNames.isEmpty()
                && excludedMethodNames.isEmpty();

    }

    public StompFilterModel(StompFilterModel stompFilterModel) {
        this.includedMethodNames.addAll(stompFilterModel.includedMethodNames);
        this.includedClassNames.addAll(stompFilterModel.includedClassNames);
        this.excludedMethodNames.addAll(stompFilterModel.excludedMethodNames);
        this.excludedClassNames.addAll(stompFilterModel.excludedClassNames);
        this.followEditor = stompFilterModel.followEditor;
        this.candidateFilterType = stompFilterModel.candidateFilterType;
    }

    public StompFilterModel() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StompFilterModel that = (StompFilterModel) o;
        return followEditor == that.followEditor && includedClassNames.equals(
                that.includedClassNames) && excludedClassNames.equals(
                that.excludedClassNames) && includedMethodNames.equals(
                that.includedMethodNames) && excludedMethodNames.equals(
                that.excludedMethodNames) && candidateFilterType == that.candidateFilterType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(includedClassNames, excludedClassNames, includedMethodNames, excludedMethodNames,
                followEditor,
                candidateFilterType);
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

    public void setFrom(StompFilterModel stompFilterModel) {
        this.includedMethodNames.clear();
        this.includedClassNames.clear();

        this.excludedMethodNames.clear();
        this.excludedClassNames.clear();


        this.includedMethodNames.addAll(stompFilterModel.includedMethodNames);
        this.includedClassNames.addAll(stompFilterModel.includedClassNames);
        this.excludedMethodNames.addAll(stompFilterModel.excludedMethodNames);
        this.excludedClassNames.addAll(stompFilterModel.excludedClassNames);
        this.followEditor = stompFilterModel.followEditor;
        this.candidateFilterType = stompFilterModel.candidateFilterType;

    }
}
