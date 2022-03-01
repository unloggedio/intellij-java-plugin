package com.insidious.plugin.factory;

import com.insidious.plugin.actions.Constants;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.extension.InsidiousExecutor;
import com.insidious.plugin.extension.InsidiousJavaDebugProcess;
import com.insidious.plugin.extension.InsidiousRunConfigType;
import com.insidious.plugin.extension.connector.InsidiousJDIConnector;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.network.Client;
import com.insidious.plugin.network.pojo.ExceptionResponse;
import com.insidious.plugin.network.pojo.ExecutionSession;
import com.insidious.plugin.network.pojo.exceptions.ProjectDoesNotExistException;
import com.insidious.plugin.network.pojo.exceptions.UnauthorizedException;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.ui.CredentialsToolbar;
import com.insidious.plugin.ui.HorBugTable;
import com.insidious.plugin.ui.LogicBugs;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Storage;
import org.slf4j.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes;

@Storage("insidious.xml")
public class InsidiousService {
    private final static Logger logger = LoggerUtil.getInstance(InsidiousService.class);
    private final Project project;
    private final InsidiousConfigurationState insidiousConfiguration;
    private final Module currentModule;
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
        logger.info("started insidious service - project name [{}]", project.getName());
        currentModule = ModuleManager.getInstance(project).getModules()[0];
        logger.info("current module [{}]", currentModule.getName());
        this.insidiousConfiguration = project.getService(InsidiousConfigurationState.class);
        this.client = new Client(this.insidiousConfiguration.getServerUrl());
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
                        Messages.showErrorDialog(project, e.getMessage(), "Failed to signin VideoBug");
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
            return;
        }

        if (passwordText == null) {
            credentialsToolbarWindow.setErrorLabel("Enter a valid Password");
            return;
        }

        insidiousConfiguration.setServerUrl(serverUrl);
        insidiousConfiguration.setUsername(usernameText);

        try {
            client = new Client(serverUrl, usernameText, passwordText);

            Credentials credentials = new Credentials(insidiousConfiguration.getUsername(), passwordText);
            insidiousCredentials = createCredentialAttributes("VideoBug", insidiousConfiguration.getUsername());
            PasswordSafe.getInstance().set(insidiousCredentials, credentials);

        } catch (UnauthorizedException e) {

            logger.error("Failed to signin for user [{}]", usernameText, e);
            e.printStackTrace();
            if (credentialsToolbarWindow != null) {
                credentialsToolbarWindow.setErrorLable("Sign in failed!");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Messages.showErrorDialog(project, "Failed to connect with server - " + e.getMessage(), "Failed");
        }
        ApplicationManager.getApplication().invokeLater(this::initiateUI);
    }

    private void setupProject() {

        try {
            logger.info("try to set project to - [{}]", currentModule.getName());
            client.setProject(currentModule.getName());
            getErrors(0);
            generateAppToken();
        } catch (ProjectDoesNotExistException e1) {
            int choice = Messages.showDialog(project, "No project by name [" + currentModule.getName() + "]. Do you want to create a new project ?",
                    "Failed to get project", new String[]{"Yes", "No"}, 0, null);
            if (choice == 0) {
                createProject(currentModule.getName(), new NewProjectCallback() {
                    @Override
                    public void error(String errorMessage) {
                        logger.error("failed to create project - {}", errorMessage);
                        Messages.showErrorDialog(project, errorMessage, "Failed to create new project for [" + currentModule.getName() + "]");
                    }

                    @Override
                    public void success(String projectId) {
                        logger.info("create new project for [{}] -> [{}]", currentModule.getName(), projectId);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            setupProject();
                        });
                    }
                });
            }
        } catch (UnauthorizedException | IOException e) {
            e.printStackTrace();
            Messages.showErrorDialog(project, e.getMessage(), "Failed to query existing project");
        }
    }

    public void generateAppToken() {
        getProjectToken(new ProjectTokenCallback() {
            @Override
            public void error(String message) {
                Messages.showErrorDialog(project, message, "Failed to generate app token");
                credentialsToolbarWindow.setErrorLabel(message);
            }

            @Override
            public void success(String token) {
                InsidiousService.this.appToken = token;
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
        this.client.getTracesByClassForProjectAndSessionId(getProjectSessionErrorsCallback);
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
                        Messages.showErrorDialog(project, errorResponse.getMessage(), "Failed to get traces");
                    }

                    @Override
                    public void success(List<TracePoint> tracePointCollection) {
                        if (tracePointCollection.size() == 0) {
                            Messages.showErrorDialog(project, "No data availalbe, or data may have been deleted!", "No Data");
                        } else {
                            logicBugs.setTracePoints(tracePointCollection);
                        }
                    }
                });
    }

    public void getErrors(int pageNum) {


        if (this.client.getCurrentSession() == null) {
            loadSession();
            return;
        }
        logger.info("get traces for session - [{}]", client.getCurrentSession().getId());

        List<String> classList = Arrays.asList(PropertiesComponent.getInstance().getValue(Constants.ERROR_NAMES));
        getTracesByClassForProjectAndSessionId(classList,
                new GetProjectSessionErrorsCallback() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {
                        logger.error("failed to get trace points from server - {}", errorResponse);
                        Messages.showErrorDialog(project, errorResponse.getMessage(), "Failed to load sessions");
                    }

                    @Override
                    public void success(List<TracePoint> tracePointCollection) {
                        logger.info("got [{}] trace points from server", tracePointCollection.size());
                        if (tracePointCollection.size() == 0) {
                            Messages.showErrorDialog(project, "No data available, or data may have been deleted!", "No Data");
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

        @NotNull RunConfiguration runConfiguration = ConfigurationTypeUtil.findConfigurationType(InsidiousRunConfigType.class).createTemplateConfiguration(project);
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

        if (isLoggedIn()) {
            if (bugsTable != null) {
                return;
            }

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

    public void setAppTokenOnUi() {
        logger.info("set app token - " + appToken);
        credentialsToolbarWindow.setText("java -javaagent:\"" + "<PATH-TO-THE-VIDEOBUG-JAVA-AGENT>"
                + "=i=<YOUR-PACKAGE-NAME>,"
                + "server="
                + insidiousConfiguration.serverUrl
                + ",token="
                + appToken + "\""
                + " -jar" + " <PATH-TO-YOUR-PROJECT-JAR>");
    }

    public void loadSession() {
        ApplicationManager.getApplication().invokeLater(this::startDebugSession);
        this.client.getProjectSessions(new GetProjectSessionsCallback() {
            @Override
            public void error(String message) {
                logger.error("failed to load project sessions - {}", message);
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog(project, "No sessions found for project " + currentModule.getName(), "Failed to get sessions");
                });
            }

            @Override
            public void success(List<ExecutionSession> executionSessionList) {
                logger.info("got [{}] sessions for project", executionSessionList.size());
                if (executionSessionList.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "No sessions found for project " + currentModule.getName() + ". Start recording new sessions with the java agent", "Failed to get sessions");
                    });
                    return;
                }

                client.setSession(executionSessionList.get(0));
                getErrors(0);

            }
        });
    }

    public void setTracePoint(TracePoint selectedTrace, DirectionType directionType) throws Exception {
        connector.setTracePoint(selectedTrace, directionType);
        if (debugSession.isPaused()) {
            debugSession.resume();
        }
        debugSession.pause();
    }


    public Client getClient() {
        return client;
    }

    public void setToolWindow(ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        init();
    }
}
