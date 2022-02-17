package extension.jwdp.event;

import com.intellij.openapi.diagnostic.Logger;
import extension.CommandSender;
import extension.jwdp.RequestMessage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogMessageEvent extends RequestMessage {
    private static final Logger logger = Logger.getInstance(LogMessageEvent.class);
    public static String EVENT_NAME = "LogMessage";
    private static final DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
    private final String message;
    private final String threadName;
    private final String loggerName;
    private final long timeInMillis;


    public LogMessageEvent(String message, String threadName, String loggerName, long timeInMillis) {
        this.message = message;
        this.threadName = threadName;
        this.loggerName = loggerName;
        this.timeInMillis = timeInMillis;
    }


    public void process(CommandSender commandSender) {
        logger.debug(
                String.format("%sBridge - %s [%s] %s - %s", commandSender.getDebugProcess().getRunProfileName(), formatter
                .format(new Date(this.timeInMillis)), this.threadName, this.loggerName, this.message));
    }
}


