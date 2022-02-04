package ui;

import actions.Constants;
import callbacks.FilteredDataEventsCallback;
import callbacks.GetProjectSessionErrorsCallback;
import callbacks.GetProjectSessionsCallback;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import factory.ProjectService;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import network.pojo.ExceptionResponse;
import network.pojo.ExecutionSession;
import network.pojo.FilteredDataEventsRequest;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import pojo.Bugs;
import pojo.VarsValues;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HorBugTable {
    private static final Logger logger = Logger.getInstance(HorBugTable.class);
    private final ProjectService projectService;
    OkHttpClient client;
    Callback errorCallback, lastSessioncallback;
    JSONObject errorsJson, dataPointsJson, sessionJson;
    DefaultTableModel defaultTableModel, varsDefaultTableModel, bugTypeTableModel;
    Object[] headers;
    List<VarsValues> dataList;
    String executionSessionId;
    Project project;
    String basepath;
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
    private JTable bugTypes;
    private JPanel customBugPanel;
    private JButton custombugButton;
    private JButton applybutton;
    private JTextField traceIdfield;
    private JLabel someLable;
    private List<Bugs> bugList;
    private ToolWindow toolWindow;

    public HorBugTable(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        basepath = this.project.getBasePath();

        this.projectService = project.getService(ProjectService.class);

        this.projectService.setHorBugTable(this);

        this.projectService.setServerEndpoint(PropertiesComponent.getInstance().getValue(Constants.BASE_URL));

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

        bugTypeTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }

            @Override
            public Class getColumnClass(int columnIndex) {
                if (columnIndex == 1) {
                    return Boolean.class;
                }
                return String.class;
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
        custombugButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showDialog();
            }
        });

        initBugTypeTable();
        applybutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                getLastSessionsForTraces();
            }
        });
    }

    public JPanel getContent() {
        return panel1;
    }


    public void setTableValues() {

        defaultTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        getLastSessions();

        JTableHeader header = this.bugs.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        this.bugs.setCellEditor(this.bugs.getDefaultEditor(Boolean.class));


        centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.bugs.setModel(defaultTableModel);
        this.bugs.setDefaultRenderer(Object.class, centerRenderer);
        this.bugs.setAutoCreateRowSorter(true);

    }

    private void getErrors(int pageNum, String sessionId) throws Exception {

        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        List<String> classList = Arrays.asList(PropertiesComponent.getInstance().getValue(Constants.ERROR_NAMES));
        project.getService(ProjectService.class).getTracesByClassForProjectAndSessionId(
                projectId, sessionId, classList,
                new GetProjectSessionErrorsCallback() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {

                    }

                    @Override
                    public void success(List<Bugs> bugsCollection) {
                        parseTableItems(bugsCollection);
                    }
                });

    }

    private void getTraces(int pageNum, String sessionId, String traceValue) {
        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        project.getService(ProjectService.class).getTracesByClassForProjectAndSessionIdAndTracevalue(projectId, sessionId,
                traceValue, new GetProjectSessionErrorsCallback() {
                    @Override
                    public void error(ExceptionResponse errorResponse) {

                    }

                    @Override
                    public void success(List<Bugs> bugsCollection) {
                        parseTableItems(bugsCollection);
                    }
                });
    }


    private void parseTableItems(List<Bugs> bugsCollection) {
        this.bugList = bugsCollection;
        Object[][] sampleObject = new Object[bugList.size()][];
        Object[] headers = {"Type of Crash", "ClassName", "LineNum", "ThreadId"};

        int i = 0;
        for (Bugs bugs : bugList) {
            String className = bugs.getClassname().substring(bugs.getClassname().lastIndexOf('/') + 1);
            sampleObject[i] = new String[]{bugs.getExceptionClass().substring(bugs.getExceptionClass().lastIndexOf('.') + 1), className, String.valueOf(bugs.getLinenum()), String.valueOf(bugs.getThreadId())};
            i++;
        }

        defaultTableModel.setDataVector(sampleObject, headers);
    }

    private void loadBug(int rowNum) throws IOException {
        Bugs selectedTrace = bugList.get(rowNum);
        FilteredDataEventsRequest filteredDataEventsRequest = new FilteredDataEventsRequest();
        filteredDataEventsRequest.setSessionId(selectedTrace.getExecutionSessionId());
        filteredDataEventsRequest.setThreadId(selectedTrace.getThreadId());
        filteredDataEventsRequest.setValueId(Collections.singletonList(selectedTrace.getValue()));
        filteredDataEventsRequest.setPageSize(200);
        filteredDataEventsRequest.setPageNumber(0);
        filteredDataEventsRequest.setDebugPoints(Collections.emptyList());
        filteredDataEventsRequest.setSortOrder("DESC");

        logger.info(String.format("Fetch for sessions [%s] on thread [%s]", executionSessionId, selectedTrace.getThreadId()));


        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        project.getService(ProjectService.class).filterDataEvents(
                projectId, filteredDataEventsRequest,
                new FilteredDataEventsCallback() {
                    @Override
                    public void error(ExceptionResponse errrorResponse) {
                        System.out.print(errrorResponse);
                    }

                    @Override
                    public void success(List<VarsValues> dataList) {

                        String content = JSONArray.toJSONString(dataList);
                        String path = project.getBasePath() + "/variablevalues.json";
                        File file = new File(path);
                        FileWriter fileWriter = null;
                        try {
                            fileWriter = new FileWriter(file);
                            fileWriter.write(content);
                            fileWriter.close();
                            project.getService(ProjectService.class).startTracer(selectedTrace, "DESC", "exceptions");
                        } catch (Exception e) {
                            e.printStackTrace();
                            ExceptionResponse exceptionResponse = new ExceptionResponse();
                            exceptionResponse.setMessage(e.getMessage());
                            error(exceptionResponse);
                            return;
                        }

                    }
                }
        );

    }

    public void setVariables(Collection<VarsValues> dataListTemp) {
        JTableHeader header = this.varsValuesTable.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        Object[] headers = {"Variable Name", "Variable Value"};

        String[][] sampleObject = new String[dataListTemp.size()][];

        int i = 0;
        for (VarsValues varsValues : dataListTemp) {
            sampleObject[i] = new String[]{varsValues.getVariableName(), varsValues.getVariableValue()};
            i++;
        }

        if (centerRenderer == null) {
            centerRenderer = new DefaultTableCellRenderer();
        }
        varsDefaultTableModel.setDataVector(sampleObject, headers);
        this.varsValuesTable.setModel(varsDefaultTableModel);
        this.varsValuesTable.setDefaultRenderer(Object.class, centerRenderer);
        this.varsValuesTable.setAutoCreateRowSorter(true);

    }

    private void getLastSessions() {
        project.getService(ProjectService.class).getProjectSessions(
                PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID),
                new GetProjectSessionsCallback() {
                    @Override
                    public void error(String message) {
                        if (message.equals("401")) {
                            clearAll();
                            showCredentialsWindow();
                        }
                    }

                    @Override
                    public void success(List<ExecutionSession> executionSessionList) {
                        try {
                            getErrors(0, executionSessionList.get(0).getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void getLastSessionsForTraces() {
        project.getService(ProjectService.class).getProjectSessions(
                PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID),
                new GetProjectSessionsCallback() {
                    @Override
                    public void error(String message) {
                        if (message.equals("401")) {
                            clearAll();
                            showCredentialsWindow();
                        }
                    }

                    @Override
                    public void success(List<ExecutionSession> executionSessionList) {
                        try {
                            getTraces(0, executionSessionList.get(0).getId(), traceIdfield.getText());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void initBugTypeTable() {
        JTableHeader header = this.bugTypes.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        Object[] headers = {"Error Type", "Track it?"};
        Object[][] errorTypes = {
                {"java.lang.NullPointerException", true},
                {"java.lang.ArrayIndexOutOfBoundsException", true},
                {"java.lang.StackOverflowError", true},
                {"java.lang.IllegalArgumentException", true},
                {"java.lang.IllegalThreadStateException", true},
                {"java.lang.IllegalStateException", true},
                {"java.lang.RuntimeException", true},
                {"java.io.IOException", true},
                {"java.io.FileNotFoundException", true},
                {"java.net.SocketException", true},
                {"java.net.UnknownHostException", true},
                {"java.lang.ArithmeticException", true}
        };

        bugTypeTableModel.setDataVector(errorTypes, headers);
        bugTypes.setModel(bugTypeTableModel);

        bugTypes.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {

                if ((Boolean) bugTypes.getModel().getValueAt(tableModelEvent.getFirstRow(), 1)) {
                    addErrorValue((String) bugTypes.getModel().getValueAt(tableModelEvent.getFirstRow(), 0));
                } else {
                    removeValue((String) bugTypes.getModel().getValueAt(tableModelEvent.getFirstRow(), 0));
                }

            }
        });
    }

    private void showDialog() {
        String value = Messages.showInputDialog("Error Type", "What's the Error Name?", null);

        if (value == null || value == "") {
            return;
        }

        addErrorValue(value);
    }

    private void addErrorValue(String value) {
        String existingValue = PropertiesComponent.getInstance().getValue(Constants.ERROR_NAMES, "");
        if (existingValue.contains(value)) {
            return;
        }
        if (existingValue.equals("")) {
            existingValue = value;
        } else {
            existingValue = existingValue + "," + value;
        }

        storeValue(existingValue);
    }

    private void removeValue(String value) {
        String existingValue = PropertiesComponent.getInstance().getValue(Constants.ERROR_NAMES, "");
        if (existingValue.equals("")) {
            return;
        }

        if (existingValue.contains(value + ",")) {
            existingValue = existingValue.replaceAll(value + ",", "");
        }
        if (existingValue.contains("," + value)) {
            existingValue = existingValue.replaceAll("," + value, "");
        }
        if (existingValue.contains(value)) {
            existingValue = existingValue.replaceAll(value, "");
        }

        storeValue(existingValue);
    }

    private void storeValue(String value) {
        System.out.print(value + "\n");
        PropertiesComponent.getInstance().setValue(Constants.ERROR_NAMES, value);
    }

    private void clearAll() {
        PropertiesComponent.getInstance().setValue(Constants.TOKEN, "");
    }

    private void showCredentialsWindow() {

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Content content = toolWindow.getContentManager().findContent("Exceptions");
                if (content != null) {
                    toolWindow.getContentManager().removeContent(content, true);
                    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                    Credentials credentials = new Credentials(project, toolWindow);
                    Content credentialContent = contentFactory.createContent(credentials.getContent(), "Credentials", false);
                    toolWindow.getContentManager().addContent(credentialContent);
                }
            }
        });

    }
}
