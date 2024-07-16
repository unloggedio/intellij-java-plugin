package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousConfigurationState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.payment.util.PopUpUtil;
import com.insidious.plugin.util.BrowserRouteUtils;

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
    private JScrollPane keyAreaScroll;

    private final InsidiousService insidiousService;
    private final AuthenticationService authenticationService;

    public PaymentActivationScreen(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.authenticationService = new AuthenticationService(insidiousService.getProject().getService(InsidiousConfigurationState.class));

        // Add ActionListener to payButton
        payButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BrowserRouteUtils.routeInBrowser("https://buy.stripe.com/fZeg1jc4I5UV1Gw146",
                        "<a href='https://buy.stripe.com/fZeg1jc4I5UV1Gw146'>Follow Payment Link</a> to make payment for premium",
                        "ROUTE_TO_PAY_PREMIUM");
            }
        });

        keyArea.setLineWrap(true);

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
     *
     * @return the main JPanel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Updates the status label based on the text entered in keyArea.
     */
    private void updateStatus() {
        String enteredText = keyArea.getText().trim().replaceAll("\\s", "");
        //TODO: Will be a boolean here received from authentication service
        if (authenticationService.validateAndStoreToken(enteredText)) {
            setStatus("Activated \u2714", Color.decode("#1F8A3C"));
            insidiousService.removeGetPremiumPanel();
            PopUpUtil.closeCurrentPopup();
        } else {
            setStatus("Error - wrong product key \u26A0", Color.decode("#E46A76"));
        }
    }

    /**
     * Sets the status text and color.
     *
     * @param text  the status text
     * @param color the color for the status text
     */
    private void setStatus(String text, Color color) {
        status.setText(text);
        status.setForeground(color);
    }
}
