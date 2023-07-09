package com.insidious.plugin.factory;

import java.util.function.Supplier;

public enum GutterState {
//    NO_AGENT("Unlogged agent jar not found. Please download and save to $HOME/.unlogged/unlogged-java-agent.jar",
//            "No agent found"),
    EXECUTE("Compile and re-execute all input and check for differences", "Re-execute all"),
    DIFF("One or more return value was different from original value", "Difference identified"),
    NO_DIFF("All the returned values match the original values", "No difference identified"),
    PROCESS_NOT_RUNNING("Unable to connect to the process, cannot execute methods", "Process not running"),
    PROCESS_RUNNING("Process is online, can direct execute methods", "Process running"),
    DATA_AVAILABLE("Have new candidates for the method", "Candidates available");

    private final String toolTipText;
    private final String accessibleText;

    GutterState(String tooltipText, String accessibleText) {
        this.toolTipText = tooltipText;
        this.accessibleText = accessibleText;
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public Supplier<String> getAccessibleTextProvider() {
        return () -> accessibleText;
    }
}
