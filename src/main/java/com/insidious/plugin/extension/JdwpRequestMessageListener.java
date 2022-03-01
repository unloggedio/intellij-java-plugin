package com.insidious.plugin.extension;

import com.insidious.plugin.extension.jwdp.RequestMessage;

import java.util.EventListener;

public interface JdwpRequestMessageListener extends EventListener {
    void requestMessageReceived(RequestMessage paramRequestMessage);
}


