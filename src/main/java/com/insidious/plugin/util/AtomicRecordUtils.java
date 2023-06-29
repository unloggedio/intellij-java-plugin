package com.insidious.plugin.util;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.insidious.plugin.factory.InsidiousService.HOSTNAME;

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

    public static StoredCandidate createCandidateFor(StoredCandidate metadata, AgentCommandResponse<String> response)
    {
        StoredCandidate candidate = new StoredCandidate();
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setMethodHash(metadata.getMethodHash());
        candidate.setMethodArguments(metadata.getMethodArguments());
        candidate.setException(response.getResponseType().equals(ResponseType.NORMAL) ?
                false : true);
        candidate.setReturnValue(response.getMethodReturnValue());
        //to be updated
        candidate.setProbSerializedValue(metadata.getProbSerializedValue());
        //to be updated
        candidate.setMethodName(metadata.getMethodName());
        candidate.setEntryProbeIndex(metadata.getEntryProbeIndex());
        candidate.setBooleanType(metadata.isBooleanType());
        candidate.setException(response.getResponseType().equals(ResponseType.EXCEPTION) ? true : false);
        candidate.setReturnValueClassname(response.getResponseClassName());

        if(metadata.getMetadata()!=null)
        {
            candidate.setMetadata(metadata.getMetadata());
            candidate.getMetadata().setHostMachineName(HOSTNAME);
            candidate.getMetadata().setRecordedBy(HOSTNAME);
        }
        else {
            StoredCandidateMetadata metadata1 = new StoredCandidateMetadata();
            metadata1.setCandidateStatus(null);
            metadata1.setTimestamp(response.getTimestamp());
            metadata1.setRecordedBy(HOSTNAME);
            metadata1.setHostMachineName(HOSTNAME);
            candidate.setMetadata(metadata1);
        }
        return candidate;
    }
}
