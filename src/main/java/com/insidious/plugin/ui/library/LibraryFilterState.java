package com.insidious.plugin.ui.library;

import com.insidious.plugin.ui.methodscope.CandidateFilterType;

import java.util.HashSet;
import java.util.Set;

public class LibraryFilterState {
    private final Set<String> includedClassNames = new HashSet<>();
    private final Set<String> excludedClassNames = new HashSet<>();
    private final Set<String> includedMethodNames = new HashSet<>();
    private final Set<String> excludedMethodNames = new HashSet<>();
    public boolean followEditor;
    public CandidateFilterType candidateFilterType;
    public ItemFilterType itemFilterType = ItemFilterType.SavedReplay;

    public ItemFilterType getItemFilterType() {
        return itemFilterType;
    }

    public void setItemFilterType(ItemFilterType itemFilterType) {
        this.itemFilterType = itemFilterType;
    }

    public LibraryFilterState(LibraryFilterState libraryFilterState) {
        this.includedMethodNames.addAll(libraryFilterState.includedMethodNames);
        this.includedClassNames.addAll(libraryFilterState.includedClassNames);
        this.excludedMethodNames.addAll(libraryFilterState.excludedMethodNames);
        this.excludedClassNames.addAll(libraryFilterState.excludedClassNames);
        this.followEditor = libraryFilterState.followEditor;
        this.candidateFilterType = libraryFilterState.candidateFilterType;
        this.itemFilterType = libraryFilterState.itemFilterType;
    }

    public LibraryFilterState() {
    }

    public CandidateFilterType getCandidateFilterType() {
        return candidateFilterType;
    }

    public void setCandidateFilterType(CandidateFilterType candidateFilterType) {
        this.candidateFilterType = candidateFilterType;
    }

    public ItemFilterType selectedItemType() {
        return itemFilterType;
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

	public void setFrom(LibraryFilterState libraryFilterState) {
        this.includedMethodNames.clear();
        this.includedClassNames.clear();

        this.excludedMethodNames.clear();
        this.excludedClassNames.clear();

		
        this.includedMethodNames.addAll(libraryFilterState.includedMethodNames);
        this.includedClassNames.addAll(libraryFilterState.includedClassNames);
        this.excludedMethodNames.addAll(libraryFilterState.excludedMethodNames);
        this.excludedClassNames.addAll(libraryFilterState.excludedClassNames);
        this.followEditor = libraryFilterState.followEditor;
        this.candidateFilterType = libraryFilterState.candidateFilterType;
    }
}
