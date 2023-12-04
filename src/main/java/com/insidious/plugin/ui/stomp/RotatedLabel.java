package com.insidious.plugin.ui.stomp;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class RotatedLabel extends JLabel {
    private boolean needsRotate;

    public RotatedLabel(String text) {
        super(text);
    }

    @Override
    public Dimension getSize() {
        if (!needsRotate) {
            return super.getSize();
        }

        Dimension size = super.getSize();
        return new Dimension(size.height, size.width);
    }


    @Override
    public int getHeight() {
        return getSize().height;
    }

    @Override
    public int getWidth() {
        return getSize().width;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D gr = (Graphics2D) g.create();

        gr.transform(AffineTransform.getQuadrantRotateInstance(1));
        gr.translate(0, -getSize().getWidth());
        needsRotate = true;
        super.paintComponent(gr);
        needsRotate = false;
    }
}