package com.insidious.plugin.ui.stomp;

import javax.swing.*;
import java.awt.*;

public class RoundedBorderLabel extends JLabel {

    private int radius; // Radius of the rounded border

    public RoundedBorderLabel(String text, int radius) {
        super(text);
        this.radius = radius;
        setOpaque(false); // Make the label non-opaque
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the background with rounded corners
        g2d.setColor(getBackground());
        g2d.fillRoundRect(0 + 1, 0 + 1, getWidth() - 2, getHeight() - 2, radius, radius);

        // Draw the text
        super.paintComponent(g2d);

        g2d.dispose();
    }

//    @Override
//    protected void paintBorder(Graphics g) {
//        Graphics2D g2d = (Graphics2D) g.create();
//        g2d.setColor(getBackground());
//        g2d.drawRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
//        g2d.dispose();
//    }
}