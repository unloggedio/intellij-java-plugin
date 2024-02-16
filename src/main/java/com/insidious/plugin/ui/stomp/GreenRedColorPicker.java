package com.insidious.plugin.ui.stomp;

import java.awt.*;

public class GreenRedColorPicker {

    public static String pickColor(double number) {
        if (number < 0) {
            number = 0;
        } else if (number > 10000) {
            number = 10000;
        }

        // Normalize the number to a range between 0.0 and 1.0
        double normalized = number / 10000;

        // Interpolate hue between green (120°) and red (0°)
        // Adjust these values if you need a different shade
        double hue = (1.0f - normalized) * 120.0f / 360.0f; // 120° for green, 0° for red

        // Convert HSB to RGB
        Color color = Color.getHSBColor(Double.valueOf(hue).floatValue(), 1.0f, 0.5f); // Full saturation and brightness
        // for vivid
        // colors

        // Convert RGB to Hexadecimal
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static void main(String[] args) {
        // Example usage
        String colorHex = pickColor(5000); // Midpoint should give a color between green and red
        System.out.println("Color in Hex: " + colorHex);
    }
}