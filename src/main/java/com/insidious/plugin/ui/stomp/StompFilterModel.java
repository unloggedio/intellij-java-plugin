package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.methodscope.CandidateFilterType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.kotlin.fir.resolve.calls.Candidate;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class StompFilterModel {
    private final Set<String> includedClassNames = new CopyOnWriteArraySet<>();
    private final Set<String> excludedClassNames = new CopyOnWriteArraySet<>();
    private final Set<String> includedMethodNames = new CopyOnWriteArraySet<>();
    private final Set<String> excludedMethodNames = new CopyOnWriteArraySet<>();
    boolean followEditor;
    CandidateFilterType candidateFilterType = CandidateFilterType.ALL;

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

    public boolean isEmpty() {
        return includedClassNames.isEmpty()
                && includedMethodNames.isEmpty()
                && excludedClassNames.isEmpty()
                && excludedMethodNames.isEmpty();

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

	public CandidateFilterType getCandidateFilterType() {
        return this.candidateFilterType;
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



    public void clearIncluded() {
        includedClassNames.clear();
        includedMethodNames.clear();
    }

    public void clearExcluded() {
        excludedClassNames.clear();
        excludedMethodNames.clear();
    }
}
