package actions;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import factory.DebuggerFactory;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.collections.MapIterator;
import org.jetbrains.annotations.NotNull;
import pojo.VarsValues;
import ui.HorBugTable;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JumpBack extends AnAction {
    Color backgroundColor = new Color(240, 57, 45, 80);
    Project project;
    FileEditorManager editorManager;
    Editor editor;
    TextAttributes textattributes;
    ToolWindow toolWindow;


    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
        editorManager = FileEditorManager.getInstance(project);
        editor = editorManager.getSelectedTextEditor();
        toolWindow = ToolWindowManager.getInstance(project).getToolWindow("VideoBug");

        try {
            jumpBackToLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void jumpBackToLine() throws IOException {
        String basePath = project.getBasePath();
        highlight();
    }

    private void highlight() throws IOException {
        int lineNum = PropertiesComponent.getInstance().getInt(Constants.CURRENT_LINE, 0);
        lineNum--;

        String path = project.getBasePath() + "/variablevalues.json";

        String content = Files.readString(Paths.get(path));

        JSONArray jsonArray = (JSONArray)JSONValue.parse(content);

        List<VarsValues> dataList = Constants.convert(jsonArray);

        Map<Integer, List<VarsValues>> groupedData = dataList.stream().collect(Collectors.groupingBy(e -> e.getLineNum()));
        List<Integer> sortedKey = groupedData.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());

        if (!sortedKey.contains(lineNum)) {
            return;
        }

        List<VarsValues> dataListTemp = groupedData.get(lineNum);


        HorBugTable horBugTable = ServiceManager.getService(project, DebuggerFactory.ProjectService.class).getHoBugTable();

        horBugTable.setVariables(dataListTemp);

//        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
//        Content content1 = contentFactory.createContent(horBugTable.getContent(), "TESTA", false);
//
//        toolWindow.getContentManager().removeAllContents(true);
//        toolWindow.getContentManager().addContent(content1);

        editor.getMarkupModel().removeAllHighlighters();
        editor.getMarkupModel().addLineHighlighter(lineNum -1, HighlighterLayer.CARET_ROW, textattributes);
    }
}
