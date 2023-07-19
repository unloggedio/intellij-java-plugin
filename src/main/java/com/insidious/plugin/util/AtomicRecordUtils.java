package com.insidious.plugin.util;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;

import java.util.*;
import java.util.stream.Collectors;

import static com.insidious.plugin.factory.InsidiousService.HOSTNAME;

public class AtomicRecordUtils {

    public static StoredCandidate createCandidateFor(StoredCandidate metadata, AgentCommandResponse<String> response) {
        StoredCandidate candidate = new StoredCandidate();
        candidate.setCandidateId(metadata.getCandidateId());
        candidate.setMethodArguments(metadata.getMethodArguments());
        candidate.setException(!response.getResponseType().equals(ResponseType.NORMAL));
        candidate.setReturnValue(response.getMethodReturnValue());
        //to be updated
        candidate.setProbSerializedValue(metadata.getProbSerializedValue());
        //to be updated
        candidate.setMethod(metadata.getMethod());
        candidate.setEntryProbeIndex(metadata.getEntryProbeIndex());
        candidate.setReturnValueClassname(response.getResponseClassName());

        if (metadata.getMetadata() != null) {
            candidate.setMetadata(metadata.getMetadata());
            candidate.getMetadata().setHostMachineName(HOSTNAME);
            candidate.getMetadata().setRecordedBy(HOSTNAME);
        } else {
            StoredCandidateMetadata metadata1 = new StoredCandidateMetadata();
            metadata1.setCandidateStatus(null);
            metadata1.setTimestamp(response.getTimestamp());
            metadata1.setRecordedBy(HOSTNAME);
            metadata1.setHostMachineName(HOSTNAME);
            candidate.setMetadata(metadata1);
        }
        return candidate;
    }

    // todo: explain
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
