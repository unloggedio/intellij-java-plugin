package com.insidious.plugin.util;

import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;

import java.util.ArrayList;
import java.util.List;

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
}
