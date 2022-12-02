package com.insidious.plugin.extension;

import com.intellij.notification.*;
import com.intellij.notification.impl.NotificationFullContent;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InsidiousNotification {
    public static final String DISPLAY_ID = InsidiousBundle.message("ide.notification.groupDisplayId");
    public static NotificationGroup balloonNotificationGroup = NotificationGroup.findRegisteredGroup(DISPLAY_ID);
    private static final String TITLE = InsidiousBundle.message("ide.notification.messageTitle");

    private static Icon unloggedIcon = IconLoader.getIcon("/icons/png/logo_unlogged.png", InsidiousNotification.class);
    public static void notifyMessage(String message, NotificationType notificationType) {
        Notification notification = new Notification(InsidiousNotification.DISPLAY_ID, "Unlogged", message, notificationType,
                new NotificationListener.UrlOpeningListener(true));
        if(!notificationType.equals(NotificationType.ERROR))
        {
            notification.setIcon(unloggedIcon);
        }
        Notifications.Bus.notify(notification);
    }
}


