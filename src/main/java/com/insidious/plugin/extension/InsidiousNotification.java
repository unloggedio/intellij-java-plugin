package com.insidious.plugin.extension;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationFullContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousNotification {
    private static final String DISPLAY_ID = InsidiousBundle.message("ide.notification.groupDisplayId");
    public static NotificationGroup balloonNotificationGroup = NotificationGroup.findRegisteredGroup(DISPLAY_ID);
    private static final String TITLE = InsidiousBundle.message("ide.notification.messageTitle");

}


