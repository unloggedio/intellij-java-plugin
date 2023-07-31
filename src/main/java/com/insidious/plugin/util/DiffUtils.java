package com.insidious.plugin.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.assertions.*;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceInstance;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.intellij.openapi.diagnostic.Logger;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DiffUtils {
    static final private Logger logger = LoggerUtil.getInstance(DiffUtils.class);
    static final private ObjectMapper objectMapper = new ObjectMapper();

    static public DifferenceResult calculateDifferences(
            StoredCandidate testCandidateMetadata,
            AgentCommandResponse<String> agentCommandResponse
    ) {

        AtomicAssertion testAssertions = testCandidateMetadata.getTestAssertions();
        if (testAssertions != null && AtomicAssertionUtils.countAssertions(testAssertions) > 1) {
            Map<String, Object> leftOnlyMap = new HashMap<>();
            Map<String, Object> rightOnlyMap = new HashMap<>();
            List<DifferenceInstance> differencesList = new ArrayList<>();
            try {
                JsonNode responseNode = objectMapper.readTree(agentCommandResponse.getMethodReturnValue());
                AssertionResult result = AssertionEngine.executeAssertions(
                        testAssertions, responseNode);

                List<AtomicAssertion> flatAssertionList = AtomicAssertionUtils.flattenAssertionMap(testAssertions);

                Map<String, Boolean> results = result.getResults();


                for (AtomicAssertion atomicAssertion : flatAssertionList) {
                    if (
                            atomicAssertion.getAssertionType() == AssertionType.ALLOF ||
                                    atomicAssertion.getAssertionType() == AssertionType.NOTALLOF ||
                                    atomicAssertion.getAssertionType() == AssertionType.NOTANYOF ||
                                    atomicAssertion.getAssertionType() == AssertionType.ANYOF
                    ) {
                        continue;
                    }
                    Boolean subResult = results.get(atomicAssertion.getId());
                    if (!subResult) {
                        differencesList.add(
                                new DifferenceInstance(
                                        atomicAssertion.getExpression() == Expression.SELF ?
                                                atomicAssertion.getKey() : atomicAssertion.getExpression()
                                                + "(" + atomicAssertion.getKey() + ")",
                                        atomicAssertion.getExpectedValue(),
                                        responseNode.at(atomicAssertion.getKey()),
                                        DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE));
                    }
                }


                return new DifferenceResult(
                        differencesList, result.isPassing() ? DiffResultType.SAME : DiffResultType.DIFF,
                        leftOnlyMap, rightOnlyMap
                );

            } catch (Exception e) {

                differencesList.add(
                        new DifferenceInstance(
                                "Invalid assertion",
                                e.getMessage(),
                                "",
                                DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY
                        )
                );
                return new DifferenceResult(
                        differencesList, DiffResultType.DIFF, leftOnlyMap, rightOnlyMap);

            }
        }

        String originalString = testCandidateMetadata.getReturnValue();

        if (testCandidateMetadata.isReturnValueIsBoolean()) {
            if (isNumeric(originalString)) {
                originalString = "0".equals(originalString) ? "false" : "true";
            }
        }

        // void return value
        if (testCandidateMetadata.getReturnValueClassname() == null) {
            originalString = "null";
        }

        String actualString = String.valueOf(agentCommandResponse.getMethodReturnValue());

        if (testCandidateMetadata.isException() ||
                (agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION))) {
            //exception flow wip
            if (testCandidateMetadata.isException()) {
                //load before as exception
                DifferenceResult res = calculateDifferences(originalString, actualString,
                        agentCommandResponse.getResponseType());
                if (res.getDiffResultType().equals(DiffResultType.ACTUAL_EXCEPTION)) {
                    res.setDiffResultType(DiffResultType.BOTH_EXCEPTION);
                } else {
                    res.setDiffResultType(DiffResultType.ORIGINAL_EXCEPTION);
                }
                return res;
            } else {
                //load before as normal
                return calculateDifferences(originalString, actualString, agentCommandResponse.getResponseType());
            }
        }
        boolean isDifferent = true;
        if (agentCommandResponse.getResponseType() == null || agentCommandResponse.getResponseType() == ResponseType.FAILED) {
            return new DifferenceResult(new LinkedList<>(), DiffResultType.DIFF, null, null);
        }

        if (agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION)) {
            try {
                String responseClassName = agentCommandResponse.getResponseClassName();
                String expectedClassName = testCandidateMetadata.getReturnValueClassname();

                isDifferent = responseClassName.equals(expectedClassName);
                if (!isDifferent) {
                    return new DifferenceResult(new LinkedList<>(), DiffResultType.SAME, null, null);
//                    return differenceResult;
                }

            } catch (Exception e) {
                logger.warn(
                        "failed to match expected and returned type: " +
                                agentCommandResponse + "\n" + testCandidateMetadata, e);
            }
        }
        return calculateDifferences(originalString, actualString, agentCommandResponse.getResponseType());
    }

    static private DifferenceResult calculateDifferences(String originalString, String actualString, ResponseType responseType) {
        //replace Boolean with enum
        if (responseType != null &&
                (responseType.equals(ResponseType.EXCEPTION) || responseType.equals(ResponseType.FAILED))) {
            return new DifferenceResult(null, DiffResultType.ACTUAL_EXCEPTION, getFlatMapFor(originalString), null);
        }
        try {
            Map<String, Object> m1;
            if (originalString == null || originalString.isEmpty()) {
                m1 = new TreeMap<>();
            } else {
                m1 = (Map<String, Object>) (objectMapper.readValue(originalString, Map.class));
            }
            Map<String, Object> m2 = (Map<String, Object>) (objectMapper.readValue(actualString, Map.class));
            if (m2 == null) {
                m2 = new HashMap<>();
            }

            MapDifference<String, Object> res = Maps.difference(flatten(m1), flatten(m2));
//            System.out.println(res);

//            res.entriesOnlyOnLeft().forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, Object> leftOnly = res.entriesOnlyOnLeft();

//            res.entriesOnlyOnRight().forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, Object> rightOnly = res.entriesOnlyOnRight();

//            res.entriesDiffering().forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, MapDifference.ValueDifference<Object>> differences = res.entriesDiffering();
            List<DifferenceInstance> differenceInstances = getDifferenceModel(leftOnly,
                    rightOnly, differences);
            if (differenceInstances.size() == 0) {
                //no differences
                return new DifferenceResult(differenceInstances, DiffResultType.SAME, leftOnly, rightOnly);
            } else if (originalString == null || originalString.isEmpty()) {
                return new DifferenceResult(differenceInstances, DiffResultType.NO_ORIGINAL, leftOnly, rightOnly);
            } else {
//                merge left and right differences
//                or iterate and create a new pojo that works with 1 table model
                return new DifferenceResult(differenceInstances, DiffResultType.DIFF, leftOnly, rightOnly);
            }
        } catch (Exception e) {
            logger.warn("Failed to read old or new value", e);
            if ((originalString == null && actualString == null) || Objects.equals(originalString, actualString)) {
                return new DifferenceResult(new LinkedList<>(), DiffResultType.SAME, null, null);
            }
            //this.statusLabel.setText("Differences Found.");
            //happens for malformed jsons or primitives.
            DifferenceInstance instance = new DifferenceInstance("Return Value", originalString, actualString,
                    DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE);
            ArrayList<DifferenceInstance> differenceInstances = new ArrayList<>();
            differenceInstances.add(instance);
            return new DifferenceResult(differenceInstances, DiffResultType.DIFF, null, null);
        }
    }

    public static Map<String, Object> getFlatMapFor(String s1) {
        ObjectMapper om = new ObjectMapper();
        try {
            Map<String, Object> m1;
            if (s1 == null || s1.isEmpty()) {
                m1 = new TreeMap<>();
            } else {
                m1 = (Map<String, Object>) (om.readValue(s1, Map.class));
                m1 = flatten(m1);
            }
            return m1;
        } catch (Exception e) {
            logger.warn("Flatmap make Exception: ", e);
            Map<String, Object> m1 = new TreeMap<>();
            m1.put("value", s1);
            return m1;
        }
    }

    static private List<DifferenceInstance> getDifferenceModel(
            Map<String, Object> left, Map<String, Object> right,
            Map<String, MapDifference.ValueDifference<Object>> differences
    ) {
        ArrayList<DifferenceInstance> differenceInstances = new ArrayList<>();
        for (String key : differences.keySet()) {
            DifferenceInstance instance = new DifferenceInstance(key, differences.get(key).leftValue(),
                    differences.get(key).rightValue(), DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE);
            differenceInstances.add(instance);
        }
        for (String key : left.keySet()) {
            DifferenceInstance instance = new DifferenceInstance(key, left.get(key),
                    "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
            differenceInstances.add(instance);
        }
        for (String key : right.keySet()) {
            DifferenceInstance instance = new DifferenceInstance(key, "",
                    right.get(key), DifferenceInstance.DIFFERENCE_TYPE.RIGHT_ONLY);
            differenceInstances.add(instance);
        }
        return differenceInstances;
    }

    static public Map<String, Object> flatten(Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(DiffUtils::flattenEntry)
                .collect(LinkedHashMap::new, (m, e) -> m.put("/" + e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    static private Stream<Map.Entry<String, Object>> flattenEntry(Map.Entry<String, Object> entry) {
        if (entry == null) {
            return Stream.empty();
        }

        if (entry.getValue() instanceof Map<?, ?>) {
            return ((Map<?, ?>) entry.getValue()).entrySet().stream()
                    .flatMap(e -> flattenEntry(
                            new AbstractMap.SimpleEntry<>(entry.getKey() + "/" + e.getKey(), e.getValue())));
        }

        if (entry.getValue() instanceof List<?>) {
            List<?> list = (List<?>) entry.getValue();
            return IntStream.range(0, list.size())
                    .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey() + "/" + i, list.get(i)))
                    .flatMap(DiffUtils::flattenEntry);
        }

        return Stream.of(entry);
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }


}
