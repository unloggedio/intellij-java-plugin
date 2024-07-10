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
                BrowserRouteUtils.routeInBrowser("https://buy.stripe.com/fZeg1jc4I5UV1Gw146",
                        "<a href='https://buy.stripe.com/fZeg1jc4I5UV1Gw146'>Follow Payment Link</a> to make payment for premium",
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
