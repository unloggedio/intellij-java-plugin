package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousService;

import javax.swing.*;

public class PaymentActivationScreen {
    private JPanel mainPanel;
    private JPanel textPanel;
    private JPanel buttonPanel;
    private JButton payButton;
    private JButton activateButton;
    private JTextArea keyArea;
    private JLabel description;
    private JLabel keyLabel;
    private JLabel status;

    private final InsidiousService insidiousService;

    public PaymentActivationScreen(InsidiousService insidiousService) {
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
