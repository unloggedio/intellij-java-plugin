package com.insidious.plugin.extension.jwdp.event;

import com.insidious.plugin.extension.CommandSender;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.extension.jwdp.RequestMessage;
import com.insidious.plugin.extension.util.BookmarksUtil;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.impl.XDebugSessionImpl;

import java.io.IOException;


public class RewoundEvent
        extends RequestMessage {
    private static final Logger logger = LoggerUtil.getInstance(RewoundEvent.class);
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


