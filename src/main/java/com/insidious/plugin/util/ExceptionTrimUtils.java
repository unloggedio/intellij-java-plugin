package com.insidious.plugin.util;

public class ExceptionTrimUtils {
    public static String trimStackTraceElements(StackTraceElement[] stackTraceElements)
    {
        StringBuilder stackTraceBuilder = new StringBuilder();
        for (int i=0;i<=stackTraceElements.length-1;i++) {
            stackTraceBuilder.append("__")
                    .append(stackTraceElements[i].getFileName())
                    .append("_")
                    .append(stackTraceElements[i].getLineNumber());
        }
        stackTraceBuilder.append("ENDS");
        return stackTraceBuilder.toString();
    }
}
