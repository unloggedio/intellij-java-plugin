package extension.jwdp;

import com.intellij.openapi.diagnostic.Logger;
import extension.CommandSender;
import extension.model.command.CommandRequest;
import extension.model.command.GetAllBookmarksResponse;

import java.io.IOException;

public class GetAllBookmarksCommand extends Command<CommandRequest> {
    private static final Logger logger = Logger.getInstance(GetAllBookmarksCommand.class);
    public static String COMMAND_NAME = "GetAllBookmarks";
    private GetAllBookmarksResponse response;

    public GetAllBookmarksCommand() {
        this(new CommandRequest(COMMAND_NAME));
    }

    public GetAllBookmarksCommand(CommandRequest request) {
        super(request);
    }


    public void process(CommandSender commandSender) throws IOException {
        logger.debug("Processing get bookmarks request");
        this.response = commandSender.sendMessage(getCommandRequest(), GetAllBookmarksResponse.class).orElseThrow(() -> new RuntimeException("Response is empty for GetAllBookmarks"));


        processResponse(commandSender);
    }

    protected void processResponse(CommandSender commandSender) {
        logger.debug("Processing get bookmarks response");
        GetAllBookmarksResponse.Bookmark[] udbBookmarks = this.response.getBookmarks();
        if (udbBookmarks != null) {

//       Timeline timeline = TimelineManager.getTimeline(commandSender
//           .getDebugProcess().getSession().getSessionName());


//            List<InsidiousBookmark> collect = (List<InsidiousBookmark>) Arrays.stream(udbBookmarks).map(udbBookmark -> timeline.findBookmark(udbBookmark.getName())).filter(Objects::nonNull).sorted().collect(Collectors.toList());
//            if (collect.isEmpty()) {
//                logger.debug("No bookmarks found in debuggee.");
//            } else {
//                timeline.setBookmarkInfo(collect);
//            }
        }
    }
}


