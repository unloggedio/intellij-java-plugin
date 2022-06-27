package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.MultipartUtility;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.VideobugNetworkClient;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.SigninRequest;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.client.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.client.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.extension.InsidiousExecutor;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.extension.InsidiousRunConfigType;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.ui.ConfigurationWindow;
import com.insidious.plugin.ui.SearchByTypeWindow;
import com.insidious.plugin.ui.SearchByValueWindow;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.visitor.GradleFileVisitor;
import com.insidious.plugin.visitor.PomFileVisitor;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.Consumer;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes;

@Storage("insidious.xml")
public class InsidiousService implements Disposable {
    public static final String HOSTNAME = System.getProperty("user.name");
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private final String DEFAULT_PACKAGE_NAME = "YOUR.PACKAGE.NAME";
    private Project project;
    private InsidiousConfigurationState insidiousConfiguration;
    private ExecutorService threadPool;
    private VideobugClientInterface client;
    private Module currentModule;
    private String packageName = "YOUR.PACKAGE.NAME";
    private SearchByTypeWindow bugsTable;
    private SearchByValueWindow logicBugs;
    private XDebugSession debugSession;
    private InsidiousJavaDebugProcess debugProcess;
    private InsidiousJDIConnector connector;
    private ConfigurationWindow credentialsToolbarWindow;
    private ToolWindow toolWindow;
    private CredentialAttributes insidiousCredentials;
    private String appToken;
    private String javaAgentString;
    private TracePoint pendingTrace;
    private TracePoint pendingSelectTrace;
    private final long pluginSessionId = new Date().getTime();


    public InsidiousService(Project project) {
        try {


            this.project = project;
            threadPool = Executors.newFixedThreadPool(4);

            logger.info("started insidious service - project name - " + project.getName());
            if (ModuleManager.getInstance(project).getModules().length == 0) {
                logger.warn("no module found in the project");
            } else {
                currentModule = ModuleManager.getInstance(project).getModules()[0];
                logger.info("current module - " + currentModule.getName());
            }

            debugSession = getActiveDebugSession(project.getService(XDebuggerManager.class).getDebugSessions());

            ReadAction.run(this::getProjectPackageName);
            threadPool.submit(this::logLogFileLocation);
            threadPool.submit(this::startDebugSession);

            this.insidiousConfiguration = project.getService(InsidiousConfigurationState.class);

            String pathToSessions = Constants.VIDEOBUG_HOME_PATH + "/sessions";
            Path.of(pathToSessions).toFile().mkdirs();
            this.client = new VideobugLocalClient(pathToSessions);
//        this.client = new VideobugNetworkClient(insidiousConfiguration.serverUrl);
            ReadAction.run(InsidiousService.this::checkAndEnsureJavaAgentCache);

            Set<String> exceptionClassList = insidiousConfiguration.exceptionClassMap.entrySet().stream()
                    .filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toSet());

            this.client.onNewException(exceptionClassList, new VideobugExceptionCallback() {
                final Set<TracePoint> mostRecentTracePoints = new HashSet<>();

                @Override
                public void onNewTracePoints(List<TracePoint> tracePoints) {
                    if (mostRecentTracePoints.size() > 0) {
                        List<TracePoint> newTracePoints = new LinkedList<>();
                        for (TracePoint tracePoint : tracePoints) {
                            if (!mostRecentTracePoints.contains(tracePoint)) {
                                newTracePoints.add(tracePoint);
                            }
                        }
                        mostRecentTracePoints.clear();
                        mostRecentTracePoints.addAll(tracePoints);
                        if (newTracePoints.size() == 0) {
                            return;
                        }
                        tracePoints = newTracePoints;
                    } else {
                        mostRecentTracePoints.addAll(tracePoints);
                    }

                    String messageContent = "Got " + tracePoints.size() + " new matching trace points";
                    StringBuilder messageBuilder = new StringBuilder();
                    Map<String, List<TracePoint>> pointsByClass = tracePoints.stream().collect(
                            Collectors.groupingBy(TracePoint::getClassname));
                    for (Map.Entry<String, List<TracePoint>> classTracePoint : pointsByClass.entrySet()) {
                        String className = classTracePoint.getKey();
                        List<TracePoint> classTracePoints = classTracePoint.getValue();
                        Map<String, List<TracePoint>> tracePointsByException = classTracePoints.stream().collect(
                                Collectors.groupingBy(TracePoint::getExceptionClass));

                        for (Map.Entry<String, List<TracePoint>> exceptionTracePoint : tracePointsByException.entrySet()) {
                            String exceptionClassName = exceptionTracePoint.getKey();
                            Map<Long, List<TracePoint>> pointsByLine = exceptionTracePoint.getValue()
                                    .stream().collect(
                                            Collectors.groupingBy(
                                                    TracePoint::getLinenum
                                            ));

                            for (Map.Entry<Long, List<TracePoint>> lineExceptionPoints : pointsByLine.entrySet()) {

                                String[] exceptionClassNameParts = exceptionClassName.split("\\.");
                                String[] classNameParts = className.split("/");
                                messageBuilder.append(lineExceptionPoints.getValue().size());
                                messageBuilder.append(" ");
                                messageBuilder.append(exceptionClassNameParts[exceptionClassNameParts.length - 1]);
                                messageBuilder.append(" on line ").append(lineExceptionPoints.getKey());
                                messageBuilder.append(" in ").append("<a>").append(classNameParts[classNameParts.length - 1]).append("</a>");
                                messageBuilder.append("<br>");
                            }


                        }


                    }


//                    Notification notification = InsidiousNotification.
//                            balloonNotificationGroup.createNotification("New exception",
//                                    "These just happened",
//                                    messageBuilder.toString(), NotificationType.INFORMATION);
//                    Notifications.Bus.notify(notification);
//                    getHorBugTable().setTracePoints(tracePoints);
                }
            });
        } catch (Throwable e) {
            logger.error("exception in videobug service init", e);
        }
    }

    private XDebugSession getActiveDebugSession(XDebugSession[] debugSessions) {
        for (XDebugSession session : debugSessions) {
            if (session.getDebugProcess() instanceof InsidiousJavaDebugProcess) {
                return session;
            }
        }
        return null;
    }

    public void checkAndEnsureJavaAgentCache() {
        checkAndEnsureJavaAgent(false, new AgentJarDownloadCompleteCallback() {
            @Override
            public void error(String message) {

            }

            @Override
            public void success(String url, String path) {
                InsidiousNotification.notifyMessage(
                        "Agent jar download complete", NotificationType.INFORMATION
                );
            }
        });
    }

    public void checkAndEnsureJavaAgent(boolean overwrite, AgentJarDownloadCompleteCallback agentJarDownloadCompleteCallback) {

        File insidiousFolder = new File(Constants.VIDEOBUG_HOME_PATH.toString());
        if (!insidiousFolder.exists()) {
            insidiousFolder.mkdir();
        }

        if (overwrite) {
            Constants.VIDEOBUG_AGENT_PATH.toFile().delete();
        }

        if (!Constants.VIDEOBUG_AGENT_PATH.toFile().exists() && !overwrite) {
            InsidiousNotification.notifyMessage(
                    "java agent does not exist, downloading to $HOME/.videobug/videobug-java-agent.jar. Please wait for download to finish.",
                    NotificationType.INFORMATION
            );
        }

        if (Constants.VIDEOBUG_AGENT_PATH.toFile().exists()) {
            return;
        }


        client.getAgentDownloadUrl(new AgentDownloadUrlCallback() {
            @Override
            public void error(String error) {
                logger.error("failed to get url from server to download the java agent - " + error);
                agentJarDownloadCompleteCallback.error("failed to get url from server to download the java agent - " + error);
            }

            @Override
            public void success(String url) {
                try {
                    logger.info("agent download link: " + url + ", downloading to path " + Constants.VIDEOBUG_AGENT_PATH.toAbsolutePath());
                    client.downloadAgentFromUrl(url, Constants.VIDEOBUG_AGENT_PATH.toString(), overwrite);
                    setAppTokenOnUi();
                    agentJarDownloadCompleteCallback.success(url, Constants.VIDEOBUG_AGENT_PATH.toString());
                } catch (Exception e) {
                    logger.info("failed to download agent - ", e);
                }
            }
        });

    }

    private void logLogFileLocation() {
//        String logFileNotificationContent = "Insidious log file location - " + LoggerUtil.getLogFilePath();
//
//
//        notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("com.insidious");
//        if (notificationGroup != null) {
//            notificationGroup
//                    .createNotification(logFileNotificationContent, NotificationType.INFORMATION)
//                    .notify(project);
//        }
    }

    private void getProjectPackageName() {
//        try {
        logger.info("looking up package name for the module [" + currentModule.getName() + "]");
        @NotNull PsiFile[] pomFileSearchResult = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));
        if (pomFileSearchResult.length > 0) {
            @NotNull XmlFile pomPsiFile = (XmlFile) pomFileSearchResult[0];
            logger.info("found pom.xml file at");

            PomFileVisitor visitor = new PomFileVisitor();
            pomPsiFile.accept(visitor);
            if (visitor.getPackageName() != null) {
                packageName = visitor.getPackageName();
                setAppTokenOnUi();
                return;
            }
        }

        @NotNull PsiFile[] gradleFileSearchResult = FilenameIndex.getFilesByName(project, "build.gradle", GlobalSearchScope.projectScope(project));
        if (gradleFileSearchResult.length > 0) {
            logger.info("found build.gradle file at");
            @NotNull PsiFile gradlePsiFile = gradleFileSearchResult[0];
            GradleFileVisitor visitor = new GradleFileVisitor();
            gradlePsiFile.accept(visitor);
            if (visitor.getPackageName() != null) {
                packageName = visitor.getPackageName();
                setAppTokenOnUi();
            }
        }
        packageName = "org/company/package";
        setAppTokenOnUi();
    }

    public String getJarPath() {
        return Constants.VIDEOBUG_AGENT_PATH.toAbsolutePath().toString();
    }

    public void init() {

        if (!StringUtil.isEmpty(insidiousConfiguration.getUsername())) {
            logger.info("username is not empty in configuration - [" + insidiousConfiguration.getUsername() + "] with server url " +
                    insidiousConfiguration.getServerUrl());
            insidiousCredentials = createCredentialAttributes("VideoBug", insidiousConfiguration.getUsername());
            if (insidiousCredentials != null) {
                Credentials credentials = PasswordSafe.getInstance().get(insidiousCredentials);
                if (credentials != null) {
                    String password = credentials.getPasswordAsString();
                    try {
                        if (password != null) {
                            // signin(insidiousConfiguration.serverUrl, insidiousConfiguration.username, password);
                            return;
                        }
                    } catch (Exception e) {
                        logger.error("failed to signin", e);
                        InsidiousNotification.notifyMessage("Failed to sign in -" + e.getMessage(),
                                NotificationType.ERROR);
                    }
                }
            }
        }
        ApplicationManager.getApplication().invokeLater(this::initiateUI);
    }

    public boolean isLoggedIn() {
        return this.client.getToken() != null;
    }

    public SearchByTypeWindow getHorBugTable() {
        return bugsTable;
    }

    public void signup(String serverUrl, String usernameText, String passwordText, SignUpCallback signupCallback) {
        this.client = new VideobugNetworkClient(serverUrl);
        this.client.signup(serverUrl, usernameText, passwordText, signupCallback);
    }


    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public void signin(String serverUrl, String usernameText, String passwordText) throws IOException {
        this.client = new VideobugNetworkClient(serverUrl);
        logger.info("signin with username [" + usernameText + "] on server " + serverUrl);

        if (!isValidEmailAddress(usernameText)) {
            credentialsToolbarWindow.setErrorLabel("Enter a valid email address");
            ApplicationManager.getApplication().invokeLater(this::initiateUI);
            return;
        }

        if (passwordText == null || passwordText.length() < 4) {
            ApplicationManager.getApplication().invokeLater(this::initiateUI);
            if (credentialsToolbarWindow != null) {
                credentialsToolbarWindow.setErrorLabel("Enter a valid password, at least 4 characters");
            }
            return;
        }

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("email", usernameText);
        eventProperties.put("server", serverUrl);
        UsageInsightTracker.getInstance().RecordEvent("SignInAttempt", eventProperties);

        insidiousConfiguration.setServerUrl(serverUrl);
        insidiousConfiguration.setUsername(usernameText);

        try {
            client.signin(SigninRequest.from(serverUrl, usernameText, passwordText),
                    new SignInCallback() {
                        @Override
                        public void error(String errorMessage) {
                            if (credentialsToolbarWindow != null) {
                                credentialsToolbarWindow.setErrorLabel("Sign in failed: " + errorMessage);
                            }

                            JSONObject eventProperties = new JSONObject();
                            eventProperties.put("email", usernameText);
                            eventProperties.put("server", serverUrl);
                            UsageInsightTracker.getInstance().RecordEvent("SignInFailed", eventProperties);


                            InsidiousNotification.notifyMessage("Failed to login VideoBug at ["
                                            + serverUrl + "] for module ["
                                            + currentModule.getName() + "]",
                                    NotificationType.ERROR);

                        }

                        @Override
                        public void success(String token) {
                            ReadAction.run(() -> InsidiousService.this.ensureAgentJar(false));
                            ReadAction.run(InsidiousService.this::setupProject);

                            JSONObject eventProperties = new JSONObject();
                            eventProperties.put("email", usernameText);
                            eventProperties.put("server", serverUrl);
                            UsageInsightTracker.getInstance().RecordEvent("SignInSuccess", eventProperties);


                            Credentials credentials = new Credentials(insidiousConfiguration.getUsername(), passwordText);
                            insidiousCredentials = createCredentialAttributes(
                                    "VideoBug", insidiousConfiguration.getUsername());
                            PasswordSafe.getInstance().set(insidiousCredentials, credentials);

                            ApplicationManager.getApplication().invokeLater(() -> {
                                InsidiousNotification.notifyMessage("VideoBug logged in at [" + serverUrl
                                                + "] for module [" + currentModule.getName() + "]",
                                        NotificationType.INFORMATION);

                            });
                        }
                    });


        } catch (UnauthorizedException e) {

            logger.error("Failed to signin for user " + usernameText, e);
            e.printStackTrace();
            if (credentialsToolbarWindow != null) {
                credentialsToolbarWindow.setErrorLabel("Sign in failed!");
            }
        } catch (Throwable e) {
            logger.error("failed to connect with server", e);
            InsidiousNotification.notifyMessage("Failed to connect with server - " + e.getMessage(),
                    NotificationType.ERROR);
        }
        ApplicationManager.getApplication().invokeLater(this::initiateUI);
    }

    private void selectExecutionSession() {
        ExecutionSession executionSession = new ExecutionSession();

        try {
            DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();

            if (1 < 2) {
                if (sessions.getItems().size() > 0) {
                    client.setSession(sessions.getItems().get(0));
                }
                return;
            }

            @NotNull List<String> sessionIds = sessions.getItems().stream().map(e -> {
                return "Session [" + e.getSessionId() + "] @" + e.getHostname() + " at " + e.getCreatedAt();
            }).collect(Collectors.toList());
            @NotNull JBPopup sessionPopup = JBPopupFactory.getInstance().createPopupChooserBuilder(
                    sessionIds
            ).setItemChosenCallback(new Consumer<String>() {
                @Override
                public void consume(String selectedExecutionSession) {
                    logger.info("session selected for module [" + currentModule.getName() + "] => " + selectedExecutionSession);
                    try {
                        String sessionId = selectedExecutionSession.split("\\]")[0].split("\\]")[1];
                        executionSession.setSessionId(sessionId);
                    } catch (Exception e) {
                        logger.error("failed to extract session id", e);
                        executionSession.setSessionId(sessions.getItems().get(0).getSessionId());
                    }
                    try {
                        client.setSession(executionSession);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).addListener(new JBPopupListener() {


                @Override
                public void onClosed(@NotNull LightweightWindowEvent event) {
                    try {
                        getTracesByType(getSelectedExceptionClassList());
                    } catch (IOException e) {
                        e.printStackTrace();
                        logger.error("failed to get errors", e);
                    }
                }
            }).createPopup();

            ApplicationManager.getApplication().invokeLater(() -> {
                sessionPopup.show(RelativePoint.getSouthEastOf(toolWindow.getComponent()));
            });


        } catch (APICallException | IOException e) {
            logger.error("failed to fetch sessions", e);
            e.printStackTrace();
        }


    }

    public List<String> getSelectedExceptionClassList() {
        return insidiousConfiguration.exceptionClassMap.entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void setupProject() {

        if (currentModule == null) {
            currentModule = ModuleManager.getInstance(project).getModules()[0];
        }

        try {
            logger.info("try to set project to - " + currentModule.getName());
            client.setProject(currentModule.getName());
//            getErrors(getSelectedExceptionClassList(), 0);
            generateAppToken();
        } catch (ProjectDoesNotExistException e1) {
            createProject(currentModule.getName(), new NewProjectCallback() {
                @Override
                public void error(String errorMessage) {

                    logger.error("failed to create project - {}", errorMessage);

                    InsidiousNotification.notifyMessage("Failed to create new project for ["
                                    + currentModule.getName() + "] on server [" + insidiousConfiguration.serverUrl,
                            NotificationType.ERROR);


                }

                @Override
                public void success(String projectId) {
                    logger.info("created new project for " + currentModule.getName() + " -> " + projectId);
                    ApplicationManager.getApplication().invokeLater(() -> setupProject());
                }
            });

        } catch (UnauthorizedException | IOException e) {
            logger.error("failed to setup project", e);
            InsidiousNotification.notifyMessage(e.getMessage(), NotificationType.ERROR);

        }
    }

    public void generateAppToken() {
        getProjectToken(new ProjectTokenCallback() {
            @Override
            public void error(String message) {
                InsidiousNotification.notifyMessage("Failed to generate app token for module [" + currentModule.getName() + "]",
                        NotificationType.ERROR);

                credentialsToolbarWindow.setErrorLabel(message);
            }

            @Override
            public void success(String token) {
                InsidiousService.this.appToken = token;
                ReadAction.run(() -> {
                    selectExecutionSession();
                });
                setAppTokenOnUi();
            }
        });
    }


    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {
        this.client.getProjectByName(projectName, getProjectCallback);
    }

    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        this.client.createProject(projectName, newProjectCallback);
    }

    public void getProjectToken(ProjectTokenCallback projectTokenCallback) {
        this.client.getProjectToken(projectTokenCallback);
    }

    public void getTracesByTypeName(
            List<String> classList,
            GetProjectSessionTracePointsCallback getProjectSessionErrorsCallback) {
        this.client.getTracesByObjectType(classList, -1, getProjectSessionErrorsCallback);
    }

    public void getTracesByValue(int pageNum, String traceValue) throws IOException {


        if (this.client.getCurrentSession() == null) {
            loadSession();
            return;
        }

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("trace", traceValue);
        eventProperties.put("sessionId", client.getCurrentSession().getSessionId());
        eventProperties.put("projectId", client.getCurrentSession().getProjectId());
        UsageInsightTracker.getInstance().RecordEvent("GetTracesByValue", eventProperties);

        insidiousConfiguration.addSearchQuery(traceValue, 0);
        logicBugs.updateSearchResultsList();

        AtomicInteger done = new AtomicInteger(0);
        ProgressManager.getInstance().run(new Task.Modal(project, "Unlogged", true) {

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Searching by value [" + traceValue + "] in session " + client.getCurrentSession().getSessionId());

                getTracesByStringValue(traceValue,
                        new GetProjectSessionTracePointsCallback() {
                            @Override
                            public void error(ExceptionResponse errorResponse) {
                                eventProperties.put("error", errorResponse.getError());
                                eventProperties.put("message", errorResponse.getMessage());
                                eventProperties.put("path", errorResponse.getPath());
                                eventProperties.put("trace", errorResponse.getTrace());
                                UsageInsightTracker.getInstance().RecordEvent("ErroredGetTracesByValue", eventProperties);

                                InsidiousNotification.notifyMessage("Failed to get traces: " + errorResponse.getMessage(),
                                        NotificationType.ERROR);
                                done.addAndGet(1);
                            }

                            @Override
                            public void success(List<TracePoint> tracePoints) {
                                done.addAndGet(1);
                                if (tracePoints.size() == 0) {

                                    eventProperties.put("trace", traceValue);
                                    UsageInsightTracker.getInstance().RecordEvent("NoResultGetTracesByValue", eventProperties);

                                    ApplicationManager.getApplication()
                                            .invokeAndWait(() -> InsidiousNotification.notifyMessage(
                                                    "No results matched for string [" + traceValue + "]",
                                                    NotificationType.INFORMATION));
                                } else {
                                    eventProperties.put("trace", traceValue);
                                    eventProperties.put("count", tracePoints.size());
                                    UsageInsightTracker.getInstance().RecordEvent("YesResultGetTracesByValue", eventProperties);

                                    insidiousConfiguration.addSearchQuery(traceValue, tracePoints.size());
                                    tracePoints = tracePoints.stream().filter(e -> e.getLinenum() != 0)
                                            .collect(Collectors.toList());
                                    tracePoints.forEach(e -> {
                                        e.setExecutionSessionId(client.getCurrentSession().getSessionId());
                                    });
                                    logicBugs.setTracePoints(tracePoints);
                                    logicBugs.updateSearchResultsList();
                                }
                            }
                        });
                while (client instanceof VideobugNetworkClient && done.get() == 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new ProcessCanceledException(e);
                    }
                }
            }
        });

    }


    public void getTracesByType(List<String> classList) throws IOException {
        JSONObject eventProperties = new JSONObject();
        if (this.client.getCurrentSession() == null) {
            loadSession();
            return;
        }

        eventProperties.put("query", classList.toString());
        eventProperties.put("sessionId", client.getCurrentSession().getSessionId());
        eventProperties.put("projectId", client.getCurrentSession().getProjectId());
        UsageInsightTracker.getInstance().RecordEvent("GetTracesByType", eventProperties);


        String sessionId = client.getCurrentSession().getSessionId();
        logger.info("get traces for session - " + sessionId);

        ProgressManager.getInstance().run(new Task.Modal(project, "Unlogged", true) {
            public void run(ProgressIndicator indicator) {
                indicator.setText("Searching in session: " + client.getCurrentSession().getSessionId());
                indicator.setText2("Filtering by class types");
                AtomicInteger done = new AtomicInteger(0);
                getTracesByTypeName(classList, new GetProjectSessionTracePointsCallback() {

                    @Override
                    public void error(ExceptionResponse errorResponse) {
                        logger.error("failed to get trace points from server - {}", errorResponse.getMessage());
                        done.addAndGet(1);

                        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                            ProgressIndicatorProvider.getGlobalProgressIndicator().cancel();
                        }


                        JSONObject eventProperties = new JSONObject();
                        eventProperties.put("error", errorResponse.getError());
                        eventProperties.put("message", errorResponse.getMessage());
                        eventProperties.put("path", errorResponse.getPath());
                        eventProperties.put("trace", errorResponse.getTrace());
                        UsageInsightTracker.getInstance().RecordEvent("ErroredGetTracesByType", eventProperties);


                        String message = errorResponse.getMessage();
                        if (message == null) {
                            message = "No results matched";
                        }
                        InsidiousNotification.notifyMessage("Failed to get trace points from server: "
                                + message, NotificationType.ERROR);
                    }

                    @Override
                    public void success(List<TracePoint> tracePoints) {
                        logger.info("got [" + tracePoints.size() + "] trace points from server");


                        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                            ProgressIndicatorProvider.getGlobalProgressIndicator().cancel();
                        }


                        if (tracePoints.size() == 0) {

                            JSONObject eventProperties = new JSONObject();
                            UsageInsightTracker.getInstance().RecordEvent("NoResultGetTracesByType", eventProperties);

                            InsidiousNotification.notifyMessage(
                                    "No Exception data events matched in the last session [" + sessionId + "]",
                                    NotificationType.INFORMATION);

                        } else {
                            JSONObject eventProperties = new JSONObject();
                            eventProperties.put("count", tracePoints.size());
                            UsageInsightTracker.getInstance().RecordEvent("YesResultGetTracesByType", eventProperties);

                            tracePoints = tracePoints.stream()
                                    .filter(e -> e.getLinenum() != 0).collect(Collectors.toList());
                            bugsTable.setTracePoints(tracePoints);
                        }
                        done.addAndGet(1);

                    }
                });
                while (client instanceof VideobugNetworkClient && done.get() == 0) {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        throw new ProcessCanceledException(e);
                    }
                }
            }
        });


    }

    private void getTracesByStringValue(String traceValue,
                                        GetProjectSessionTracePointsCallback getProjectSessionErrorsCallback) {
        try {
            this.client.getTracesByObjectValue(traceValue, getProjectSessionErrorsCallback);
        } catch (ProcessCanceledException pce) {
            throw pce;
        } catch (Throwable e) {
            ExceptionResponse errorResponse = new ExceptionResponse();
            errorResponse.setMessage("Failed to search: " + e.getMessage());
            errorResponse.setTrace(traceValue);
            errorResponse.setStatus(0);
            getProjectSessionErrorsCallback.error(errorResponse);
        }

    }


    public synchronized void startDebugSession() {
        logger.info("start debug session");

        debugSession = getActiveDebugSession(project.getService(XDebuggerManager.class).getDebugSessions());


        if (debugSession != null) {
            return;
        }
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("module", currentModule.getName());
        UsageInsightTracker.getInstance().RecordEvent("StartDebugSession", eventProperties);

        @NotNull RunConfiguration runConfiguration = ConfigurationTypeUtil.
                findConfigurationType(InsidiousRunConfigType.class).createTemplateConfiguration(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(project,
                        new InsidiousExecutor(), runConfiguration).build();
                ProgramRunnerUtil.executeConfiguration(env, false, false);
            } catch (Throwable e) {
                logger.error("failed to execute configuration", e);
            }
        });


    }

    public Project getProject() {
        return project;
    }

    public InsidiousJavaDebugProcess getDebugProcess() {
        return debugProcess;
    }

    public void setDebugProcess(InsidiousJavaDebugProcess debugProcess) {
        this.debugProcess = debugProcess;

        if (this.logicBugs != null && debugProcess != null) {
            this.logicBugs.bringToFocus(toolWindow);
        }
    }


    public void setDebugSession(XDebugSession session) {
        this.debugSession = session;
        if (session != null && pendingSelectTrace != null) {
            TracePoint selectNow = pendingSelectTrace;
            pendingSelectTrace = null;
            setTracePoint(selectNow);
        }
    }

    public void setConnector(InsidiousJDIConnector connector) {
        this.connector = connector;
        if (pendingTrace != null) {
            try {
                connector.setTracePoint(pendingTrace, null);
                debugProcess.startPausing();
            } catch (Exception e) {
                logger.error("failed to set trace point", e);
            }
            pendingTrace = null;
        }
    }

    public void showCredentialsWindow() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Content exceptionContent = toolWindow.getContentManager().findContent("Exceptions");
                Content traceContent = toolWindow.getContentManager().findContent("Traces");
                if (exceptionContent != null) {
                    toolWindow.getContentManager().removeContent(exceptionContent, true);
                }
                if (traceContent != null) {
                    toolWindow.getContentManager().removeContent(traceContent, true);
                }
                ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                ConfigurationWindow credentialsToolbar = new ConfigurationWindow(project, toolWindow);
                Content credentialContent = contentFactory.createContent(credentialsToolbar.getContent(), "Credentials", false);
                toolWindow.getContentManager().addContent(credentialContent);
            }
        });
    }

    public void addAgentToRunConfig() {


        List<RunnerAndConfigurationSettings> allSettings
                = project.getService(RunManager.class).getAllSettings();

        for (RunnerAndConfigurationSettings runSetting : allSettings) {
            logger.info("runner config - " + runSetting.getName());

            if (runSetting.getConfiguration() instanceof ApplicationConfiguration) {
                ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) runSetting.getConfiguration();
                String currentVMParams = applicationConfiguration.getVMParameters();
                String newVmOptions = currentVMParams;
                newVmOptions = VideobugUtils.addAgentToVMParams(currentVMParams, javaAgentString);
                applicationConfiguration.setVMParameters(newVmOptions.trim());
            }
        }
    }


    public void initiateUI() {
        logger.info("initiate ui");
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();


        if (isLoggedIn() && bugsTable == null) {

            bugsTable = new SearchByTypeWindow(project, this);
            logicBugs = new SearchByValueWindow(project, this);

            @NotNull Content bugsContent = contentFactory.createContent(bugsTable.getContent(), "Exceptions", false);
            this.toolWindow.getContentManager().addContent(bugsContent);

            Content traceContent = contentFactory.createContent(logicBugs.getContent(), "Traces", false);
            @NotNull Content logicbugContent = contentFactory.createContent(logicBugs.getContent(), "Traces", false);
            this.toolWindow.getContentManager().addContent(logicbugContent);
            this.logicBugs.bringToFocus(toolWindow);

            Content content = this.toolWindow.getContentManager().findContent("Exceptions");
            if (content == null) {
                this.toolWindow.getContentManager().addContent(bugsContent);
            }
            Content traceContent2 = this.toolWindow.getContentManager().findContent("Traces");
            if (traceContent2 == null) {
                this.toolWindow.getContentManager().addContent(traceContent);
            }
        }
        if (isLoggedIn() && client.getProject() == null) {
            logger.info("user is logged in by project is null, setting up project");
            setupProject();
        }
        if (credentialsToolbarWindow == null) {
            credentialsToolbarWindow = new ConfigurationWindow(project, this.toolWindow);
            @NotNull Content credentialContent = contentFactory.createContent(credentialsToolbarWindow.getContent(), "Credentials", false);
            this.toolWindow.getContentManager().addContent(credentialContent);
        }

    }

    public String getJavaAgentString() {
        return javaAgentString;
    }

    public String getVideoBugAgentPath() {
        return Constants.VIDEOBUG_AGENT_PATH.toAbsolutePath().toString();
    }

    public void setAppTokenOnUi() {
        logger.info("set app token - " + appToken + " with package name " + packageName);

        String[] vmParamsToAdd = new String[]{
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH
                        + "=i=" + (packageName == null ? DEFAULT_PACKAGE_NAME : packageName.replaceAll("\\.", "/"))
                        + ",server=" + (insidiousConfiguration != null ? insidiousConfiguration.serverUrl : "https://cloud.bug.video")
                        + ",token=" + appToken + "\""
        };

        javaAgentString = String.join(" ", vmParamsToAdd);

        if (credentialsToolbarWindow != null) {
            credentialsToolbarWindow.setText(javaAgentString);
        }
        if (appToken != null && !Objects.equals(packageName, "YOUR.PACKAGE.NAME")) {
            addAgentToRunConfig();
        }
    }

    public void loadSession() throws IOException {

        if (currentModule == null) {
            currentModule = ModuleManager.getInstance(project).getModules()[0];
        }

//        ApplicationManager.getApplication().invokeLater(this::startDebugSession);
        this.client.getProjectSessions(new GetProjectSessionsCallback() {
            @Override
            public void error(String message) {
                logger.error("failed to load project sessions - {}", message);
                ApplicationManager.getApplication().invokeLater(() ->
                        InsidiousNotification.notifyMessage("No sessions found for module [" + currentModule.getName() + "]",
                                NotificationType.INFORMATION));
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) throws IOException {
                logger.info("got [" + executionSessionList.size() + "] sessions for project");
                if (executionSessionList.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() -> {

                        if (InsidiousNotification.balloonNotificationGroup != null) {
                            InsidiousNotification.notifyMessage("No sessions found for project " + currentModule.getName() +
                                            ". Start recording new sessions with the java agent",
                                    NotificationType.INFORMATION);
                        } else {

                            InsidiousNotification.notifyMessage(
                                    "No sessions found" + " for project " + currentModule.getName() +
                                            " start recording new sessions with the java agent",
                                    NotificationType.INFORMATION);
                        }
                    });
                    return;
                }

                client.setSession(executionSessionList.get(0));
//                getErrors(getSelectedExceptionClassList(), 0);

            }
        });
    }

    public void setTracePoint(TracePoint selectedTrace) {

        JSONObject eventProperties = new JSONObject();
        eventProperties.put("value", selectedTrace.getValue());
        eventProperties.put("classId", selectedTrace.getClassId());
        eventProperties.put("className", selectedTrace.getClassname());
        eventProperties.put("dataId", selectedTrace.getDataId());
        eventProperties.put("fileName", selectedTrace.getFilename());
        eventProperties.put("nanoTime", selectedTrace.getNanoTime());
        eventProperties.put("lineNumber", selectedTrace.getLinenum());
        eventProperties.put("threadId", selectedTrace.getThreadId());


        UsageInsightTracker.getInstance().RecordEvent("FetchByTracePoint", null);

        if (debugSession == null ||  getActiveDebugSession(project.getService(XDebuggerManager.class).getDebugSessions()) == null) {
            UsageInsightTracker.getInstance().RecordEvent("StartDebugSessionAtSelectTracepoint", null);
            pendingSelectTrace = selectedTrace;
            startDebugSession();
            return;
        }

        ProgressManager.getInstance().run(new Task.Modal(project, "Unlogged", true) {
            public void run(ProgressIndicator indicator) {

                try {
                    logger.info("set trace point in connector => " + selectedTrace.getClassname());
                    indicator.setText("Loading trace point " + selectedTrace.getClassname() + ":"
                            + selectedTrace.getLinenum() + " for value " + selectedTrace.getValue() + " in thread "
                            + selectedTrace.getThreadId() + " at time " + DateFormat.getInstance().format(
                            new Date(selectedTrace.getNanoTime())
                    ));
                    if (connector != null) {
                        connector.setTracePoint(selectedTrace, indicator);
                    } else {
                        pendingTrace = selectedTrace;
                    }


                } catch (ProcessCanceledException pce) {
                    throw pce;
                } catch (Exception e) {
                    logger.error("failed to set trace point", e);
                    InsidiousNotification.notifyMessage("Failed to set select trace point " + e.getMessage(),
                            NotificationType.ERROR);
                    return;
                } finally {
                    indicator.stop();
                }

                if (debugSession.isPaused()) {
                    debugSession.resume();
                }
                debugSession.pause();


            }
        });


    }

    public void setExceptionClassList(Map<String, Boolean> exceptionClassList) {
        insidiousConfiguration.exceptionClassMap = exceptionClassList;
    }


    public VideobugClientInterface getClient() {
        return client;
    }

    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        init();
    }

    public Map<String, Boolean> getDefaultExceptionClassList() {
        return insidiousConfiguration.exceptionClassMap;
    }

    public InsidiousConfigurationState getConfiguration() {
        return insidiousConfiguration;
    }

    public void logout() {
        InsidiousConfigurationState newConfig = new InsidiousConfigurationState();
        insidiousConfiguration.exceptionClassMap = newConfig.exceptionClassMap;
        insidiousConfiguration.setServerUrl(newConfig.serverUrl);
        insidiousConfiguration.setUsername(newConfig.username);

        Credentials credentials = PasswordSafe.getInstance().get(insidiousCredentials);
        if (credentials != null) {
            PasswordSafe.getInstance().set(insidiousCredentials, null);
        }


    }

    public void ensureAgentJar(boolean overwrite) {
        checkAndEnsureJavaAgent(overwrite, new AgentJarDownloadCompleteCallback() {
            @Override
            public void error(String message) {
                InsidiousNotification.notifyMessage(
                        "Failed to download java agent: " + message, NotificationType.ERROR
                );
            }

            @Override
            public void success(String url, String path) {
                InsidiousNotification.notifyMessage(
                        "Agent jar download complete", NotificationType.INFORMATION
                );
            }
        });

    }

    public void refreshSession() throws APICallException, IOException {
        logger.info("fetch latest session for module: " + currentModule.getName());
        client.setProject(currentModule.getName());

        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        if (sessions.getItems().size() == 0) {
            InsidiousNotification.notifyMessage("No sessions available for module ["
                            + currentModule.getName() + "]",
                    NotificationType.ERROR);
            return;
        }
        client.setSession(sessions.getItems().get(0));
    }

    public void focusExceptionWindow() {
        if (this.logicBugs == null) {
            initiateUI();
        }
        if (this.logicBugs != null) {
            this.logicBugs.bringToFocus(toolWindow);
        }
    }

    public void initiateUseLocal() {
        client = new VideobugLocalClient(Constants.VIDEOBUG_AGENT_PATH + "/sessions");
        UsageInsightTracker.getInstance().RecordEvent("InitiateUseLocal", null);


        ReadAction.run(() -> this.ensureAgentJar(false));
        ReadAction.run(InsidiousService.this::setupProject);

        ApplicationManager.getApplication().invokeLater(() -> {
            InsidiousService.this.initiateUI();
            InsidiousNotification.notifyMessage("VideoBug logged in at [" + "disk://localhost"
                            + "] for module [" + currentModule.getName() + "]",
                    NotificationType.INFORMATION);
            Messages.showMessageDialog("Copy the JVM parameter and configure it for your application" +
                            "and start running your application to start record.",
                    "Videobug", Messages.getInformationIcon());
        });
    }

    @Override
    public void dispose() {
        if (this.client != null) {
            this.client.close();
            this.client = null;
            currentModule = null;
        }
    }

//    public void uploadSessionToServer() throws IOException {
//        String pathToSessions = project.getBasePath();
//        assert pathToSessions != null;
//        Path.of(pathToSessions).toFile().mkdirs();
//
//        VideobugLocalClient localClient = new VideobugLocalClient(pathToSessions);
//        localClient.getProjectSessions(new GetProjectSessionsCallback() {
//            @Override
//            public void error(String message) {
//                InsidiousNotification.notifyMessage("Session upload failed - " + message,
//                        NotificationType.ERROR);
//            }
//
//            @Override
//            public void success(List<ExecutionSession> executionSessionList) throws IOException {
//                if (executionSessionList.size() == 0) {
//                    InsidiousNotification.notifyMessage("No sessions found. Run the application with the videobug agent to create a session",
//                            NotificationType.ERROR);
//                    return;
//                }
//                ExecutionSession latestSession = executionSessionList.get(0);
//                localClient.setSession(latestSession);
//                List<File> sessionArchives = localClient.getSessionFiles();
//                InsidiousNotification.notifyMessage("" +
//                                "Uploading " + sessionArchives.size() + " archives to server ["
//                                + InsidiousService.this.client.getEndpoint() + "]",
//                        NotificationType.INFORMATION);
//                for (File sessionArchive : sessionArchives) {
//                    sendPOSTRequest(
//                            InsidiousService.this.client.getEndpoint() + "/checkpoint/uploadArchive",
//                            localClient.getCurrentSession().getSessionId(),
//                            sessionArchive.getAbsolutePath(),
//                            InsidiousService.this.appToken
//                    );
//                }
//            }
//        });
//    }

    public void sendPOSTRequest(String url, String sessionId, String attachmentFilePath, String token) throws IOException {

        String charset = "UTF-8";
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "insidious/1.0.1");
        headers.put("Authorization", "Bearer " + token);

        MultipartUtility form = new MultipartUtility(url, charset, headers);

        File binaryFile = new File(attachmentFilePath);
        form.addFilePart("file", binaryFile);
        form.addFormField("sessionId", sessionId);
        form.addFormField("hostname", HOSTNAME);

        String response = form.finish();

    }


    public void generateAndUploadReport() {
        UsageInsightTracker.getInstance().RecordEvent("DiagnosticReport", null);
        DiagnosticService diagnosticService = new DiagnosticService(new VersionManager(), this.project, this.currentModule);
        diagnosticService.generateAndUploadReport();
    }
}
