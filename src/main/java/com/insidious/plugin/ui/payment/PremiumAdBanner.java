package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.payment.util.PopUpUtil;
import com.insidious.plugin.util.BrowserRouteUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PremiumAdBanner {
    private JPanel nonPremiumPanel;
    private JLabel getPremium;
    private JLabel content;
    private JPanel banner;
    private JPanel premiumPanel;
    private JLabel premiumUser;
    private JLabel premiumContent;
    private JPanel mainPanel;
    private JLabel discord;
    private String source;
    private final InsidiousService insidiousService;
    private boolean isPremiumUser; // Flag to indicate if the user is premium, by default it is false


    public PremiumAdBanner(InsidiousService insidiousService, String source) {

        this.insidiousService = insidiousService;
        this.source = source;

        getPremium.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getPremium.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                PopUpUtil.showPaymentPopUp(insidiousService);
            }
        });

        discord.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        discord.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserRouteUtils.routeInBrowser("https://discord.gg/Hhwvay8uTa",
                        "Reach out to us on <a href='https://discord.gg/Hhwvay8uTa'>Discord</a>",
                        "routeToDiscord");
            }
        });
        // Initialize the panels based on the flag
        updatePanelVisibility();
    }

    /**
     * Sets the isPremiumUser flag and updates the panel visibility.
     *
     * @param isPremiumUser boolean indicating if the user is premium
     */
    public void setPremiumUserFlag(boolean isPremiumUser) {
        this.isPremiumUser = isPremiumUser;
        updatePanelVisibility();
    }

    /**
     * Updates the visibility of the panels based on the isPremiumUser flag.
     */
    private void updatePanelVisibility() {
        if (isPremiumUser) {
            premiumPanel.setVisible(true);
            nonPremiumPanel.setVisible(false);
        } else {
            premiumPanel.setVisible(false);
            nonPremiumPanel.setVisible(true);
        }
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
