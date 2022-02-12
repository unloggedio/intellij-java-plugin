package factory;

import callbacks.*;
import com.intellij.execution.ExecutionException;
import com.intellij.openapi.project.Project;
import network.Client;
import network.pojo.FilteredDataEventsRequest;
import pojo.Bugs;
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
        this.client.getTracesByClassForProjectAndSessionId(projectId, sessionId, classList, getProjectSessionErrorsCallback);
    }

    public void getTracesByClassForProjectAndSessionIdAndTracevalue(String projectId, String sessionId, String traceId,
                                                                    GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) {
        this.client.getTracesByClassForProjectAndSessionIdAndTracevalue(projectId, sessionId, traceId, getProjectSessionErrorsCallback);
    }

    public void filterDataEvents(String projectId, FilteredDataEventsRequest filteredDataEventsRequest, FilteredDataEventsCallback filteredDataEventsCallback) {
        this.client.filterDataEvents(projectId, filteredDataEventsRequest, filteredDataEventsCallback);
    }

    public void startTracer(Bugs selectedTrace, String order, String source) throws IOException, ExecutionException {
        tracer = new CodeTracer(project, selectedTrace.getExecutionSessionId(), client, source);
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
}
