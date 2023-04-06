package com.insidious.plugin.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.*;

import java.io.IOException;

public class AgentClient {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Logger logger = LoggerUtil.getInstance(AgentClient.class);
    private final String agentUrl;
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentClient(String baseUrl) {
        this.agentUrl = baseUrl;
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
            logger.error("Failed to invoke call to agent server: " + e.getMessage());
            AgentCommandResponse agentCommandResponse = new AgentCommandResponse();
            agentCommandResponse.setResponseType(ResponseType.FAILED);
            agentCommandResponse.setMessage("Failed to invoke call to agent server: " + e.getMessage());
            return agentCommandResponse;
        }

    }
}
