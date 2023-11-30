package com.insidious.plugin.ui.stomp;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

public class DisconnectedAnd {
    private JLabel isLiveLabel;
    private JPanel mainPanel;

    public DisconnectedAnd() {
//        isLiveLabel.setBorder(new RoundedBorder(new JBColor(JBColor.ORANGE, new Color(255, 255, 255, 255)), 20));
    }


    public Component getComponent() {
        return mainPanel;
    }
}
