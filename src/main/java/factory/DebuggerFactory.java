package factory;

import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import ui.Credentials;
import ui.HorBugTable;

import java.io.IOException;

public class DebuggerFactory implements ToolWindowFactory {
    Project currentProject;
    Callback callback;
    OkHttpClient client;
    String projectname;

    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
//        HorBugTable bugsTable = new HorBugTable(toolWindow);
//        bugsTable.setTableValues();
        this.currentProject = project;
        try {
            checkProject();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        Credentials credentials = new Credentials();
//
//        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
//        Content content1 = contentFactory.createContent(bugsTable.getContent(), "Bugs", false);
//        Content content2 = contentFactory.createContent(credentials.getContent(), "Credentials", false);
//        toolWindow.getContentManager().addContent(content1);
//        toolWindow.getContentManager().addContent(content2);
    }

    private void checkProject() throws IOException {
        projectname = ModuleManager.getInstance(currentProject).getModules()[0].getName();

        String storedName = PropertiesComponent.getInstance().getValue(Constants.PROJECT_NAME);

        if (storedName != projectname) {
            createProject(projectname);
            return;
        }
        return;
    }

    private void createProject(String projectName) throws IOException {
        callback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                    JSONObject jsonObject = (JSONObject) JSONValue.parse(responseBody.string());

                    PropertiesComponent.getInstance().setValue(Constants.PROJECT_ID, jsonObject.getAsString("id"));

                    PropertiesComponent.getInstance().setValue(Constants.PROJECT_NAME, projectName);

                    System.out.print(PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID));
                }
            }
        };

        String url =  PropertiesComponent.getInstance().getValue(Constants.BASE_URL, "http://localhost:8080");
        if (url == "") {
            return;
        }

        post(url + Constants.CREATE_PROJECT + "?name=" + projectname, "");
    }

    private void post(String url, String json) throws IOException {
        client = new OkHttpClient();
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + PropertiesComponent.getInstance().getValue(Constants.TOKEN))
                .build();

        client.newCall(request).enqueue(callback);
    }

}

