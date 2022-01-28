package ui;

import callbacks.FilteredDataEventsCallback;
import callbacks.GetProjectSessionErrorsCallback;
import callbacks.GetProjectSessionsCallback;
import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
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
import net.minidev.json.JSONObject;
import network.pojo.ExceptionResponse;
import network.pojo.ExecutionSession;
import network.pojo.FilteredDataEventsRequest;
import okhttp3.*;
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
import java.io.IOException;
import java.util.*;
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
    private JTable bugTypes;
    private JPanel customBugPanel;
    private JButton custombugButton;
    private JLabel someLable;
    private List<Bugs> bugList;

    public HorBugTable(Project project, ToolWindow toolWindow) {
        this.project = project;
        basepath = this.project.getBasePath();

        this.projectService = project.getService(ProjectService.class);

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

        bugTypeTableModel = new DefaultTableModel() {
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
        custombugButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //showDialog();
            }
        });

        initBugTypeTable();
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

    public void hideAll() {
        scrollpanel.setVisible(false);
    }

    private void getErrors(int pageNum, String sessionId) throws Exception {

        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        List<String> classList = Arrays.asList(Constants.NPE);
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


    private void parseTableItems(List<Bugs> bugsCollection) {
        this.bugList = bugsCollection;
        Object[][] sampleObject = new Object[bugList.size()][];
        Object[] headers = {"Type of Crash", "ClassName", "LineNum", "ThreadId"};

        int i = 0;
        for (Bugs bugs : bugList) {
            String className = bugs.getClassname().substring(bugs.getClassname().lastIndexOf('/') + 1);
            sampleObject[i] = new String[]{"NullPointerException", className, String.valueOf(bugs.getLinenum()), String.valueOf(bugs.getThreadId())};
            i++;
        }

        defaultTableModel.setDataVector(sampleObject, headers);
    }

    private void loadBug(int rowNum) throws IOException {
        Bugs bugs = bugList.get(rowNum);
        FilteredDataEventsRequest filteredDataEventsRequest = new FilteredDataEventsRequest();
        filteredDataEventsRequest.setSessionId(bugs.getExecutionSessionId());
        filteredDataEventsRequest.setThreadId(bugs.getThreadId());
        filteredDataEventsRequest.setValueId(Collections.singletonList(bugs.getValue()));
        filteredDataEventsRequest.setPageSize(200);
        filteredDataEventsRequest.setPageNumber(0);
        filteredDataEventsRequest.setDebugPoints(Collections.emptyList());
        filteredDataEventsRequest.setSortOrder("DESC");

        logger.info(String.format("Fetch for sessions [%s] on thread [%s]", executionSessionId, bugs.getThreadId()));


        String projectId = PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID);
        project.getService(ProjectService.class).filterDataEvents(
                projectId, filteredDataEventsRequest,
                new FilteredDataEventsCallback() {
                    @Override
                    public void error(ExceptionResponse errrorResponse) {

                    }

                    @Override
                    public void success() {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                highlightCrash(bugs.getFilename(), (int) bugs.getLinenum());
                            }
                        });
                    }
                }
        );

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

        project.getService(ProjectService.class).setCurrentLineNumber(linenumber - 1);

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

    private void getLastSessions() {
        project.getService(ProjectService.class).getProjectSessions(
                PropertiesComponent.getInstance().getValue(Constants.PROJECT_ID),
                new GetProjectSessionsCallback() {
                    @Override
                    public void error() {

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

    private void initBugTypeTable() {
        JTableHeader header = this.bugTypes.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        Object[] headers = {"Error Name", "Error Type"};
        Object[][] errorTypes = {
                {"NullPoiterException", "java.lang.NullPointerException"},
                {"Out of Index", "java.lang.ArrayIndexOutOfBoundsException"}
        };
        bugTypeTableModel.setDataVector(errorTypes, headers);
        bugTypes.setModel(bugTypeTableModel);
    }

}
