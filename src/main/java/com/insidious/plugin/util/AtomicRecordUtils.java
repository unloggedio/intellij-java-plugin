package com.insidious.plugin.util;

import com.insidious.plugin.coverage.FilteredCandidateResponseList;
import com.insidious.plugin.pojo.atomic.StoredCandidate;

import java.util.*;
import java.util.stream.Collectors;

public class AtomicRecordUtils {


    public static FilteredCandidateResponseList filterStoredCandidates(List<StoredCandidate> candidates) {
        List<StoredCandidate> savedCandidates = candidates.stream()
                .filter(e -> e.getCandidateId() != null)
                .collect(Collectors.toList());

        List<StoredCandidate> finalCandidateList = new ArrayList<>(savedCandidates);
        List<String> updatedCandidateIds = new ArrayList<>();

        List<StoredCandidate> unsavedCandidates = candidates.stream()
                .filter(e -> e.getCandidateId() == null)
                .collect(Collectors.toList());

        Map<String, List<StoredCandidate>> savedCandidatesByArgumentsMap = savedCandidates.stream()
                .collect(Collectors.groupingBy(StoredCandidate::calculateCandidateHash));
        filterCandidatesForFloatAndDoubleTypes(savedCandidatesByArgumentsMap);

        Map<String, List<StoredCandidate>> unsavedCandidatesByArguments = unsavedCandidates.stream()
                .collect(Collectors.groupingBy(StoredCandidate::calculateCandidateHash));
        filterCandidatesForFloatAndDoubleTypes(unsavedCandidatesByArguments);

        for (String commaSeparatedArguments : unsavedCandidatesByArguments.keySet()) {
            if (!savedCandidatesByArgumentsMap.containsKey(commaSeparatedArguments)) {
                //use only the saved ones
                finalCandidateList.add(unsavedCandidatesByArguments.get(commaSeparatedArguments).get(0));
            } else {
                StoredCandidate oneUnsavedCandidate = unsavedCandidatesByArguments.get(commaSeparatedArguments).get(0);
                Set<Integer> newLineNumberSet = unsavedCandidatesByArguments
                        .get(commaSeparatedArguments)
                        .stream().map(StoredCandidate::getLineNumbers).flatMap(Collection::stream)
                        .collect(Collectors.toSet());

                List<StoredCandidate> savedCandidatesByArgumentList = savedCandidatesByArgumentsMap
                        .get(commaSeparatedArguments);
                Set<Integer> existingLineNumberSet = savedCandidatesByArgumentList
                        .stream().map(StoredCandidate::getLineNumbers).flatMap(Collection::stream)
                        .collect(Collectors.toSet());

                final ArrayList<Integer> newLineNumberList = new ArrayList<>(newLineNumberSet);
                if (!existingLineNumberSet.equals(newLineNumberSet)) {
                    updatedCandidateIds.addAll(
                            savedCandidatesByArgumentList.stream()
                                    .peek(e -> {
                                        e.setLineNumbers(new ArrayList<>(newLineNumberList));
                                        e.setEntryProbeIndex(oneUnsavedCandidate.getEntryProbeIndex());
                                    })
                                    .map(StoredCandidate::getCandidateId)
                                    .collect(Collectors.toList())
                    );
                } else {
                    savedCandidatesByArgumentList
                            .forEach(e -> {
                                e.setEntryProbeIndex(oneUnsavedCandidate.getEntryProbeIndex());
                            });
                }
            }
        }
        return new FilteredCandidateResponseList(finalCandidateList, updatedCandidateIds);
    }

    private static void filterCandidatesForFloatAndDoubleTypes(Map<String, List<StoredCandidate>> aggregate) {
        aggregate.forEach((e, v) -> {
            v.forEach(sc -> {
                if (sc.getReturnValueClassname() == null) {
                    sc.setReturnValueClassname("null");
                }
                sc.setReturnValue(ParameterUtils.processResponseForFloatAndDoubleTypes(
                        sc.getReturnValueClassname(), sc.getReturnValue()));
            });
        });
    }
}
