package com.insidious.plugin.factory;

import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.extension.InsidiousExecutor;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousRunConfigType;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.network.Client;
import com.insidious.plugin.network.pojo.DataResponse;
import com.insidious.plugin.network.pojo.ExceptionResponse;
import com.insidious.plugin.network.pojo.ExecutionSession;
import com.insidious.plugin.network.pojo.SessionUpdatedCallback;
import com.insidious.plugin.network.pojo.exceptions.APICallException;
import com.insidious.plugin.network.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.network.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.parser.GradleFileVisitor;
import com.insidious.plugin.parser.PomFileVisitor;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.ui.CredentialsToolbar;
import com.insidious.plugin.ui.HorBugTable;
import com.insidious.plugin.ui.LogicBugs;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Consumer;
import org.slf4j.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes;

@Storage("insidious.xml")
public class InsidiousService {
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private final Project project;
    private final InsidiousConfigurationState insidiousConfiguration;
    private final Path videoBugHomePath = Path.of(System.getProperty("user.home"), ".VideoBug");
    private final String agentJarName = "videobug-java-agent.jar";
    private final Path videoBugAgentPath = Path.of(videoBugHomePath.toAbsolutePath().toString(), agentJarName);
    private NotificationGroup notificationGroup;
    private String projectTargetJarLocation = "<PATH-TO-YOUR-PROJECT-JAR>";
    private Module currentModule;
    private String packageName = "com.insidious.plugin";
    private HorBugTable bugsTable;
    private LogicBugs logicBugs;
    private Client client;
    private CodeTracer tracer;
    private ProcessHandler processHandler;
    private XDebugSession debugSession;
    private InsidiousJavaDebugProcess debugProcess;
    private InsidiousJDIConnector connector;
    private CredentialsToolbar credentialsToolbarWindow;
    private ToolWindow toolWindow;
    private CredentialAttributes insidiousCredentials;
    private String appToken;


    public InsidiousService(Project project) {
        this.project = project;
        String logFileNotificationContent = "Insidious log file location - " +
                LoggerUtil.getLogFilePath();

        logger.info("started insidious service - project name [{}]", project.getName());
        if (ModuleManager.getInstance(project).getModules().length == 0) {
            logger.warn("no module found in the project");
        } else {
            currentModule = ModuleManager.getInstance(project).getModules()[0];
            logger.info("current module [{}]", currentModule.getName());
        }


        ReadAction.nonBlocking(this::getProjectPackageName).submit(Executors.newSingleThreadExecutor());
        ReadAction.nonBlocking(this::logLogFileLocation).submit(Executors.newSingleThreadExecutor());

        this.insidiousConfiguration = project.getService(InsidiousConfigurationState.class);
        this.client = new Client(this.insidiousConfiguration.getServerUrl());
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
        this.client.signup(serverUrl, usernameText, passwordText, signupCallback);
    }


    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public void signin(String serverUrl, String usernameText, String passwordText) throws IOException {

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
            client = new Client(serverUrl, usernameText, passwordText);

            ReadAction.nonBlocking(this::checkAndEnsureJavaAgentCache).submit(Executors.newSingleThreadExecutor());
            ReadAction.nonBlocking(this::identifyTargetJar).submit(Executors.newSingleThreadExecutor());
            ReadAction.nonBlocking(this::startDebugSession).submit(Executors.newSingleThreadExecutor());


            Credentials credentials = new Credentials(insidiousConfiguration.getUsername(), passwordText);
            insidiousCredentials = createCredentialAttributes("VideoBug", insidiousConfiguration.getUsername());
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
                return "Session [" + e.getId() + "] @" + e.getHostname() + " at " + e.getCreatedAt();
            }).collect(Collectors.toList());
            @NotNull JBPopup sessionPopup = JBPopupFactory.getInstance().createPopupChooserBuilder(
                    sessionIds
            ).setItemChosenCallback(new Consumer<String>() {
                @Override
                public void consume(String selectedExecutionSession) {
                    logger.info("session selected for module [" + currentModule.getName() + "] => ", selectedExecutionSession);
                    try {
                        String sessionId = selectedExecutionSession.split("\\]")[0].split("\\]")[1];
                        executionSession.setId(sessionId);
                    } catch (Exception e) {
                        logger.error("failed to extract session id", e);
                        executionSession.setId(sessions.getItems().get(0).getId());
                    }
                    client.setSession(executionSession);
                }
            }).addListener(new JBPopupListener() {


                @Override
                public void onClosed(@NotNull LightweightWindowEvent event) {
                    getErrors(getSelectedExceptionClassList(), 0);
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
                }).submit(Executors.newSingleThreadExecutor());
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

    public void getTracesByClassForProjectAndSessionId(
            List<String> classList,
            GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {
        this.client.getTracesByClassForProjectAndSessionId(classList, getProjectSessionErrorsCallback);
    }

    public void getTraces(int pageNum, String traceValue) {

        if (this.client.getCurrentSession() == null) {
            loadSession();
            return;
        }


        getTracesByClassForProjectAndSessionIdAndTraceValue(traceValue,
                new GetProjectSessionErrorsCallback() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {
                        Notifications.Bus.notify(notificationGroup
                                        .createNotification("Failed to get traces: " + errorResponse.getMessage(),
                                                NotificationType.ERROR),
                                project);

                    }

                    @Override
                    public void success(List<TracePoint> tracePointCollection) {
                        if (tracePointCollection.size() == 0) {
                            ApplicationManager.getApplication().invokeAndWait(() -> {

                                Notifications.Bus.notify(notificationGroup
                                                .createNotification("No data available, or data may have been deleted!",
                                                        NotificationType.ERROR),
                                        project);


                            });
                        } else {
                            logicBugs.setTracePoints(tracePointCollection);
                        }
                    }
                });
    }

    public void getErrors(List<String> classList, int pageNum) {


        if (this.client.getCurrentSession() == null) {
            loadSession();
            return;
        }
        logger.info("get traces for session - [{}]", client.getCurrentSession().getId());

        getTracesByClassForProjectAndSessionId(classList,
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
                    public void success(List<TracePoint> tracePointCollection) {
                        logger.info("got [{}] trace points from server", tracePointCollection.size());
                        if (tracePointCollection.size() == 0) {
                            ApplicationManager.getApplication().invokeAndWait(() -> {

                                Notifications.Bus.notify(notificationGroup
                                                .createNotification("No Exception data events matched in the last session",
                                                        NotificationType.INFORMATION),
                                        project);

                            });
                        } else {
                            bugsTable.setTracePoints(tracePointCollection);
                        }
                    }
                });

    }

    public void getTracesByClassForProjectAndSessionIdAndTraceValue(String traceId,
                                                                    GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {
        this.client.getTracesByClassForProjectAndSessionIdAndTracevalue(traceId, getProjectSessionErrorsCallback);
    }


    public synchronized void startDebugSession() {
        logger.info("start debug session");
        if (debugSession != null) {
            return;
        }

        @NotNull RunConfiguration runConfiguration = ConfigurationTypeUtil.
                findConfigurationType(InsidiousRunConfigType.class).createTemplateConfiguration(project);
        @NotNull ExecutionEnvironment env = null;
        try {
            env = ExecutionEnvironmentBuilder.create(project,
                    new InsidiousExecutor(),
                    runConfiguration).build();
            ProgramRunnerUtil.executeConfiguration(env, false, true);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    public ProcessHandler getProcessHandler() {
        return processHandler;
    }

    public void setProcessHandler(ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public Project getProject() {
        return project;
    }

    public InsidiousJavaDebugProcess getDebugProcess() {
        return debugProcess;
    }

    public void setDebugProcess(InsidiousJavaDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

    public XDebugSession getDebugSession() {
        return debugSession;
    }

    public void setDebugSession(XDebugSession session) {
        this.debugSession = session;
    }

    public void setConnector(InsidiousJDIConnector connector) {
        this.connector = connector;
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


    public void initiateUI() {
        logger.info("initiate ui");
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        if (credentialsToolbarWindow == null) {
            credentialsToolbarWindow = new CredentialsToolbar(project, this.toolWindow);
            @NotNull Content credentialContent = contentFactory.createContent(credentialsToolbarWindow.getContent(), "Credentials", false);
            this.toolWindow.getContentManager().addContent(credentialContent);
        }

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
    }

    public void identifyTargetJar() {
        File targetFolder = new File(Path.of(project.getBasePath(), "target").toAbsolutePath().toString());
        if (targetFolder.exists()) {
            for (File targetFile : Objects.requireNonNull(targetFolder.listFiles())) {
                if (targetFile.getAbsolutePath().endsWith(".jar")) {
                    projectTargetJarLocation = targetFile.getAbsolutePath();
                    setAppTokenOnUi();
                    break;
                }
            }
        }
    }

    public void setAppTokenOnUi() {
        logger.info("set app token - {} with package name [{}]" + appToken, packageName);
        if (credentialsToolbarWindow != null) {


            credentialsToolbarWindow.setText("-javaagent:\"" + videoBugAgentPath
                    + "=i="
                    + packageName.replaceAll("\\.", "/")
                    + ","
                    + "server="
                    + insidiousConfiguration.serverUrl
                    + ",format=single,token="
                    + appToken + "\"");


//            credentialsToolbarWindow.setText("java -javaagent:\"" + videoBugAgentPath
//                    + "=i="
//                    + packageName.replaceAll("\\.", "/")
//                    + ","
//                    + "server="
//                    + insidiousConfiguration.serverUrl
//                    + ",token="
//                    + appToken + "\""
//                    + " -jar " + projectTargetJarLocation);
//
//
        }
    }

    public void loadSession() {

        if (currentModule == null) {
            currentModule = ModuleManager.getInstance(project).getModules()[0];
        }

        ApplicationManager.getApplication().invokeLater(this::startDebugSession);
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
            public void success(List<ExecutionSession> executionSessionList) {
                logger.info("got [{}] sessions for project", executionSessionList.size());
                if (executionSessionList.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() -> {

                        Notifications.Bus.notify(notificationGroup
                                        .createNotification("No sessions found for project " + currentModule.getName() +
                                                        ". Start recording new sessions with the java agent",
                                                NotificationType.INFORMATION),
                                project);
                    });
                    return;
                }

                client.setSession(executionSessionList.get(0));
                getErrors(getSelectedExceptionClassList(), 0);

            }
        });
    }

    public void setTracePoint(TracePoint selectedTrace, DirectionType directionType) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                logger.info("set trace point in connector => [{}]", selectedTrace.getClassname());
                connector.setTracePoint(selectedTrace, directionType);
            } catch (Exception e) {
                e.printStackTrace();
                Notifications.Bus.notify(notificationGroup
                                .createNotification("Failed to set select tace point " + e.getMessage(),
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


    public Client getClient() {
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

    public void downloadAgent() throws IOException {


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
        }
        client.setSession(sessions.getItems().get(0));
    }

    public void setExecutionSessionId(String sessionId) {
        ExecutionSession session = new ExecutionSession();
        session.setId(sessionId);
        this.client.setSession(session);
    }
}
