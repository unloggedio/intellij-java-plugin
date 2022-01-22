package actions;

import Network.GETCalls;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TestAction extends AnAction {
    int lineNum = 0;
    Color backgroundColor = new Color(255, 87, 51);
    String url;
    Project project;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        VirtualFile file = LocalFileSystem
        .getInstance().findFileByIoFile(new File("/Users/shardul/Projects/VideoBug/tests/intellij-sdk-code-samples/SampleProject/src/TestFile.java"));

        FileEditorManager.getInstance(e.getProject()).openFile(file, true);

        final TextAttributes textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
        final FileEditorManager editorManager = FileEditorManager.getInstance(e.getProject());
        final Editor editor = editorManager.getSelectedTextEditor();

        editor.getMarkupModel().removeAllHighlighters();
        lineNum++;
        editor.getMarkupModel().addLineHighlighter(lineNum, HighlighterLayer.CARET_ROW, textattributes);


    }


    private void takeAction(String expression) {
        switch(expression)
        {
            case Constants.CLASSINFO:
                url = "project/05be02bf-47d0-4bc6-87ba-9f7150b83946/execution/3e5d52e7-efcf-433a-aae6-cfa43761067d/classinfo";
                break;

            case Constants.DATAINFO:
                url = "project/05be02bf-47d0-4bc6-87ba-9f7150b83946/execution/3e5d52e7-efcf-433a-aae6-cfa43761067d/datainfo";
                break;

            case Constants.DATAEVENTS:
                url = "project/05be02bf-47d0-4bc6-87ba-9f7150b83946/execution/3e5d52e7-efcf-433a-aae6-cfa43761067d/dataevents";
                break;

            default :
                break;
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
}
