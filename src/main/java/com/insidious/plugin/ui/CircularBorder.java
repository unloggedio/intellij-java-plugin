package com.insidious.plugin.ui;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class CircularBorder extends AbstractBorder {
    private final Color color;
    private final int thickness;
    private final int radius;

    public CircularBorder(Color color, int thickness, int radius) {
        this.color = color;
        this.thickness = thickness;
        this.radius = radius;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(color);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Adjust the position and size to account for the thickness of the border
        g2d.setStroke(new BasicStroke(thickness));
        int offset = radius;
        int width1 = radius;
//        int height1 = height - thickness - 1;
        int height1 = width1;
        g2d.drawOval(x + offset, y + offset, width1 * 2, height1 * 2);

        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius, radius, radius, radius);
    }

    @Override
    public boolean isBorderOpaque() {
        return true;
    }
}