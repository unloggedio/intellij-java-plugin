package com.insidious.plugin.auto.autoCIUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.UsageInsightTracker;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class AgentClientLite {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.of(5, ChronoUnit.MINUTES))
            .readTimeout(Duration.of(5, ChronoUnit.MINUTES))
            .connectTimeout(Duration.of(500, ChronoUnit.MILLIS)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final String agentUrl = "http://localhost:12100";
    public static final String NO_SERVER_CONNECT_ERROR_MESSAGE = "Failed to invoke call to agent server: \n" +
            "Make sure the process is running with java unlogged-sdk\n\n";


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
                    new TypeReference<AgentCommandResponse<String>>() {
                    });
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
            UsageInsightTracker.getInstance().RecordEvent("AGENT_RESPONSE_THROW", properties);

            AgentCommandResponse<String> agentCommandResponse = new AgentCommandResponse<String>(ResponseType.FAILED);
            agentCommandResponse.setMessage(NO_SERVER_CONNECT_ERROR_MESSAGE + e.getMessage());
            return agentCommandResponse;
        }
    }

    public boolean isConnected() {
        Request pingRequest = new Request.Builder()
                .url(agentUrl + "/ping")
                .build();
        try {
            Response response = client.newCall(pingRequest).execute();
            String responseBody = response.body().string();
            System.out.println("Ping response : " + responseBody);
            response.close();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
