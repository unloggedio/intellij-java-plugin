package com.insidious.plugin.ui.stomp;

import java.awt.*;

public class StringColorPicker {
    public static String pickColor(String str) {
        int hash = hashCode(str);
        float hue = (hash % 360) / 360.0f; // Normalize hue to be between 0.0 and 1.0
        float saturation = 1.0f; // 100% saturation for vibrant colors
        float lightness = 0.5f; // 50% lightness for good contrast

        // Convert HSL to RGB
        Color color = Color.getHSBColor(hue, saturation, lightness);

        // Convert RGB to Hexadecimal
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        return hex;
    }

    private static int hashCode(String str) {
        int hash = 0;
        for (int i = 0; i < str.length(); i++) {
            hash = str.charAt(i) + ((hash << 5) - hash);
        }
        return hash;
    }

    public static void main(String[] args) {
        // Example usage
        String colorHex = pickColor("YourStringHere");
        System.out.println("Color in Hex: " + colorHex);
    }
}