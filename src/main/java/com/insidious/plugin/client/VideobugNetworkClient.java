package com.insidious.plugin.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.StringInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.actions.Constants;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.pojo.*;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.client.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.client.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.util.text.Strings;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
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
    OkHttpClient client;
    private String endpoint;
    private ProjectItem project;
    private String token;
    private ExecutionSession session;

    public VideobugNetworkClient(String endpoint) {
        this.endpoint = endpoint;
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
    public void setSession(ExecutionSession session) {
        this.session = session;
    }

    @Override
    public void signup(String serverUrl, String username, String password, SignUpCallback callback) {
        logger.info("Sign up for email => " + username);
        JSONObject json = new JSONObject();
        json.put("email", username);
        json.put("password", password);
        post(serverUrl + SIGN_UP_URL, json.toJSONString(), new Callback() {
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
    public void signin(SigninRequest signinRequest, SignInCallback signInCallback) throws UnauthorizedException {
        logger.info("Sign in for email => " + signinRequest.getEmail());
        JSONObject json = new JSONObject();
        json.put("email", signinRequest.getEmail());
        json.put("password", signinRequest.getPassword());

        post(signinRequest.getEndpoint() + SIGN_IN_URL, json.toJSONString(), new Callback() {
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
                        JSONObject jsonObject = (JSONObject) JSONValue.parse(Objects.requireNonNull(response.body()).string());
                        VideobugNetworkClient.this.token = jsonObject.getAsString(Constants.TOKEN);
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
                    JSONObject jsonProjects = (JSONObject) JSONValue.parse(Objects.requireNonNull(response.body()).string());
                    JSONArray jsonArray = (JSONArray) jsonProjects.get("items");
                    if (jsonArray.size() == 0) {
                        getProjectCallback.noSuchProject();
                    } else {
                        JSONObject projectIdJson = (JSONObject) jsonArray.get(0);
                        String project_id = projectIdJson.getAsString("id");
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
        logger.info("create project on server - [{}]", projectName);
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
                    logger.info("create project successful response - {}", responseBody);
                    JSONObject jsonObject = (JSONObject) JSONValue.parse(responseBody);
                    String projectId = jsonObject.getAsString("id");
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
                logger.info("project token response - {}", responseBody);
                JSONObject jsonObject = (JSONObject) JSONValue.parse(responseBody);
                projectTokenCallback.success(jsonObject.getAsString(Constants.TOKEN));
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
        Request request = new Request.Builder()
                .url(url.startsWith("http") ? url : endpoint + url)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);

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
                    logger.info("got [{}] sessions for project [{}] - {}", sessionList.getItems().size(), project.getId(), responseBody);
                    getProjectSessionsCallback.success(sessionList.getItems());
                }
            }
        });
    }

    @Override
    public DataResponse<ExecutionSession> fetchProjectSessions() throws APICallException, IOException {

        String executionsUrl = PROJECT_URL + "/" + this.project.getId() + PROJECT_EXECUTIONS_URL;
        TypeReference<DataResponse<ExecutionSession>> typeReference = new TypeReference<>() {
        };

        return get(executionsUrl, typeReference);
    }

    @Override
    public void getTracesByObjectType(Collection<String> classList, int depth, GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {

        String url = PROJECT_URL
                + "/" + this.project.getId()
                + TRACE_BY_EXCEPTION
                + "/" + this.session.getSessionId()
                + "?exceptionClass="
                + Strings.join(classList, ",")
                + "&pageNumber=" + 0
                + "&pageSize=500";
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

                    TypeReference<DataResponse<DataEventWithSessionId>> typeReference = new TypeReference<>() {
                    };

                    DataResponse<DataEventWithSessionId> traceResponse =
                            objectMapper.readValue(responseBodyString, typeReference);

                    List<TracePoint> tracePoints = getTracePoints(traceResponse);

                    getProjectSessionErrorsCallback.success(tracePoints);
                }
            }
        });

    }

    @Override
    public void getTracesByObjectValue(String value,
                                       GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {

        String url = PROJECT_URL
                + "/" + this.project.getId()
                + TRACE_BY_STRING
                + "/" + this.session.getSessionId()
                + "?traceValue="
                + value
                + "&pageNumber=" + 0
                + "&pageSize=500";
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
                    getProjectSessionErrorsCallback.success(tracePoints);
                }

            }
        });

    }

    @Override
    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) throws Exception {
        String url = PROJECT_URL + "/" + project.getId() + FILTER_DATA_EVENTS_URL;
        logger.info("url to fetch data events => [{}] with [{}]", endpoint,
                objectMapper.writeValueAsString(filteredDataEventsRequest));
        Response response = postSync(url, objectMapper.writeValueAsString(filteredDataEventsRequest));

        String responseBodyString = Objects.requireNonNull(response.body()).string();
        if (response.code() != 200) {
            logger.error("error response from filterDataEvents  [{}] - [{}]", response.code(), responseBodyString);
            ExceptionResponse errorResponse = JSONValue.parse(responseBodyString, ExceptionResponse.class);
            throw new Exception(errorResponse.getMessage());
        }

        TypeReference<DataResponse<DataEventStream>> typeReference = new TypeReference<>() {
        };
//
        DataResponse<DataEventStream> dataResponse = objectMapper.readValue(responseBodyString, typeReference);

        DataEventStream responseStream = dataResponse.getItems().get(0);

        int eventCount = responseStream.getStream().length / (8 + 4 + 8);

        List<DataEventWithSessionId> dataEventsList = new ArrayList<>(eventCount);

        ByteBuffer streamReader = ByteBuffer.wrap(responseStream.getStream());

        while (streamReader.hasRemaining()) {
            long timestamp = streamReader.getLong();
            int dataId = streamReader.getInt();
            long valueId = streamReader.getLong();
            DataEventWithSessionId event = new DataEventWithSessionId();
            event.setNanoTime(timestamp);
            event.setDataId(dataId);
            event.setValue(valueId);
            event.setSessionId(session.getSessionId());
            event.setThreadId(filteredDataEventsRequest.getThreadId());


            dataEventsList.add(event);


        }

        ResponseMetadata metadata = dataResponse.getMetadata();
        Map<String, ClassInfo> classInfo = metadata.getClassInfo();
        Map<String, DataInfo> dataInfo = metadata.getDataInfo();

        Map<String, StringInfo> stringInfo = metadata.getStringInfo();
        Map<String, ObjectInfo> objectInfo = metadata.getObjectInfo();
        Map<String, TypeInfo> typeInfo = metadata.getTypeInfo();

        return new ReplayData(dataEventsList, classInfo, dataInfo, stringInfo,
                objectInfo, typeInfo, filteredDataEventsRequest.getSortOrder());

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
        get(endpoint + "/api/data/java-agent-jar-link", new Callback() {
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
}
