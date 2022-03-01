package com.insidious.plugin.extension.model.command;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;

public class DisassembleResponse implements CommandResponse {
    private long startPc;
    private long endPc;
    private String description;
    private DisassembledCode[] instructions;

    public long getStartPc() {
        return this.startPc;
    }

    public void setStartPc(long startPc) {
        this.startPc = startPc;
    }

    public long getEndPc() {
        return this.endPc;
    }

    public void setEndPc(long endPc) {
        this.endPc = endPc;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DisassembledCode[] getInstructions() {
        return this.instructions;
    }

    public void setInstructions(DisassembledCode[] instructions) {
        this.instructions = instructions;
    }

    public String toString() {
        return "InsidiousDisassembleResponse{startPc=" + this.startPc + ", endPc=" + this.endPc + ", description='" + this.description + '\'' + ", disassembledInstructions=" +


                Arrays.toString(this.instructions) + '}';
    }

    public static class DisassembledCode {
        private long pc;
        private String description;

        public DisassembledCode(long pc, String description) {
            this.pc = pc;
            this.description = description;
        }

        private DisassembledCode() {
        }

        public long getPc() {
            return this.pc;
        }

        @JsonIgnore
        public String getPcHex() {
            return String.format("0x%X", Long.valueOf(this.pc));
        }

        public String getDescription() {
            return this.description;
        }


        public String toString() {
            return getPcHex() + '\t' +

                    getDescription() + '\n';
        }
    }
}


