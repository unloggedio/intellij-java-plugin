package factory;

import actions.Constants;
import callbacks.*;
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.xdebugger.XDebugSession;
import extension.InsidiousExecutor;
import extension.InsidiousJavaDebugProcess;
import extension.InsidiousRunConfigType;
import extension.connector.InsidiousJDIConnector;
import network.Client;
import network.pojo.FilteredDataEventsRequest;
import network.pojo.exceptions.UnauthorizedException;
import org.jetbrains.annotations.NotNull;
import pojo.TracePoint;
import ui.CredentialsToolbar;
import ui.HorBugTable;
import ui.LogicBugs;

import java.io.IOException;
import java.util.List;

import static com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes;

@Storage("insidious.xml")
public class ProjectService {
    private final static Logger logger = Logger.getInstance(ProjectService.class);
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
    private String projectName;
    private CredentialAttributes insidiousCredentials;

    public ProjectService(Project project) {
        this.project = project;
        currentModule = ModuleManager.getInstance(project).getModules()[0];
        this.insidiousConfiguration = project.getService(InsidiousConfigurationState.class);
        this.client = new Client(this.insidiousConfiguration.getServerUrl());
        if (!StringUtil.isEmpty(insidiousConfiguration.getUsername())) {
            insidiousCredentials = createCredentialAttributes("VideoBug", insidiousConfiguration.getUsername());
            if (insidiousCredentials != null) {
                Credentials credentials = PasswordSafe.getInstance().get(insidiousCredentials);
                if (credentials != null) {
                    String password = credentials.getPasswordAsString();
                    try {
                        this.client = new Client(insidiousConfiguration.getServerUrl(), currentModule.getName(), insidiousConfiguration.getUsername(), password);
                    } catch (IOException | UnauthorizedException e) {
                        Messages.showErrorDialog(project, e.getMessage(), "Failed to login VideoBug");
                        e.printStackTrace();
                    }
                }
            }
        }
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

    public void signin(String serverUrl, String usernameText, String passwordText) {

        client = new Client(serverUrl);

        if (!isValidEmailAddress(usernameText)) {
            credentialsToolbarWindow.setErrorLabel("Enter a valid email address");
            return;
        }

        if (passwordText == null) {
            credentialsToolbarWindow.setErrorLabel("Enter a valid Password");
            return;
        }

        this.client.signin(usernameText, passwordText, new SignInCallback() {
            @Override
            public void error(String errorString) {
                logger.error("Failed to login - " + errorString);
                credentialsToolbarWindow.setErrorLabel(errorString);
                signup(serverUrl, usernameText, passwordText, new SignUpCallback() {
                    @Override
                    public void error(String message) {
                        credentialsToolbarWindow.setErrorLabel(message);
                    }

                    @Override
                    public void success() {

                    }
                });
            }

            @Override
            public void success(String token) {
                insidiousConfiguration.setServerUrl(serverUrl);
                insidiousConfiguration.setUsername(usernameText);

                Credentials credentials = new Credentials(insidiousConfiguration.getUsername(), passwordText);
                insidiousCredentials = createCredentialAttributes("VideoBug", insidiousConfiguration.getUsername());
                PasswordSafe.getInstance().set(insidiousCredentials, credentials);

                try {
                    getAndCheckProject();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


    }


    private void createProject() throws IOException {
        project.getService(ProjectService.class).createProject(projectName, new NewProjectCallback() {
            @Override
            public void error() {

            }

            @Override
            public void success(String projectId) {
                PropertiesComponent.getInstance().setValue(Constants.PROJECT_ID, projectId);
                PropertiesComponent.getInstance().setValue(Constants.PROJECT_NAME, projectName);
                try {
                    getProjectToken();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void getProjectToken() throws IOException {

        getProjectToken(
                PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID), new ProjectTokenCallback() {
                    @Override
                    public void error(String message) {
                        credentialsToolbarWindow.setErrorLabel(message);
                    }

                    @Override
                    public void success(String token) {
                        PropertiesComponent.getInstance().setValue(Constants.PROJECT_TOKEN, token);

                        HorBugTable bugsTable = new HorBugTable(project, toolWindow);
                        LogicBugs logicBugs = new LogicBugs(project, toolWindow);
                        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                        try {
                            bugsTable.setTableValues();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Content bugsContent = contentFactory.createContent(bugsTable.getContent(), "Exceptions", false);
                        Content traceContent = contentFactory.createContent(logicBugs.getContent(), "Traces", false);
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                Content content = toolWindow.getContentManager().findContent("Exceptions");
                                if (content == null) {
                                    toolWindow.getContentManager().addContent(bugsContent);
                                }
                                Content traceContent2 = toolWindow.getContentManager().findContent("Traces");
                                if (traceContent2 == null) {
                                    toolWindow.getContentManager().addContent(traceContent);
                                }
                            }
                        });

                        credentialsToolbarWindow.setText("java -javaagent:\"" + "<PATH-TO-THE-VIDEOBUG-JAVA-AGENT>"
                                + "=i=<YOUR-PACKAGE-NAME>,"
                                + "server="
                                + PropertiesComponent.getInstance().getValue(Constants.BASE_URL)
                                + ",token="
                                + PropertiesComponent.getInstance().getValue(Constants.PROJECT_TOKEN) + "\""
                                + " -jar" + " <PATH-TO-YOUR-PROJECT-JAR>");
                    }
                });
    }


    private void getAndCheckProject() {
        projectName = ModuleManager.getInstance(project).getModules()[0].getName();
        getProjectByName(projectName, new GetProjectCallback() {
            @Override
            public void error(String message) {
                Messages.showErrorDialog(message, "Failed to get project");
            }

            @Override
            public void success(String projectId) {
                PropertiesComponent.getInstance().setValue(Constants.PROJECT_ID, projectId);
                getProjectToken(projectId, new ProjectTokenCallback() {
                    @Override
                    public void error(String message) {
                        credentialsToolbarWindow.setErrorLabel(message);
                    }

                    @Override
                    public void success(String token) {
                        try {
                            getProjectToken();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void noSuchProject() {
                try {
                    createProject();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {
        this.client.getProjectByName(projectName, getProjectCallback);
    }

    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        this.client.createProject(projectName, newProjectCallback);
    }

    public void getProjectToken(String projectId, ProjectTokenCallback projectTokenCallback) {
        this.client.getProjectToken(projectId, projectTokenCallback);
    }

    public void getProjectSessions(String projectId, GetProjectSessionsCallback getProjectSessionsCallback) {
        this.client.getProjectSessions(projectId, getProjectSessionsCallback);
    }

    public void getTracesByClassForProjectAndSessionId(String projectId, String sessionId,
                                                       List<String> classList,
                                                       GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {
        this.client.getTracesByClassForProjectAndSessionId(projectId, sessionId, getProjectSessionErrorsCallback);
    }

    public void getTracesByClassForProjectAndSessionIdAndTracevalue(String projectId, String sessionId, String traceId,
                                                                    GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {
        this.client.getTracesByClassForProjectAndSessionIdAndTracevalue(projectId, sessionId, traceId, getProjectSessionErrorsCallback);
    }

    public void filterDataEvents(String projectId, FilteredDataEventsRequest filteredDataEventsRequest, FilteredDataEventsCallback filteredDataEventsCallback) {
        this.client.filterDataEvents(projectId, filteredDataEventsRequest, filteredDataEventsCallback);
    }

    public void startDebugSession() {

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

    public void startTracer(TracePoint selectedTrace, String order, String source) throws IOException {
        tracer = new CodeTracer(project, selectedTrace, order, source);
    }

    public void back() {
        if (tracer != null) {
            tracer.back();
        }
    }

    public void forward() {
        if (tracer != null) {
            tracer.forward();
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


    public void initiateUI(@NotNull ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

        if (!isLoggedIn()) {
            credentialsToolbarWindow = new CredentialsToolbar(project, this.toolWindow);
            @NotNull Content credentialContent = contentFactory.createContent(credentialsToolbarWindow.getContent(), "Credentials", false);
            toolWindow.getContentManager().addContent(credentialContent);
        } else {
            bugsTable = new HorBugTable(project, this.toolWindow);
            @NotNull Content bugsContent = contentFactory.createContent(bugsTable.getContent(), "Exceptions", false);
            toolWindow.getContentManager().addContent(bugsContent);

            logicBugs = new LogicBugs(project, this.toolWindow);
            @NotNull Content logicbugContent = contentFactory.createContent(logicBugs.getContent(), "Traces", false);
            toolWindow.getContentManager().addContent(logicbugContent);

            try {
                bugsTable.setTableValues();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setTracePoint(TracePoint selectedTrace) throws Exception {
        connector.setTracePoint(selectedTrace);
        if (debugSession.isPaused()) {
            debugSession.resume();
        }
        debugSession.pause();
    }


    public Client getClient() {
        return client;
    }
}
