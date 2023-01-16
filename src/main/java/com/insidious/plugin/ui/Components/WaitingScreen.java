package com.insidious.plugin.ui.Components;

import javax.swing.*;

public class WaitingScreen {
    private JPanel basePanel;
    private JLabel mainTextLabel;
    private JPanel container;

    public WaitingScreen()
    {
        System.out.println("Waiting screen");
    }
    public JPanel getCompenent()
    {
        return basePanel;
    }
}
