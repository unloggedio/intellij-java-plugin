package extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.StringReference;
import extension.jwdp.RequestMessage;
import extension.model.command.CommandRequest;

import java.io.*;
import java.net.Socket;
import java.util.Optional;

public class CommandSender {
    private static final Logger logger = Logger.getInstance(CommandSender.class);

    private InsidiousJavaDebugProcess debugProcess;
    private MessageReceiver pollerThread;
    private int lastThreadId;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final Socket socket;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CommandSender(RemoteConnection remoteConnection) throws IOException {
        logger.info("connect to " + remoteConnection
                .getHostName() + ":" + remoteConnection.getPort());
        this.socket = new Socket(remoteConnection.getHostName(), remoteConnection.getPort());
        this.in = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        OutputStream os = this.socket.getOutputStream();
        os.write("BridgeSender".getBytes());
        os.flush();
        logger.info("handshake written");
        this.out = new DataOutputStream(new BufferedOutputStream(os));
        logger.debug("Sleep for 200ms to allow the bridge message thread to start");
        try {
            Thread.sleep(200L);
        } catch (InterruptedException interruptedException) {
        }
    }

    public void sendDirectMessage(CommandRequest request) throws IOException {
        String jsonStr = this.objectMapper.writeValueAsString(request);
        this.out.writeUTF(jsonStr);
        this.out.flush();
    }

    public <T> Optional<T> sendMessage(CommandRequest request, Class<T> responseClass) throws IOException {
        T result = null;
        String jsonStr = this.objectMapper.writeValueAsString(request);
        logger.info("Sending message to bridge:" + jsonStr);
        StringReference strRef = this.debugProcess.getConnector().createString(jsonStr);
        Optional<String> resultStr = getStringValue(strRef);
        if (resultStr.isPresent()) {
            result = RequestMessage.createMessage(resultStr.get(), responseClass);
        }
        return Optional.ofNullable(result);
    }

    public Optional<String> sendMessage(CommandRequest request) throws IOException {
        String jsonStr = this.objectMapper.writeValueAsString(request);
        logger.info("Sending message to bridge:" + jsonStr);
        StringReference strRef = this.debugProcess.getConnector().createString(jsonStr);
        return getStringValue(strRef);
    }

    private Optional<String> getStringValue(StringReference strRef) {
        String result = null;
        if (strRef != null && strRef.uniqueID() > 0L) {
            strRef.disableCollection();
            result = strRef.value();
            logger.info("Response from bridge: " + result);
            strRef.enableCollection();
        }
        return Optional.ofNullable(result);
    }

    public void stopThreads() {
        if (this.pollerThread != null) {
            this.pollerThread.closeWithDelay();
        }
    }

    public InsidiousJavaDebugProcess getDebugProcess() {
        return this.debugProcess;
    }

    public void setDebugProcess(InsidiousJavaDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
        this.pollerThread = new MessageReceiver(this);
        this.pollerThread.start();
        try {
            Thread.sleep(200L);
        } catch (InterruptedException interruptedException) {
        }
    }

    public int getLastThreadId() {
        return this.lastThreadId;
    }

    public void setLastThreadId(int lastThreadId) {
        this.lastThreadId = lastThreadId;
    }

    public InsidiousApplicationState getState() {
        return (this.debugProcess != null) ? this.debugProcess.getState() : null;
    }
}


