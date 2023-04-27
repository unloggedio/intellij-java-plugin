package com.insidious.plugin.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentClient {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final String NO_SERVER_CONNECT_ERROR_MESSAGE = "Failed to invoke call to agent server: \n" +
            "Make sure the process is running with unlogged java agent\n\n";
    private static final Logger logger = LoggerUtil.getInstance(AgentClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String agentUrl;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.of(2, ChronoUnit.MINUTES))
            .connectTimeout(Duration.of(500, ChronoUnit.MILLIS)).build();
    private final Request pingRequest;
    private final ConnectionChecker connectionChecker;
    private final ConnectionStateListener connectionStateListener;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private ServerMetadata serverMetadata;

    public AgentClient(String baseUrl, ConnectionStateListener connectionStateListener) {
        this.agentUrl = baseUrl;
        pingRequest = new Request.Builder()
                .url(agentUrl + "/ping")
                .build();
        this.connectionStateListener = connectionStateListener;
        connectionChecker = new ConnectionChecker(this);
        threadPool.submit(connectionChecker);
    }

    public void close() {
        threadPool.shutdownNow();
    }

    public AgentCommandResponse<String> executeCommand(AgentCommandRequest agentCommandRequest) throws IOException {

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(agentCommandRequest), JSON);
        Request request = new Request.Builder()
                .url(agentUrl + "/command")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, new TypeReference<AgentCommandResponse<String>>() {
            });
        } catch (Throwable e) {
            logger.warn("Failed to invoke call to agent server: " + e.getMessage());
            AgentCommandResponse<String> agentCommandResponse = new AgentCommandResponse<String>(ResponseType.FAILED);
            agentCommandResponse.setMessage(NO_SERVER_CONNECT_ERROR_MESSAGE + e.getMessage());
            return agentCommandResponse;
        }

    }

    public AgentCommandResponse<ServerMetadata> ping() {
        try {
            Response response = client.newCall(pingRequest).execute();
            String responseBody = response.body().string();
            response.close();
            return objectMapper.readValue(responseBody, new TypeReference<AgentCommandResponse<ServerMetadata>>() {
            });
        } catch (Throwable e) {
            AgentCommandResponse<ServerMetadata> agentCommandResponse = new AgentCommandResponse<>(ResponseType.FAILED);
            agentCommandResponse.setMessage(NO_SERVER_CONNECT_ERROR_MESSAGE + e.getMessage());
            return agentCommandResponse;
        }
    }

    public boolean isConnected() {
        return connectionChecker.currentState;
    }

    public ServerMetadata getServerMetadata() {
        return serverMetadata;
    }

    public class ConnectionChecker implements Runnable {

        private final AgentClient agentClient;
        private boolean currentState = false;

        public ConnectionChecker(AgentClient agentClient) {
            this.agentClient = agentClient;
        }

        @Override
        public void run() {
            while (true) {
                AgentCommandResponse<ServerMetadata> response = agentClient.ping();
                boolean newState = response.getResponseType().equals(ResponseType.NORMAL);
                if (newState && !currentState) {
                    currentState = true;
                    AgentClient.this.serverMetadata = response.getMethodReturnValue();
                    connectionStateListener.onConnectedToAgentServer(AgentClient.this.serverMetadata);
                } else if (!newState && currentState) {
                    currentState = false;
                    connectionStateListener.onDisconnectedFromAgentServer();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
