package extension;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import extension.jwdp.RequestMessage;

public class MessageEvent extends RequestMessage {
    public static final String Insidious_MESSAGE_ERROR_COMMAND = "error";
    public static final String Insidious_MESSAGE_WARNING_COMMAND = "warning";
    public static final String Insidious_MESSAGE_INFO_COMMAND = "info";
    public static final String Insidious_MESSAGE_CONSOLE_OUTPUT = "console_output";
    public static final String Insidious_MESSAGE_CONSOLE_ERROR = "console_error";
    private static final Logger logger = Logger.getInstance(MessageEvent.class);
    public static String EVENT_NAME = "Message";
    private final String message;
    private final String type;

    public MessageEvent(String message, String type) {
        this.message = message;
        this.type = type;
    }


    public void process(CommandSender commandSender) {
        logger.debug("process " + this.type);
        if ("console_output".equals(this.type)) {
            consoleLog(commandSender.getDebugProcess(), ConsoleViewContentType.NORMAL_OUTPUT);
        } else if ("console_error".equals(this.type)) {
            consoleLog(commandSender.getDebugProcess(), ConsoleViewContentType.ERROR_OUTPUT);
        } else if ("error".equals(this.type)) {
            InsidiousApplicationState state = getState(commandSender);
            state.setErrored(true);
            showNotification(NotificationType.ERROR);
            commandSender.getDebugProcess().stop();
        } else if ("warning".equals(this.type)) {
            showNotification(NotificationType.WARNING);
        } else {

            showNotification(NotificationType.INFORMATION);
        }
    }

    private InsidiousApplicationState getState(CommandSender commandSender) {
        return commandSender
                .getDebugProcess()
                .getProcessHandler()
                .getUserData(InsidiousApplicationState.KEY);
    }

    private void showNotification(NotificationType notificationType) {
        logger.info(notificationType + " " + this.message);

        Notification notification = InsidiousNotification.balloonNotificationGroup.createNotification(this.message, notificationType);

        Notifications.Bus.notify(notification);
    }


    private void consoleLog(XDebugProcess debugProcess, ConsoleViewContentType contentType) {
        InsidiousApplicationState state = debugProcess.getProcessHandler().getUserData(InsidiousApplicationState.KEY);
        state.getConsoleView().print(this.message, contentType);
    }

    public String getType() {
        return this.type;
    }

    public String getMessage() {
        return this.message;
    }
}


