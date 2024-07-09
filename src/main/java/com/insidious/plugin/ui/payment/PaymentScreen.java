package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.payment.util.PopUpUtil;
import com.insidious.plugin.util.BrowserRouteUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PaymentScreen {
    private JPanel mainPanel;
    private JPanel textPanel;
    private JPanel buttonPanel;
    private JButton payButton;
    private JButton activateButton;
    private JLabel point1;
    private JLabel point2;
    private JLabel point3;
    private final InsidiousService insidiousService;


    public PaymentScreen(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;

        // Add ActionListener to payButton
        payButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrowserRouteUtils.routeInBrowser("https://read.unlogged.io/cirunner/",
                        "<a href='https://read.unlogged.io/cirunner/'>Documentation</a> for running unlogged replay tests from CLI/Maven/Gradle",
                        "ROUTE_TO_PAY_PREMIUM");
            }
        });

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
     *
     * @return the main JPanel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }
}
