package com.insidious.plugin.ui;

import com.insidious.plugin.factory.InsidiousService;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class IntroductionPanel {
    private JButton skipButton;
    private JPanel directInvokePanel;
    private JPanel mainPanel;
    private JPanel replayPanel;
    private JPanel assertionPanel;
    private JPanel mockingPanel;

    public IntroductionPanel(InsidiousService insidiousService) {
        skipButton.addActionListener(e -> {
            insidiousService.addAllTabs();
            insidiousService.focusAtomicTestsWindow();
        });
    }

    public JComponent getContent() {
        return mainPanel;
    }
}
