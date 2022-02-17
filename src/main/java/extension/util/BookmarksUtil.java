package extension.util;

import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.ThreadReference;
import extension.CommandSender;
import extension.InsidiousJavaDebugProcess;
import extension.InsidiousNotification;
import extension.InsidiousXSuspendContext;
import extension.jwdp.GetAllBookmarksCommand;
import extension.jwdp.SetBookmarkCommand;
import extension.model.InsidiousBookmark;
import extension.model.command.SetBookmarkRequest;

import java.io.IOException;


public final class BookmarksUtil {
    public static final String START_OF_PROGRAM_HISTORY_MESSAGE = "Start of program history";
    public static final String END_OF_PROGRAM_HISTORY_MESSAGE = "End of program history";
    private static final Logger LOGGER = Logger.getInstance(BookmarksUtil.class);

    public static void gotoStartLocation(InsidiousJavaDebugProcess process) {
        LOGGER.debug("Start of program history");
//        TimelineManager.getTimeline(process.getSession().getSessionName())
//                .getLocationState()
//                .setStart();
        Notifications.Bus.notify(InsidiousNotification.balloonNotificationGroup
                .createNotification("Start of program history", NotificationType.INFORMATION), process

                .getProject());
    }

    public static void gotoEndLocation(InsidiousJavaDebugProcess process) {
        LOGGER.debug("End of program history");
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
        LOGGER.debug("Called addBookmark " + bookmarkName + " at " + bookmark


                .getPsiClass() + ":" + bookmark

                .getLineNum());
        if (commandSender == null) {
            LOGGER.info(
                    "Timeline won't be displayed until the Insidious thread is initialised.");
        } else {

            LOGGER.debug("Processing set bookmark " + bookmarkName);
            (new SetBookmarkCommand(new SetBookmarkRequest(bookmarkName))).process(commandSender);
            refreshBookmarks(commandSender);
        }
    }

    public static void refreshBookmarks(CommandSender commandSender) {
        try {
            (new GetAllBookmarksCommand()).process(commandSender);
        } catch (Exception e) {
            LOGGER.error(e);
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
            LOGGER.info("Updating suspended position in ui");
            insidiousJavaDebugProcess.getSession().positionReached(InsidiousXSuspendContext);
        }
    }
}


