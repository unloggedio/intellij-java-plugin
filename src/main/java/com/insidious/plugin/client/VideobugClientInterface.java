package com.insidious.plugin.client;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.SigninRequest;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.client.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.client.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.PageInfo;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface VideobugClientInterface {
    ExecutionSession getCurrentSession();

    void setSession(ExecutionSession session) throws IOException;

    void signup(String serverUrl, String username, String password, SignUpCallback callback);

    void signin(SigninRequest signinRequest, SignInCallback signInCallback) throws UnauthorizedException;

    void getProjectByName(String projectName, GetProjectCallback getProjectCallback);

    ProjectItem fetchProjectByName(String projectName)
            throws ProjectDoesNotExistException, UnauthorizedException, IOException;

    void createProject(String projectName, NewProjectCallback newProjectCallback);

    void getProjectToken(ProjectTokenCallback projectTokenCallback);

    void getProjectSessions(GetProjectSessionsCallback getProjectSessionsCallback) throws IOException;

    DataResponse<ExecutionSession> fetchProjectSessions() throws APICallException, IOException;

    @NotNull
    default List<TracePoint> getTracePoints(DataResponse<DataEventWithSessionId> traceResponse) {
        return traceResponse.getItems().stream()
                .map(e -> TracePoint.fromDataEvent(e, traceResponse))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    ClassWeaveInfo getSessionClassWeave(String sessionId);

    void queryTracePointsByProbe(SearchQuery searchQuery, String sessionid,
                                 ClientCallBack<TracePoint> tracePointsCallback);

    ReplayData fetchObjectHistoryByObjectId(
            Long objectId,
            Integer threadId,
            Long nanoTime,
            PageInfo pageInfo
    );

    void queryTracePointsByTypes(SearchQuery classList, String sessionId, int historyDepth,
                                 ClientCallBack<TracePoint> getProjectSessionErrorsCallback);

    void queryTracePointsByValue(SearchQuery value, String sessionId,
                                 ClientCallBack<TracePoint> getProjectSessionErrorsCallback);

    ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) throws APICallException;


    String getToken();

    ProjectItem getProject();

    void setProject(String projectName)
            throws ProjectDoesNotExistException, UnauthorizedException, IOException;

    String getEndpoint();

    void getAgentDownloadUrl(AgentDownloadUrlCallback agentDownloadUrlCallback);

    void downloadAgentFromUrl(String url, String insidiousLocalPath, boolean overwrite);

    void close();

    void onNewException(Collection<String> typeNameList, VideobugExceptionCallback videobugExceptionCallback);


    void getMethods(String sessionId, Integer typeId,
                    ClientCallBack<TestCandidate> tracePointsCallback);

    void getObjectsByType(SearchQuery searchQuery, String sessionId,
                          ClientCallBack<ObjectsWithTypeInfo> clientCallBack);
}
