package com.insidious.plugin.ui.payment.util;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.payment.PaymentScreen;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.*;

import javax.swing.*;
import java.awt.*;

/**
 * Utility class for creating and managing pop-up dialog for Payments.
 */
public class PopUpUtil {

    // Holds the current instance of the popup
    private static JBPopup currentPopup;

    /**
     * Shows a pop-up dialog for Payments.
     *
     * @param insidiousService the service providing context for the plugin
     */
    public static void showPaymentPopUp(InsidiousService insidiousService) {

        // Close the existing popup if it is showing
        if (currentPopup != null && currentPopup.isVisible()) {
            currentPopup.cancel();
        }

        // Create the main panel for the test runner injector UI
        PaymentScreen paymentScreen = new PaymentScreen(insidiousService);
        JComponent paymentComponent = paymentScreen.getMainPanel();

        // Set maximum size for the popup component
        Dimension max = paymentComponent.getMaximumSize();
        paymentComponent.setMaximumSize(new Dimension((int) max.getWidth(), (int) max.getHeight()));

        // Create the popup builder and configure its properties
        ComponentPopupBuilder paymentPopUp = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(paymentComponent, null);
        currentPopup = paymentPopUp
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setCancelButton(new IconButton("Close", AllIcons.Actions.CloseDarkGrey))
                .setCancelKeyEnabled(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setResizable(false)
                .setCancelOnClickOutside(false)
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .setBelongsToGlobalPopupStack(true)
                .setTitle("Get Unlogged Premium!")
                .setTitleIcon(new ActiveIcon(UIUtils.UNLOGGED_ICON_DARK_SVG))
                .createPopup();
        currentPopup.showCenteredInCurrentWindow(insidiousService.getProject());

        ApplicationManager.getApplication().invokeLater(() -> {
            Dimension size = currentPopup.getSize();
            currentPopup.setSize(new Dimension((int) size.getWidth(), (int) size.getHeight()));
        });
    }
}
