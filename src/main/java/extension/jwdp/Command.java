package extension.jwdp;


import extension.model.command.CommandRequest;

public abstract class Command<T extends CommandRequest>
        extends RequestMessage {
    private final T commandRequest;

    protected Command(T commandRequest) {
        this.commandRequest = commandRequest;
    }

    public T getCommandRequest() {
        return this.commandRequest;
    }
}


