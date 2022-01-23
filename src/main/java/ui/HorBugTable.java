package ui;

import Network.GETCalls;
import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import pojo.Bugs;
import pojo.VarsValues;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HorBugTable {
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
    OkHttpClient client;
    Callback errorCallback;
    JSONObject errorsJson, dataPointsJson;
    DefaultTableModel defaultTableModel;
    Object[] headers;
    List<Bugs> bugList;
    List<VarsValues> dataList;
    String executionSessionId;
    Project project;

    public HorBugTable(Project project, ToolWindow toolWindow) {
        this.project = project;
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
        getErrors(0);

        Object[] headers = {"Type", "FileName", "LineNum", "Time"};
        Object[][] objects = {
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:01"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Jan-2022 10:00:02"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:03"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Jan-2022 10:00:04"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:05"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Jan-2022 10:00:06"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:07"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19","12-Jan-2022 10:00:08"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:09"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Dec-2022 10:00:11"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:14"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Jan-2022 00:00:01"}
        };
        JTableHeader header = this.bugs.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        this.bugs.setCellEditor(this.bugs.getDefaultEditor( Boolean.class ) );

        //defaultTableModel.setDataVector(objects, headers);


        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.bugs.setModel(defaultTableModel);
        this.bugs.setDefaultRenderer(Object.class, centerRenderer);
        this.bugs.setAutoCreateRowSorter(true);
    }

    public void setVariable(String variablestr) {
        variables.setText(variablestr);
    }

    public void setVarValue(String value) {
        values.setText(value);
    }

    public void hideAll() {
        panel12.setVisible(false);
    }

    private void getErrors(int pageNum) throws Exception {
        errorCallback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                    errorsJson = (JSONObject) JSONValue.parse(responseBody.string());
                    parseTableItems();
                }
            }
        };

    String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
    GETCalls getCalls = new GETCalls();
    getCalls.getCall(PropertiesComponent.getInstance().getValue(Constants.BASE_URL) + Constants.PROJECT_URL
            + "/" + projectId
            + "/traceByException?exceptionClass="
            + Constants.NPE
            + "&pageNumber=" + String.valueOf(pageNum)
            + "&pageSize=10", errorCallback);

    }


    private void parseTableItems() {
        JSONArray jsonArray = (JSONArray)errorsJson.get("items");
        JSONObject metadata = (JSONObject)errorsJson.get("metadata");
        JSONObject classInfo = (JSONObject) metadata.get("classInfo");
        JSONObject dataInfo = (JSONObject) metadata.get("dataInfo");

        bugList = new ArrayList<>();

        for (int i=0; i<jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            long dataId = jsonObject.getAsNumber("dataId").longValue();
            long threadId = jsonObject.getAsNumber("threadId").longValue();
            long valueId = jsonObject.getAsNumber("value").longValue();
            String executionSessionId = jsonObject.getAsString("executionSessionId");
            long classId;
            long line;
            String sessionId;
            String filename, classname;
            JSONObject dataInfoObject = (JSONObject)dataInfo.get(dataId + "_" + executionSessionId);
            if (dataInfoObject != null) {
                classId = dataInfoObject.getAsNumber("classId").longValue();
                line = dataInfoObject.getAsNumber("line").longValue();
                sessionId = dataInfoObject.getAsString("sessionId");

                JSONObject tempClass = (JSONObject)classInfo.get(String.valueOf(classId) + "_" + sessionId);
                filename = tempClass.getAsString("filename");
                classname = tempClass.getAsString("className");
                Bugs bug = new Bugs(classId, line, dataId, threadId, valueId, executionSessionId, filename, classname);
                bugList.add(bug);
            }

        }

        Object[][] sampleObject = new Object[bugList.size()][];
        Object[] headers = {"ClassName", "LineNum", "ThreadId"};

        for (int i=0; i < bugList.size(); i++) {
            sampleObject[i] = new String[]{bugList.get(i).getClassname(), String.valueOf(bugList.get(i).getLinenum()), String.valueOf(bugList.get(i).getThreadId())};
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
        dataJson.put("pageSize", 10);
        dataJson.put("pageNumber", 0);
        dataJson.put("debugPoints", new JSONArray());
        dataJson.put("sortOrder", "DESC");

        Callback datapointsCallback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                    dataPointsJson = (JSONObject) JSONValue.parse(responseBody.string());
                    parseDatapoints();
                }
            }
        };

        post(PropertiesComponent.getInstance().getValue(Constants.BASE_URL)
                + Constants.PROJECT_URL
                + "/"
                + PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID)
                + "/filterDataEvents", dataJson.toJSONString(), datapointsCallback);
    }

    private  void post(String url, String json, Callback callback) throws IOException {
        client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request.Builder builder = new Request.Builder();

        builder.url(url);
        String token = PropertiesComponent.getInstance().getValue(Constants.TOKEN);
        if ( token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);

        Request request = builder.build();

        client.newCall(request).enqueue(callback);
    }

    private void parseDatapoints() throws IOException {
        JSONArray datapointsArray = (JSONArray)dataPointsJson.get("items");
        JSONObject metadata = (JSONObject)dataPointsJson.get("metadata");
        JSONObject classInfo = (JSONObject) metadata.get("classInfo");
        JSONObject dataInfo = (JSONObject) metadata.get("dataInfo");
        JSONObject stringInfo = (JSONObject) metadata.get("stringInfo");

        dataList = new ArrayList<>();

        for (int i=0; i<datapointsArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) datapointsArray.get(i);
            long dataId = jsonObject.getAsNumber("dataId").longValue();
            long dataValue = jsonObject.getAsNumber("value").longValue();
            JSONObject dataInfoTemp = (JSONObject)dataInfo.get(String.valueOf(dataId) + "_" + executionSessionId);
            JSONObject attributesMap = (JSONObject) dataInfoTemp.get("attributesMap");
            String variableName = attributesMap.getAsString("Name");
            long classId = dataInfoTemp.getAsNumber("classId").longValue();
            int lineNum = dataInfoTemp.getAsNumber("line").intValue();
            JSONObject classInfoTemp = (JSONObject) classInfo.get(String.valueOf(classId) + "_" + executionSessionId);
            String filename = classInfoTemp.getAsString("filename");
            JSONObject tempStringJson = (JSONObject) stringInfo.get(String.valueOf(dataValue) + "_" + executionSessionId);
            String dataIdstr = String.valueOf(dataValue);
            if (tempStringJson != null) {
                dataIdstr = tempStringJson.getAsString("content");
            }

            if (variableName != null) {
                VarsValues varsValues = new VarsValues(lineNum, filename, variableName, dataIdstr);
                dataList.add(varsValues);
            }
        }

        String debugpoints = JSONArray.toJSONString(dataList) ;
        String path = project.getBasePath() + "/DebugPoints.json";
        File file = new File(path);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(debugpoints);
        fileWriter.close();
    }

}
