package com.insidious.plugin.extension.model.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandRequest {
    private final String commandName;

    public CommandRequest(@JsonProperty("commandName") String commandName) {
        this.commandName = commandName;
    }

    public String getCommandName() {
        return this.commandName;
    }


    public String toString() {
        return String.format("Command<%s>{commandName=" + this.commandName + '}', getClass().getSimpleName());
    }
}


