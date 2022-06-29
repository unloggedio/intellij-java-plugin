package com.insidious.plugin.ui;

import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.SearchRecord;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.VectorUtils;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

public class SearchByValueWindow {
    private static final Logger logger = LoggerUtil.getInstance(SearchByValueWindow.class);
    private final Project project;
    private final InsidiousService insidiousService;
    DefaultTableCellRenderer centerRenderer;
    private JPanel mainpanel;
    private JPanel searchControl;
    private JTable bugsTable;
    private JTable varsvalueTable;
    private JTextField searchValueInput;
    private JButton searchButton;
    private JButton fetchBackwardButton;
    private JProgressBar variableProgressbar;
    private JScrollPane scrollpanel;
    private JTable searchHistoryTable;
    private JPanel searchtablepanel;
    private JScrollPane searchtablescrollpane;
    private JPanel resultsPanel;
    private JButton fetchForwardButton;
    private List<TracePoint> tracePointList = new LinkedList<>();
    private DefaultTableModel defaultTableModelTraces, defaultTableModelvarsValues, searchHistoryTableModel;
    private ReentrantLock lock;
    Vector<Object> headers = VectorUtils.convertToVector(new Object[]{"ClassName", "LineNum", "ThreadId", "Timestamp"});

    public SearchByValueWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;

        searchButton.addActionListener(actionEvent -> {
            doSearch();
        });

        searchValueInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    doSearch();
                    return;
                }
                super.keyPressed(e);
            }
        });


        fetchBackwardButton.addActionListener(actionEvent -> loadBug(bugsTable.getSelectedRow()));
//        fetchForwardButton.addActionListener(actionEvent -> loadBug(bugsTable.getSelectedRow(), DirectionType.FORWARDS));

        initTables();
        updateSearchResultsList();
        ApplicationManager.getApplication().invokeLater(this::refreshSearchHistory);
        //variableProgressbar.setVisible(false);
    }

    private void loadBug(int rowNum) {
        logger.info("load trace point" + rowNum);
        logger.info("load trace by row number: " + rowNum);
        if (rowNum == -1 || rowNum > tracePointList.size()) {
            InsidiousNotification.notifyMessage("Please select a trace point to replay execution", NotificationType.ERROR);
            return;
        }
        TracePoint selectedTrace = tracePointList.get(rowNum);
        try {
            logger.info("Fetch by trace string [" + selectedTrace.getDataId() + "] for session ["
                    + selectedTrace.getExecutionSession().getSessionId() + "] on thread" + selectedTrace.getThreadId());
            insidiousService.setTracePoint(selectedTrace);
        } catch (Exception e) {
            logger.error("failed to load trace point", e);
            Messages.showErrorDialog(project, "Failed to fetch session events: " + e.getMessage(), "Unlogged");
        }

    }

    private void refreshSearchHistory() {
        List<SearchRecord> items = insidiousService.getConfiguration().getSearchRecords();
    }

    private void doSearch() {

        if (searchValueInput.getText().equals("")) {
            Notifications.Bus.notify(
                    InsidiousNotification.balloonNotificationGroup
                            .createNotification("Cannot search with empty string", NotificationType.ERROR),
                    project);
            return;
        }
        this.tracePointList.clear();
        this.parseTableItems();
        try {
            insidiousService.refreshSession();
            insidiousService.getTracesByValue(0, searchValueInput.getText());
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

        defaultTableModelTraces.setColumnIdentifiers(headers);

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
                    searchValueInput.setText(selectedSearchResult.getQuery());
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

    private void parseTableItems() {
        Object[][] sampleObject = new Object[tracePointList.size()][];

        int i = 0;
        for (TracePoint tracePoint : this.tracePointList) {
            String className = tracePoint.getClassname().substring(
                    tracePoint.getClassname().lastIndexOf('/') + 1);

            sampleObject[i] = new String[]{className,
                    String.valueOf(tracePoint.getLinenum()),
                    String.valueOf(tracePoint.getThreadId()),
                    new Date(tracePoint.getRecordedAt()).toString()};

            i++;
        }

        Vector<Vector> dataVector = new Vector<>();
        dataVector.addAll(VectorUtils.convertToVector(sampleObject));
        defaultTableModelTraces.setDataVector(dataVector, headers);
    }

    public void addTracePoints(List<TracePoint> tracePointCollection) {
        scrollpanel.setVisible(true);
        this.tracePointList.addAll(tracePointCollection);
        parseTableItems();
    }

    public void updateSearchResultsList() {
//        this.searchResults = insidiousService.getConfiguration().getSearchRecords().stream().sorted();

        JTableHeader header = this.searchHistoryTable.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        List<SearchRecord> searchResults = insidiousService.getConfiguration().getSearchRecords();
        Object[][] searchResultRows = new Object[searchResults.size()][];
        Object[] headers = {"Search String", "TimeStamp", "# Matched"};

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
