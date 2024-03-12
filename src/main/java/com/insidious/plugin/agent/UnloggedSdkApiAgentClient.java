package com.insidious.plugin.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class UnloggedSdkApiAgentClient {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final String NO_SERVER_CONNECT_ERROR_MESSAGE = "Failed to invoke call to agent server: \n" +
            "Make sure the process is running with java unlogged-sdk\n\n";
    private static final Logger logger = LoggerUtil.getInstance(UnloggedSdkApiAgentClient.class);
    private final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final String agentUrl;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.of(5, ChronoUnit.MINUTES))
            .readTimeout(Duration.of(5, ChronoUnit.MINUTES))
            .connectTimeout(Duration.of(500, ChronoUnit.MILLIS)).build();
    private final Request pingRequest;

    public UnloggedSdkApiAgentClient(String baseUrl) {
        this.agentUrl = baseUrl;
        pingRequest = new Request.Builder()
                .url(agentUrl + "/ping")
                .build();
    }

    public AgentCommandResponse<String> executeCommand(AgentCommandRequest agentCommandRequest) throws IOException {
        agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);

        RequestBody body = RequestBody.create(objectMapper.writeValueAsString(agentCommandRequest), JSON);
        Request request = new Request.Builder()
                .url(agentUrl + "/command")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            AgentCommandResponse<String> agentCommandResponse = objectMapper.readValue(responseBody,
                    new TypeReference<>() {});
            JSONObject eventProperties = new JSONObject();
            if (
                    agentCommandResponse.getResponseType().equals(ResponseType.EXCEPTION) ||
                            agentCommandResponse.getResponseType().equals(ResponseType.FAILED)

            ) {
                eventProperties.put("response", agentCommandResponse.getMethodReturnValue());
                eventProperties.put("responseClass", agentCommandResponse.getResponseClassName());
            }
            UsageInsightTracker.getInstance().RecordEvent(
                    agentCommandRequest.getCommand() + "_AGENT_RESPONSE_" + agentCommandResponse.getResponseType(),
                    eventProperties);
            return agentCommandResponse;
        } catch (Throwable e) {
            JSONObject properties = new JSONObject();
            properties.put("exception", e.getClass());
            properties.put("message", e.getMessage());
            properties.put("stacktrace", objectMapper.writeValueAsString(e.getStackTrace()));
            UsageInsightTracker.getInstance().RecordEvent("AGENT_RESPONSE_THROW", properties);

            logger.warn("Failed to invoke call to agent server: " + e.getMessage());
            AgentCommandResponse<String> agentCommandResponse = new AgentCommandResponse<String>(ResponseType.FAILED);
            agentCommandResponse.setMessage(NO_SERVER_CONNECT_ERROR_MESSAGE + e.getMessage());
            return agentCommandResponse;
        }

    }

    public AgentCommandResponse<ServerMetadata> ping() {
        try {
            Response response = client.newCall(pingRequest).execute();
            assert response.body() != null;
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

}
