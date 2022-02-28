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
import network.pojo.ExceptionResponse;
import network.pojo.FilteredDataEventsRequest;
import network.pojo.exceptions.ProjectDoesNotExistException;
import network.pojo.exceptions.UnauthorizedException;
import org.jetbrains.annotations.NotNull;
import pojo.TracePoint;
import ui.CredentialsToolbar;
import ui.HorBugTable;
import ui.LogicBugs;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes;

@Storage("insidious.xml")
public class InsidiousService {
    private final static Logger logger = Logger.getInstance(InsidiousService.class);
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

    public InsidiousService(Project project) {
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
                        signin(insidiousConfiguration.serverUrl, insidiousConfiguration.username, password);
                    } catch (IOException e) {
                        Messages.showErrorDialog(project, e.getMessage(), "Failed to signin VideoBug");
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

    public void signin(String serverUrl, String usernameText, String passwordText) throws IOException {

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

            e.printStackTrace();

            int choice = Messages.showDialog(project, "Do you want to try to create account with these credentials ? Signup might fail if account already exists",
                    "Failed to sign in", new String[]{"Yes", "No"}, 0, null);
            if (choice == 0) {
                signup(insidiousConfiguration.serverUrl, insidiousConfiguration.username, passwordText, new SignUpCallback() {
                    @Override
                    public void error(String string) {
                        Messages.showErrorDialog(project, string, "Failed to signup");
                    }

                    @Override
                    public void success() {
                        try {
                            signin(serverUrl, usernameText, passwordText);
                        } catch (IOException ex) {
                            Messages.showErrorDialog(project, ex.getMessage(), "Failed to connect with server");
                        }
                    }
                });
            }
            e.printStackTrace();


            return;
        } catch (IOException e) {
            e.printStackTrace();
            Messages.showErrorDialog(project, "Failed to connect with server - " + e.getMessage(), "Failed");
            return;
        }
        setupProject();


    }

    private void setupProject() throws IOException {


        try {
            client.setProject(currentModule.getName());
        } catch (ProjectDoesNotExistException e1) {
            int choice = Messages.showDialog(project, "No project by name [" + currentModule.getName() + "]. Do you want to create a new project ?",
                    "Failed to get project", new String[]{"Yes", "No"}, 0, null);
            if (choice == 0) {
                createProject(currentModule.getName(), new NewProjectCallback() {
                    @Override
                    public void error(String errorMessage) {
                        Messages.showErrorDialog(project, errorMessage, "Failed to create new project for [" + currentModule.getName() + "]");
                    }

                    @Override
                    public void success(String projectId) {
                        try {
                            setupProject();
                        } catch (IOException e) {
                            Messages.showErrorDialog(project, e.getMessage(), "Failed to setup project");
                        }
                    }
                });
            }
            return;
        } catch (UnauthorizedException e) {
            e.printStackTrace();
            Messages.showErrorDialog(project, e.getMessage(), "Failed to query existing project");
        }

        getProjectToken(new ProjectTokenCallback() {
            @Override
            public void error(String message) {
                Messages.showErrorDialog(project, message, "Failed to generate app token");
                credentialsToolbarWindow.setErrorLabel(message);
            }

            @Override
            public void success(String token) {

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

                getHorBugTable().setCommandText("java -javaagent:\"" + "<PATH-TO-THE-VIDEOBUG-JAVA-AGENT>"
                        + "=i=<YOUR-PACKAGE-NAME>,"
                        + "server="
                        + insidiousConfiguration.serverUrl
                        + ",token="
                        + token + "\""
                        + " -jar" + " <PATH-TO-YOUR-PROJECT-JAR>");

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

    public void getProjectSessions(String projectId, GetProjectSessionsCallback getProjectSessionsCallback) {
        this.client.getProjectSessions(projectId, getProjectSessionsCallback);
    }

    public void getTracesByClassForProjectAndSessionId(String projectId,
                                                       List<String> classList,
                                                       GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {
        this.client.getTracesByClassForProjectAndSessionId(projectId, getProjectSessionErrorsCallback);
    }

    public void getTraces(int pageNum, String traceValue) {
        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        project.getService(InsidiousService.class).getTracesByClassForProjectAndSessionIdAndTracevalue(projectId,
                traceValue, new GetProjectSessionErrorsCallback() {
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

        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        List<String> classList = Arrays.asList(PropertiesComponent.getInstance().getValue(Constants.ERROR_NAMES));
        project.getService(InsidiousService.class).getTracesByClassForProjectAndSessionId(
                projectId, classList,
                new GetProjectSessionErrorsCallback() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {
                        Messages.showErrorDialog(project, errorResponse.getMessage(), "Failed to load sessions");
                    }

                    @Override
                    public void success(List<TracePoint> tracePointCollection) {
                        if (tracePointCollection.size() == 0) {
                            Messages.showErrorDialog(project, "No data available, or data may have been deleted!", "No Data");
                        } else {
                            bugsTable.setTracePoints(tracePointCollection);
                        }
                    }
                });

    }

    public void getTracesByClassForProjectAndSessionIdAndTracevalue(String projectId, String traceId,
                                                                    GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {
        this.client.getTracesByClassForProjectAndSessionIdAndTracevalue(projectId, traceId, getProjectSessionErrorsCallback);
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
