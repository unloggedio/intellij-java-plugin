package extension.jwdp;

import com.intellij.openapi.diagnostic.Logger;
import extension.CommandSender;
import extension.model.command.SetBookmarkRequest;

import java.io.IOException;

public class SetBookmarkCommand extends Command<SetBookmarkRequest> {
    private static final Logger logger = Logger.getInstance(SetBookmarkCommand.class);
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


