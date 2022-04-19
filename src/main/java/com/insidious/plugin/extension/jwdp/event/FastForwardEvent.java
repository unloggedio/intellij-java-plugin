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
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.sun.jdi.ThreadReference;
import org.slf4j.Logger;

import java.io.IOException;


public class FastForwardEvent
        extends RequestMessage {
    private static final Logger LOGGER = LoggerUtil.getInstance(FastForwardEvent.class);

    public static String EVENT_NAME = "VMFastForward";

    private final int threadId;

    public FastForwardEvent(int threadId) {
        this.threadId = threadId;
    }


    public void process(CommandSender commandSender) throws IOException {
        LOGGER.debug("End of program history");
        InsidiousJavaDebugProcess debugProcess = commandSender.getDebugProcess();
        XDebugSessionImpl session = (XDebugSessionImpl) debugProcess.getSession();


        Notification notification = InsidiousNotification.balloonNotificationGroup.createNotification("End of program history", NotificationType.INFORMATION);

        Notifications.Bus.notify(notification, session.getProject());


        ThreadReference currentThread = debugProcess.getConnector().getThreadReferenceWithUniqueId(this.threadId);
//        TimelineManager.getTimeline(commandSender.getDebugProcess().getSession().getSessionName())
//                .getLocationState()
//                .setEnd();
        BookmarksUtil.suspendThread(currentThread, commandSender.getDebugProcess());
    }
}


