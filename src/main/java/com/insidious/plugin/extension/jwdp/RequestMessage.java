package com.insidious.plugin.extension.jwdp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.extension.CommandSender;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;

public abstract class RequestMessage {
    private static final Logger logger = LoggerUtil.getInstance(RequestMessage.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> T createMessage(String commandStr, Class<T> clazz) {
        T command = null;
        try {
            command = OBJECT_MAPPER.readValue(commandStr, clazz);
        } catch (IOException ex) {
            logger.error("Couldn't parse command " + commandStr, ex);
        }
        return command;
    }

    public abstract void process(CommandSender paramCommandSender) throws IOException;
}


