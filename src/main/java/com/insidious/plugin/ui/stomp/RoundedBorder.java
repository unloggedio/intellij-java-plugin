package com.insidious.plugin.ui.stomp;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.border.AbstractBorder;
import java.awt.*;

class RoundedBorder extends AbstractBorder {
    private int radius;
    private int padding;

    RoundedBorder(int radius, int padding) {
        this.radius = radius;
        this.padding = padding;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(c.getForeground());

        // Adjusting the dimensions and position for padding
        int adjustedWidth = width - 1 - 2 * padding;
        int adjustedHeight = height - 1 - 2 * padding;
        g2d.setColor(JBColor.RED);
        g2d.drawRoundRect(x + padding, y + padding, adjustedWidth, adjustedHeight, radius, radius);

        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        // Adjust insets for padding
        return JBUI.insets(padding);
    }
}