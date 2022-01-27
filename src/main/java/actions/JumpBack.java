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
import factory.ProjectService;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONValue;
import org.jetbrains.annotations.NotNull;
import pojo.VarsValues;
import ui.HorBugTable;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JumpBack extends AnAction {
    Color backgroundColor = new Color(240, 57, 45, 80);
    Project project;
    FileEditorManager editorManager;
    Editor editor;
    TextAttributes textattributes;
    ToolWindow toolWindow;
    HorBugTable horBugTable;
    int readIndex;
    List<VarsValues> linevalues;
    Map<Integer, List<VarsValues>> groupedData;
    List<Integer> sortedKey;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = e.getProject();
        textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
        editorManager = FileEditorManager.getInstance(project);
        editor = editorManager.getSelectedTextEditor();
        toolWindow = ToolWindowManager.getInstance(project).getToolWindow("VideoBug");


        try {
            highlight();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void highlight() throws IOException {
        readIndex = PropertiesComponent.getInstance().getInt(Constants.TRACK_LINE, 0);

        int currentLine = ProjectService.getInstance().getCurrentLineNumber();


        String path = project.getBasePath() + "/variablevalues.json";

        String content = Files.readString(Paths.get(path));

        JSONArray jsonArray = (JSONArray) JSONValue.parse(content);

        List<VarsValues> dataList = Constants.convert(jsonArray);

        groupedData = dataList.stream().collect(Collectors.groupingBy(e -> e.getLineNum()));
        sortedKey = groupedData.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());

        linevalues = new LinkedList<>();


        for (VarsValues varsValues : dataList) {
            if (varsValues.getLineNum() <= currentLine) {
                linevalues.add(varsValues);
            }
        }

        updateColorData();
    }

    private void updateColorData() {

        VarsValues first = linevalues.get(0);
        HorBugTable horBugTable = ServiceManager.getService(project, ProjectService.class).getHorBugTable();
        horBugTable.setVariables(linevalues);
        editor.getMarkupModel().removeAllHighlighters();
        int nextLineNumber = first.getLineNum() - 1;
        editor.getMarkupModel().addLineHighlighter(nextLineNumber, HighlighterLayer.CARET_ROW, textattributes);
        ProjectService.getInstance().setCurrentLineNumber(nextLineNumber);
        readIndex++;
        PropertiesComponent.getInstance().setValue(Constants.TRACK_LINE, readIndex, 0);
    }
}
