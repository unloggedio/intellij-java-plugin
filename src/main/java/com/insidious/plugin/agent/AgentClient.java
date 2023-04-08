package com.insidious.plugin.agent;

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
    private static final Logger logger = LoggerUtil.getInstance(AgentClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String agentUrl;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.of(300, ChronoUnit.MILLIS))
            .writeTimeout(Duration.of(100, ChronoUnit.MILLIS))
            .readTimeout(Duration.of(100, ChronoUnit.MILLIS))
            .connectTimeout(Duration.of(100, ChronoUnit.MILLIS)).build();
    private final Request pingRequest;
    private final ConnectionChecker connectionChecker;
    private final ConnectionStateListener connectionStateListener;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(1);

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

    public AgentCommandResponse executeCommand(AgentCommandRequest agentCommandRequest) throws IOException {

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(agentCommandRequest), JSON);
        Request request = new Request.Builder()
                .url(agentUrl + "/command")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, AgentCommandResponse.class);
        } catch (Exception e) {
            logger.warn("Failed to invoke call to agent server: " + e.getMessage());
            AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
            agentCommandResponse.setResponseType(ResponseType.FAILED);
            agentCommandResponse.setMessage("Failed to invoke call to agent server: " + e.getMessage());
            return agentCommandResponse;
        }

    }

    public AgentCommandResponse ping() {
        try {
            Response response = client.newCall(pingRequest).execute();
            String responseBody = response.body().string();
            response.close();
            return objectMapper.readValue(responseBody, AgentCommandResponse.class);
        } catch (Throwable e) {
            AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
            agentCommandResponse.setResponseType(ResponseType.FAILED);
            agentCommandResponse.setMessage("Failed to invoke call to agent server: " + e.getMessage());
            return agentCommandResponse;
        }
    }

    public boolean isConnected() {
        return connectionChecker.currentState;
    }

    public static class ConnectionChecker implements Runnable {

        private final AgentClient agentClient;
        private boolean currentState = false;

        public ConnectionChecker(AgentClient agentClient) {
            this.agentClient = agentClient;
        }

        @Override
        public void run() {
            while (true) {
                AgentCommandResponse response = agentClient.ping();
                boolean newState = response.getResponseType().equals(ResponseType.NORMAL);
                if (newState && !currentState) {
                    currentState = true;
                    agentClient.connectionStateListener.onConnectedToAgentServer();
                } else if (!newState && currentState) {
                    currentState = false;
                    agentClient.connectionStateListener.onDisconnectedFromAgentServer();
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
