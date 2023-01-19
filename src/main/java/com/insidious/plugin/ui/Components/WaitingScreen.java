package com.insidious.plugin.ui.Components;

import javax.swing.*;

public class WaitingScreen {
    private JPanel basePanel;
    private JLabel mainTextLabel;
    private JPanel container;
    public WaitingScreen() {}
    public JPanel getCompenent()
    {
        return basePanel;
    }
    public void setText(String text)
    {
        this.mainTextLabel.setText(text);
    }
}
