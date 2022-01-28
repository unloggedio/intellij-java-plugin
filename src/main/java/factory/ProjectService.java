package factory;

import callbacks.SignInCallback;
import com.intellij.openapi.project.Project;
import network.Client;
import okhttp3.Callback;
import ui.HorBugTable;

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

    public void signup(String usernameText, String passwordText, Callback signupCallback) {
        this.client.signup(usernameText, passwordText, signupCallback);
    }

    public void signin(String usernameText, String passwordText, SignInCallback signupCallback) {
        this.client.signin(usernameText, passwordText, signupCallback);
    }
}
