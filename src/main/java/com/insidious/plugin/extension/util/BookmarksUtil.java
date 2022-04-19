package com.insidious.plugin.extension.util;

import com.insidious.plugin.extension.CommandSender;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.extension.InsidiousXSuspendContext;
import com.insidious.plugin.extension.jwdp.GetAllBookmarksCommand;
import com.insidious.plugin.extension.jwdp.SetBookmarkCommand;
import com.insidious.plugin.extension.model.InsidiousBookmark;
import com.insidious.plugin.extension.model.command.SetBookmarkRequest;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.sun.jdi.ThreadReference;
import org.slf4j.Logger;

import java.io.IOException;


public final class BookmarksUtil {
    public static final String START_OF_PROGRAM_HISTORY_MESSAGE = "Start of program history";
    public static final String END_OF_PROGRAM_HISTORY_MESSAGE = "End of program history";
    private static final Logger logger = LoggerUtil.getInstance(BookmarksUtil.class);

    public static void gotoStartLocation(InsidiousJavaDebugProcess process) {
        logger.debug("Start of program history");
//        TimelineManager.getTimeline(process.getSession().getSessionName())
//                .getLocationState()
//                .setStart();
        Notifications.Bus.notify(InsidiousNotification.balloonNotificationGroup
                .createNotification("Start of program history", NotificationType.INFORMATION), process

                .getProject());
    }

    public static void gotoEndLocation(InsidiousJavaDebugProcess process) {
        logger.debug("End of program history");
//        TimelineManager.getTimeline(process.getSession().getSessionName())
//                .getLocationState()
//                .setEnd();
        Notifications.Bus.notify(InsidiousNotification.balloonNotificationGroup
                .createNotification("End of program history", NotificationType.INFORMATION), process

                .getProject());
    }

    public static void setLocationStateUnknown(InsidiousJavaDebugProcess process) {
//        TimelineManager.getTimeline(process.getSession().getSessionName())
//                .getLocationState()
//                .setUnknown();
    }


    public static void applyBookmark(InsidiousBookmark bookmark, CommandSender commandSender) throws IOException {
        createBookmarkInt(bookmark, commandSender);
    }


    private static void createBookmarkInt(InsidiousBookmark bookmark, CommandSender commandSender) throws IOException {
        String bookmarkName = bookmark.getName();
        logger.debug("Called addBookmark " + bookmarkName + " at " + bookmark


                .getPsiClass() + ":" + bookmark

                .getLineNum());
        if (commandSender == null) {
            logger.info(
                    "Timeline won't be displayed until the Insidious thread is initialised.");
        } else {

            logger.debug("Processing set bookmark " + bookmarkName);
            (new SetBookmarkCommand(new SetBookmarkRequest(bookmarkName))).process(commandSender);
            refreshBookmarks(commandSender);
        }
    }

    public static void refreshBookmarks(CommandSender commandSender) {
        try {
            (new GetAllBookmarksCommand()).process(commandSender);
        } catch (Exception e) {
            logger.error("failed", e);
//            TimelineManager.getTimeline(commandSender
//                            .getDebugProcess().getSession().getSessionName())
//                    .setDisconnected();
        }
    }


    public static void suspendThread(ThreadReference threadReference, InsidiousJavaDebugProcess process) {
        suspendThread(threadReference, true, true, process);
    }


    public static void suspendThread(ThreadReference threadReference, boolean isBookmarkEvent, boolean updatePosition, InsidiousJavaDebugProcess insidiousJavaDebugProcess) {
        InsidiousXSuspendContext InsidiousXSuspendContext = new InsidiousXSuspendContext(insidiousJavaDebugProcess, threadReference, 2, isBookmarkEvent);


        if (!threadReference.isSuspended()) {
            insidiousJavaDebugProcess.startPausing();
        } else {
            insidiousJavaDebugProcess.notifyPaused(InsidiousXSuspendContext);
        }
        if (updatePosition) {
            logger.info("Updating suspended position in com.insidious.plugin.ui");
            insidiousJavaDebugProcess.getSession().positionReached(InsidiousXSuspendContext);
        }
    }
}


