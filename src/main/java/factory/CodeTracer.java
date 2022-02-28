package factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import pojo.TracePoint;
import pojo.DataEvent;
import ui.HorBugTable;
import ui.LogicBugs;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeTracer {
    private final TracePoint trace;
    private final Project project;
    private final String order;
    private final FileEditorManager editorManager;
    private final List<DataEvent> varsValue;
    Color backgroundColor = new Color(240, 57, 45, 80);
    private final TextAttributes textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
    private int currentIndex;
    private DataEvent highlightedVariable;
    private String source;

    public CodeTracer(Project project, TracePoint trace, String order, String source) throws IOException {
        this.project = project;
        this.trace = trace;
        this.order = order;
        editorManager = FileEditorManager.getInstance(project);
        this.source = source;
        String path = project.getBasePath() + "/variablevalues.json";
        String content = Files.readString(Paths.get(path));

        TypeReference<List<DataEvent>> typeReference = new TypeReference<>() {
        };
        varsValue = new ObjectMapper().readValue(content, typeReference);
        setTraceIndex(0, source);
    }

    public void setTraceIndex(int i, String source) {
        this.currentIndex = i;
        Collection<DataEvent> variableList = collectVariableData();
        updateHighlight(variableList, source);
    }


    private Collection<DataEvent> collectVariableData() {
        highlightedVariable = varsValue.get(this.currentIndex);
        int highlightedLineNumber = highlightedVariable.getLineNum();
        int currentLineNumber = highlightedLineNumber;

        Map<String, DataEvent> variableMap = new HashMap<>();

        variableMap.put(highlightedVariable.getVariableName(), highlightedVariable);

        int collectedIndex = this.currentIndex + 1;

        Map<String, Boolean> tracker = new HashMap<>();

        while (collectedIndex < this.varsValue.size()) {
            DataEvent variable = varsValue.get(collectedIndex);

            if (variable.getLineNum() > currentLineNumber) {
                break;
            }
            currentLineNumber = variable.getLineNum();


            String variableHash = variable.hash();
            if (tracker.containsKey(variableHash)) {
                break;
            }
            tracker.put(variableHash, true);

            collectedIndex++;
            if (variableMap.containsKey(variable.getVariableName())) {
                continue;
            }


            variableMap.put(variable.getVariableName(), variable);
        }
        return variableMap.values();

    }


    private void updateHighlight(Collection<DataEvent> variableList, String source) {
        if (source.equals("exceptions")){
            HorBugTable horBugTable = project.getService(InsidiousService.class).getHorBugTable();
            horBugTable.setVariables(variableList);
        }
        else if (source.equals("traces")) {
            LogicBugs logicBugs = project.getService(InsidiousService.class).getLogicBugs();
            logicBugs.setVariables(variableList);
        }
        else {
            return;
        }


        String path = project.getBasePath() + "/src/main/java/" + highlightedVariable.getFilename() + ".java";


        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(path));

        assert file != null;

        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager.getInstance(project).openFile(file, true);

            @Nullable Editor editor = editorManager.getSelectedTextEditor();
            assert editor != null;
            editor.getMarkupModel().removeAllHighlighters();
            editor.getMarkupModel().addLineHighlighter(highlightedVariable.getLineNum() - 1, HighlighterLayer.CARET_ROW, textattributes);
        });


    }

    public void back() {
        highlightedVariable = varsValue.get(this.currentIndex);
        int nextIndex;
        if (order.equals("DESC")) {
            nextIndex = this.currentIndex + 1;
            while (nextIndex < varsValue.size() && varsValue.get(nextIndex).getLineNum() == highlightedVariable.getLineNum()) {
                nextIndex++;
            }
        } else {
            nextIndex = this.currentIndex - 1;
            while (nextIndex > 0 && varsValue.get(nextIndex).getLineNum() == highlightedVariable.getLineNum()) {
                nextIndex--;
            }
        }
        if (nextIndex > 0 && nextIndex < varsValue.size()) {
            setTraceIndex(nextIndex, this.source);
        }
    }

    public void forward() {
        highlightedVariable = varsValue.get(this.currentIndex);
        int nextIndex;
        if (order.equals("DESC")) {
            nextIndex = this.currentIndex - 1;
            while (nextIndex < varsValue.size() && varsValue.get(nextIndex).getLineNum() == highlightedVariable.getLineNum()) {
                nextIndex--;
            }
        } else {
            nextIndex = this.currentIndex + 1;
            while (nextIndex > 0 && varsValue.get(nextIndex).getLineNum() == highlightedVariable.getLineNum()) {
                nextIndex++;
            }
        }
        if (nextIndex > 0 && nextIndex < varsValue.size()) {
            setTraceIndex(nextIndex, this.source);
        }
    }
}
