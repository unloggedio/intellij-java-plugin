package com.insidious.plugin.util;

import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.StoredCandidate;

import java.util.ArrayList;
import java.util.List;

public class AtomicRecordUtils {

    public static List<StoredCandidate> convertToStoredcandidates(List<com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata> testCandidateMetadataList)
    {
        List<StoredCandidate> candidates = new ArrayList<>();
        for(TestCandidateMetadata candidateMetadata:testCandidateMetadataList)
        {
            StoredCandidate candidate = new StoredCandidate(candidateMetadata);
            candidates.add(candidate);
        }
        return candidates;
    }
}
