package com.insidious.plugin.ui.stomp;

import com.intellij.ui.JBColor;

import java.awt.*;

public enum ExecutionTimeCategory {
    INSTANTANEOUS("#1F8A3C", "#1F8a3C"), // Dark Green
    FAST("#228B22", "#1F8a3C"),         // Lighter Green
    MODERATE("#9ACD32", "#1F8a3C"),     // Yellow-Green
    SLOW("#D2691E", "#1F8a3C"),         // Orange-Red
    GLACIAL("#8B0000", "#1F8a3C");      // Dark Red

    private final String colorHex;
    private final String darkColorHex;
    private final Color darkColor;
    private final Color lightColor;
    private final JBColor jbColor;

    public Color getDarkColor() {
        return darkColor;
    }

    public Color getLightColor() {
        return lightColor;
    }

    public JBColor getJbColor() {
        return jbColor;
    }

    ExecutionTimeCategory(String lightColorHex, String darkColorHex) {
        this.colorHex = lightColorHex;
        this.darkColorHex = darkColorHex;
        lightColor = Color.decode(colorHex);
        darkColor = Color.decode(colorHex);
        jbColor = new JBColor(lightColorHex, darkColor);
    }

    public String getDarkColorHex() {
        return darkColorHex;
    }

    public String getColorHex() {
        return colorHex;
    }
}