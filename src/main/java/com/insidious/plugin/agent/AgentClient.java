package com.insidious.plugin.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;

public class AgentClient {
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
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
        }

    }
}
