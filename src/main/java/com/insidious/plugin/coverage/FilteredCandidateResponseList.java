package com.insidious.plugin.coverage;

import com.insidious.plugin.pojo.atomic.StoredCandidate;

import java.util.List;

public class FilteredCandidateResponseList {
    List<StoredCandidate> candidateList;
    List<String> updatedCandidateIds;

    public FilteredCandidateResponseList(List<StoredCandidate> candidateList, List<String> updatedCandidateIds) {
        this.candidateList = candidateList;
        this.updatedCandidateIds = updatedCandidateIds;
    }

    public List<StoredCandidate> getCandidateList() {
        return candidateList;
    }

    public List<String> getUpdatedCandidateIds() {
        return updatedCandidateIds;
    }
}
