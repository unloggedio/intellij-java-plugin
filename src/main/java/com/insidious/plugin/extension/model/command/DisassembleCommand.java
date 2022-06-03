package com.insidious.plugin.extension.model.command;

import com.insidious.plugin.extension.CommandSender;
import com.insidious.plugin.extension.jwdp.Command;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;

public class DisassembleCommand extends Command<DisassembleRequest> {
    private static final Logger logger = LoggerUtil.getInstance(DisassembleCommand.class);
    public static String COMMAND_NAME = "Disassemble";
    private DisassembleResponse response;

    public DisassembleCommand(DisassembleRequest request) {
        super(request);
    }


    public void process(CommandSender commandSender) throws IOException {
        this


                .response = commandSender.sendMessage(getCommandRequest(), DisassembleResponse.class).orElseThrow(() -> new RuntimeException("Couldn't get the disassembled code for pc=" + getCommandRequest().getPc()));


        processResponse();
    }


    protected void processResponse() throws IOException {
        logger.debug("received disassembled code with startPc=" + this.response

                .getStartPc() + " endPc=" + this.response

                .getEndPc() + " description=" + this.response

                .getDescription() + " countOfInstructions=" + (this.response

                .getInstructions()).length);


        StringBuffer content = new StringBuffer(String.format("Stopped at pc: 0x%X, %s\n", Long.valueOf(getCommandRequest().getPc()), this.response.getDescription()));

        int currentLineStart = 0;
        int currentLineEnd = 0;


        DisassembleResponse.DisassembledCode[] instructions = this.response.getInstructions();
        for (int i = 0; instructions != null && i < instructions.length; i++) {
            DisassembleResponse.DisassembledCode instruction = instructions[i];
            String instructionStr = instruction.toString();

            if (instruction.getPc() == getCommandRequest().getPc()) {

                currentLineStart = content.length();
                currentLineEnd = currentLineStart + instructionStr.length();
            }


            content.append(instructionStr);
        }


//        InsidiousToolWindowFactory.getInstance()
//                .setContent(content.toString(), currentLineStart, currentLineEnd);
    }
}


