package com.insidious.plugin.videobugclient;

import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.videobugclient.pojo.*;
import com.insidious.plugin.videobugclient.pojo.exceptions.APICallException;
import com.insidious.plugin.videobugclient.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.videobugclient.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.pojo.TracePoint;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface VideobugClientInterface {
    ExecutionSession getCurrentSession();

    void setSession(ExecutionSession session) throws IOException;

    void signup(String serverUrl, String username, String password, SignUpCallback callback);

    void signin(SigninRequest signinRequest, SignInCallback signInCallback) throws UnauthorizedException;

    void getProjectByName(String projectName, GetProjectCallback getProjectCallback);

    ProjectItem fetchProjectByName(String projectName) throws ProjectDoesNotExistException, UnauthorizedException, IOException;

    void createProject(String projectName, NewProjectCallback newProjectCallback);

    void getProjectToken(ProjectTokenCallback projectTokenCallback);

    void getProjectSessions(GetProjectSessionsCallback getProjectSessionsCallback) throws IOException;

    DataResponse<ExecutionSession> fetchProjectSessions() throws APICallException, IOException;

    void getTracesByObjectType(List<String> classList, GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) throws IOException;

    @NotNull
    default List<TracePoint> getTracePoints(DataResponse<DataEventWithSessionId> traceResponse) {
        return traceResponse.getItems().stream()
                .map(e -> TracePoint.fromDataEvent(e, traceResponse))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    void getTracesByObjectValue(String value,
                                GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) throws IOException;

    ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) throws Exception;

    String getToken();

    ProjectItem getProject();

    void setProject(String projectName) throws ProjectDoesNotExistException, UnauthorizedException, IOException;

    String getEndpoint();

    void getAgentDownloadUrl(AgentDownloadUrlCallback agentDownloadUrlCallback);

    void downloadAgentFromUrl(String url, String insidiousLocalPath, boolean overwrite);
}
