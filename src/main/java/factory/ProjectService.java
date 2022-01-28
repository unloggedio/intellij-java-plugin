package factory;

import callbacks.*;
import com.intellij.openapi.project.Project;
import network.Client;
import network.pojo.FilteredDataEventsRequest;
import ui.HorBugTable;

import java.util.List;

public class ProjectService {
    private final Project project;
    private HorBugTable bugsTable;
    private int currentLineNumber;
    private Client client;

    public ProjectService(Project project) {
        this.project = project;
    }

    public HorBugTable getHorBugTable() {
        return bugsTable;
    }

    public void setHorBugTable(HorBugTable bugsTable) {
        this.bugsTable = bugsTable;
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

    public void filterDataEvents(String projectId, FilteredDataEventsRequest filteredDataEventsRequest, FilteredDataEventsCallback filteredDataEventsCallback) {
        this.client.filterDataEvents(projectId, filteredDataEventsRequest, filteredDataEventsCallback);
    }
}
