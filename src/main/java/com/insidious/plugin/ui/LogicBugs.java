package com.insidious.plugin.ui;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.SearchRecord;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class LogicBugs {
    private static final Logger logger = LoggerUtil.getInstance(LogicBugs.class);
    private final Project project;
    private final InsidiousService insidiousService;
    DefaultTableCellRenderer centerRenderer;
    private JPanel mainpanel;
    private JPanel searchPanel;
    private JTable bugsTable;
    private JTable varsvalueTable;
    private JTextField traceIdfield;
    private JLabel searchLabel;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton fetchBackwardButton;
    private JProgressBar progressBarfield;
    private JLabel errorLabel;
    private JProgressBar variableProgressbar;
    private JScrollPane scrollpanel;
    private JTable searchHistoryTable;
    private JPanel searchtablepanel;
    private JScrollPane searchtablescrollpane;
    private JButton fetchForwardButton;
    private List<TracePoint> bugList;
    private DefaultTableModel defaultTableModelTraces, defaultTableModelvarsValues, searchHistoryTableModel;
    private ReentrantLock lock;

    public LogicBugs(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;

        refreshButton.addActionListener(e -> {
            doSearch();
        });
        searchButton.addActionListener(actionEvent -> {
            doSearch();
        });


        fetchBackwardButton.addActionListener(actionEvent -> loadBug(bugsTable.getSelectedRow()));
//        fetchForwardButton.addActionListener(actionEvent -> loadBug(bugsTable.getSelectedRow(), DirectionType.FORWARDS));

        initTables();
        updateSearchResultsList();
        ApplicationManager.getApplication().invokeLater(this::refreshSearchHistory);
        //variableProgressbar.setVisible(false);
    }

    private void refreshSearchHistory() {
        List<SearchRecord> items = insidiousService.getConfiguration().getSearchRecords();
    }

    private void doSearch() {
        if (traceIdfield.getText().equals("")) {
            Notifications.Bus.notify(
                    InsidiousNotification.balloonNotificationGroup
                            .createNotification("Cannot search with empty string", NotificationType.ERROR),
                    project);
            return;
        }

        setTracePoints(List.of());
        try {
            insidiousService.refreshSession();
            insidiousService.getTraces(0, traceIdfield.getText());
        } catch (APICallException | IOException e) {
            logger.error("Failed to refresh sessions", e);
        }
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

        searchHistoryTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }


        };


        JTableHeader header = this.bugsTable.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);


        this.bugsTable.setCellEditor(this.bugsTable.getDefaultEditor(Boolean.class));
        this.bugsTable.setModel(defaultTableModelTraces);
        this.bugsTable.setDefaultRenderer(Object.class, centerRenderer);
        this.bugsTable.setAutoCreateRowSorter(true);


        this.searchHistoryTable.setCellEditor(this.searchHistoryTable.getDefaultEditor(String.class));
        this.searchHistoryTable.setModel(searchHistoryTableModel);
        this.searchHistoryTable.setDefaultRenderer(Object.class, centerRenderer);
        this.searchHistoryTable.setAutoCreateRowSorter(true);

        lock = new ReentrantLock();

        this.searchHistoryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public synchronized void valueChanged(ListSelectionEvent e) {
                if (lock.isLocked()) {
                    return;
                }
                if (!lock.tryLock()) {
                    return;
                }
                List<SearchRecord> searchResults = insidiousService.getConfiguration().getSearchRecords();
                try {
                    int firstItemSelected = e.getFirstIndex();
                    if (firstItemSelected < 0 || firstItemSelected >= searchResults.size()) {
                        return;
                    }
                    SearchRecord selectedSearchResult = searchResults.get(firstItemSelected);
                    traceIdfield.setText(selectedSearchResult.getQuery());
                    doSearch();
                } catch (Exception ex) {
                    logger.error("failed to do search", ex);
                } finally {
                    lock.unlock();
                }
            }
        });


    }

    public JPanel getContent() {
        return mainpanel;
    }

    private void parseTableItems(List<TracePoint> tracePointCollection) {
        this.bugList = tracePointCollection;
        Object[][] sampleObject = new Object[bugList.size()][];
        Object[] headers = {"ClassName", "LineNum", "ThreadId", "Timestamp"};

        int i = 0;
        for (TracePoint tracePoint : bugList) {
            String className = tracePoint.getClassname().substring(
                    tracePoint.getClassname().lastIndexOf('/') + 1);

            sampleObject[i] = new String[]{className,
                    String.valueOf(tracePoint.getLinenum()),
                    String.valueOf(tracePoint.getThreadId()),
                    new Date(tracePoint.getRecordedAt()).toString()};

            i++;
        }

        defaultTableModelTraces.setDataVector(sampleObject, headers);
    }

    private void loadBug(int rowNum) {
        logger.info("load trace point" + rowNum);
//        XBreakpoint[] breakpoints = XDebuggerManager.getInstance(project).getBreakpointManager().getAllBreakpoints();
//
//        List<DebugPoint> breakpointList = new ArrayList<>();
//
//        try {
//            for (XBreakpoint breakpoint : breakpoints) {
//                if (breakpoint.getType() instanceof XLineBreakpointType) {
//                    DebugPoint debugPoint = new DebugPoint();
//                    XSourcePosition sourcePosition = breakpoint.getSourcePosition();
//                    logger.info("note break point position in file " +
//                            sourcePosition.getFile() + "] at line [" + sourcePosition.getLine() + "]");
//                    try {
//                        debugPoint.setFile(sourcePosition.getFile().toString().split("/src/main/java/")[1].split(".java")[0]);
//                        debugPoint.setLineNumber(sourcePosition.getLine());
//                        breakpointList.add(debugPoint);
//                    } catch (Exception e) {
//                        logger.error("debug break point not added in query", e);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error("failed to load break points", e);
//        }

        logger.info("load trace by row number: " + rowNum);
        TracePoint selectedTrace = bugList.get(rowNum);
        try {
            logger.info("Fetch by trace string [" + selectedTrace.getDataId() + "] for session ["
                    + selectedTrace.getExecutionSessionId() + "] on thread" + selectedTrace.getThreadId());
            insidiousService.setTracePoint(selectedTrace);
        } catch (Exception e) {
            logger.error("failed to load trace point", e);
            Messages.showErrorDialog(project, e.getMessage(), "Failed to fetch session events");
        }

    }

    public void setTracePoints(List<TracePoint> tracePointCollection) {
        scrollpanel.setVisible(true);
        parseTableItems(tracePointCollection);
    }

    public void updateSearchResultsList() {
//        this.searchResults = insidiousService.getConfiguration().getSearchRecords().stream().sorted();

        JTableHeader header = this.searchHistoryTable.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        List<SearchRecord> searchResults = insidiousService.getConfiguration().getSearchRecords();
        Object[][] searchResultRows = new Object[searchResults.size()][];
        Object[] headers = {"Search String", "TimeStamp", "Occurances"};

        int i = 0;
        for (SearchRecord searchRecord : searchResults) {

            searchResultRows[i] = new String[]{
                    searchRecord.getQuery(),
                    searchRecord.getLastQueryDate().toString(),
                    String.valueOf(searchRecord.getLastSearchResultCount())
            };
            i++;
        }

        searchHistoryTableModel.setDataVector(searchResultRows, headers);

        this.searchHistoryTable.setModel(searchHistoryTableModel);
        this.searchHistoryTable.setDefaultRenderer(Object.class, centerRenderer);
        this.searchHistoryTable.setAutoCreateRowSorter(true);


    }

    public void bringToFocus(ToolWindow toolWindow) {

        toolWindow.getContentManager().setSelectedContent(
                toolWindow.getContentManager().getContents()[0],
                true);

    }
}
