package actions;

import Network.GETCalls;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import net.minidev.json.JSONArray;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class SearchAction extends AnAction {
    Project project;
    @Override
    public void actionPerformed(AnActionEvent e) {
        project = e.getProject();
        JSONArray jsonArray = new JSONArray();
        XBreakpointManager  breakpointManager = XDebuggerManager.getInstance(e.getProject()).getBreakpointManager();
        final Collection<XBreakpoint<?>> toRemove = new ArrayList<XBreakpoint<?>>();

        for (XBreakpoint<?> breakpoint : breakpointManager.getAllBreakpoints()) {
            final XSourcePosition position = breakpoint.getSourcePosition();
            if (position != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("lineNum", position.getLine());
                jsonObject.put("file", position.getFile().toString().split("src")[1]);
                jsonArray.add(jsonObject);
            }
        }

        PropertiesComponent.getInstance().setValue("debugPoints", jsonArray.toString());

        try {
            loadThreadIds("");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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

    private void loadThreadIds(String response) throws IOException {
        List<String> threadIdArray = new ArrayList<>();
        String path = project.getBasePath() + "/threadId.json";
        String content = new String(Files.readAllBytes(Paths.get(path)));
        JSONObject jsonObject = (JSONObject) JSONValue.parse(content);
        JSONArray jsonArray = (JSONArray)jsonObject.get("items");

        for (int i=0; i < jsonArray.size(); i++) {
            JSONObject threadObject = (JSONObject) jsonArray.get(i);
            threadIdArray.add("Thread Id: " + String.valueOf(threadObject.get("threadId")));
        }

        List<String> newList = new ArrayList<String>(new HashSet<String>(threadIdArray));

        MyComboBox myComboBox = (MyComboBox) ActionManager.getInstance().getAction("searchThread");
        myComboBox.updateUI(newList);

    }
}
