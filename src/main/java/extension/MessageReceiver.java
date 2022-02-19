package extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import extension.jwdp.RequestMessage;
import extension.jwdp.event.*;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class MessageReceiver
        extends Thread {
    private static final Logger logger = Logger.getInstance(MessageReceiver.class);
    private static final int SLEEP_INTERVAL = 2000;
    private Socket socket;
    private DataInputStream in;
    private CommandSender commandSender;
    private boolean running = true;

    public MessageReceiver(CommandSender commandSender) {
        try {
            this.commandSender = commandSender;
            setDaemon(true);
            setName(getClass().getSimpleName());

            RemoteConnection remoteConnection = commandSender.getDebugProcess().getRemoteConnection();
            logger.info(
                    String.format("Connect to %s", remoteConnection.getHostName()));
            this.socket = new Socket(remoteConnection.getHostName(), 8080);
            OutputStream out = this.socket.getOutputStream();
            this.in = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
            out.write("BridgeReceiver".getBytes());
            out.flush();
            logger.info("handshake written");
        } catch (Exception e) {
            logger.error("Could not create socket", e);
        }
    }


    public void run() {
        try {
            logger.info("in run");
            ObjectMapper objectMapper = new ObjectMapper();
            while (this.running) {
                String str = this.in.readUTF();
                JsonNode node = objectMapper.readTree(str);
                if (node == null || node.isNull() || node.isMissingNode()) {
                    logger.info("input closed");
                    break;
                }
                processEvent(node);
            }
        } catch (SocketException e) {
            logger.info("socket closed");
        } catch (Exception e) {
            logger.warn("error reading json command", e);
        }
    }

    private void processEvent(JsonNode node) throws Exception {
        GotoCompleteEvent gotoCompleteEvent = null;
        String eventName = node.get("eventName").asText();
        RequestMessage event = null;
        if (MessageEvent.EVENT_NAME.equals(eventName)) {
            MessageEvent messageEvent = new MessageEvent(node.get("message").asText(), node.get("type").asText());
        } else if (LogMessageEvent.EVENT_NAME.equals(eventName)) {


            LogMessageEvent logMessageEvent = new LogMessageEvent(node.get("message").asText(), node.get("threadName").asText(), node.get("loggerName").asText(), node.get("timeInMillis").asLong());
        } else if (VMPausedEvent.EVENT_NAME.equals(eventName)) {
            VMPausedEvent vMPausedEvent = new VMPausedEvent(node.get("pc").asLong());
        } else if (RewoundEvent.EVENT_NAME.equals(eventName)) {
            RewoundEvent rewoundEvent = new RewoundEvent(node.get("threadId").asInt());
        } else if (SafeBoundaryEvent.EVENT_NAME.equals(eventName)) {
            SafeBoundaryEvent safeBoundaryEvent = new SafeBoundaryEvent(node.get("type").asText(), node.get("bbcount").asLong());
        } else if (FastForwardEvent.EVENT_NAME.equals(eventName)) {
            FastForwardEvent fastForwardEvent = new FastForwardEvent(node.get("threadId").asInt());
        } else if ("GotoComplete".equals(eventName)) {


            gotoCompleteEvent = new GotoCompleteEvent(node.get("action").asText(), node.get("threadId").asInt());
        } else {
            logger.error("Unrecognized event name: " + eventName);
            return;
        }
        this.commandSender.getDebugProcess().requestMessageReceived(gotoCompleteEvent);
        gotoCompleteEvent.process(this.commandSender);
    }

    private void close() {
        try {
            this.running = false;
            this.socket.close();
            logger.info("socket closed");
        } catch (Exception e) {
            logger.error("Unexpected exception", e);
        }
    }

    public void closeWithDelay() {
        (new Thread(() -> {

            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                logger.debug(e);
            }

            close();
        })).start();
    }
}


