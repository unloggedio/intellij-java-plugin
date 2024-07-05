package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.payment.util.PopUpUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

        // Add ActionListener to the activate button
        activateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PopUpUtil.switchToActivationScreen(insidiousService);
            }
        });
    }

    /**
     * Returns the main panel of the UI.
     * @return the main JPanel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }
}
