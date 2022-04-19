package com.insidious.plugin.extension.jwdp;

import com.insidious.plugin.extension.CommandSender;
import com.insidious.plugin.extension.model.command.SetBookmarkRequest;
import com.insidious.plugin.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.IOException;

public class SetBookmarkCommand extends Command<SetBookmarkRequest> {
    private static final Logger logger = LoggerUtil.getInstance(SetBookmarkCommand.class);
    public static String COMMAND_NAME = "SetBookmark";

    public SetBookmarkCommand(SetBookmarkRequest request) {
        super(request);
    }


    public void process(CommandSender commandSender) throws IOException {
        SetBookmarkRequest request = getCommandRequest();
        logger.debug("Processing set bookmark " + getCommandRequest().getName());
        commandSender.sendMessage(request);
    }
}


