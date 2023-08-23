package com.insidious.plugin.util;

import com.insidious.plugin.coverage.FilteredCandidateResponseList;
import com.insidious.plugin.pojo.atomic.StoredCandidate;

import java.util.*;
import java.util.stream.Collectors;

public class AtomicRecordUtils {


    public static FilteredCandidateResponseList filterStoredCandidates(List<StoredCandidate> candidates) {
//        Map<Long, StoredCandidate> selectedCandidates = new TreeMap<>();
//        Map<String, StoredCandidate> candidatesByArguments = new HashMap<>();

        List<StoredCandidate> savedCandidates = candidates.stream()
                .filter(e -> e.getCandidateId() != null)
                .collect(Collectors.toList());

        List<StoredCandidate> finalCandidateList = new ArrayList<>(savedCandidates);
        List<String> updatedCandidateIds = new ArrayList<>();

        List<StoredCandidate> unsavedCandidates = candidates.stream()
                .filter(e -> e.getCandidateId() == null)
                .collect(Collectors.toList());

        Map<String, List<StoredCandidate>> savedCandidatesByArgumentsMap = savedCandidates.stream()
                .collect(Collectors.groupingBy(e -> StringUtils.join(e.getMethodArguments(), ",")));


        Map<String, List<StoredCandidate>> unsavedCandidatesByArguments = unsavedCandidates.stream()
                .collect(Collectors.groupingBy(e -> StringUtils.join(e.getMethodArguments(), ",")));

//        Map<String, Boolean> lineNumbersUpdates = new HashMap<>();

        for (String commaSeparatedArguments : unsavedCandidatesByArguments.keySet()) {
            if (!savedCandidatesByArgumentsMap.containsKey(commaSeparatedArguments)) {
                finalCandidateList.addAll(unsavedCandidatesByArguments.get(commaSeparatedArguments));
            } else {
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
                                    .peek(e -> e.setLineNumbers(new ArrayList<>(newLineNumberList)))
                                    .map(StoredCandidate::getCandidateId)
                                    .collect(Collectors.toList())
                    );
                }
            }
        }


//        for (StoredCandidate unsavedCandidate : unsavedCandidates) {
//            String commaSeparatedArguments = StringUtils.join(unsavedCandidate.getMethodArguments(), ",");
//            if (savedCandidatesByArgumentsMap.containsKey(commaSeparatedArguments)) {
//                // update line numbers on the existing saved candidates
//                boolean clearExistingLineNumbers = false;
//                if (!lineNumbersUpdates.containsKey(commaSeparatedArguments)) {
//                    clearExistingLineNumbers = true;
//                    lineNumbersUpdates.put(commaSeparatedArguments, true);
//                }
//                List<StoredCandidate> existingSavedCandidates = savedCandidatesByArgumentsMap.get(commaSeparatedArguments);
//                for (StoredCandidate existingSavedCandidate : existingSavedCandidates) {
//                    if (clearExistingLineNumbers) {
//                        existingSavedCandidate.getLineNumbers().clear();
//                    }
//                    existingSavedCandidate.getLineNumbers().addAll(unsavedCandidate.getLineNumbers());
//                }
//            } else {
//                finalCandidateList.add(unsavedCandidate);
//            }
//        }


//        for (StoredCandidate candidate : candidates) {
//            if (!selectedCandidates.containsKey(candidate.getEntryProbeIndex())) {
//                selectedCandidates.put(candidate.getEntryProbeIndex(), candidate);
//            } else {
//                //saved candidate
//                if (candidate.getCandidateId() != null) {
//                    selectedCandidates.put(candidate.getEntryProbeIndex(), candidate);
//                }
//            }
//        }
        return new FilteredCandidateResponseList(finalCandidateList, updatedCandidateIds);
    }
}
