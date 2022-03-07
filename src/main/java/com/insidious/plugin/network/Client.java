package com.insidious.plugin.network;

import com.insidious.plugin.actions.Constants;
import com.insidious.plugin.callbacks.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.text.Strings;
import org.slf4j.Logger;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.DataInfo;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.model.StringInfo;
import com.insidious.plugin.extension.model.TypeInfo;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import com.insidious.plugin.network.pojo.*;
import com.insidious.plugin.network.pojo.exceptions.APICallException;
import com.insidious.plugin.network.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.network.pojo.exceptions.UnauthorizedException;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final String SIGN_IN_URL = "/api/auth/signin";
    public static final String SIGN_UP_URL = "/api/auth/signup";
    public static final String PROJECTS_URL = "/api/data/projects";
    public static final String PROJECT_URL = "/api/data/project";
    public static final String PROJECT_EXECUTIONS_URL = "/executions";
    public static final String FILTER_DATA_EVENTS_URL = "/filterDataEvents";
    public static final String TRACE_BY_EXCEPTION = "/traceByException";
    public static final String TRACE_BY_STRING = "/traceByString";
    public static final String GENERATE_PROJECT_TOKEN_URL = "/api/auth/generateAgentToken";
    private final Logger logger = LoggerUtil.getInstance(Client.class);
    private final String endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    OkHttpClient client;
    private ProjectItem project;
    private String token;
    private ExecutionSession session;

    public Client(String endpoint) {
        this.endpoint = endpoint;
        client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();

    }

    public Client(String endpoint, String username, String password) throws IOException, UnauthorizedException {
        this.endpoint = endpoint;
        client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();

        token = this.generateToken(username, password);
        if (token == null) {
            throw new UnauthorizedException("failed to sign in with provided credentials [" + username + "]");
        }
    }

    public ExecutionSession getCurrentSession() {
        return session;
    }

    public void setSession(ExecutionSession session) {
        this.session = session;
    }

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

                if (response.code() == 200) {
                    callback.success();
                } else {
                    callback.error(response.body().string());
                }
            }
        });
    }

    public void signin(String username, String password, SignInCallback signInCallback) {
        logger.info("Sign in for email => " + username);
        JSONObject json = new JSONObject();
        json.put("email", username);
        json.put("password", password);
        post(endpoint + SIGN_IN_URL, json.toJSONString(), new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                signInCallback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                logger.info("Sign in successful");

                if (response.code() == 401) {
                    signInCallback.error(response.message());
                    return;
                }

                JSONObject jsonObject = (JSONObject) JSONValue.parse(response.body().string());
                String token = jsonObject.getAsString(Constants.TOKEN);

                signInCallback.success(token);
            }
        });
    }

    public String generateToken(String username, String password) throws IOException {
        logger.info("Sign in for email => " + username);
        JSONObject json = new JSONObject();
        json.put("email", username);
        json.put("password", password);
        Response response = postSync(endpoint + SIGN_IN_URL, json.toJSONString());

        JSONObject jsonObject = null;
        jsonObject = (JSONObject) JSONValue.parse(response.body().string());

        return jsonObject.getAsString(Constants.TOKEN);

    }

    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {
        logger.info("Get project by name => " + projectName);

        get(endpoint + Constants.PROJECT_URL + "?name=" + projectName, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                getProjectCallback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                JSONObject jsonProjects = (JSONObject) JSONValue.parse(response.body().string());
                JSONArray jsonArray = (JSONArray) jsonProjects.get("items");
                if (jsonArray.size() == 0) {
                    getProjectCallback.noSuchProject();
                } else {
                    JSONObject projectIdJson = (JSONObject) jsonArray.get(0);
                    String project_id = projectIdJson.getAsString("id");
                    getProjectCallback.success(project_id);
                }

            }
        });
    }

    public ProjectItem fetchProjectByName(String projectName) throws ProjectDoesNotExistException, UnauthorizedException, IOException {
        logger.info("Get project by name => " + projectName);
        TypeReference<DataResponse<ProjectItem>> typeReference = new TypeReference<>() {
        };
        DataResponse<ProjectItem> projectList = get(endpoint + Constants.PROJECT_URL + "?name=" + projectName, typeReference);

        if (projectList.getItems().size() == 0) {
            throw new ProjectDoesNotExistException(projectName);
        }

        return projectList.getItems().get(0);

    }

    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        logger.info("create project on server - [{}]", projectName);
        post(endpoint + PROJECT_URL + "?name=" + projectName, "", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("failed to create project", e);
                newProjectCallback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseBody = response.body().string();
                logger.info("create project successful response - {}", responseBody);
                JSONObject jsonObject = (JSONObject) JSONValue.parse(responseBody);
                String projectId = jsonObject.getAsString("id");
                newProjectCallback.success(projectId);
            }
        });
    }

    public String createProject(String projectName) throws APICallException, IOException {
        Response response = postSync(endpoint + PROJECT_URL + "?name=" + projectName, "");
        JSONObject jsonObject = null;

        jsonObject = (JSONObject) JSONValue.parse(response.body().string());

        String projectId = jsonObject.getAsString("id");
        this.project = new ProjectItem();
        this.project.setName(projectName);
        this.project.setId(projectId);
        this.project.setCreatedAt(new Date().toString());
        return projectId;
    }

    public void getProjectToken(ProjectTokenCallback projectTokenCallback) {
        logger.info("get project token - " + project.getId());
        post(endpoint + GENERATE_PROJECT_TOKEN_URL + "?projectId=" + project.getId(), "", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("failed to generate project token", e);
                projectTokenCallback.error(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseBody = response.body().string();
                logger.info("project token response - {}", responseBody);
                JSONObject jsonObject = (JSONObject) JSONValue.parse(responseBody);
                projectTokenCallback.success(jsonObject.getAsString(Constants.TOKEN));
            }
        });
    }

    private void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request.Builder builder = new Request.Builder();

        builder.url(url);
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

        builder.url(url);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);

        Request request = builder.build();

        return client.newCall(request).execute();
    }

    private void get(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);

    }

    private <T> T get(String url, TypeReference<T> typeReference) throws IOException, UnauthorizedException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        Response response;
        response = client.newCall(request).execute();

        if (response.code() == 401) {
            throw new UnauthorizedException();
        }

        T result = null;
        result = objectMapper.readValue(response.body().string(), typeReference);

        return result;
    }

    public void getProjectSessions(GetProjectSessionsCallback getProjectSessionsCallback) {
        logger.info("get project sessions - " + this.project.getId());
        String executionsUrl = endpoint + PROJECT_URL + "/" + this.project.getId() + PROJECT_EXECUTIONS_URL;
        get(executionsUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("failed to get project sessions", e);
                getProjectSessionsCallback.error("ioexception");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
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
                String responseBody = response.body().string();
                DataResponse<ExecutionSession> sessionList = objectMapper.readValue(responseBody, typeReference);
                logger.info("got [{}] sessions for project [{}] - {}", sessionList.getItems().size(), project.getId(), responseBody);
                getProjectSessionsCallback.success(sessionList.getItems());
            }
        });
    }

    public DataResponse<ExecutionSession> fetchProjectSessions() throws APICallException, IOException {
        String executionsUrl = endpoint + PROJECT_URL + "/" + this.project.getId() + PROJECT_EXECUTIONS_URL;
        TypeReference<DataResponse<ExecutionSession>> typeReference = new TypeReference<>() {
        };

        return get(executionsUrl, typeReference);
    }

    public void getTracesByClassForProjectAndSessionId(List<String> classList, GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {

        String url = endpoint + PROJECT_URL
                + "/" + this.project.getId()
                + TRACE_BY_EXCEPTION
                + "/" + this.session.getId()
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

                if (response.code() != 200) {
                    ExceptionResponse errorResponse = objectMapper.readValue(response.body().string(), ExceptionResponse.class);
                    getProjectSessionErrorsCallback.error(errorResponse);
                    return;
                }

                TypeReference<DataResponse<DataEventWithSessionId>> typeReference = new TypeReference<>() {
                };

                JSONObject errorsJson = (JSONObject) JSONValue.parse(response.body().string());
                JSONArray jsonArray = (JSONArray) errorsJson.get("items");
                JSONObject metadata = (JSONObject) errorsJson.get("metadata");
                JSONObject classInfo = (JSONObject) metadata.get("classInfo");
                JSONObject dataInfo = (JSONObject) metadata.get("dataInfo");
                JSONObject typesInfo = (JSONObject) metadata.get("typeInfo");
                JSONObject objectInfo = (JSONObject) metadata.get("objectInfo");

                ArrayList<TracePoint> bugList = new ArrayList<>();

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    long dataId = jsonObject.getAsNumber("dataId").longValue();
                    long threadId = jsonObject.getAsNumber("threadId").longValue();
                    long valueId = jsonObject.getAsNumber("value").longValue();
                    String executionSessionId = jsonObject.getAsString("executionSessionId");
                    long classId;
                    long line;
                    String sessionId;
                    String filename, classname;
                    JSONObject dataInfoObject = (JSONObject) dataInfo.get(String.valueOf(dataId));
                    if (dataInfoObject != null) {
                        classId = dataInfoObject.getAsNumber("classId").longValue();
                        line = dataInfoObject.getAsNumber("line").longValue();
                        sessionId = dataInfoObject.getAsString("sessionId");

                        JSONObject attributesMap = (JSONObject) dataInfoObject.get("attributesMap");

                        JSONObject tempClass = (JSONObject) classInfo.get(String.valueOf(classId));
                        filename = tempClass.getAsString("filename");
                        classname = tempClass.getAsString("className");

                        JSONObject errorKeyValueJson = (JSONObject) objectInfo.get(String.valueOf(valueId));
                        long exceptionType = errorKeyValueJson.getAsNumber("typeId").longValue();
                        JSONObject exceptionClassJson = (JSONObject) typesInfo.get(String.valueOf(exceptionType));
                        String exceptionClass = exceptionClassJson.getAsString("typeNameFromClass");
                        TracePoint bug = new TracePoint(classId,
                                line,
                                dataId,
                                threadId,
                                valueId,
                                executionSessionId,
                                filename,
                                classname,
                                exceptionClass,
                                (Long) jsonObject.get("recordedAt"));
                        bugList.add(bug);

                    }

                }

                getProjectSessionErrorsCallback.success(bugList);

            }
        });

    }

    public void getTracesByClassForProjectAndSessionIdAndTracevalue(String traceId,
                                                                    GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {

        String url = endpoint + PROJECT_URL
                + "/" + this.project.getId()
                + TRACE_BY_STRING
                + "/" + this.session.getId()
                + "?traceValue="
                + traceId
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

                if (response.code() != 200) {
                    ExceptionResponse errorResponse = objectMapper.readValue(response.body().string(), ExceptionResponse.class);
                    getProjectSessionErrorsCallback.error(errorResponse);
                    return;
                }

                TypeReference<DataResponse<DataEventWithSessionId>> typeReference = new TypeReference<>() {
                };

                JSONObject errorsJson = (JSONObject) JSONValue.parse(response.body().string());
                JSONArray jsonArray = (JSONArray) errorsJson.get("items");
                JSONObject metadata = (JSONObject) errorsJson.get("metadata");
                JSONObject classInfo = (JSONObject) metadata.get("classInfo");
                JSONObject dataInfo = (JSONObject) metadata.get("dataInfo");
                JSONObject typesInfo = (JSONObject) metadata.get("typeInfo");
                JSONObject objectInfo = (JSONObject) metadata.get("objectInfo");

                ArrayList<TracePoint> bugList = new ArrayList<>();

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                    long dataId = jsonObject.getAsNumber("dataId").longValue();
                    long threadId = jsonObject.getAsNumber("threadId").longValue();
                    long valueId = jsonObject.getAsNumber("value").longValue();
                    String executionSessionId = jsonObject.getAsString("executionSessionId");
                    long classId;
                    long line;
                    String sessionId;
                    String filename, classname;
                    JSONObject dataInfoObject = (JSONObject) dataInfo.get(String.valueOf(dataId));
                    if (dataInfoObject != null) {
                        classId = dataInfoObject.getAsNumber("classId").longValue();
                        line = dataInfoObject.getAsNumber("line").longValue();
                        sessionId = dataInfoObject.getAsString("sessionId");

                        JSONObject attributesMap = (JSONObject) dataInfoObject.get("attributesMap");

                        JSONObject tempClass = (JSONObject) classInfo.get(String.valueOf(classId));
                        filename = tempClass.getAsString("filename");
                        classname = tempClass.getAsString("className");

                        JSONObject errorKeyValueJson = (JSONObject) objectInfo.get(String.valueOf(valueId));
                        long exceptionType = errorKeyValueJson.getAsNumber("typeId").longValue();
                        JSONObject exceptionClassJson = (JSONObject) typesInfo.get(String.valueOf(exceptionType));
                        String exceptionClass = "";
                        if (exceptionClassJson != null) {
                            exceptionClass = exceptionClassJson.getAsString("typeNameFromClass");
                        }

                        TracePoint bug = new TracePoint(classId, line, dataId, threadId,
                                valueId,
                                executionSessionId,
                                filename,
                                classname,
                                exceptionClass,
                                (Long) jsonObject.get("recordedAt"));
                        bugList.add(bug);

                    }

                }

                getProjectSessionErrorsCallback.success(bugList);

            }
        });

    }

    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) throws Exception {
        String url = endpoint + PROJECT_URL + "/" + project.getId() + FILTER_DATA_EVENTS_URL;
        Response response = postSync(url, objectMapper.writeValueAsString(filteredDataEventsRequest));

        String responseBodyString = response.body().string();
        if (response.code() != 200) {
            logger.error("error response from filterDataEvents  [{}] - [{}]", response.code(), responseBodyString);
            ExceptionResponse errorResponse = JSONValue.parse(responseBodyString, ExceptionResponse.class);
            throw new Exception(errorResponse.getMessage());
        }

        TypeReference<DataResponse<DataEventWithSessionId>> typeReference = new TypeReference<>() {
        };

        DataResponse<DataEventWithSessionId> dataResponse = objectMapper.readValue(responseBodyString, typeReference);
        List<DataEventWithSessionId> dataEventsList = dataResponse.getItems();
        ResponseMetadata metadata = dataResponse.getMetadata();
        Map<String, ClassInfo> classInfo = metadata.getClassInfo();
        Map<String, DataInfo> dataInfo = metadata.getDataInfo();
        Map<String, StringInfo> stringInfo = metadata.getStringInfo();
        Map<String, ObjectInfo> objectInfo = metadata.getObjectInfo();
        Map<String, TypeInfo> typeInfo = metadata.getTypeInfo();

        return new ReplayData(dataEventsList, classInfo, dataInfo, stringInfo, objectInfo, typeInfo, filteredDataEventsRequest.getSortOrder());

    }

    public String getToken() {
        return token;
    }

    public ProjectItem getProject() {
        return this.project;
    }

    public void setProject(String projectName) throws ProjectDoesNotExistException, UnauthorizedException, IOException {
        this.project = fetchProjectByName(projectName);
    }
}
