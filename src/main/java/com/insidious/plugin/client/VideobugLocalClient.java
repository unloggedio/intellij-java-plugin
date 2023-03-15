package com.insidious.plugin.client;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.SigninRequest;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.ClassWeaveInfo;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.pojo.TracePoint;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class VideobugLocalClient implements VideobugClientInterface {

    private static final Logger logger = Logger.getInstance(VideobugLocalClient.class.getName());
    private final String pathToSessions;
    private final VideobugNetworkClient networkClient;
    private final ScheduledExecutorService threadPoolExecutor5Seconds = Executors.newScheduledThreadPool(1);
    private final Project project;
    private SessionInstance sessionInstance;
    private ProjectItem currentProject;

    public VideobugLocalClient(String pathToSessions, Project project) {
        this.project = project;
        if (!pathToSessions.endsWith("/")) {
            pathToSessions = pathToSessions + "/";
        }
        this.pathToSessions = pathToSessions;
        this.networkClient = new VideobugNetworkClient("https://cloud.bug.video");
    }

    @Override
    public ExecutionSession getCurrentSession() {
        if (this.sessionInstance == null) {
            return null;
        }
        return this.sessionInstance.getExecutionSession();
    }

    @Override
    public void signup(String serverUrl, String username, String password, SignUpCallback callback) {
        callback.success();
    }

    @Override
    public void signin(SigninRequest signinRequest, SignInCallback signInCallback) {
        signInCallback.success("localhost-token");
    }

    @Override
    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {
        getProjectCallback.success(projectName);
    }

    @Override
    public ProjectItem fetchProjectByName(String projectName) {
        ProjectItem projectItem = new ProjectItem();
        projectItem.setName(projectName);
        projectItem.setId("1");
        projectItem.setCreatedAt(new Date().toString());
        return projectItem;
    }

    @Override
    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        newProjectCallback.success("1");
    }

    @Override
    public void getProjectToken(ProjectTokenCallback projectTokenCallback) {
        projectTokenCallback.success("localhost-token");
    }

    @Override
    public void getProjectSessions(GetProjectSessionsCallback getProjectSessionsCallback) {
        getProjectSessionsCallback.success(getLocalSessions());
    }

    private List<ExecutionSession> getLocalSessions() {
        List<ExecutionSession> list = new LinkedList<>();
        logger.info(String.format("looking for sessions in [%s]", pathToSessions));
        File currentDir = new File(pathToSessions);
        if (!currentDir.exists()) {
            currentDir.mkdirs();
            return Collections.emptyList();
        }
        File[] sessionDirectories = currentDir.listFiles();
        if (sessionDirectories == null) {
            return Collections.emptyList();
        }
        for (File file : sessionDirectories) {
            if (file.isDirectory() && file.getName().contains("selogger")) {
                ExecutionSession executionSession = new ExecutionSession();
                executionSession.setSessionId(file.getName());
                executionSession.setCreatedAt(new Date(file.lastModified()));
                executionSession.setHostname("localhost");
                executionSession.setLastUpdateAt(file.lastModified());
                executionSession.setPath(file.getAbsolutePath());
                list.add(executionSession);
            }
        }

        if (list.size() > 1) {
            list.sort(Comparator.comparing(ExecutionSession::getSessionId));
            Collections.reverse(list);
            int i = -1;
            if (list.size() > 0) {

                for (ExecutionSession executionSession : list) {
                    i++;
                    if (i == 0) {
                        continue;
                    }
                    deleteDirectory(FileSystems.getDefault().getPath(this.pathToSessions, executionSession.getSessionId()).toFile());
                }
            }
        }


        return list;

    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @Override
    public DataResponse<ExecutionSession> fetchProjectSessions() {
        List<ExecutionSession> localSessions = getLocalSessions();
        return new DataResponse<>(localSessions, localSessions.size(), 1);
    }

    @Override
    public void queryTracePointsByValue(
            SearchQuery searchQuery,
            String sessionId,
            ClientCallBack<TracePoint> getProjectSessionErrorsCallback) {
        logger.info("trace by string value: " + searchQuery);
        checkSession(sessionId);
        this.sessionInstance.queryTracePointsByValue(searchQuery, getProjectSessionErrorsCallback);

    }

    @Override
    public ReplayData fetchObjectHistoryByObjectId(
            FilteredDataEventsRequest filteredDataEventsRequest
    ) throws SessionNotSelectedException {

        if (filteredDataEventsRequest.getSessionId() != null) {
            checkSession(filteredDataEventsRequest.getSessionId());
        }
        if (this.sessionInstance == null) {
            throw new SessionNotSelectedException();
        }
        ReplayData replayData = this.sessionInstance.fetchObjectHistoryByObjectId(filteredDataEventsRequest);
        replayData.setClient(this);
        return replayData;

    }

    @Override
    public void queryTracePointsByTypes(SearchQuery searchQuery,
                                        String sessionId, int historyDepth,
                                        ClientCallBack<TracePoint> clientCallBack) {
        logger.info("get trace by object type: " + searchQuery.getQuery());
        checkSession(sessionId);
        this.sessionInstance.queryTracePointsByTypes(searchQuery, clientCallBack);


    }

    @Override
    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) {
        checkSession(filteredDataEventsRequest.getSessionId());
        ReplayData replayData = this.sessionInstance.fetchDataEvents(filteredDataEventsRequest);
        replayData.setClient(this);
        return replayData;
    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText2(text1);
            }
        }
    }

    @Override
    public String getToken() {
        return "localhost-token";
    }

    @Override
    public ProjectItem getProject() {
        return this.currentProject;
    }

    @Override
    public void setProject(String projectName) {
        ProjectItem currentProject = new ProjectItem();
        currentProject.setName(projectName);
        currentProject.setId("1");
        currentProject.setCreatedAt(new Date().toString());
        this.currentProject = currentProject;
    }

    @Override
    public String getEndpoint() {
        return pathToSessions;
    }

    @Override
    public void getAgentDownloadUrl(AgentDownloadUrlCallback agentDownloadUrlCallback) {
        networkClient.getAgentDownloadUrl(agentDownloadUrlCallback);
    }

    @Override
    public void downloadAgentFromUrl(String url, String insidiousLocalPath, boolean overwrite) {
        networkClient.downloadAgentFromUrl(url, insidiousLocalPath, overwrite);
    }

    @Override
    public void close() {
        threadPoolExecutor5Seconds.shutdown();
    }

//    @Override
//    public void onNewException(Collection<String> typeNameList, VideobugExceptionCallback videobugExceptionCallback) {
//
//
//        threadPoolExecutor5Seconds.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                if (1 < 2) {
//                    return;
//                }
//
//                List<ExecutionSession> sessions = getLocalSessions();
//                ExecutionSession executionSession = sessions.get(0);
//                try {
//                    setSessionInstance(new SessionInstance(executionSession));
//                } catch (SQLException | IOException e) {
//                    throw new RuntimeException(e);
//                }
//
//                queryTracePointsByTypes(SearchQuery.ByType(typeNameList),
//                        sessionInstance.getExecutionSession().getSessionId(), 2,
//                        new ClientCallBack<TracePoint>() {
//                            @Override
//                            public void error(ExceptionResponse errorResponse) {
//                                logger.info("failed to query traces by type in scheduler: " + errorResponse.getMessage());
//                            }
//
//                            @Override
//                            public void success(Collection<TracePoint> tracePoints) {
//                                if (tracePoints.size() > 0) {
//                                    videobugExceptionCallback.onNewTracePoints(tracePoints);
//                                }
//                            }
//
//                            @Override
//                            public void completed() {
//
//                            }
//                        });
//            }
//        }, 5, 5, TimeUnit.SECONDS);
//    }

    @Override
    public ClassWeaveInfo getSessionClassWeave(String sessionId) {
        checkSession(sessionId);
        return this.sessionInstance.getClassWeaveInfo();

    }

    private void checkSession(String sessionId) {
        if (this.sessionInstance == null || !this.sessionInstance.getExecutionSession()
                .getSessionId()
                .equals(sessionId)) {
            ExecutionSession executionSession = new ExecutionSession();
            executionSession.setSessionId(sessionId);
            try {
                this.setSessionInstance(new SessionInstance(executionSession, project));
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void queryTracePointsByEventType(
            SearchQuery searchQuery,
            String sessionId,
            ClientCallBack<TracePoint> tracePointsCallback) {
        checkSession(sessionId);
        logger.info("trace by probe ids: " + searchQuery);
        checkProgressIndicator("Searching locally by value [" + searchQuery.getQuery() + "]", null);
        this.sessionInstance.queryTracePointsByEventType(searchQuery, tracePointsCallback);


    }

    @Override
    public TypeInfo getTypeInfoByName(String sessionId, String type) {
        return this.sessionInstance.getTypeInfo(type);
    }


    @Override
    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    @Override
    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

}
