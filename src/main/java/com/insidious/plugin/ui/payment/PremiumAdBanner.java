package com.insidious.plugin.ui.payment;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.payment.util.PopUpUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PremiumAdBanner {
    private JPanel premiumPanel;
    private JLabel getPremium;
    private JLabel content;
    private String source;
    private final InsidiousService insidiousService;

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
    }

    /**
     * Returns the main panel of the UI.
     *
     * @return the main JPanel
     */
    public JPanel getPremiumPanel() {
        return premiumPanel;
    }
}
