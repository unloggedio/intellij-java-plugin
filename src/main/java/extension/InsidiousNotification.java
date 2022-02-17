package extension;

import com.intellij.notification.*;
import com.intellij.notification.impl.NotificationFullContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousNotification {
    private static final String DISPLAY_ID = InsidiousBundle.message("ide.notification.groupDisplayId");
    public static NotificationGroup balloonNotificationGroup = new NotificationGroup(DISPLAY_ID, NotificationDisplayType.BALLOON, true, null, null);
    private static final String TITLE = InsidiousBundle.message("ide.notification.messageTitle");

    public static final class FullContentNotification
            extends Notification
            implements NotificationFullContent {
        public FullContentNotification(@NotNull String title, @NotNull String content, @NotNull NotificationType type, @Nullable NotificationListener listener) {
            super(InsidiousNotification.DISPLAY_ID, title, content, type, listener);
        }
    }
}


