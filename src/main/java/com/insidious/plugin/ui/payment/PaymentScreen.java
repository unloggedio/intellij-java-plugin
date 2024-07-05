package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousService;

import javax.swing.*;

public class PaymentScreen {
    private JPanel mainPanel;
    private JPanel textPanel;
    private JPanel buttonPanel;
    private JButton payButton;
    private JButton activateButton;
    private JLabel featureList;
    private final InsidiousService insidiousService;


    public PaymentScreen(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
    }

    /**
     * Returns the main panel of the UI.
     * @return the main JPanel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }
}
