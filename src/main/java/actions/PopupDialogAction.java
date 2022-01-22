package actions;

import Network.GETCalls;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class PopupDialogAction extends AnAction {

    Project project;


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();

        System.out.print(e.getProject().getBasePath());

        String value = Messages.showInputDialog("Trace ID", "What's the trace Id?", null);

        if (value == null || value == "") {
            return;
        }

        PropertiesComponent.getInstance().setValue("traceId", value);

        getCalls(Constants.BASE_URL + value);

        Messages.showInfoMessage("Please Mark Your Debug Points", "Debug Points?");

    }

    private void getCalls(String url) {
        GETCalls getCalls = new GETCalls();
        try {
            getCalls.getCall(url, new Callback(){
                @Override public void onFailure(Call call, IOException e)
                {
                    e.printStackTrace();
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        Headers responseHeaders = response.headers();
                        for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                            System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                        }
                        String path = project.getBasePath() + "-Project-Info";
                        File file = new File(path);
                        FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write(responseBody.string());
                        fileWriter.close();
                    }
                }});
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
