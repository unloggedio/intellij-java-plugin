package ui;

import callbacks.*;
import network.GETCalls;
import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import factory.ProjectService;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Credentials {

    String usernameText;
    String videobugURL, passwordText;
    Project project;
    String projectName, project_id;
    ToolWindow toolWindow;
    private JPanel panel1;
    private JTextField username;
    private JLabel usernameLable;
    private JLabel passwordLable;
    private JPasswordField password;
    private JTextField videobugServerURLTextField;
    private JButton signupSigninButton;
    private JLabel errorLable;

    public Credentials(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        signupSigninButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                usernameText = username.getText();
                passwordText = new String(password.getPassword());
                videobugURL = videobugServerURLTextField.getText();

                project.getService(ProjectService.class).setServerEndpoint(videobugURL);

                if (!isValidEmailAddress(usernameText)) {
                    errorLable.setText("Enter a valid email address");
                    return;
                }

                if (passwordText == null) {
                    errorLable.setText("Enter a valid Password");
                    return;
                }

                try {
                    signin();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public JPanel getContent() {
        return panel1;
    }


    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    private void signin() throws IOException {
        project.getService(ProjectService.class).signin(usernameText, passwordText, new SignInCallback() {
            @Override
            public void error() {
                try {
                    signup();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void success(String token) {
                PropertiesComponent.getInstance().setValue(Constants.TOKEN, token);
                try {
                    getAndCheckProject();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void signup() throws IOException {
        project.getService(ProjectService.class).signup(usernameText, passwordText, new SignUpCallback() {
            @Override
            public void error() {

            }

            @Override
            public void success() {
                try {
                    signin();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void getAndCheckProject() {
        projectName = ModuleManager.getInstance(project).getModules()[0].getName();
        project.getService(ProjectService.class).getProjectByName(projectName, new GetProjectCallback() {
            @Override
            public void error() {

            }

            @Override
            public void success(String projectId) {
                PropertiesComponent.getInstance().setValue(Constants.PROJECT_ID, projectId);
                try {
                    getProjectToken();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

        project.getService(ProjectService.class).getProjectToken(
                PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID), new ProjectTokenCallback() {
            @Override
            public void error() {

            }

            @Override
            public void success(String token) {
                PropertiesComponent.getInstance().setValue(Constants.PROJECT_TOKEN, token);

                HorBugTable bugTable = project.getService(ProjectService.class).getHorBugTable();
                try {
                    bugTable.setTableValues();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Content bugsContent = ContentFactory.SERVICE.getInstance().createContent(bugTable.getContent(), "Crashes", false);

                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Content content = toolWindow.getContentManager().findContent("Crashes");
                        if (content == null) {
                            toolWindow.getContentManager().addContent(bugsContent);
                        }
                    }
                });

                errorLable.setText("All Set! Check BugsTable now");


            }
        });
    }

}
