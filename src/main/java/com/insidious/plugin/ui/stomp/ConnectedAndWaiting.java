package com.insidious.plugin.ui.stomp;

import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

public class ConnectedAndWaiting {
    private JPanel mainPanel;
    private JLabel isLiveLabel;

    public ConnectedAndWaiting() {
        AnimatedIcon icon = new AnimatedIcon(
                125,
                AllIcons.Process.Step_1,
                AllIcons.Process.Step_2,
                AllIcons.Process.Step_3,
                AllIcons.Process.Step_4,
                AllIcons.Process.Step_5,
                AllIcons.Process.Step_6,
                AllIcons.Process.Step_7,
                AllIcons.Process.Step_8
        );
        isLiveLabel.setIcon(icon);
//        isLiveLabel.setBorder(new RoundedBorder(new JBColor(JBColor.ORANGE, new Color(255, 255, 255, 255)), 20));
    }

    public Component getComponent() {
        return mainPanel;
    }
}
