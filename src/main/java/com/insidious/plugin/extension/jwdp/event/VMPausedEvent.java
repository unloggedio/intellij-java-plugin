package com.insidious.plugin.extension.jwdp.event;


import com.insidious.plugin.extension.CommandSender;
import com.insidious.plugin.extension.jwdp.RequestMessage;
import com.insidious.plugin.extension.model.command.DisassembleCommand;
import com.insidious.plugin.extension.model.command.DisassembleRequest;

import java.io.IOException;

public class VMPausedEvent extends RequestMessage {
    public static String EVENT_NAME = "VMPaused";
    private final long pc;

    public VMPausedEvent(long pc) {
        this.pc = pc;
    }


    public void process(CommandSender commandSender) throws IOException {
        if (this.pc > 0L) {
            DisassembleRequest disassembleRequest = new DisassembleRequest(this.pc);
            DisassembleCommand disassembleCommand = new DisassembleCommand(disassembleRequest);
            disassembleCommand.process(commandSender);
        }
    }
}


