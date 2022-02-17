package extension.jwdp.event;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import extension.CommandSender;
import extension.InsidiousJavaDebugProcess;
import extension.InsidiousNotification;
import extension.jwdp.RequestMessage;
import extension.util.BookmarksUtil;

import java.io.IOException;


public class RewoundEvent
        extends RequestMessage {
    private static final Logger logger = Logger.getInstance(RewoundEvent.class);
    public static String EVENT_NAME = "VMRewound";
    private final int threadId;

    public RewoundEvent(int threadId) {
        this.threadId = threadId;
    }


    public void process(CommandSender commandSender) throws IOException {
        logger.debug("Start of program history");
        InsidiousJavaDebugProcess debugProcess = commandSender.getDebugProcess();
        XDebugSessionImpl session = (XDebugSessionImpl) debugProcess.getSession();


        Notification notification = InsidiousNotification.balloonNotificationGroup.createNotification("Start of program history", NotificationType.INFORMATION);

        Notifications.Bus.notify(notification, session.getProject());
//     TimelineManager.getTimeline(commandSender.getDebugProcess().getSession().getSessionName())
//       .getLocationState()
//       .setStart();
        BookmarksUtil.suspendThread(debugProcess
                .getConnector().getThreadReferenceWithUniqueId(this.threadId), commandSender
                .getDebugProcess());
    }
}


