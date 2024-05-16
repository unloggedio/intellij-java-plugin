package com.insidious.plugin.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.callbacks.GetProjectSessionsCallback;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.SessionMode;
import com.insidious.plugin.upload.SourceModel;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.DumbService;
import io.netty.util.concurrent.DefaultThreadFactory;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SessionLoader implements Runnable, GetProjectSessionsCallback, Disposable {


    private static final Logger logger = LoggerUtil.getInstance(SessionLoader.class);
    private final ExecutorService ourPool;
    private final List<GetProjectSessionsCallback> listeners = new ArrayList<>();
    private VideobugClientInterface client;
    private List<ExecutionSession> lastResult;

    public SessionLoader() {
        ourPool = Executors.newFixedThreadPool(1, new DefaultThreadFactory("UnloggedAppThreadPool"));
        ourPool.submit(this);
    }

    @Override
    public void error(String message) {
        logger.warn("Failed to get sessions: " + message);
    }

    @Override
    public void success(List<ExecutionSession> executionSessionList) {
        lastResult = executionSessionList;
        for (GetProjectSessionsCallback listener : listeners) {
            listener.success(executionSessionList);
        }

    }


    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(3000);
                if (client == null) {
                    continue;
                }
                logger.debug("Check for new sessions");
                client.getProjectSessions(this);
            } catch (InterruptedException ie) {
                logger.warn("Session loader interrupted: " + ie.getMessage());
                break;
            }
        }
    }

    public void addSessionCallbackListener(GetProjectSessionsCallback getProjectSessionsCallback, SourceModel sourceModel, String packageName) {
		
		// ping network for discovery
    	// TODO: filter logic: hostname, packageName, date start and end
		List<ExecutionSession> listExecutionSession = sessionDiscovery(sourceModel, packageName);
		lastResult.addAll(listExecutionSession);

        this.listeners.add(getProjectSessionsCallback);
        if (lastResult != null) {
            getProjectSessionsCallback.success(lastResult);
        }
    }

    public void setClient(VideobugClientInterface client) {
        this.client = client;
    }

    public void removeListener(GetProjectSessionsCallback getProjectSessionsCallback) {
        this.listeners.remove(getProjectSessionsCallback);
    }

    @Override
    public void dispose() {
        ourPool.shutdown();
    }

	private void get(String url, Callback callback) {
		
		final OkHttpClient httpClient = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        
		Request.Builder builder = new Request.Builder().url(url);
        Request request = builder.build();
        Call call = httpClient.newCall(request);
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

	public List<ExecutionSession> sessionDiscovery(SourceModel sourceModel, String packageName){
		
		List<ExecutionSession> executionSessionList = new ArrayList<>();
		if (sourceModel.getServerEndpoint() == "") {
			return executionSessionList;
		}

		String url = sourceModel.getServerEndpoint() + "/discovery" + "?packageName=" + packageName;
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
					ExecutionSession[] executionSessionLocal = objectMapper.readValue(responseBody, ExecutionSession[].class);
					for (int i=0;i<=executionSessionLocal.length-1;i++) {
						executionSessionLocal[i].setSessionMode(SessionMode.REMOTE);
						executionSessionList.add(executionSessionLocal[i]);
					}
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

        return executionSessionList;
	}
}
