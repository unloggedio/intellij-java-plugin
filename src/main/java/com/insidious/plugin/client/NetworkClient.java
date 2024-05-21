package com.insidious.plugin.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.SigninRequest;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.client.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.constants.SessionMode;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.upload.SourceModel;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NetworkClient implements VideobugClientInterface{

    private static final Logger logger = LoggerUtil.getInstance(VideobugClientInterface.class);

    private SourceModel sourceModel;
    private String packageName;
    private SessionInstanceInterface sessionInstance;

	public NetworkClient(SourceModel sourceModel) {
		this.sourceModel = sourceModel;
	}

    @Override
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public ExecutionSession getCurrentSession() {
        return this.sessionInstance.getExecutionSession();
    }

    @Override
    public void setSessionInstance(SessionInstanceInterface sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    @Override
    public void signup(String serverUrl, String username, String password, SignUpCallback callback) {

    }

    @Override
    public void signin(SigninRequest signinRequest, SignInCallback signInCallback) {

    }

    @Override
    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {

    }

    @Override
    public ProjectItem fetchProjectByName(String projectName) throws ProjectDoesNotExistException, IOException {
        return null;
    }

    @Override
    public void createProject(String projectName, NewProjectCallback newProjectCallback) {

    }

    @Override
    public void getProjectToken(ProjectTokenCallback projectTokenCallback) {

    }

    @Override
    public void getProjectSessions(GetProjectSessionsCallback getProjectSessionsCallback) {
        List<ExecutionSession> listExecutionSession = this.sessionDiscovery();
        getProjectSessionsCallback.success(listExecutionSession);
    }

    @Override
    public DataResponse<ExecutionSession> fetchProjectSessions() throws APICallException, IOException {
        return null;
    }

    @Override
    public void queryTracePointsByEventType(SearchQuery searchQuery, String sessionId, ClientCallBack<TracePoint> tracePointsCallback) {

    }

    @Override
    public void queryTracePointsByTypes(SearchQuery classList, String sessionId, int historyDepth, ClientCallBack<TracePoint> getProjectSessionErrorsCallback) {

    }

    @Override
    public void queryTracePointsByValue(SearchQuery value, String sessionId, ClientCallBack<TracePoint> getProjectSessionErrorsCallback) {

    }

    @Override
    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) throws APICallException {
        return null;
    }

    @Override
    public String getToken() {
        return null;
    }

    @Override
    public ProjectItem getProject() {
        return null;
    }

    @Override
    public void setProject(String projectName) throws ProjectDoesNotExistException, IOException {

    }

    @Override
    public String getEndpoint() {
        return null;
    }

    @Override
    public void getAgentDownloadUrl(AgentDownloadUrlCallback agentDownloadUrlCallback) {

    }

    @Override
    public void downloadAgentFromUrl(String url, String insidiousLocalPath, boolean overwrite) {

    }

    @Override
    public void close() {

    }

    @Override
    public TypeInfo getTypeInfoByName(String sessionId, String type) {
        return this.sessionInstance.getTypeInfo(type);
    }

    @Override
    public SessionInstanceInterface getSessionInstance() {
        return this.sessionInstance;
    }

    @Override
    public ReplayData fetchObjectHistoryByObjectId(FilteredDataEventsRequest filteredDataEventsRequest) {
        ReplayData replayData = this.sessionInstance.fetchObjectHistoryByObjectId(filteredDataEventsRequest);
        replayData.setClient(this);
        return replayData;
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

    public List<ExecutionSession> sessionDiscovery(){

        List<ExecutionSession> executionSessionList = new ArrayList<>();
        if (this.sourceModel.getServerEndpoint() == "") {
            return executionSessionList;
        }

        String url = this.sourceModel.getServerEndpoint() + "/discovery" + "?packageName=" + this.packageName;
        logger.info("discovery call url = " + url);
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
