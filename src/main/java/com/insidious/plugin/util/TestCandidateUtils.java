package com.insidious.plugin.util;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.atomic.StoredCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TestCandidateUtils {
    public static List<String> buildArgumentValuesFromTestCandidate(TestCandidateMetadata testCandidateMetadata) {
        List<String> methodArgumentValues = new ArrayList<>();
        MethodCallExpression mce = testCandidateMetadata.getMainMethod();
        for (DataEventWithSessionId argumentProbe : mce.getArgumentProbes()) {
            if (argumentProbe.getSerializedValue() != null && argumentProbe.getSerializedValue().length > 0) {
                methodArgumentValues.add(new String(argumentProbe.getSerializedValue()));
            } else {
                methodArgumentValues.add(String.valueOf(argumentProbe.getValue()));
            }
        }
        return methodArgumentValues;
    }


    public static int getCandidateSimilarityHash(TestCandidateMetadata metadata) {
        List<String> inputs = buildArgumentValuesFromTestCandidate(metadata);
        String output = new String(metadata.getMainMethod().getReturnDataEvent().getSerializedValue());
        String concat = inputs + output;
        return concat.hashCode();
    }

    public static List<TestCandidateMetadata> deDuplicateList(List<TestCandidateMetadata> list) {
        Map<Integer, TestCandidateMetadata> candidateHashMap = new TreeMap<>();
        for (TestCandidateMetadata metadata : list) {
            int candidateHash = TestCandidateUtils.getCandidateSimilarityHash(metadata);
            if (!candidateHashMap.containsKey(candidateHash)) {
                candidateHashMap.put(candidateHash, metadata);
            }
        }
        return new ArrayList<>(candidateHashMap.values());
    }
}
