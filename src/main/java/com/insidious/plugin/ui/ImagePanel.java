package com.insidious.plugin.ui;

import com.insidious.plugin.util.StreamUtil;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class ImagePanel extends JPanel {
    private Image image;

    public ImagePanel(String imagePath) throws IOException {
        // Load the image
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(imagePath);
        byte[] imageBytes = StreamUtil.streamToBytes(inputStream);
        ImageIcon imageIcon = new ImageIcon(imageBytes);
        image = imageIcon.getImage();
        Dimension size = new Dimension(image.getWidth(null), image.getHeight(null));
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setSize(size);
        setLayout(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the image on the panel
        g.drawImage(image, 0, 0, null);
    }

    public static void main(String[] args) {
        // Create the frame
        JFrame frame = new JFrame();
        // Add the ImagePanel to the frame with your image path
        ImagePanel comp = null;
        try {
            comp = new ImagePanel("images/mocks-introduction.jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        frame.add(comp);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}