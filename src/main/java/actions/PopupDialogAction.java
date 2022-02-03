package actions;

import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import network.GETCalls;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import network.pojo.DebugPoint;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PopupDialogAction extends AnAction {

    Project project;


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();

        XBreakpoint[] breakpoints = XDebuggerManager.getInstance(project).getBreakpointManager().getAllBreakpoints();

        List<DebugPoint> breakpointList = new ArrayList<>();

        for (XBreakpoint breakpoint : breakpoints) {
            if (breakpoint.getType() instanceof XLineBreakpointType) {
                DebugPoint debugPoint = new DebugPoint();
                debugPoint.setFile(breakpoint.getSourcePosition().getFile().toString().split("/src/main/java/")[1]);
                debugPoint.setLineNumber(breakpoint.getSourcePosition().getLine());
                breakpointList.add(debugPoint);
            }
        }



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
