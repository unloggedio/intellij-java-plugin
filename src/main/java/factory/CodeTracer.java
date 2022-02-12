package factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import extension.InsidiousDebugProcessStarter;
import extension.InsidiousExecutor;
import extension.InsidiousProgramRunner;
import network.Client;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pojo.Bugs;
import pojo.VarsValues;
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
    private final Project project;
    Color backgroundColor = new Color(240, 57, 45, 80);
    private final TextAttributes textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
    private Client client;
    private Bugs trace;
    private String order;
    private FileEditorManager editorManager;
    private final List<VarsValues> varsValue;
    private final String source;
    private String sessionId;
    private int currentIndex;
    private VarsValues highlightedVariable;

    public CodeTracer(Project project, Bugs trace, String order, String source) throws IOException, ExecutionException {
        this.project = project;
        this.trace = trace;
        this.order = order;
        editorManager = FileEditorManager.getInstance(project);
        this.source = source;
        String path = project.getBasePath() + "/variablevalues.json";
        String content = Files.readString(Paths.get(path));
        TypeReference<List<VarsValues>> typeReference = new TypeReference<>() {
        };
        varsValue = new ObjectMapper().readValue(content, typeReference);
        setTraceIndex(0);
    }

    public CodeTracer(Project project, String sessionId, Client client, String source) throws IOException, ExecutionException {
        this.project = project;
        this.sessionId = sessionId;
        this.client = client;
        this.source = source;

        String path = project.getBasePath() + "/variablevalues.json";
        String content = Files.readString(Paths.get(path));
        TypeReference<List<VarsValues>> typeReference = new TypeReference<>() {
        };
        varsValue = new ObjectMapper().readValue(content, typeReference);

//
//        Executor executor = new InsidiousExecutor();
//        RunManagerImpl runManager = new RunManagerImpl(project, null);
//
//
//        RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(runManager);
//
//        ProgramRunner runner = new InsidiousProgramRunner();
//
//        ExecutionEnvironment environment = new ExecutionEnvironment(executor, runner, settings, project);
//        XDebugProcessStarter processStarter = new InsidiousDebugProcessStarter(sessionId, client, varsValue.get(0).getFilename());
//
//
//        @NotNull XDebugSession session = XDebuggerManager.getInstance(project).startSession(environment, processStarter);
//        session.resume();
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
        if (source.equals("exceptions")) {
            HorBugTable horBugTable = project.getService(ProjectService.class).getHorBugTable();
            horBugTable.setVariables(variableList);
        } else if (source.equals("traces")) {
            LogicBugs logicBugs = project.getService(ProjectService.class).getLogicBugs();
            logicBugs.setVariables(variableList);
        } else {
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
            setTraceIndex(nextIndex);
        }
    }

    public void forward() {
        highlightedVariable = varsValue.get(this.currentIndex);
        int nextIndex;
        if (order.equals("DESC")) {
            nextIndex = this.currentIndex - 1;
            while (nextIndex > -1 && varsValue.get(nextIndex).getLineNum() == highlightedVariable.getLineNum()) {
                nextIndex--;
            }
        } else {
            nextIndex = this.currentIndex + 1;
            while (nextIndex < varsValue.size() && varsValue.get(nextIndex).getLineNum() == highlightedVariable.getLineNum()) {
                nextIndex++;
            }
        }
        if (nextIndex >= 0 && nextIndex < varsValue.size()) {
            setTraceIndex(nextIndex);
        }
    }
}
