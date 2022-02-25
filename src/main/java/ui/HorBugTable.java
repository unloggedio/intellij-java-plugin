package ui;

import actions.Constants;
import callbacks.GetProjectSessionErrorsCallback;
import callbacks.GetProjectSessionsCallback;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import factory.ProjectService;
import net.minidev.json.JSONObject;
import network.pojo.ExceptionResponse;
import network.pojo.ExecutionSession;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import pojo.DataEvent;
import pojo.TracePoint;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class HorBugTable {
    private static final Logger logger = Logger.getInstance(HorBugTable.class);
    private final ProjectService projectService;
    private final ToolWindow toolWindow;
    OkHttpClient client;
    Callback errorCallback, lastSessioncallback;
    JSONObject errorsJson, dataPointsJson, sessionJson;
    DefaultTableModel defaultTableModel, varsDefaultTableModel, bugTypeTableModel;
    Object[] headers;
    List<DataEvent> dataList;
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
    private JProgressBar progressBarfield;
    private JLabel errorLabel;
    private JProgressBar variableProgressbar;
    private JLabel varvalueErrorLabel;
    private JButton applybutton;
    private JTextField traceIdfield;
    private JLabel someLable;
    private List<TracePoint> bugList;

    public HorBugTable(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        basepath = this.project.getBasePath();

        this.projectService = project.getService(ProjectService.class);

        this.projectService.setHorBugTable(this);

        fetchSessionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                loadBug(bugs.getSelectedRow());
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

        variableProgressbar.setVisible(false);

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
                    public void success(List<TracePoint> tracePointCollection) {
                        updateProgressbar("bugs", 100);
                        if (tracePointCollection.size() == 0) {
                            updateErrorLabel("No data available, or data may have been deleted!");
                        } else {
                            updateErrorLabel("");
                            scrollpanel.setVisible(true);
                            parseTableItems(tracePointCollection);
                        }

                    }
                });

    }

    private void parseTableItems(List<TracePoint> tracePointCollection) {
        this.bugList = tracePointCollection;
        Object[][] sampleObject = new Object[bugList.size()][];
        Object[] headers = {"Type of Crash", "ClassName", "LineNum", "ThreadId", "Timestamp"};

        int i = 0;
        for (TracePoint tracePoint : bugList) {
            String className = tracePoint.getClassname().substring(tracePoint.getClassname().lastIndexOf('/') + 1);
            sampleObject[i] = new String[]{
                    tracePoint.getExceptionClass().substring(tracePoint.getExceptionClass().lastIndexOf('.') + 1),
                    className,
                    String.valueOf(tracePoint.getLinenum()),
                    String.valueOf(tracePoint.getThreadId()),
                    new Date(tracePoint.getRecordedAt()).toString()
            };
            i++;
        }

        defaultTableModel.setDataVector(sampleObject, headers);
    }

    private void loadBug(int rowNum) {
        TracePoint selectedTrace = bugList.get(rowNum);
        try {
            logger.info(String.format("Fetch for sessions [%s] on thread [%s]", executionSessionId, selectedTrace.getThreadId()));
            projectService.setTracePoint(selectedTrace);
        } catch (Exception e) {
            e.printStackTrace();
            Messages.showErrorDialog(project, e.getMessage(), "Failed");
        }
    }

    public void setVariables(Collection<DataEvent> dataListTemp) {
        JTableHeader header = this.varsValuesTable.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        Object[] headers = {"Variable Name", "Variable Value"};

        String[][] sampleObject = new String[dataListTemp.size()][];

        int i = 0;
        for (DataEvent dataEvent : dataListTemp) {
            sampleObject[i] = new String[]{dataEvent.getVariableName(), dataEvent.getVariableValue()};
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
                            updateProgressbar("bugs", 50);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        hideTable("bugs");
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

    public void showCredentialsWindow() {


    }

    private void hideTable(String table) {
        if (table.equals("bugs")) {
            progressBarfield.setVisible(true);
            scrollpanel.setVisible(false);
        } else if (table.equals("varsvalues")) {
            variableProgressbar.setVisible(true);
            varsvaluePane.setVisible(false);
        }

    }

    private void updateProgressbar(String table, int value) {

        if (table.equals("bugs")) {
            progressBarfield.setValue(value);
            if (value == 100) {
                progressBarfield.setVisible(false);
            }
        } else if (table.equals("varsvalues")) {
            variableProgressbar.setValue(value);
            if (value == 100) {
                variableProgressbar.setVisible(false);
            }
        }

    }

    private void updateErrorLabel(String text) {
        errorLabel.setText(text);
    }
}
