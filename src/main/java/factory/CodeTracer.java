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
import pojo.Bugs;
import pojo.VarsValues;
import ui.HorBugTable;

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
    private final Bugs trace;
    private final Project project;
    private final String order;
    private final FileEditorManager editorManager;
    private final List<VarsValues> varsValue;
    Color backgroundColor = new Color(240, 57, 45, 80);
    private final TextAttributes textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
    private int currentIndex;
    private VarsValues highlightedVariable;


    public CodeTracer(Project project, Bugs trace, String order) throws IOException {
        this.project = project;
        this.trace = trace;
        this.order = order;
        editorManager = FileEditorManager.getInstance(project);

        String path = project.getBasePath() + "/variablevalues.json";
        String content = Files.readString(Paths.get(path));

        TypeReference<List<VarsValues>> typeReference = new TypeReference<>() {
        };
        varsValue = new ObjectMapper().readValue(content, typeReference);
        setTraceIndex(0);
    }

    public void setTraceIndex(int i) {
        this.currentIndex = i;
        Collection<VarsValues> variableList = collectVariableData();
        updateHighlight(variableList);
    }


    private Collection<VarsValues> collectVariableData() {
        highlightedVariable = varsValue.get(this.currentIndex);
        int highlightedLineNumber = highlightedVariable.getLineNum();
        int currentLineNumber = highlightedLineNumber;

        Map<String, VarsValues> variableMap = new HashMap<>();

        variableMap.put(highlightedVariable.getVariableName(), highlightedVariable);

        int collectedIndex = this.currentIndex + 1;

        Map<String, Boolean> tracker = new HashMap<>();

        while (collectedIndex < this.varsValue.size()) {
            VarsValues variable = varsValue.get(collectedIndex);

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


    private void updateHighlight(Collection<VarsValues> variableList) {
        HorBugTable horBugTable = project.getService(ProjectService.class).getHorBugTable();
        horBugTable.setVariables(variableList);

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
            setTraceIndex(nextIndex);
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
            setTraceIndex(nextIndex);
        }
    }
}
