package extension.model.command;

import extension.jwdp.SetBookmarkCommand;

public class SetBookmarkRequest extends CommandRequest {
    private String name;

    public SetBookmarkRequest(String name) {
        this();
        this.name = name;
    }

    private SetBookmarkRequest() {
        super(SetBookmarkCommand.COMMAND_NAME);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}


