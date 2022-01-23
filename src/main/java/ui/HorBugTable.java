package ui;

import Network.GETCalls;
import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.wm.ToolWindow;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import pojo.Bugs;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    JSONObject errorsJson;
    DefaultTableModel defaultTableModel;
    Object[] headers;

    public HorBugTable(ToolWindow toolWindow) {

    }

    public JPanel getContent() {
        return panel1;
    }

    public void setTableValues() throws Exception {

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
        defaultTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


        defaultTableModel.setDataVector(objects, headers);

        this.bugs.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                //System.out.print(bugs.getValueAt(bugs.getSelectedRow(), 0).toString());
            }
        });

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

                    System.out.print(responseBody.string());
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

        List<Bugs> bugList = new ArrayList<>();

        for (int i=0; i<jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            long dataId = (Long)jsonObject.get("dataId");
            long threadId = (Long)jsonObject.get("threadId");
            long valueId = (Long)jsonObject.get("value");
            String executionSessionId = jsonObject.getAsString("executionSessionId");
            long classId;
            long line;
            String sessionId;
            String filename, classname;
            JSONObject dataInfoObject = (JSONObject)dataInfo.get(dataId + "_" + executionSessionId);
            if (dataInfoObject != null) {
                classId = (Long) dataInfoObject.get("classId");
                line = (Long) dataInfoObject.get("line");
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



}
