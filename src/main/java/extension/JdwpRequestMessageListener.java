package extension;

import extension.jwdp.RequestMessage;

import java.util.EventListener;

public interface JdwpRequestMessageListener extends EventListener {
    void requestMessageReceived(RequestMessage paramRequestMessage);
}


