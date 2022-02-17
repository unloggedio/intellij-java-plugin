package extension.jwdp.event;


import extension.CommandSender;
import extension.jwdp.RequestMessage;
import extension.model.command.DisassembleCommand;
import extension.model.command.DisassembleRequest;

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


