package com.insidious.plugin.extension.model.command;

import com.insidious.plugin.extension.jwdp.SetBookmarkCommand;

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


