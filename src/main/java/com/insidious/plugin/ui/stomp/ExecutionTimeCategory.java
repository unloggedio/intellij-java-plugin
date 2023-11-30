package com.insidious.plugin.ui.stomp;

public enum ExecutionTimeCategory {
    INSTANTANEOUS("#006400"), // Dark Green
    FAST("#228B22"),         // Lighter Green
    MODERATE("#9ACD32"),     // Yellow-Green
    SLOW("#D2691E"),         // Orange-Red
    GLACIAL("#8B0000");      // Dark Red

    private final String colorHex;

    ExecutionTimeCategory(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getColorHex() {
        return colorHex;
    }
}