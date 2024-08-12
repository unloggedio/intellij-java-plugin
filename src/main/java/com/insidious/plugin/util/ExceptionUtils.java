package com.insidious.plugin.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ExceptionUtils {

    private static final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();

    public static String prettyPrintException(String exceptionJson) {
        try {
            JsonNode exceptionJsonNode = objectMapper.readValue(exceptionJson, JsonNode.class);
            StringBuilder stringBuilder = new StringBuilder();
            prettyPrintException(exceptionJsonNode, stringBuilder);
            return stringBuilder.toString();
        } catch (JsonProcessingException e) {
            return exceptionJson;
        }
    }

    public static String prettyPrintException(byte[] exceptionJson) {
        try {
            JsonNode exceptionJsonNode = objectMapper.readValue(exceptionJson, JsonNode.class);
            StringBuilder stringBuilder = new StringBuilder();
            prettyPrintException(exceptionJsonNode, stringBuilder);
            return stringBuilder.toString();
        } catch (IOException e) {
            return new String(exceptionJson);
        }
    }

    private static void prettyPrintException(JsonNode exceptionJsonNode, StringBuilder stringBuilder) {
        JsonNode cause = exceptionJsonNode.get("cause");
        JsonNode stackTrace = exceptionJsonNode.get("stackTrace");
        JsonNode message = exceptionJsonNode.get("message");
//        JsonNode targetException = exceptionJsonNode.get("targetException");

        if (message != null) {
            stringBuilder.append("Caused by: ").append(message.asText()).append("\n");
        }
        if (stackTrace != null) {
            prettyPrintStackTrace(stackTrace, stringBuilder);
        }
        if (cause != null) {
            prettyPrintException(cause, stringBuilder);
        }
    }

    private static void prettyPrintStackTrace(JsonNode stackTrace, StringBuilder stringBuilder) {
        int stackSize = stackTrace.size();
        for (int i = 0; i < stackSize; i++) {
            JsonNode stackItem = stackTrace.get(i);
            if (!stackItem.has("className")) {
                stringBuilder.append(stackItem.asText()).append("\n");
                continue;
            }
            stringBuilder.append("    at ")
                    .append(stackItem.get("className").asText()).append(".")
                    .append(stackItem.get("methodName").asText())
                    .append("(")
                    .append(stackItem.get("nativeMethod").asBoolean() ?
                            "Native Method" :
                            (stackItem.get("fileName").asText() + ":" + stackItem.get("lineNumber").asText()))
                    .append(")\n");
        }
    }

    public static String trimStackTraceElements(StackTraceElement[] stackTraceElements) {
        return Arrays.stream(stackTraceElements)
                .filter(stackTraceElement -> stackTraceElement.getClassName().contains("com.insidious"))
                .map(element -> element.getFileName() + " " + element.getLineNumber())
                .collect(Collectors.joining("\n")) + "\nENDS";
    }
}
