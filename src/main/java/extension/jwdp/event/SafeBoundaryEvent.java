package extension.jwdp.event;

import com.intellij.openapi.diagnostic.Logger;
import extension.CommandSender;
import extension.InsidiousApplicationState;
import extension.InsidiousJavaDebugProcess;
import extension.jwdp.RequestMessage;

import java.io.IOException;

public class SafeBoundaryEvent extends RequestMessage {
    private static final Logger logger = Logger.getInstance(SafeBoundaryEvent.class);
    public static String EVENT_NAME = "SafeBoundary";
    public String TYPE_START = "start";
    public String TYPE_END = "end";
    private final String type;
    private final long bbcount;

    public SafeBoundaryEvent(String type, long bbcount) {
        this.type = type;
        this.bbcount = bbcount;
    }


    public void process(CommandSender commandSender) throws IOException {
        logger.debug("Processing SafeBoundaryEvent " + this.type + " with bbcount " + this.bbcount);
        if (this.TYPE_START.equals(this.type)) {
            InsidiousJavaDebugProcess InsidiousJavaDebugProcess = commandSender.getDebugProcess();

            InsidiousApplicationState state = InsidiousJavaDebugProcess.getProcessHandler().getUserData(InsidiousApplicationState.KEY);
            state.setInitialSafeBbCount(Long.valueOf(this.bbcount));
        }
    }
}


