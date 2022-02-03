package network;

import actions.Constants;
import callbacks.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import network.pojo.*;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import pojo.Bugs;
import pojo.VarsValues;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Client {
    public static final String SIGN_IN_URL = "/api/auth/signin";
    public static final String SIGN_UP_URL = "/api/auth/signin";
    public static final String PROJECTS_URL = "/api/data/projects";
    public static final String PROJECT_URL = "/api/data/project";
    public static final String PROJECT_EXECUTIONS_URL = "/executions";
    public static final String FILTER_DATA_EVENTS_URL = "/filterDataEvents";
    public static final String TRACE_BY_EXCEPTION = "/traceByException";
    public static final String TRACE_BY_STRING = "/traceByString";
    public static final String GENERATE_PROJECT_TOKEN_URL = "/api/auth/generateAgentToken";
    private final Logger logger = Logger.getInstance(Client.class);
    private final String endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    OkHttpClient client;

    public Client(String endpoint) {
        this.endpoint = endpoint;
        client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();

    }

    public void signup(String username, String password, SignUpCallback callback) {
        logger.info("Sign up for email => " + username);
        JSONObject json = new JSONObject();
        json.put("email", username);
        json.put("password", password);
        post(endpoint + SIGN_UP_URL, json.toJSONString(), new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.error();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                callback.success();
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
                signInCallback.error();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                logger.info("Sign in successful");

                if (response.code() == 401) {
                    signInCallback.error();
                    return;
                }

                JSONObject jsonObject = (JSONObject) JSONValue.parse(response.body().string());
                String token = jsonObject.getAsString(Constants.TOKEN);

                signInCallback.success(token);
            }
        });
    }


    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {
        logger.info("Get project by name => " + projectName);

        get(endpoint + Constants.PROJECT_URL + "?name=" + projectName, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                getProjectCallback.error();
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

    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        post(endpoint + PROJECT_URL + "?name=" + projectName, "", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                newProjectCallback.error();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JSONObject jsonObject = (JSONObject) JSONValue.parse(response.body().string());
                String projectId = jsonObject.getAsString("id");
                newProjectCallback.success(projectId);
            }
        });
    }

    public void getProjectToken(String projectId, ProjectTokenCallback projectTokenCallback) {
        post(endpoint + GENERATE_PROJECT_TOKEN_URL + "?projectId=" + projectId, "", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                projectTokenCallback.error();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JSONObject jsonObject = (JSONObject) JSONValue.parse(response.body().string());
                projectTokenCallback.success(jsonObject.getAsString(Constants.TOKEN));
            }
        });
    }


    private void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request.Builder builder = new Request.Builder();

        builder.url(url);
        String token = PropertiesComponent.getInstance().getValue(Constants.TOKEN);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);

        Request request = builder.build();

        client.newCall(request).enqueue(callback);
    }

    private void get(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + PropertiesComponent.getInstance().getValue(Constants.TOKEN))
                .build();
        client.newCall(request).enqueue(callback);

    }

    public void getProjectSessions(String projectId, GetProjectSessionsCallback getProjectSessionsCallback) {
        String executionsUrl = endpoint + PROJECT_URL + "/" + projectId + PROJECT_EXECUTIONS_URL;
        get(executionsUrl, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                getProjectSessionsCallback.error();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.body() == null) {
                    getProjectSessionsCallback.error();
                    return;
                }

                TypeReference<DataResponse<ExecutionSession>> typeReference = new TypeReference<>() {
                };
                DataResponse<ExecutionSession> sessionList = objectMapper.readValue(response.body().string(), typeReference);
                getProjectSessionsCallback.success(sessionList.getItems());
            }
        });
    }

    public void getTracesByClassForProjectAndSessionId(String projectId, String sessionId,
                                                       GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {

        String url = endpoint + PROJECT_URL
                + "/" + projectId
                + TRACE_BY_EXCEPTION
                + "/" + sessionId
                + "?exceptionClass="
                + PropertiesComponent.getInstance().getValue(Constants.ERROR_NAMES)
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

                ArrayList<Bugs> bugList = new ArrayList<>();

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
                    JSONObject dataInfoObject = (JSONObject) dataInfo.get(dataId + "_" + executionSessionId);
                    if (dataInfoObject != null) {
                        classId = dataInfoObject.getAsNumber("classId").longValue();
                        line = dataInfoObject.getAsNumber("line").longValue();
                        sessionId = dataInfoObject.getAsString("sessionId");

                        JSONObject attributesMap = (JSONObject) dataInfoObject.get("attributesMap");

                        JSONObject tempClass = (JSONObject) classInfo.get(classId + "_" + sessionId);
                        filename = tempClass.getAsString("filename");
                        classname = tempClass.getAsString("className");

                        JSONObject errorKeyValueJson = (JSONObject)objectInfo.get(valueId + "_" + sessionId);
                        long exceptionType = errorKeyValueJson.getAsNumber("typeId").longValue();
                        JSONObject exceptionClassJson = (JSONObject)typesInfo.get(exceptionType + "_" + sessionId);
                        String exceptionClass = exceptionClassJson.getAsString("typeNameFromClass");
                        Bugs bug = new Bugs(classId, line, dataId, threadId, valueId, executionSessionId, filename, classname, exceptionClass);
                        bugList.add(bug);

                    }

                }

                getProjectSessionErrorsCallback.success(bugList);

            }
        });

    }

    public void filterDataEvents(String projectId, FilteredDataEventsRequest filteredDataEventsRequest,
                                 FilteredDataEventsCallback filteredDataEventsCallback) {
        try {
            String url = endpoint + PROJECT_URL + "/" + projectId + FILTER_DATA_EVENTS_URL;
            String executionSessionId = filteredDataEventsRequest.getSessionId();

            post(url, objectMapper.writeValueAsString(filteredDataEventsRequest), new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    ExceptionResponse exceptionResponse = new ExceptionResponse();
                    exceptionResponse.setMessage(e.getMessage());
                    filteredDataEventsCallback.error(exceptionResponse);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                    if (response.code() != 200) {
                        ExceptionResponse errrorResponse = JSONValue.parse(response.body().string(), ExceptionResponse.class);
                        filteredDataEventsCallback.error(errrorResponse);
                        return;
                    }

                    TypeReference<DataResponse<DataEventWithSessionId>> typeReference = new TypeReference<>() {
                    };

                    DataResponse<DataEventWithSessionId> dataResponse = objectMapper.readValue(response.body().string(), typeReference);
                    List<DataEventWithSessionId> datapointsArray = dataResponse.getItems();
                    Map<String, Object> metadata = dataResponse.getMetadata();
                    Map<String, Object> classInfo = (Map<String, Object>) metadata.get("classInfo");
                    Map<String, Object> dataInfo = (Map<String, Object>) metadata.get("dataInfo");
                    Map<String, Object> stringInfo = (Map<String, Object>) metadata.get("stringInfo");

                    ArrayList<VarsValues> dataList = new ArrayList<>();

                    for (DataEventWithSessionId dataEvent : datapointsArray) {
                        long dataId = dataEvent.getDataId();
                        long dataValue = dataEvent.getValue();
                        Map<String, Object> dataInfoTemp = (Map<String, Object>) dataInfo.get(dataId + "_" + executionSessionId);
                        Map<String, Object> attributesMap = (Map<String, Object>) dataInfoTemp.get("attributesMap");

                        if (attributesMap.containsKey("Instruction")) {
                            continue;
                        }

                        String variableName = (String) attributesMap.get("Name");

                        if (variableName == null) {
                            continue;
                        }

                        if (Arrays.asList("<init>", "makeConcatWithConstants").contains(variableName)) {
                            continue;
                        }

                        String variableType = (String) attributesMap.get("Type");
                        int classId = (int) dataInfoTemp.get("classId");
                        int lineNum = (int) dataInfoTemp.get("line");
                        Map<String, Object> classInfoTemp = (Map<String, Object>) classInfo.get(classId + "_" + executionSessionId);
                        String filename = (String) classInfoTemp.get("filename");


                        String dataIdstr = String.valueOf(dataValue);

                        if (variableType != null) {
                            if (variableType.contains("java/lang/String")) {
                                Map<String, Object> tempStringJson = (Map<String, Object>) stringInfo.get(dataValue + "_" + executionSessionId);
                                if (tempStringJson != null) {
                                    dataIdstr = (String) tempStringJson.get("content");
                                }
                            }
                        }

                        long nanoTime = dataEvent.getNanoTime();
                        VarsValues varsValues = new VarsValues(lineNum, filename, variableName, dataIdstr, nanoTime);
                        dataList.add(varsValues);
                    }



                    filteredDataEventsCallback.success(dataList);
                }
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
