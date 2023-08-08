package com.insidious.plugin.assertions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public enum AssertionType {
    ALLOF,
    ANYOF,
    NOTALLOF,
    NOTANYOF,
    EQUAL,
    EQUAL_IGNORE_CASE,
    NOT_EQUAL,
    FALSE,
    MATCHES_REGEX,
    NOT_MATCHES_REGEX,
    TRUE,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    NOT_NULL,
    NULL,
    EMPTY,
    NOT_EMPTY,
    CONTAINS_KEY,
    CONTAINS_ITEM,
    NOT_CONTAINS_ITEM,
    CONTAINS_STRING,
    NOT_CONTAINS_KEY,
    NOT_CONTAINS_STRING;

    private static final Logger logger = LoggerUtil.getInstance(AssertionType.class);

    public static boolean arrayNodeContains(ArrayNode arrayNode, JsonNode node) {
        Stream<JsonNode> nodeStream = StreamSupport.stream(arrayNode.spliterator(), false);
        return nodeStream.anyMatch(j -> j.equals(node));
    }

    @Override
    public String toString() {
        switch (this) {

            case ANYOF:
                return "or";
            case ALLOF:
                return "and";
            case NOTALLOF:
                return "not and";
            case NOTANYOF:
                return "not or";
            case EQUAL:
                return "equals";
            case EQUAL_IGNORE_CASE:
                return "equals case insensitive";
            case NOT_EQUAL:
                return "not equals";
            case FALSE:
                return "false";
            case TRUE:
                return "true";
            case LESS_THAN:
                return "less than";
            case LESS_THAN_OR_EQUAL:
                return "<=";
            case GREATER_THAN:
                return "greater than";
            case MATCHES_REGEX:
                return "matches regex";
            case NOT_MATCHES_REGEX:
                return "not matches regex";
            case GREATER_THAN_OR_EQUAL:
                return ">=";
            case NOT_NULL:
                return "is not null";
            case NULL:
                return "is null";
            case EMPTY:
                return "is empty array";
            case NOT_EMPTY:
                return "is not empty array";
            case CONTAINS_KEY:
                return "object has field";
            case CONTAINS_ITEM:
                return "array has item";
            case NOT_CONTAINS_ITEM:
                return "array does not have item";
            case CONTAINS_STRING:
                return "has substring";
            case NOT_CONTAINS_KEY:
                return "object does not have field";
            case NOT_CONTAINS_STRING:
                return "not has substring";
        }
        return "unknown-assertion-type";
    }

    public boolean verify(JsonNode actualValue, JsonNode expectedValue) {

        try {
            switch (this) {
                case EQUAL:
                    return Objects.equals(actualValue, expectedValue);
                case EQUAL_IGNORE_CASE:
                    return Objects.equals(actualValue.toString().toLowerCase(), expectedValue.toString().toLowerCase());
                case NOT_EQUAL:
                    return !Objects.equals(actualValue, expectedValue);
                case FALSE:
                    return Objects.equals(actualValue.asBoolean(), false);
                case TRUE:
                    return Objects.equals(actualValue.asBoolean(), true);
                case LESS_THAN:
                    if (expectedValue == null) {
                        return false;
                    }
                    return actualValue.asDouble() < expectedValue.asDouble();
                case GREATER_THAN:
                    if (expectedValue == null) {
                        return false;
                    }
                    return actualValue.asDouble() > expectedValue.asDouble();
                case LESS_THAN_OR_EQUAL:
                    if (expectedValue == null) {
                        return false;
                    }
                    return actualValue.asDouble() <= expectedValue.asDouble();
                case GREATER_THAN_OR_EQUAL:
                    if (expectedValue == null) {
                        return false;
                    }
                    return actualValue.asDouble() >= expectedValue.asDouble();
                case NULL:
                    return actualValue.isNull();
                case EMPTY:
                    return actualValue.isEmpty();
                case NOT_EMPTY:
                    return !actualValue.isEmpty();
                case NOT_NULL:
                    return !actualValue.isNull();
                case MATCHES_REGEX:
                    return Pattern.compile(expectedValue.asText()).matcher(actualValue.asText()).matches();
                case NOT_MATCHES_REGEX:
                    return !Pattern.compile(expectedValue.asText()).matcher(actualValue.asText()).matches();
                case CONTAINS_KEY:
                    return actualValue.has(expectedValue.asText());
                case NOT_CONTAINS_KEY:
                    return !actualValue.has(expectedValue.asText());
                case CONTAINS_ITEM:
                    return arrayNodeContains((ArrayNode) actualValue, expectedValue);
                case NOT_CONTAINS_ITEM:
                    return !arrayNodeContains((ArrayNode) actualValue, expectedValue);
                case CONTAINS_STRING:
                    return actualValue.asText().contains(expectedValue.asText());
                case NOT_CONTAINS_STRING:
                    return !actualValue.asText().contains(expectedValue.asText());

            }
            return false;
        } catch (Exception e) {
            logger.warn("Assertion exception: ", e);
            return false;
        }
    }
}
