package com.insidious.plugin.util;

import com.insidious.plugin.pojo.atomic.StoredCandidate;

import java.util.*;

public class AtomicRecordUtils {


    public static List<StoredCandidate> filterStoredCandidates(List<StoredCandidate> candidates) {
        Map<Long, StoredCandidate> selectedCandidates = new TreeMap<>();
        for (StoredCandidate candidate : candidates) {
            if (!selectedCandidates.containsKey(candidate.getEntryProbeIndex())) {
                selectedCandidates.put(candidate.getEntryProbeIndex(), candidate);
            } else {
                //saved candidate
                if (candidate.getCandidateId() != null) {
                    selectedCandidates.put(candidate.getEntryProbeIndex(), candidate);
                }
            }
        }
        return new ArrayList<>(selectedCandidates.values());
    }
}
