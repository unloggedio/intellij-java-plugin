package com.insidious.plugin.util;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.intellij.notification.NotificationType;

import java.awt.*;

public class BrowserRouteUtils {
    public static void routeInBrowser(String browserLink, String failedNotificationMessage, String eventName) {
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(browserLink));
            } catch (Exception e) {
                //Handle this
            }
        } else {
            InsidiousNotification.notifyMessage(
                    failedNotificationMessage, NotificationType.INFORMATION);
        }
        UsageInsightTracker.getInstance().RecordEvent(
                eventName, null);
    }
}
