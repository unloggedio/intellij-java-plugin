package com.insidious.plugin.ui.payment;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.payment.util.CommonPaymentUtil;
import com.intellij.notification.NotificationType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

        // Add ActionListener to payButton
        payButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CommonPaymentUtil.routeToPayment();
            }
        });

        // Add ActionListener to activateButton
        activateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatus();
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

    /**
     * Updates the status label based on the text entered in keyArea.
     */
    private void updateStatus() {
        String enteredText = keyArea.getText().trim();

        //TODO: Will be a boolean here received from authentication service
        if ("Akshat Jain".equals(enteredText)) {
            setStatus("Activated \u2714", Color.decode("#1F8A3C"));
        } else {
            setStatus("Error - wrong product key \u26A0", Color.decode("#E46A76"));
        }
    }

    /**
     * Sets the status text and color.
     * @param text the status text
     * @param color the color for the status text
     */
    private void setStatus(String text, Color color) {
        status.setText(text);
        status.setForeground(color);
    }
}
