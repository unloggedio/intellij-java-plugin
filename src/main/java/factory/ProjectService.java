package factory;

import callbacks.*;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import extension.InsidiousExecutor;
import extension.InsidiousJavaDebugProcess;
import extension.InsidiousRunConfigType;
import extension.connector.InsidiousJDIConnector;
import network.Client;
import network.pojo.FilteredDataEventsRequest;
import org.jetbrains.annotations.NotNull;
import pojo.TracePoint;
import ui.HorBugTable;
import ui.LogicBugs;

import java.io.IOException;
import java.util.List;

public class ProjectService {
    private final Project project;
    private HorBugTable bugsTable;
    private LogicBugs logicBugs;
    private int currentLineNumber;
    private Client client;
    private CodeTracer tracer;
    private ProcessHandler processHandler;
    private XDebugSession debugSession;
    private InsidiousJavaDebugProcess debugProcess;
    private InsidiousJDIConnector connector;

    public ProjectService(Project project) {
        this.project = project;
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

    public int getCurrentLineNumber() {
        return currentLineNumber;
    }

    public void setCurrentLineNumber(int currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
    }

    public void setServerEndpoint(String videobugURL) {
        this.client = new Client(videobugURL);
    }

    public void signup(String usernameText, String passwordText, SignUpCallback signupCallback) {
        this.client.signup(usernameText, passwordText, signupCallback);
    }

    public void signin(String usernameText, String passwordText, SignInCallback signupCallback) {
        this.client.signin(usernameText, passwordText, signupCallback);
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

    public InsidiousJDIConnector getConnector() {
        return connector;
    }

    public void setConnector(InsidiousJDIConnector connector) {
        this.connector = connector;
    }
}
