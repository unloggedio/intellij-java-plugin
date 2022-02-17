package extension.jwdp.event;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import extension.CommandSender;
import extension.InsidiousJavaDebugProcess;
import extension.jwdp.RequestMessage;
import extension.util.BookmarksUtil;

import java.io.IOException;


public final class GotoCompleteEvent
        extends RequestMessage {
    public static final String EVENT_NAME = "GotoComplete";
    private static final Logger LOGGER = Logger.getInstance(GotoCompleteEvent.class);
    private final String action;

    private final int threadId;

    public GotoCompleteEvent(String action, int threadId) {
        this.action = action;
        this.threadId = threadId;
    }


    public void process(CommandSender commandSender) throws IOException {
        LOGGER.info(
                "Processing response for action: " + this.action + ", threadId: " + this.threadId);


        switch (this.action) {
            case "GotoTimestamp":
                gotoTimestamp(commandSender, true);
                return;
            case "bookmark_1":
                BookmarksUtil.gotoStartLocation(commandSender.getDebugProcess());
                return;
            case "bookmark_1000":
                BookmarksUtil.gotoEndLocation(commandSender.getDebugProcess());
                return;
        }
        commandSender.setLastThreadId(this.threadId);
        BookmarksUtil.setLocationStateUnknown(commandSender.getDebugProcess());
    }


    public void gotoTimestamp(CommandSender commandSender, boolean highlightLine) {
        ApplicationManager.getApplication()
                .invokeLater(() -> {
                    InsidiousJavaDebugProcess debugProcess = commandSender.getDebugProcess();
                    debugProcess.stopPerformanceTiming();
                    commandSender.setLastThreadId(this.threadId);
                    BookmarksUtil.suspendThread(debugProcess.getConnector().getThreadReferenceWithUniqueId(this.threadId), false, highlightLine, debugProcess);
                });
    }
}


