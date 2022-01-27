package ui;

import Network.GETCalls;
import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import factory.ProjectService;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import pojo.Bugs;
import pojo.VarsValues;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HorBugTable {
    private final ProjectService projectService;
    OkHttpClient client;
    Callback errorCallback, lastSessioncallback;
    JSONObject errorsJson, dataPointsJson, sessionJson;
    DefaultTableModel defaultTableModel, varsDefaultTableModel;
    Object[] headers;
    List<Bugs> bugList;
    List<VarsValues> dataList;
    String executionSessionId;
    Project project;
    String basepath;
    Editor editor;
    FileEditorManager editorManager;
    TextAttributes textattributes;
    Color backgroundColor = new Color(240, 57, 45, 80);
    DefaultTableCellRenderer centerRenderer;
    private JPanel panel1;
    private JTable bugs;
    private JPanel panel11;
    private JPanel panel12;
    private JScrollPane scrollpanel;
    private JPanel varpanel;
    private JLabel variables;
    private JLabel values;
    private JButton fetchSessionButton;
    private JButton refreshButton;
    private JTable varsValue;
    private JScrollPane varsvaluePane;
    private JTable varsValuesTable;
    private JLabel someLable;

    private static final Logger logger = Logger.getInstance(HorBugTable.class);

    public HorBugTable(Project project, ToolWindow toolWindow) {
        this.project = project;
        basepath = this.project.getBasePath();

        this.projectService = ServiceManager.getService(project, ProjectService.class);

        this.projectService.setHorBugTable(this);

        textattributes = new TextAttributes(null, backgroundColor, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);

        fetchSessionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    loadBug(bugs.getSelectedRow());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        varsDefaultTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    scrollpanel.setVisible(true);
                    setTableValues();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public JPanel getContent() {
        return panel1;
    }


    public void setTableValues() throws Exception {

        defaultTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        getLastSessions(0);

        JTableHeader header = this.bugs.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        this.bugs.setCellEditor(this.bugs.getDefaultEditor(Boolean.class));

        centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.bugs.setModel(defaultTableModel);
        this.bugs.setDefaultRenderer(Object.class, centerRenderer);
        this.bugs.setAutoCreateRowSorter(true);

    }

    public void hideAll() {
        scrollpanel.setVisible(false);
    }

    private void getErrors(int pageNum, String sessionId) throws Exception {
        errorCallback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        clearAll();
                        throw new IOException("Unexpected code " + response);
                    }
                    errorsJson = (JSONObject) JSONValue.parse(responseBody.string());
                    parseTableItems();
                }
            }
        };

        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        GETCalls getCalls = new GETCalls();
        getCalls.getCall(PropertiesComponent.getInstance().getValue(Constants.BASE_URL) + Constants.PROJECT_URL
                + "/" + projectId
                + "/traceByException"
                + "/" + sessionId
                + "?exceptionClass="
                + Constants.NPE
                + "&pageNumber=" + pageNum
                + "&pageSize=30", errorCallback);

    }


    private void parseTableItems() {
        JSONArray jsonArray = (JSONArray) errorsJson.get("items");
        JSONObject metadata = (JSONObject) errorsJson.get("metadata");
        JSONObject classInfo = (JSONObject) metadata.get("classInfo");
        JSONObject dataInfo = (JSONObject) metadata.get("dataInfo");
        JSONObject typesInfo = (JSONObject) metadata.get("typeInfo");

        bugList = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            long dataId = jsonObject.getAsNumber("dataId").longValue();
            long threadId = jsonObject.getAsNumber("threadId").longValue();
            long valueId = jsonObject.getAsNumber("value").longValue();
            String executionSessionId = jsonObject.getAsString("executionSessionId");
            long classId;
            long line;
            String sessionId;
            String filename, classname;
            JSONObject dataInfoObject = (JSONObject) dataInfo.get(dataId + "_" + executionSessionId);
            if (dataInfoObject != null) {
                classId = dataInfoObject.getAsNumber("classId").longValue();
                line = dataInfoObject.getAsNumber("line").longValue();
                sessionId = dataInfoObject.getAsString("sessionId");

                JSONObject attributesMap = (JSONObject) dataInfoObject.get("attributesMap");

                JSONObject tempClass = (JSONObject)classInfo.get(String.valueOf(classId) + "_" + sessionId);
                filename = tempClass.getAsString("filename");
                classname = tempClass.getAsString("className");
                Bugs bug = new Bugs(classId, line, dataId, threadId, valueId, executionSessionId, filename, classname);
                bugList.add(bug);


            }

        }

        Object[][] sampleObject = new Object[bugList.size()][];
        Object[] headers = {"Type of Crash", "ClassName", "LineNum", "ThreadId"};

        for (int i=0; i < bugList.size(); i++) {
            sampleObject[i] = new String[]{"NullPointerException", bugList.get(i).getClassname(), String.valueOf(bugList.get(i).getLinenum()), String.valueOf(bugList.get(i).getThreadId())};
        }

        defaultTableModel.setDataVector(sampleObject, headers);
    }

    private void loadBug(int rowNum) throws IOException {
        Bugs bugs = bugList.get(rowNum);
        JSONObject dataJson = new JSONObject();
        executionSessionId = bugs.getExecutionSessionId();
        dataJson.put("sessionId", executionSessionId);
        dataJson.put("threadId", bugs.getThreadId());
        JSONArray arr = new JSONArray();
        arr.add(bugs.getValue());
        dataJson.put("valueId", arr);
        dataJson.put("pageSize", 200);
        dataJson.put("pageNumber", 0);
        dataJson.put("debugPoints", new JSONArray());
        dataJson.put("sortOrder", "DESC");

        logger.info(String.format("Fetch for sessions [%s] on thread [%s]", executionSessionId,  bugs.getThreadId()));

        Callback datapointsCallback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        clearAll();
                        throw new IOException("Unexpected code " + response);
                    }
                    dataPointsJson = (JSONObject) JSONValue.parse(responseBody.string());
                    parseDatapoints();
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            highlightCrash(bugs.getFilename(), (int) bugs.getLinenum());
                        }
                    });
                }
            }
        };


        post(PropertiesComponent.getInstance().getValue(Constants.BASE_URL)
                + Constants.PROJECT_URL
                + "/"
                + PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID)
                + "/filterDataEvents", dataJson.toJSONString(), datapointsCallback);
    }

    private void post(String url, String json, Callback callback) throws IOException {
        client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request.Builder builder = new Request.Builder();

        builder.url(url);
        String token = PropertiesComponent.getInstance().getValue(Constants.TOKEN);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);

        Request request = builder.build();

        client.newCall(request).enqueue(callback);
    }

    private void parseDatapoints() throws IOException {
        JSONArray datapointsArray = (JSONArray) dataPointsJson.get("items");
        JSONObject metadata = (JSONObject) dataPointsJson.get("metadata");
        JSONObject classInfo = (JSONObject) metadata.get("classInfo");
        JSONObject dataInfo = (JSONObject) metadata.get("dataInfo");
        JSONObject stringInfo = (JSONObject) metadata.get("stringInfo");

        dataList = new ArrayList<>();
        Map<String, VarsValues> variableMap = new HashMap<>();

        for (int i = 0; i < datapointsArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) datapointsArray.get(i);
            long dataId = jsonObject.getAsNumber("dataId").longValue();
            long dataValue = jsonObject.getAsNumber("value").longValue();
            JSONObject dataInfoTemp = (JSONObject) dataInfo.get(dataId + "_" + executionSessionId);
            JSONObject attributesMap = (JSONObject) dataInfoTemp.get("attributesMap");

            if (attributesMap.containsKey("Instruction")) {
                continue;
            }

            String variableName = attributesMap.getAsString("Name");

            if (variableName == null) {
                continue;
            }

            if (Arrays.asList("<init>", "makeConcatWithConstants").contains(variableName)) {
                continue;
            }

            if (variableMap.containsKey(variableName)) {
                continue;
            }

            String variableType = attributesMap.getAsString("Type");
            long classId = dataInfoTemp.getAsNumber("classId").longValue();
            int lineNum = dataInfoTemp.getAsNumber("line").intValue();
            JSONObject classInfoTemp = (JSONObject) classInfo.get(classId + "_" + executionSessionId);
            String filename = classInfoTemp.getAsString("filename");


            String dataIdstr = String.valueOf(dataValue);

            if (variableType != null) {
                if (variableType.contains("java/lang/String")) {
                    JSONObject tempStringJson = (JSONObject) stringInfo.get(dataValue + "_" + executionSessionId);
                    if (tempStringJson != null) {
                        dataIdstr = tempStringJson.getAsString("content");
                    }
                }
            }

            Number time = jsonObject.getAsNumber("nanoTime");
            long nanoTime = 0;
                nanoTime = time.longValue();


            VarsValues varsValues = new VarsValues(lineNum, filename, variableName, dataIdstr, nanoTime);



            variableMap.put(variableName, varsValues);
        }

        String content = JSONArray.toJSONString(Arrays.asList(variableMap.values().toArray()));
        String path = project.getBasePath() + "/variablevalues.json";
        File file = new File(path);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(content);
        fileWriter.close();
    }

    private void highlightCrash(String filename, int linenumber) {
        String path = basepath + "/src/main/java/" + filename + ".java";


        VirtualFile file = LocalFileSystem
                .getInstance().findFileByIoFile(new File(path));

        FileEditorManager.getInstance(project).openFile(file, true);

        editorManager = FileEditorManager.getInstance(project);
        editor = editorManager.getSelectedTextEditor();

        editor.getMarkupModel().removeAllHighlighters();
        editor.getMarkupModel().addLineHighlighter(linenumber - 1, HighlighterLayer.CARET_ROW, textattributes);

        ProjectService.getInstance().setCurrentLineNumber(linenumber - 1);

        PropertiesComponent.getInstance().setValue(Constants.TRACK_LINE, 0, 0);
    }

    public void setVariables(List<VarsValues> dataListTemp) {
        JTableHeader header = this.varsValuesTable.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        Object[] headers = {"Variable Name", "Variable Value"};

        String[][] sampleObject = new String[dataListTemp.size()][];
        for (int i = 0; i < dataListTemp.size(); i++) {
            sampleObject[i] = new String[]{dataListTemp.get(i).getVariableName(), dataListTemp.get(i).getVariableValue()};
        }
        if (centerRenderer == null) {
            centerRenderer = new DefaultTableCellRenderer();
        }
        varsDefaultTableModel.setDataVector(sampleObject, headers);
        this.varsValuesTable.setModel(varsDefaultTableModel);
        this.varsValuesTable.setDefaultRenderer(Object.class, centerRenderer);
        this.varsValuesTable.setAutoCreateRowSorter(true);

    }

    private void clearAll() {
        PropertiesComponent.getInstance().setValue(Constants.BASE_URL, "");
        PropertiesComponent.getInstance().setValue(Constants.TOKEN, "");
        PropertiesComponent.getInstance().setValue(Constants.PROJECT_ID, "");
        PropertiesComponent.getInstance().setValue(Constants.PROJECT_TOKEN, "");
    }

    private void getLastSessions(int pageNum) throws Exception {
        lastSessioncallback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        clearAll();
                        throw new IOException("Unexpected code " + response);
                    }
                    sessionJson = (JSONObject) JSONValue.parse(responseBody.string());
                    JSONArray sessionArray = (JSONArray) sessionJson.get("items");
                    JSONObject firstItem = (JSONObject) sessionArray.get(0);
                    getErrors(0, firstItem.getAsString("id"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        String url = PropertiesComponent.getInstance().getValue(Constants.BASE_URL)
                + Constants.PROJECT_URL
                + "/"
                + PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID)
                + "/executions?"
                + "&pageNumber=" + pageNum
                + "&pageSize=30";

        GETCalls getCalls = new GETCalls();
        getCalls.getCall(url, lastSessioncallback);
    }

}
