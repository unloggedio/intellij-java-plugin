package com.insidious.plugin.extension;

import com.intellij.notification.*;
import com.intellij.notification.impl.NotificationFullContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousNotification {
    public static final String DISPLAY_ID = InsidiousBundle.message("ide.notification.groupDisplayId");
    public static NotificationGroup balloonNotificationGroup = NotificationGroup.findRegisteredGroup(DISPLAY_ID);
    private static final String TITLE = InsidiousBundle.message("ide.notification.messageTitle");

    public static void notifyMessage(String message, NotificationType notificationType) {
        Notifications.Bus.notify(
                new Notification(InsidiousNotification.DISPLAY_ID, "VideoBug", message, notificationType)
        );
    }

}


