package com.insidious.plugin.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.assertions.*;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceInstance;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.intellij.openapi.diagnostic.Logger;

import java.util.*;

import static com.insidious.plugin.util.ParameterUtils.processResponseForFloatAndDoubleTypes;

public class DiffUtils {
    static final private Logger logger = LoggerUtil.getInstance(DiffUtils.class);
    static final private ObjectMapper objectMapper = ObjectMapperInstance.getInstance();

    static public DifferenceResult calculateDifferences(
            StoredCandidate testCandidateMetadata,
            AgentCommandResponse<String> agentCommandResponse
    ) {
        AtomicAssertion testAssertions = testCandidateMetadata.getTestAssertions();
        String returnValueAsString = processResponseForFloatAndDoubleTypes(
                agentCommandResponse.getResponseClassName(),
                String.valueOf(agentCommandResponse.getMethodReturnValue()));
        String responseClassname = agentCommandResponse.getResponseClassName();
        if (responseClassname != null
                && responseClassname.equals("java.lang.String")
                && !returnValueAsString.startsWith("\"") && !returnValueAsString.endsWith("\"")
                && !returnValueAsString.equals("null")
        ) {
            returnValueAsString = "\"" + returnValueAsString + "\"";
        }
        if (responseClassname == null) {
            responseClassname = "";
        }

        if (testAssertions != null && AtomicAssertionUtils.countAssertions(testAssertions) > 0) {
            JsonNode leftOnlyMap = objectMapper.createObjectNode();
            JsonNode rightOnlyMap = objectMapper.createObjectNode();
            List<DifferenceInstance> differencesList = new ArrayList<>();
            DiffResultType diffResultType;
            try {
                JsonNode responseNode = objectMapper.readTree(returnValueAsString);
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
                        Boolean subResult = results.get(atomicAssertion.getId());
                        if (!subResult) {
                            differencesList.add(
                                    new DifferenceInstance(
                                            atomicAssertion.getAssertionType().toString()
                                                    + " " + atomicAssertion.getSubAssertions().size()
                                                    + " assertions",
                                            "true",
                                            "false",
                                            DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE));
                        }

                        continue;
                    }
                    Boolean subResult = results.get(atomicAssertion.getId());
                    if (!subResult) {
                        String key = atomicAssertion.getExpression() == Expression.SELF ?
                                atomicAssertion.getKey() : atomicAssertion.getExpression()
                                + "(" + atomicAssertion.getKey() + ")";
                        key = "[" + key + "] " + atomicAssertion.getAssertionType().toString();
                        differencesList.add(
                                new DifferenceInstance(
                                        key,
                                        atomicAssertion.getExpectedValue(),
                                        JsonTreeUtils.getValueFromJsonNode(responseNode, atomicAssertion.getKey()),
                                        DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE));
                    }
                }


                diffResultType = result.isPassing() ? DiffResultType.SAME : DiffResultType.DIFF;

            } catch (Exception e) {

                differencesList.add(
                        new DifferenceInstance(
                                "Invalid assertion",
                                e.getMessage(),
                                "",
                                DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY
                        )
                );
                diffResultType = DiffResultType.DIFF;

            }
            return new DifferenceResult(differencesList, diffResultType, leftOnlyMap, rightOnlyMap);
        }
        String expectedStringFromCandidate = testCandidateMetadata.getReturnValue();
        if ("java.lang.String".equals(testCandidateMetadata.getReturnValueClassname())
                && !expectedStringFromCandidate.startsWith("\"")
                && !expectedStringFromCandidate.endsWith("\"")
                && !"null".equals(expectedStringFromCandidate)
        ) {
            expectedStringFromCandidate = "\"" + expectedStringFromCandidate + "\"";
        }

        if (testCandidateMetadata.isReturnValueIsBoolean()) {
            if (isNumeric(expectedStringFromCandidate)) {
                expectedStringFromCandidate = "0".equals(expectedStringFromCandidate) ? "false" : "true";
            }
        }

        // void return value
        if ("void".equals(responseClassname) && "0".equals(expectedStringFromCandidate)) {
            expectedStringFromCandidate = "null";
        }


        if ("float".equals(responseClassname)
                || responseClassname.equalsIgnoreCase("java.lang.float")) {
            expectedStringFromCandidate = ParameterUtils.getFloatValue(returnValueAsString);
            returnValueAsString = ParameterUtils.getFloatValue(returnValueAsString);
        }

        if ("double".equals(responseClassname)
                || responseClassname.equalsIgnoreCase("java.lang.double")) {
            expectedStringFromCandidate = ParameterUtils.getDoubleValue(returnValueAsString);
            returnValueAsString = ParameterUtils.getDoubleValue(returnValueAsString);
        }

        if (testCandidateMetadata.isException() ||
                (agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION))) {
            //exception flow wip
            if (testCandidateMetadata.isException()) {
                //load before as exception
                DifferenceResult res = calculateDifferences(expectedStringFromCandidate, returnValueAsString,
                        agentCommandResponse.getResponseType());
                if (res.getDiffResultType().equals(DiffResultType.ACTUAL_EXCEPTION)) {
                    res.setDiffResultType(DiffResultType.BOTH_EXCEPTION);
                } else {
                    res.setDiffResultType(DiffResultType.ORIGINAL_EXCEPTION);
                }
                return res;
            } else {
                //load before as normal
                return calculateDifferences(expectedStringFromCandidate, returnValueAsString,
                        agentCommandResponse.getResponseType());
            }
        }
        boolean isDifferent;
        if (agentCommandResponse.getResponseType() == null || agentCommandResponse.getResponseType() == ResponseType.FAILED) {
            return new DifferenceResult(new LinkedList<>(), DiffResultType.DIFF, null, null);
        }

        if (agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION)) {
            try {
                String expectedClassName = testCandidateMetadata.getReturnValueClassname();

                isDifferent = responseClassname.equals(expectedClassName);
                if (!isDifferent) {
                    return new DifferenceResult(new LinkedList<>(), DiffResultType.SAME, null, null);
                }

            } catch (Exception e) {
                logger.warn(
                        "failed to match expected and returned type: " +
                                agentCommandResponse + "\n" + testCandidateMetadata, e);
            }
        }
        return calculateDifferences(expectedStringFromCandidate, returnValueAsString,
                agentCommandResponse.getResponseType());
    }

    static private DifferenceResult calculateDifferences(String originalString, String actualString, ResponseType responseType) {
        //replace Boolean with enum
        if (responseType != null &&
                (responseType.equals(ResponseType.EXCEPTION) || responseType.equals(ResponseType.FAILED))) {
            return new DifferenceResult(null, DiffResultType.ACTUAL_EXCEPTION, getFlatMapFor(originalString), null);
        }
        try {
            JsonNode m1;
            if (originalString == null || originalString.isEmpty()) {
                m1 = objectMapper.createObjectNode();
            } else {
                m1 = objectMapper.readTree(originalString);
            }
            JsonNode m2 = objectMapper.readTree(actualString);
            if (m2 == null) {
                m2 = objectMapper.createObjectNode();
            }

            Map<String, Object> objectMapDifference = compareObjectNodes(m1, m2);
//            System.out.println(res);

//            res.entriesOnlyOnLeft().forEach((key, value) -> System.out.println(key + ": " + value));
            JsonNode leftOnly = (JsonNode) objectMapDifference.get("left");

//            res.entriesOnlyOnRight().forEach((key, value) -> System.out.println(key + ": " + value));
            JsonNode rightOnly = (JsonNode) objectMapDifference.get("right");

//            res.entriesDiffering().forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, ValueDifference> differences = (Map<String, ValueDifference>) objectMapDifference.get(
                    "differences");
            List<DifferenceInstance> differenceInstances = getDifferenceModel(leftOnly, rightOnly, differences);
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

    public static Map<String, Object> compareObjectNodes(JsonNode node1, JsonNode node2) {
        Map<String, Object> differencesMap = new HashMap<>();
        compareObjectNodes(node1, node2, "", differencesMap);
        return differencesMap;
    }

    private static void compareObjectNodes(JsonNode node1, JsonNode node2, String path,
                                           Map<String, Object> differencesMap) {
        ObjectNode leftOnly = objectMapper.createObjectNode();
        ObjectNode rightOnly = objectMapper.createObjectNode();
        ObjectNode common = objectMapper.createObjectNode();
        Map<String, ValueDifference> differences = new HashMap<>();

        Iterator<String> fieldNames = node1.fieldNames();

        int leftFieldNameCount = 0;
        while (fieldNames.hasNext()) {
            leftFieldNameCount++;
            String fieldName = fieldNames.next();
            JsonNode value1 = node1.get(fieldName);
            JsonNode value2 = node2.get(fieldName);

            if (value2 == null) {
                leftOnly.put(fieldName, value1.toString());
            } else if (value1.equals(value2)) {
                common.put(fieldName, value1.toString());
            } else {
                if (value1.isObject() && value2.isObject()) {
                    compareObjectNodes(value1, value2, path + fieldName + ".", differencesMap);
                } else {
                    if (value1.isTextual()) {
                        differences.put(fieldName, new ValueDifference(value1.textValue(), value2.textValue()));
                    } else {
                        differences.put(fieldName, new ValueDifference(value1.toString(), value2.toString()));
                    }
                }
            }
        }

        fieldNames = node2.fieldNames();
        int rightFieldNameCount = 0;
        while (fieldNames.hasNext()) {
            rightFieldNameCount++;
            String fieldName = fieldNames.next();
            JsonNode value1 = node1.get(fieldName);
            if (value1 == null) {
                rightOnly.put(fieldName, node2.get(fieldName).toString());
            }
        }
        if (leftFieldNameCount == 0 && rightFieldNameCount > 0) {
            leftOnly.put(Objects.equals(path, "") ? "/" : path, node1.toString());
        } else if (leftFieldNameCount > 0 && rightFieldNameCount == 0) {
            rightOnly.put(Objects.equals(path, "") ? "/" : path, node2.toString());
        } else if (leftFieldNameCount == 0 && rightFieldNameCount == 0) {
            if (node1.equals(node2)) {
                common.put(Objects.equals(path, "") ? "/" : path, node1.toString());
            } else {
                differences.put(Objects.equals(path, "") ? "/" : path,
                        new ValueDifference(node1.toString(), node2.toString()));
            }
        } else {
            // both objects had fields
        }

        differencesMap.put("left", leftOnly);
        differencesMap.put("right", rightOnly);
        differencesMap.put("common", common);
        differencesMap.put("differences", differences);
    }


    public static JsonNode getFlatMapFor(String s1) {
        try {
            JsonNode m1;
            if (s1 == null || s1.isEmpty() || s1.equals("null")) {
                m1 = objectMapper.getNodeFactory().objectNode();
            } else {
                JsonNode map;
                try {
                    map = objectMapper.readTree(s1);
                } catch (Exception e) {
                    map = objectMapper.getNodeFactory().textNode(s1);
                }
                m1 = map;
                m1 = flatten(m1);
            }
            return m1;
        } catch (Exception e) {
            logger.warn("Flatmap make Exception: ", e);
            JsonNode m1 = objectMapper.getNodeFactory().textNode(s1);
            return m1;
        }
    }

    static private List<DifferenceInstance> getDifferenceModel(
            JsonNode left, JsonNode right,
            Map<String, ValueDifference> differences
    ) {
        ArrayList<DifferenceInstance> differenceInstances = new ArrayList<>();
        for (String key : differences.keySet()) {
            DifferenceInstance instance = new DifferenceInstance(key, differences.get(key).leftValue(),
                    differences.get(key).rightValue(), DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE);
            differenceInstances.add(instance);
        }
        if (left instanceof ObjectNode) {
            ObjectNode leftObject = (ObjectNode) left;
            for (Iterator<String> it = leftObject.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                DifferenceInstance instance = new DifferenceInstance(key, left.get(key),
                        "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
                differenceInstances.add(instance);
            }
        } else if (left instanceof ArrayNode) {
            ArrayNode leftObject = (ArrayNode) left;
            for (Iterator<String> it = leftObject.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                DifferenceInstance instance = new DifferenceInstance(key, left.get(key),
                        "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
                differenceInstances.add(instance);
            }
        } else {
            DifferenceInstance instance = new DifferenceInstance("Left", left,
                    "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
            differenceInstances.add(instance);
        }

        if (right instanceof ObjectNode) {
            ObjectNode leftObject = (ObjectNode) right;
            for (Iterator<String> it = leftObject.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                DifferenceInstance instance = new DifferenceInstance(key, right.get(key),
                        "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
                differenceInstances.add(instance);
            }
        } else if (right instanceof ArrayNode) {
            ArrayNode leftObject = (ArrayNode) right;
            for (Iterator<String> it = leftObject.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                DifferenceInstance instance = new DifferenceInstance(key, right.get(key),
                        "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
                differenceInstances.add(instance);
            }
        } else {
            DifferenceInstance instance = new DifferenceInstance("Left", right,
                    "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
            differenceInstances.add(instance);
        }

        return differenceInstances;
    }

    public static JsonNode flatten(JsonNode node) {
        ObjectNode flattenedNode = objectMapper.createObjectNode();
        flatten("", node, flattenedNode);
        return flattenedNode;
    }

    private static void flatten(String prefix, JsonNode node, ObjectNode flattenedNode) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String newPrefix = prefix.isEmpty() ? field.getKey() : prefix + "/" + field.getKey();
                flatten(newPrefix, field.getValue(), flattenedNode);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String newPrefix = prefix + "/" + i;
                flatten(newPrefix, node.get(i), flattenedNode);
            }
        } else {
            flattenedNode.set(prefix, node);
        }
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
