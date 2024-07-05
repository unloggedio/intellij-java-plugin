package com.insidious.plugin.ui.payment.util;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.payment.PaymentActivationScreen;
import com.insidious.plugin.ui.payment.PaymentScreen;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.*;

import javax.swing.*;
import java.awt.*;

public class PopUpUtil {

    // Holds the current instance of the popup
    private static JBPopup currentPopup;
    private static JPanel popupContentPanel;

    /**
     * Shows a pop-up dialog for Payments.
     *
     * @param insidiousService the service providing context for the plugin
     */
    public static void showPaymentPopUp(InsidiousService insidiousService) {
        // Check if the popup is already showing
        if (currentPopup != null && currentPopup.isVisible()) {
            return; // Exit early if the popup is already visible
        }

        // Create the Payment Screen panel
        PaymentScreen paymentScreen = new PaymentScreen(insidiousService);
        JComponent paymentComponent = paymentScreen.getMainPanel();

        // Create and show the popup
        createAndShowPopup(insidiousService, paymentComponent, "Get Unlogged Premium!");
    }

    /**
     * Switches the content of the current popup to the Payment Activation Screen.
     *
     * @param insidiousService the service providing context for the plugin
     */
    public static void switchToActivationScreen(InsidiousService insidiousService) {
        // Check if the popup is already showing
        if (currentPopup != null && currentPopup.isVisible()) {
            // Create the Payment Activation Screen panel
            PaymentActivationScreen paymentActivationScreen = new PaymentActivationScreen(insidiousService);
            JComponent activationComponent = paymentActivationScreen.getMainPanel();

            // Update the popup with the new content
            updatePopupContent(activationComponent, "Activate Your Premium!");
        }
    }

    /**
     * Creates and shows a popup with the specified component and title.
     *
     * @param insidiousService the service providing context for the plugin
     * @param component the component to show in the popup
     * @param title the title of the popup
     */
    private static void createAndShowPopup(InsidiousService insidiousService, JComponent component, String title) {
        // Initialize the popup content panel
        popupContentPanel = new JPanel(new BorderLayout());
        popupContentPanel.add(component, BorderLayout.CENTER);

        // Create the popup builder and configure its properties
        ComponentPopupBuilder paymentPopUp = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popupContentPanel, null);
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
                .setTitle(title)
                .setTitleIcon(new ActiveIcon(UIUtils.UNLOGGED_ICON_DARK_SVG))
                .createPopup();
        currentPopup.showCenteredInCurrentWindow(insidiousService.getProject());
    }

    /**
     * Updates the content of the current popup with the specified component and title.
     *
     * @param component the new component to show in the popup
     * @param title the new title of the popup
     */
    private static void updatePopupContent(JComponent component, String title) {
        if (currentPopup != null && currentPopup.isVisible()) {
            popupContentPanel.removeAll();
            popupContentPanel.add(component, BorderLayout.CENTER);
            popupContentPanel.revalidate();
            popupContentPanel.repaint();
            currentPopup.setCaption(title);
        }
    }
}
