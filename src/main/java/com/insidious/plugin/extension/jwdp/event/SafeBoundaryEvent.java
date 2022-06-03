package com.insidious.plugin.extension.jwdp.event;

import com.insidious.plugin.extension.CommandSender;
import com.insidious.plugin.extension.InsidiousApplicationState;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.jwdp.RequestMessage;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;

public class SafeBoundaryEvent extends RequestMessage {
    private static final Logger logger = LoggerUtil.getInstance(SafeBoundaryEvent.class);
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


