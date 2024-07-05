package com.insidious.plugin.ui.payment.util;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.intellij.notification.NotificationType;

import java.awt.*;

public class CommonPaymentUtil {

    /**
     * Opens the CI documentation link in the default browser.
     * If Desktop is not supported, shows a notification with the link.
     */
    public static void routeToPayment() {
        String link = "https://read.unlogged.io/cirunner/";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
                //Handle this
            }
        } else {
            InsidiousNotification.notifyMessage(
                    "<a href='https://read.unlogged.io/cirunner/'>Documentation</a> for running unlogged replay tests from " +
                            "CLI/Maven/Gradle", NotificationType.INFORMATION);
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDocumentation", null);
    }
}