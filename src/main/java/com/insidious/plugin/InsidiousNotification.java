package com.insidious.plugin;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import java.util.Collection;

public class InsidiousNotification {
    public static final String DISPLAY_ID = InsidiousBundle.message("ide.notification.groupDisplayId");
    public final static NotificationGroup balloonNotificationGroup = NotificationGroup.findRegisteredGroup(DISPLAY_ID);
    private static final String TITLE = InsidiousBundle.message("ide.notification.messageTitle");
    private final static Icon unloggedIcon = IconLoader.getIcon("/icons/png/logo_unlogged.png",
            InsidiousNotification.class);

    public static void notifyMessage(String message, NotificationType notificationType) {
        Notification notification = new Notification(InsidiousNotification.DISPLAY_ID, "Unlogged", message,
                notificationType);
        if (!notificationType.equals(NotificationType.ERROR)) {
            notification.setIcon(unloggedIcon);
        }
        Notifications.Bus.notify(notification);
    }

    public static void notifyMessage(String message, NotificationType notificationType, Collection<? extends AnAction> newActions) {
        Notification notification = new Notification(InsidiousNotification.DISPLAY_ID, "Unlogged", message,
                notificationType);
        if (!notificationType.equals(NotificationType.ERROR)) {
            notification.setIcon(unloggedIcon);
        }
        newActions.forEach(notification::addAction);
        Notifications.Bus.notify(notification);
    }
}


