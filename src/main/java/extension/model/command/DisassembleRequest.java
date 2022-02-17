package extension.model.command;


public class DisassembleRequest extends CommandRequest {
    private long pc;

    public DisassembleRequest(long pc) {
        this();
        this.pc = pc;
    }

    private DisassembleRequest() {
        super(DisassembleCommand.COMMAND_NAME);
    }

    public long getPc() {
        return this.pc;
    }

    public void setPc(long pc) {
        this.pc = pc;
    }
}


