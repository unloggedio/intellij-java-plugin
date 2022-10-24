package com.insidious.plugin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.Constants;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.pojo.*;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.client.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.client.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VideobugNetworkClient implements VideobugClientInterface {
    public static final String SIGN_IN_URL = "/api/auth/signin";
    public static final String SIGN_UP_URL = "/api/auth/signup";
    public static final String PROJECTS_URL = "/api/data/projects";
    public static final String PROJECT_URL = "/api/data/project";
    public static final String PROJECT_EXECUTIONS_URL = "/executions";
    public static final String FILTER_DATA_EVENTS_URL = "/filterDataEvents";
    public static final String TRACE_BY_EXCEPTION = "/traceByException";
    public static final String TRACE_BY_STRING = "/traceByString";
    public static final String GENERATE_PROJECT_TOKEN_URL = "/api/auth/generateAgentToken";
    private final Logger logger = LoggerUtil.getInstance(VideobugNetworkClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TypeReference<DataResponse<DataEventWithSessionId>> typeReference = new TypeReference<>() {
    };
    OkHttpClient client;
    private String endpoint;
    private ProjectItem project;
    private String token;
    private ExecutionSession session;
    private List<ExecutionSession> sessionList;
    private SessionInstance sessionInstance;

    public VideobugNetworkClient(String endpoint) {
        this.endpoint = endpoint;
        if (this.endpoint.endsWith("/")) {
            this.endpoint = this.endpoint.substring(0, this.endpoint.length() - 1);
        }
        client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();

    }


    @Override
    public ExecutionSession getCurrentSession() {
        return session;
    }

    @Override
    public void setSessionInstance(SessionInstance sessionInstance) {
        this.session = sessionInstance.getExecutionSession();
        this.sessionInstance = sessionInstance;
    }

    @Override
    public void signup(String serverUrl, String username, String password, SignUpCallback callback) {
        logger.info("Sign up for email => " + username);
        post(serverUrl + SIGN_UP_URL, String.format(
                "{\"email\":\"%s\", \"password\":\"%s\"}", username, password
        ), new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (response) {
                    if (response.code() == 200) {
                        callback.success();
                    } else {
                        callback.error(Objects.requireNonNull(response.body()).string());
                    }
                }
            }
        });
    }

    @Override
    public void signin(SigninRequest signinRequest, SignInCallback signInCallback) {
        logger.info("Sign in for email => " + signinRequest.getEmail());
        post(signinRequest.getEndpoint() + SIGN_IN_URL,
                String.format(
                        "{\"email\":\"%s\", \"password\":\"%s\"}", signinRequest.getEmail(), signinRequest.getPassword()
                ), new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        signInCallback.error(e.getMessage());
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        try (response) {
                            if (response.code() == 401) {
                                signInCallback.error("invalid credentials");
                                return;
                            }
                            if (response.code() == 200) {

                                JSONObject jsonObject = new JSONObject(
                                        Objects.requireNonNull(response.body()).string());
                                VideobugNetworkClient.this.token = jsonObject.getString(Constants.TOKEN);
                                VideobugNetworkClient.this.endpoint = signinRequest.getEndpoint();
                                signInCallback.success(token);
                            }
                        }
                    }
                });


    }


    @Override
    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {
        logger.info("Get project by name => " + projectName);

        get(Constants.PROJECT_URL + "?name=" + projectName, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                getProjectCallback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                try (response) {
                    JSONObject jsonProjects = new JSONObject(
                            Objects.requireNonNull(response.body()).string());
                    JSONArray jsonArray = jsonProjects.getJSONArray("items");
                    if (jsonArray.length() == 0) {
                        getProjectCallback.noSuchProject();
                    } else {
                        JSONObject projectIdJson = jsonArray.getJSONObject(0);
                        String project_id = projectIdJson.getString("id");
                        getProjectCallback.success(project_id);
                    }
                }

            }
        });
    }

    @Override
    public ProjectItem fetchProjectByName(String projectName) throws ProjectDoesNotExistException, UnauthorizedException, IOException {
        logger.info("Get project by name => " + projectName);
        TypeReference<DataResponse<ProjectItem>> typeReference = new TypeReference<>() {
        };
        DataResponse<ProjectItem> projectList = get(Constants.PROJECT_URL + "?name=" + projectName, typeReference);

        if (projectList.getItems().size() == 0) {
            throw new ProjectDoesNotExistException(projectName);
        }

        return projectList.getItems().get(0);

    }

    @Override
    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        logger.info("create project on server - [" + projectName + "]");
        post(PROJECT_URL + "?name=" + projectName, "", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("failed to create project", e);
                newProjectCallback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (response) {
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    logger.info("create project successful response - " + responseBody);
                    JSONObject jsonObject = new JSONObject(responseBody);
                    String projectId = jsonObject.getString("id");
                    newProjectCallback.success(projectId);
                }
            }
        });
    }

    @Override
    public void getProjectToken(ProjectTokenCallback projectTokenCallback) {
        logger.info("get project token - " + project.getId());
        post(GENERATE_PROJECT_TOKEN_URL + "?projectId=" + project.getId(), "", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("failed to generate project token", e);
                projectTokenCallback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseBody = Objects.requireNonNull(response.body()).string();
                logger.info("project token response - " + responseBody);
                JSONObject jsonObject = new JSONObject(responseBody);
                projectTokenCallback.success(jsonObject.getString(Constants.TOKEN));
            }
        });
    }

    private void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request.Builder builder = new Request.Builder();

        builder.url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);
        Request request = builder.build();
        client.newCall(request).enqueue(callback);
    }

    private Response postSync(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request.Builder builder = new Request.Builder();

        builder.url(url.startsWith("http") ? url : endpoint + url);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);

        Request request = builder.build();

        return client.newCall(request).execute();
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
                    ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Query is in progress " + dots);
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        throw new ProcessCanceledException();
                    }

                } catch (InterruptedException e) {
                    throw new ProcessCanceledException(e);
                }
            }
        }

    }

    private <T> T get(String url, TypeReference<T> typeReference) throws IOException, UnauthorizedException {
        Request request = new Request.Builder()
                .url(url.startsWith("http") ? url : endpoint + url)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        Response response;
        response = client.newCall(request).execute();

        if (response.code() == 401) {
            throw new UnauthorizedException();
        }

        T result = null;
        result = objectMapper.readValue(Objects.requireNonNull(response.body()).string(), typeReference);

        return result;
    }

    @Override
    public DataResponse<ExecutionSession> fetchProjectSessions() throws APICallException, IOException {
        String executionsUrl = PROJECT_URL + "/" + this.project.getId() + PROJECT_EXECUTIONS_URL;
        TypeReference<DataResponse<ExecutionSession>> typeReference = new TypeReference<>() {
        };

        DataResponse<ExecutionSession> sessionDataResponse = get(executionsUrl, typeReference);
        this.sessionList = sessionDataResponse.getItems();
        return sessionDataResponse;
    }

    @Override
    public @NotNull List<TracePoint> getTracePoints(DataResponse<DataEventWithSessionId> traceResponse) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ClassWeaveInfo getSessionClassWeave(String sessionId) {
        throw new RuntimeException("not implemented");
    }


    @Override
    public void getProjectSessions(GetProjectSessionsCallback getProjectSessionsCallback) {
        logger.info("get project sessions - " + this.project.getId());
        String executionsUrl = PROJECT_URL + "/" + this.project.getId() + PROJECT_EXECUTIONS_URL;
        get(executionsUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("failed to get project sessions", e);
                getProjectSessionsCallback.error("ioexception");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (response) {
                    if (response.body() == null) {
                        logger.error("get project session response is empty");
                        getProjectSessionsCallback.error("Failed to read response from server");
                        return;
                    }
                    if (response.code() == 401) {
                        logger.error("get project session call unauthorized project id [{}] with token [{}]", project.getId(), token);
                        getProjectSessionsCallback.error("Unauthorized to get sessions for this project");
                        return;
                    }
                    if (response.code() == 400) {
                        logger.error("get project session call response code 400 [{}] with token [{}]", project.getId(), token);
                        getProjectSessionsCallback.error("Failed to connect with server to list sessions");
                        return;
                    }
                    TypeReference<DataResponse<ExecutionSession>> typeReference = new TypeReference<>() {
                    };
                    String responseBody = Objects.requireNonNull(response.body()).string();
                    DataResponse<ExecutionSession> sessionList = objectMapper.readValue(responseBody, typeReference);
                    logger.info("got [" + sessionList.getItems().size() + "] sessions for project" +
                            "[" + project.getId() + "] - " + responseBody);
                    getProjectSessionsCallback.success(sessionList.getItems());
                }
            }
        });
    }

    @Override
    public void queryTracePointsByTypes(
            SearchQuery searchQuery,
            String sessionId, int depth,
            ClientCallBack<TracePoint> getProjectSessionErrorsCallback
    ) {

        String url = PROJECT_URL
                + "/" + this.project.getId()
                + TRACE_BY_EXCEPTION
                + "/" + sessionId
                + "?exceptionClass="
                + searchQuery.getQuery()
                + "&pageNumber=" + 0
                + "&pageSize=500";


        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Querying server for events by class type");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
        }


        get(url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                ExceptionResponse errorResponse = new ExceptionResponse();
                errorResponse.setMessage(e.getMessage());
                getProjectSessionErrorsCallback.error(errorResponse);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                try (response) {
                    if (response.code() != 200) {
                        ExceptionResponse errorResponse = objectMapper.readValue(
                                Objects.requireNonNull(response.body()).string(), ExceptionResponse.class);
                        getProjectSessionErrorsCallback.error(errorResponse);
                        return;
                    }

                    String responseBodyString = Objects.requireNonNull(response.body()).string();

                    DataResponse<DataEventWithSessionId> traceResponse =
                            objectMapper.readValue(responseBodyString, typeReference);


                    if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                        ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(
                                "Mapping " + traceResponse.getItems().size() + " trace points across "
                                        + traceResponse.getMetadata().getClassInfo().size() + " classes."
                        );
                        if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                            throw new ProcessCanceledException();
                        }
                    }


                    List<TracePoint> tracePoints = getTracePoints(traceResponse);

                    tracePoints.forEach(e -> e.setExecutionSession(session));
                    getProjectSessionErrorsCallback.success(tracePoints);
                } finally {
                    getProjectSessionErrorsCallback.completed();
                }

            }
        });

    }

    @Override
    public void queryTracePointsByValue(SearchQuery searchQuery,
                                        String sessionId,
                                        ClientCallBack<TracePoint> getProjectSessionErrorsCallback) {

        String url = PROJECT_URL
                + "/" + this.project.getId()
                + TRACE_BY_STRING
                + "/" + sessionId
                + "?traceValue="
                + searchQuery.getQuery()
                + "&pageNumber=" + 0
                + "&pageSize=500";


        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .setText("Querying server for events by value [" + searchQuery.getQuery() + "]");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
        }


        get(url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                ExceptionResponse errorResponse = new ExceptionResponse();
                errorResponse.setMessage(e.getMessage());
                getProjectSessionErrorsCallback.error(errorResponse);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                try (response) {


                    String responseBodyString = Objects.requireNonNull(response.body()).string();
                    if (response.code() != 200) {
                        ExceptionResponse errorResponse =
                                objectMapper.readValue(responseBodyString, ExceptionResponse.class);
                        getProjectSessionErrorsCallback.error(errorResponse);
                        return;
                    }

                    TypeReference<DataResponse<DataEventWithSessionId>> typeReference = new TypeReference<>() {
                    };

                    DataResponse<DataEventWithSessionId> traceResponse = objectMapper.readValue(
                            responseBodyString, typeReference);

                    List<TracePoint> tracePoints = getTracePoints(traceResponse);
                    tracePoints.forEach(e -> e.setExecutionSession(session));
                    getProjectSessionErrorsCallback.success(tracePoints);
                } finally {
                    getProjectSessionErrorsCallback.completed();
                }

            }
        });

    }

    @Override
    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) throws APICallException {
        return null;
        // this needs to be done
//        String url = PROJECT_URL + "/" + project.getId() + FILTER_DATA_EVENTS_URL;
//        try {
//            logger.info("url to fetch data events => [" + endpoint + "] with [" +
//                    objectMapper.writeValueAsString(filteredDataEventsRequest) + "]");
//        } catch (JsonProcessingException e) {
//            throw new APICallException("failed to log request as json", e);
//        }
//        String responseBodyString;
//        try (Response response = postSync(url, objectMapper.writeValueAsString(filteredDataEventsRequest))) {
//            responseBodyString = Objects.requireNonNull(response.body()).string();
//            if (response.code() != 200) {
//                logger.error("error response from filterDataEvents  [" + response.code() + "] - " + responseBodyString);
//                JSONObject jsonResponse = new JSONObject(responseBodyString);
//                throw new APICallException(jsonResponse.getString("message"));
//            }
//        } catch (IOException e) {
//            throw new APICallException("failed to complete request", e);
//        }
//
//        TypeReference<DataResponse<DataEventStream>> typeReference = new TypeReference<>() {
//        };
////
//        DataResponse<DataEventStream> dataResponse = null;
//        try {
//            dataResponse = objectMapper.readValue(responseBodyString, typeReference);
//        } catch (JsonProcessingException e) {
//            throw new APICallException("failed to read response as json", e);
//        }
//
//        DataEventStream responseStream = dataResponse.getItems().get(0);
//
//        int eventCount = responseStream.getStream().length / (8 + 4 + 8);
//
//        List<DataEventWithSessionId> dataEventsList = new ArrayList<>(eventCount);
//
//        ByteBuffer streamReader = ByteBuffer.wrap(responseStream.getStream());
//
//        while (streamReader.hasRemaining()) {
//            long timestamp = streamReader.getLong();
//            int dataId = streamReader.getInt();
//            long valueId = streamReader.getLong();
//            DataEventWithSessionId event = new DataEventWithSessionId();
//            event.setNanoTime(timestamp);
//            event.setDataId(dataId);
//            event.setValue(valueId);
//            event.setSessionId(session.getSessionId());
//            event.setThreadId(filteredDataEventsRequest.getThreadId());
//
//
//            dataEventsList.add(event);
//
//
//        }
//
//        ResponseMetadata metadata = dataResponse.getMetadata();
//        Map<String, ClassInfo> classInfo = metadata.getClassInfo();
//        Map<String, DataInfo> dataInfo = metadata.getDataInfo();
//
//        Map<String, StringInfo> stringInfo = metadata.getStringInfo();
//        Map<String, ObjectInfo> objectInfo = metadata.getObjectInfo();
//        Map<String, TypeInfo> typeInfo = metadata.getTypeInfo();
//
//        Map<String, MethodInfo> methodInfoMap = new HashMap<>();
//        return new ReplayData(this, filteredDataEventsRequest, dataEventsList, classInfo, dataInfo, stringInfo,
//                objectInfo, typeInfo, methodInfoMap);

    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public ProjectItem getProject() {
        return this.project;
    }

    @Override
    public void setProject(String projectName) throws ProjectDoesNotExistException, UnauthorizedException, IOException {
        this.project = fetchProjectByName(projectName);
    }

    @Override
    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public void getAgentDownloadUrl(AgentDownloadUrlCallback agentDownloadUrlCallback) {
        get("https://cloud.bug.video" + "/api/data/java-agent-jar-link", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                agentDownloadUrlCallback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                agentDownloadUrlCallback.success(Objects.requireNonNull(response.body()).string());
            }
        });
    }

    @Override
    public void downloadAgentFromUrl(String url, String insidiousLocalPath, boolean overwrite) {
        Path fileURiString = Path.of(insidiousLocalPath);

        String absolutePath = fileURiString.toAbsolutePath().toString();
        logger.info("Downloading agent to path - " + absolutePath);

        File agentFile = new File(absolutePath);

        if (agentFile.exists() && !overwrite) {
            logger.info("java agent already exists at the path");
            return;
        }

        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOS = new FileOutputStream(absolutePath)) {
            byte[] data = new byte[1024];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                fileOS.write(data, 0, byteContent);
            }
        } catch (Exception e) {
            logger.error("failed to download java agent", e);
            // handles IO exceptions
        }


    }

    @Override
    public void close() {

    }

    @Override
    public void onNewException(Collection<String> typeNameList, VideobugExceptionCallback videobugExceptionCallback) {
//        throw new RuntimeException("not implemented");
        // todo
    }


    @Override
    public void queryTracePointsByEventType(SearchQuery searchQuery,
                                            String sessionid,
                                            ClientCallBack<TracePoint> tracePointsCallback) {

    }

    @Override
    public ReplayData fetchObjectHistoryByObjectId(FilteredDataEventsRequest filteredDataEventsRequest) {
        return null;
    }

    @Override
    public void getMethods(String sessionId, Integer typeId, ClientCallBack<TestCandidate> tracePointsCallback) {

    }

    @Override
    public void getObjectsByType(SearchQuery searchQuery, String sessionId, ClientCallBack<ObjectWithTypeInfo> clientCallBack) {

    }

    @Override
    public List<String> getSessionArchiveList(String sessionId) {
        return List.of();
    }


    @Override
    public TypeInfo getTypeInfoByName(String sessionId, String type) {
        throw new RuntimeException("not implemented yet ");
    }

    @Override
    public List<TypeInfoDocument> getAllTypes(String sessionId) {
        throw new RuntimeException("not implemented yet ");
    }

    @Override
    public SessionInstance getSessionInstance() {
        return null;
    }

    @Override
    public TestCaseService getSessionTestCaseService() {
        return null;
    }


}
