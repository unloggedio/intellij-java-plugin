package ui;

import actions.Constants;
import callbacks.FilteredDataEventsCallback;
import callbacks.GetProjectSessionErrorsCallback;
import callbacks.GetProjectSessionsCallback;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import factory.ProjectService;
import net.minidev.json.JSONArray;
import network.pojo.DebugPoint;
import network.pojo.ExceptionResponse;
import network.pojo.ExecutionSession;
import network.pojo.FilteredDataEventsRequest;
import pojo.TracePoint;
import pojo.DataEvent;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LogicBugs {
    private final Project project;
    private final ToolWindow toolWindow;
    DefaultTableCellRenderer centerRenderer;
    private JPanel mainpanel;
    private JPanel searchPanel;
    private JScrollPane varsvaluePane;
    private JTable bugsTable;
    private JTable varsvalueTable;
    private JTextField traceIdfield;
    private JLabel searchLabel;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton fetchButton;
    private JProgressBar progressBarfield;
    private JLabel errorLabel;
    private JPanel varpanel;
    private JProgressBar variableProgressbar;
    private JLabel varvalueErrorLabel;
    private JScrollPane scrollpanel;
    private List<TracePoint> bugList;
    private DefaultTableModel defaultTableModelTraces, defaultTableModelvarsValues;
    private final ProjectService projectService;

    public LogicBugs(Project project, ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
        this.project = project;
        this.projectService = project.getService(ProjectService.class);
        this.projectService.setLogicBugs(this);
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (traceIdfield.getText().equals("")) {
                    Messages.showInfoMessage("Empty String reference is not allowed", "Empty String");
                    return;
                }
                getLastSessionsForTraces();
                hideTable("bugs");
            }
        });


        fetchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    loadBug(bugsTable.getSelectedRow());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        initTables();
        variableProgressbar.setVisible(false);
    }

    private void initTables() {
        defaultTableModelTraces = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        defaultTableModelvarsValues = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTableHeader header = this.bugsTable.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        this.bugsTable.setCellEditor(this.bugsTable.getDefaultEditor(Boolean.class));


        centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.bugsTable.setModel(defaultTableModelTraces);
        this.bugsTable.setDefaultRenderer(Object.class, centerRenderer);
        this.bugsTable.setAutoCreateRowSorter(true);
    }

    public JPanel getContent() {
        return mainpanel;
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
                            updateProgressbar("bugs", 50);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
                    public void success(List<TracePoint> tracePointCollection) {
                        updateProgressbar("bugs", 100);
                        if (tracePointCollection.size() == 0) {
                            updateErrorLabel("No data availalbe, or data may have been deleted!");
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
        Object[] headers = {"ClassName", "LineNum", "ThreadId"};

        int i = 0;
        for (TracePoint tracePoint : bugList) {
            String className = tracePoint.getClassname().substring(tracePoint.getClassname().lastIndexOf('/') + 1);
            sampleObject[i] = new String[]{className, String.valueOf(tracePoint.getLinenum()), String.valueOf(tracePoint.getThreadId())};
            i++;
        }

        defaultTableModelTraces.setDataVector(sampleObject, headers);
    }

    private void loadBug(int rowNum) throws IOException {

        XBreakpoint[] breakpoints = XDebuggerManager.getInstance(project).getBreakpointManager().getAllBreakpoints();

        List<DebugPoint> breakpointList = new ArrayList<>();

        for (XBreakpoint breakpoint : breakpoints) {
            if (breakpoint.getType() instanceof XLineBreakpointType) {
                DebugPoint debugPoint = new DebugPoint();
                debugPoint.setFile(breakpoint.getSourcePosition().getFile().toString().split("/src/main/java/")[1].split(".java")[0]);
                debugPoint.setLineNumber(breakpoint.getSourcePosition().getLine());
                breakpointList.add(debugPoint);
            }
        }

//        if (breakpointList.size() == 0) {
//            Messages.showInfoMessage("Set atleast 1 breakpoint", "Break Points");
//            return;
//        }

        TracePoint selectedTrace = bugList.get(rowNum);
        FilteredDataEventsRequest filteredDataEventsRequest = new FilteredDataEventsRequest();
        filteredDataEventsRequest.setSessionId(selectedTrace.getExecutionSessionId());
        filteredDataEventsRequest.setThreadId(selectedTrace.getThreadId());
        filteredDataEventsRequest.setValueId(Collections.singletonList(selectedTrace.getValue()));
        filteredDataEventsRequest.setPageSize(200);
        filteredDataEventsRequest.setPageNumber(0);
        filteredDataEventsRequest.setDebugPoints(breakpointList);
        filteredDataEventsRequest.setSortOrder("DESC");

        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        project.getService(ProjectService.class).filterDataEvents(
                projectId, filteredDataEventsRequest,
                new FilteredDataEventsCallback() {
                    @Override
                    public void error(ExceptionResponse errrorResponse) {
                        System.out.print(errrorResponse.getMessage());
                    }

                    @Override
                    public void success(List<DataEvent> dataList) {

                        String content = JSONArray.toJSONString(dataList);
                        String path = project.getBasePath() + "/variablevalues.json";
                        File file = new File(path);
                        FileWriter fileWriter = null;
                        try {
                            fileWriter = new FileWriter(file);
                            fileWriter.write(content);
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        ApplicationManager.getApplication().invokeLater(() -> {
                            try {
                                project.getService(ProjectService.class).startTracer(selectedTrace, "DESC", "traces");
                            } catch (Exception e) {
                                e.printStackTrace();
                                ExceptionResponse exceptionResponse = new ExceptionResponse();
                                exceptionResponse.setMessage(e.getMessage());
                                error(exceptionResponse);
                                return;
                            }
                        });
                        varsvaluePane.setVisible(true);
                        updateProgressbar("varsvalues", 100);


                    }
                }
        );
        hideTable("varsvalues");
    }

    public void setVariables(Collection<DataEvent> dataListTemp) {
        JTableHeader header = this.varsvalueTable.getTableHeader();
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
        defaultTableModelvarsValues.setDataVector(sampleObject, headers);
        this.varsvalueTable.setModel(defaultTableModelvarsValues);
        this.varsvalueTable.setDefaultRenderer(Object.class, centerRenderer);
        this.varsvalueTable.setAutoCreateRowSorter(true);

    }

    private void clearAll() {
        PropertiesComponent.getInstance().setValue(Constants.TOKEN, "");
    }

    private void showCredentialsWindow() {

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Content exceptionContent = toolWindow.getContentManager().findContent("Exceptions");
                Content traceContent = toolWindow.getContentManager().findContent("Traces");
                if (exceptionContent != null) {
                    toolWindow.getContentManager().removeContent(exceptionContent, true);
                }
                if (traceContent != null) {
                    toolWindow.getContentManager().removeContent(traceContent, true);
                }
                ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
                Credentials credentials = new Credentials(project, toolWindow);
                Content credentialContent = contentFactory.createContent(credentials.getContent(), "Credentials", false);
                toolWindow.getContentManager().addContent(credentialContent);
            }
        });

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
