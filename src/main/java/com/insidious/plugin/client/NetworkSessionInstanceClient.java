package com.insidious.plugin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class NetworkSessionInstanceClient implements SessionInstanceInterface {

    private final Logger logger = LoggerUtil.getInstance(NetworkSessionInstanceClient.class);

	// client attributes
    private String endpoint;
    private String token;
    private OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	// endpoint attributes
	private String isScanEnableEndpoint = "/isScanEnable";

	// session instance attributes
    private String sessionId = "0";
	private boolean scanEnable;

    public NetworkSessionInstanceClient(String endpoint) {
        this.endpoint = endpoint;
        this.client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
    }

    private void get(String url, Callback callback) {
        Request.Builder builder = new Request.Builder()
                .url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder = builder.addHeader("Authorization", "Bearer " + token);
        }
        Request request = builder.build();
        Call call = client.newCall(request);
        call.enqueue(callback);

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            String dots = "";
            while (true) {
                try {
                    Thread.sleep(500);
                    if (call.isExecuted()) {
                        break;
                    }

                    dots = dots + ".";
                    if (dots.length() > 3) {
                        dots = ".";
                    }
                    ProgressIndicatorProvider.getGlobalProgressIndicator()
                            .setText2("Query is in progress " + dots);
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                            .isCanceled()) {
                        throw new ProcessCanceledException();
                    }

                } catch (InterruptedException e) {
                    throw new ProcessCanceledException(e);
                }
            }
        }

    }

	private void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, JSON);

        Request.Builder builder = new Request.Builder();

        builder.url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);
        Request request = builder.build();
        client.newCall(request)
                .enqueue(callback);
    }

    private Response postSync(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, JSON);

        Request.Builder builder = new Request.Builder();

        builder.url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);

        Request request = builder.build();

        return client.newCall(request)
                .execute();
    }

    @Override
    public boolean isScanEnable() {
        String url = this.endpoint + this.isScanEnableEndpoint + "?sessionId=" + this.sessionId;
        CountDownLatch latch = new CountDownLatch(1);

        get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.info("failure encountered");
                latch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                try {
					ObjectMapper objectMapper = new ObjectMapper();
					String responseBody = Objects.requireNonNull(response.body()).string();
					Map<String, Object> jsonVal = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                    scanEnable = (boolean) jsonVal.get("scanEnable");
                } finally {
                    response.close();
                    latch.countDown();
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        return scanEnable;
    }

}
