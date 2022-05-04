package com.insidious.plugin.factory;

import com.amplitude.Amplitude;
import com.amplitude.Event;
import com.insidious.plugin.callbacks.*;
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
import com.insidious.plugin.ui.CredentialsToolbar;
import com.insidious.plugin.ui.HorBugTable;
import com.insidious.plugin.ui.LogicBugs;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.visitor.GradleFileVisitor;
import com.insidious.plugin.visitor.PomFileVisitor;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.ui.FragmentedSettings;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
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
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes;

@Storage("insidious.xml")
public class InsidiousService implements Disposable {
    public static final String HOSTNAME = System.getProperty("user.name");
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private final Project project;
    private final InsidiousConfigurationState insidiousConfiguration;
    private final Path videoBugHomePath = Path.of(System.getProperty("user.home"), ".VideoBug");
    private final String agentJarName = "videobug-java-agent.jar";
    private final Path videoBugAgentPath = Path.of(videoBugHomePath.toAbsolutePath().toString(), agentJarName);
    private final String DefaultPackageName = "YOUR.PACKAGE.NAME";
    private final Amplitude amplitudeClient;
    private final ExecutorService threadPool;
    private VideobugClientInterface client;
    private NotificationGroup notificationGroup;
    private Module currentModule;
    private String packageName = "YOUR.PACKAGE.NAME";
    private HorBugTable bugsTable;
    private LogicBugs logicBugs;
    private XDebugSession debugSession;
    private InsidiousJavaDebugProcess debugProcess;
    private InsidiousJDIConnector connector;
    private CredentialsToolbar credentialsToolbarWindow;
    private ToolWindow toolWindow;
    private CredentialAttributes insidiousCredentials;
    private String appToken;
    private String javaAgentString;
    private TracePoint pendingTrace;
    private TracePoint pendingSelectTrace;


    public InsidiousService(Project project) {
        this.project = project;
        amplitudeClient = Amplitude.getInstance("PLUGIN");
        amplitudeClient.init("45a070ba1c5953b71f0585b0fdb19027");
        threadPool = Executors.newFixedThreadPool(4);

        logger.info("started insidious service - project name [{}]", project.getName());
        if (ModuleManager.getInstance(project).getModules().length == 0) {
            logger.warn("no module found in the project");
        } else {
            currentModule = ModuleManager.getInstance(project).getModules()[0];
            logger.info("current module [{}]", currentModule.getName());
        }

        debugSession = getActiveDebugSession(project.getService(XDebuggerManager.class).getDebugSessions());

        ReadAction.nonBlocking(this::getProjectPackageName).submit(threadPool);
        threadPool.submit(this::logLogFileLocation);

        this.insidiousConfiguration = project.getService(InsidiousConfigurationState.class);

        String pathToSessions = project.getBasePath();
        assert pathToSessions != null;
        Path.of(pathToSessions).toFile().mkdirs();
        this.client = new VideobugLocalClient(pathToSessions);
//        this.client = new VideobugNetworkClient(insidiousConfiguration.serverUrl);
        ReadAction.nonBlocking(InsidiousService.this::checkAndEnsureJavaAgentCache).submit(threadPool);

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
                Map<String, List<TracePoint>> pointsByClass = tracePoints.stream().collect(Collectors.groupingBy(e -> e.getClassname()));
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


                Notification notification = InsidiousNotification.
                        balloonNotificationGroup.createNotification("New exception",
                                messageBuilder.toString(), NotificationType.INFORMATION);
                Notifications.Bus.notify(notification);
                getHorBugTable().setTracePoints(tracePoints);
            }
        });
    }

    private XDebugSession getActiveDebugSession(XDebugSession[] debugSessions) {
        for (XDebugSession session : debugSessions) {
            if (session.getDebugProcess() instanceof InsidiousJavaDebugProcess) {
                return session;
            }
        }
        return null;
    }

    public NotificationGroup getNotificationGroup() {
        return notificationGroup;
    }

    public void checkAndEnsureJavaAgentCache() {
        checkAndEnsureJavaAgent(false, new AgentJarDownloadCompleteCallback() {
            @Override
            public void error(String message) {

            }

            @Override
            public void success(String url, String path) {

            }
        });
    }

    public void checkAndEnsureJavaAgent(boolean overwrite, AgentJarDownloadCompleteCallback agentJarDownloadCompleteCallback) {

        File insidiousFolder = new File(videoBugHomePath.toString());
        if (!insidiousFolder.exists()) {
            insidiousFolder.mkdir();
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
                    logger.info("agent download link: {}, downloading to path [{}]",
                            url, videoBugAgentPath.toAbsolutePath());
                    client.downloadAgentFromUrl(url, videoBugAgentPath.toString(), overwrite);
                    setAppTokenOnUi();
                    agentJarDownloadCompleteCallback.success(url, videoBugAgentPath.toString());
                } catch (Exception e) {
                    logger.info("failed to download agent - ", e);
                }
            }
        });

    }

    private void logLogFileLocation() {
        String logFileNotificationContent = "Insidious log file location - " +
                LoggerUtil.getLogFilePath();


        notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("com.insidious");
        if (notificationGroup != null) {
            notificationGroup
                    .createNotification(logFileNotificationContent, NotificationType.INFORMATION)
                    .notify(project);
        }
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
        return videoBugAgentPath.toAbsolutePath().toString();
    }

    public void init() {

        if (!StringUtil.isEmpty(insidiousConfiguration.getUsername())) {
            logger.info("username is not empty in configuration - [{}] with server url [{}]",
                    insidiousConfiguration.getUsername(),
                    insidiousConfiguration.getServerUrl());
            insidiousCredentials = createCredentialAttributes("VideoBug", insidiousConfiguration.getUsername());
            if (insidiousCredentials != null) {
                Credentials credentials = PasswordSafe.getInstance().get(insidiousCredentials);
                if (credentials != null) {
                    String password = credentials.getPasswordAsString();
                    try {
                        signin(insidiousConfiguration.serverUrl, insidiousConfiguration.username, password);
                        return;
                    } catch (IOException e) {
                        logger.error("failed to signin", e);

                        Notifications.Bus.notify(notificationGroup
                                        .createNotification("Failed to sign in -" + e.getMessage(),
                                                NotificationType.ERROR),
                                project);
                    }
                }
            }
        }
        ApplicationManager.getApplication().invokeLater(this::initiateUI);
    }

    public boolean isLoggedIn() {
        return this.client.getToken() != null;
    }

    public HorBugTable getHorBugTable() {
        return bugsTable;
    }

    public void setHorBugTable(HorBugTable bugsTable) {
        this.bugsTable = bugsTable;
    }

    public LogicBugs getLogicBugs() {
        return this.logicBugs;
    }

    public void setLogicBugs(LogicBugs logicBugs) {
        this.logicBugs = logicBugs;
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
        logger.info("signin server [{}] with username [{}]", serverUrl, usernameText);
        if (!isValidEmailAddress(usernameText)) {
            credentialsToolbarWindow.setErrorLabel("Enter a valid email address");
            ApplicationManager.getApplication().invokeLater(this::initiateUI);
            return;
        }

        if (passwordText == null) {
            ApplicationManager.getApplication().invokeLater(this::initiateUI);
            if (credentialsToolbarWindow != null) {
                credentialsToolbarWindow.setErrorLabel("Enter a valid Password");
            }
            return;
        }

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

                            if (notificationGroup != null) {
                                Notifications.Bus.notify(notificationGroup
                                                .createNotification("Failed to login VideoBug at [" + serverUrl
                                                                + "] for module [" + currentModule.getName() + "]",
                                                        NotificationType.ERROR),
                                        project);
                            }

                        }

                        @Override
                        public void success(String token) {
                            ReadAction.nonBlocking(InsidiousService.this::checkAndEnsureJavaAgentCache)
                                    .submit(threadPool);
                            ReadAction.nonBlocking(InsidiousService.this::startDebugSession)
                                    .submit(threadPool);

                            amplitudeClient.logEvent(new Event("SignedIn", HOSTNAME));

                            Credentials credentials = new Credentials(insidiousConfiguration.getUsername(), passwordText);
                            insidiousCredentials = createCredentialAttributes(
                                    "VideoBug", insidiousConfiguration.getUsername());
                            PasswordSafe.getInstance().set(insidiousCredentials, credentials);

                            ApplicationManager.getApplication().invokeLater(() -> {

                                if (notificationGroup != null) {
                                    Notifications.Bus.notify(notificationGroup
                                                    .createNotification("VideoBug logged in at [" + serverUrl
                                                                    + "] for module [" + currentModule.getName() + "]",
                                                            NotificationType.INFORMATION),
                                            project);
                                }

                            });
                        }
                    });


        } catch (UnauthorizedException e) {

            logger.error("Failed to signin for user [{}]", usernameText, e);
            e.printStackTrace();
            if (credentialsToolbarWindow != null) {
                credentialsToolbarWindow.setErrorLabel("Sign in failed!");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Notifications.Bus.notify(notificationGroup
                            .createNotification("Failed to connect with server - " + e.getMessage(),
                                    NotificationType.ERROR),
                    project);
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
                    logger.info("session selected for module [" + currentModule.getName() + "] => ", selectedExecutionSession);
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
                        getErrors(getSelectedExceptionClassList(), 0);
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
            logger.info("try to set project to - [{}]", currentModule.getName());
            client.setProject(currentModule.getName());
            getErrors(getSelectedExceptionClassList(), 0);
            generateAppToken();
        } catch (ProjectDoesNotExistException e1) {
            createProject(currentModule.getName(), new NewProjectCallback() {
                @Override
                public void error(String errorMessage) {

                    logger.error("failed to create project - {}", errorMessage);

                    Notifications.Bus.notify(notificationGroup
                                    .createNotification("Failed to create new project for ["
                                                    + currentModule.getName() + "] on server [" + insidiousConfiguration.serverUrl,
                                            NotificationType.ERROR),
                            project);


                }

                @Override
                public void success(String projectId) {
                    logger.info("created new project for [{}] -> [{}]", currentModule.getName(), projectId);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        setupProject();
                    });
                }
            });

        } catch (UnauthorizedException | IOException e) {
            e.printStackTrace();
            Notifications.Bus.notify(notificationGroup
                            .createNotification(e.getMessage(),
                                    NotificationType.ERROR),
                    project);

        }
    }

    public void generateAppToken() {
        getProjectToken(new ProjectTokenCallback() {
            @Override
            public void error(String message) {
                Notifications.Bus.notify(notificationGroup
                                .createNotification("Failed to generate app token for module [" + currentModule.getName() + "]",
                                        NotificationType.ERROR),
                        project);

                credentialsToolbarWindow.setErrorLabel(message);
            }

            @Override
            public void success(String token) {
                InsidiousService.this.appToken = token;
                ReadAction.nonBlocking(() -> {
                    selectExecutionSession();
                }).submit(threadPool);
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
            GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) throws IOException {
        this.client.getTracesByObjectType(classList, -1, getProjectSessionErrorsCallback);
    }

    public void getTraces(int pageNum, String traceValue) throws IOException {
        amplitudeClient.logEvent(new Event("GetTracesByValue", HOSTNAME));

        if (this.client.getCurrentSession() == null) {
            loadSession();
            return;
        }

        insidiousConfiguration.addSearchQuery(traceValue, 0);
        logicBugs.updateSearchResultsList();


        getTracesByStringValue(traceValue,
                new GetProjectSessionErrorsCallback() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {
                        Notifications.Bus.notify(notificationGroup
                                        .createNotification("Failed to get traces: " + errorResponse.getMessage(),
                                                NotificationType.ERROR),
                                project);

                    }

                    @Override
                    public void success(List<TracePoint> tracePoints) {
                        if (tracePoints.size() == 0) {
                            ApplicationManager.getApplication().invokeAndWait(() -> {

                                Notifications.Bus.notify(notificationGroup
                                                .createNotification("No data available, or data may have been deleted!",
                                                        NotificationType.INFORMATION),
                                        project);


                            });
                        } else {
                            insidiousConfiguration.addSearchQuery(traceValue, tracePoints.size());
                            tracePoints.forEach(e -> {
                                e.setExecutionSessionId(client.getCurrentSession().getSessionId());
                            });
                            logicBugs.setTracePoints(tracePoints);
                            logicBugs.updateSearchResultsList();
                        }
                    }
                });
    }

    public void getErrors(List<String> classList, int pageNum) throws IOException {
        amplitudeClient.logEvent(new Event("GetTracesByType", HOSTNAME));


        if (this.client.getCurrentSession() == null) {
            loadSession();
            return;
        }
        logger.info("get traces for session - [{}]", client.getCurrentSession().getSessionId());


        ReadAction.nonBlocking(() -> {
            try {
                getTracesByTypeName(classList,
                        new GetProjectSessionErrorsCallback() {
                            @Override
                            public void error(ExceptionResponse errorResponse) {
                                logger.error("failed to get trace points from server - {}", errorResponse);

                                Notifications.Bus.notify(notificationGroup
                                                .createNotification("Failed to get trace points from server: "
                                                                + errorResponse.getMessage(),
                                                        NotificationType.ERROR),
                                        project);

                            }

                            @Override
                            public void success(List<TracePoint> tracePoints) {
                                logger.info("got [{}] trace points from server", tracePoints.size());
                                if (tracePoints.size() == 0) {
                                    ApplicationManager.getApplication()
                                            .runWriteAction(
                                                    () -> Notifications.Bus.notify(notificationGroup
                                                            .createNotification(
                                                                    "No Exception data events matched in the last session",
                                                                    NotificationType.INFORMATION), project));
                                } else {

                                    tracePoints.forEach(e -> {
                                        e.setExecutionSessionId(client.getCurrentSession().getSessionId());
                                    });


                                    bugsTable.setTracePoints(tracePoints);
                                }
                            }
                        });
            } catch (IOException e) {
                logger.error("failed to get trace points by type");
            }
        }).submit(threadPool);

    }

    public void getTracesByStringValue(String traceId,
                                       GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) throws IOException {
        ReadAction.nonBlocking(() -> {
            try {
                this.client.getTracesByObjectValue(traceId, getProjectSessionErrorsCallback);
            } catch (Throwable e) {
                logger.error("failed to get traces by value", e);
            }
        }).submit(threadPool);

    }


    public synchronized void startDebugSession() {
        logger.info("start debug session");
        boolean hasInsidiousDebugSession = false;

        if (debugSession != null) {
            return;
        }

        debugSession = getActiveDebugSession(project.getService(XDebuggerManager.class).getDebugSessions());


        if (debugSession != null) {
            return;
        }
        amplitudeClient.logEvent(new Event("StartDebugSession", HOSTNAME));

        @NotNull RunConfiguration runConfiguration = ConfigurationTypeUtil.
                findConfigurationType(InsidiousRunConfigType.class).createTemplateConfiguration(project);

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ExecutionEnvironment env = ExecutionEnvironmentBuilder.create(project,
                        new InsidiousExecutor(), runConfiguration).build();
                ProgramRunnerUtil.executeConfiguration(env, false, false);
            } catch (ExecutionException e) {
                e.printStackTrace();
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
                connector.setTracePoint(pendingTrace);
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
                CredentialsToolbar credentialsToolbar = new CredentialsToolbar(project, toolWindow);
                Content credentialContent = contentFactory.createContent(credentialsToolbar.getContent(), "Credentials", false);
                toolWindow.getContentManager().addContent(credentialContent);
            }
        });
    }

    public void addAgentToRunConfig() {


        List<RunnerAndConfigurationSettings> allSettings
                = project.getService(RunManager.class).getAllSettings();

        for (RunnerAndConfigurationSettings runSetting : allSettings) {
            logger.info("runner config - {}", runSetting.getName());

            if (runSetting.getConfiguration() instanceof ApplicationConfiguration) {
                ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) runSetting.getConfiguration();
                @NotNull List<FragmentedSettings.Option> applicationOptions = applicationConfiguration.getSelectedOptions();

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

            bugsTable = new HorBugTable(project, this);
            logicBugs = new LogicBugs(project, this);

            @NotNull Content bugsContent = contentFactory.createContent(bugsTable.getContent(), "Exceptions", false);
            this.toolWindow.getContentManager().addContent(bugsContent);

            Content traceContent = contentFactory.createContent(logicBugs.getContent(), "Traces", false);
            @NotNull Content logicbugContent = contentFactory.createContent(logicBugs.getContent(), "Traces", false);
            this.toolWindow.getContentManager().addContent(logicbugContent);

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
//        if (credentialsToolbarWindow == null) {
//            credentialsToolbarWindow = new CredentialsToolbar(project, this.toolWindow);
//            @NotNull Content credentialContent = contentFactory.createContent(credentialsToolbarWindow.getContent(), "Credentials", false);
//            this.toolWindow.getContentManager().addContent(credentialContent);
//        }

    }

    public String getJavaAgentString() {
        return javaAgentString;
    }

    public String getVideoBugAgentPath() {
        return videoBugAgentPath.toAbsolutePath().toString();
    }

    public void setAppTokenOnUi() {
        logger.info("set app token - {} with package name [{}]" + appToken, packageName);

        String[] vmParamsToAdd = new String[]{
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "-javaagent:\"" + videoBugAgentPath
                        + "=i=" + (packageName == null ? DefaultPackageName : packageName.replaceAll("\\.", "/"))
                        + ",server=" + insidiousConfiguration.serverUrl
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
                ApplicationManager.getApplication().invokeLater(() -> {
                    Notifications.Bus.notify(notificationGroup
                                    .createNotification("No sessions found for module [" + currentModule.getName() + "]",
                                            NotificationType.INFORMATION),
                            project);
                });
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) throws IOException {
                logger.info("got [{}] sessions for project", executionSessionList.size());
                if (executionSessionList.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() -> {

                        if (notificationGroup != null) {
                            Notifications.Bus.notify(notificationGroup
                                            .createNotification("No sessions found for project " + currentModule.getName() +
                                                            ". Start recording new sessions with the java agent",
                                                    NotificationType.INFORMATION),
                                    project);
                        } else {

                            Notifications.Bus.notify(new Notification("com.insidious", "No sessions found for project " + currentModule.getName() +
                                            ". Start recording new sessions with the java agent",
                                            NotificationType.INFORMATION),
                                    project);
                        }
                    });
                    return;
                }

                client.setSession(executionSessionList.get(0));
                getErrors(getSelectedExceptionClassList(), 0);

            }
        });
    }

    public void setTracePoint(TracePoint selectedTrace) {
        amplitudeClient.logEvent(new Event("FetchByTracePoint", HOSTNAME));

        if (debugSession == null) {
            startDebugSession();
            pendingSelectTrace = selectedTrace;
            return;
        }


        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                logger.info("set trace point in connector => [{}]", selectedTrace.getClassname());

                if (connector != null) {
                    connector.setTracePoint(selectedTrace);
                } else {
                    pendingTrace = selectedTrace;
                }


            } catch (Exception e) {
                e.printStackTrace();
                Notifications.Bus.notify(notificationGroup
                                .createNotification("Failed to set select trace point " + e.getMessage(),
                                        NotificationType.ERROR),
                        project);
                return;
            }

            if (debugSession.isPaused()) {
                debugSession.resume();
            }
            debugSession.pause();
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

    public void downloadAgent() {
        checkAndEnsureJavaAgent(true, new AgentJarDownloadCompleteCallback() {
            @Override
            public void error(String message) {

            }

            @Override
            public void success(String url, String path) {
                try {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public void refreshSession() throws APICallException, IOException {
        logger.info("fetch latest session for module: {}", currentModule.getName());
        DataResponse<ExecutionSession> sessions = client.fetchProjectSessions();
        if (sessions.getItems().size() == 0) {
            Notifications.Bus.notify(notificationGroup
                            .createNotification("No sessions available for module ["
                                            + currentModule.getName() + "]",
                                    NotificationType.ERROR),
                    project);
            return;
        }
        client.setSession(sessions.getItems().get(0));
    }

    public void focusExceptionWindow() {
        this.logicBugs.bringToFocus(toolWindow);
    }

    public void initiateUseLocal() {
        client = new VideobugLocalClient(Objects.requireNonNull(project.getBasePath()));
        amplitudeClient.logEvent(new Event("InitiateUseLocal", HOSTNAME));


        ReadAction.nonBlocking(InsidiousService.this::startDebugSession).submit(threadPool);

        ApplicationManager.getApplication().invokeLater(() -> {
            InsidiousService.this.initiateUI();
            if (notificationGroup != null) {
                Notifications.Bus.notify(notificationGroup
                                .createNotification("VideoBug logged in at [" + "disk://localhost"
                                                + "] for module [" + currentModule.getName() + "]",
                                        NotificationType.INFORMATION),
                        project);
            }
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
}
