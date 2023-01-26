package com.insidious.plugin.ui;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.pojo.SearchRecord;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.VectorUtils;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

public class SearchByValueWindow extends SearchByWindowCommon {
    private static final Logger logger = LoggerUtil.getInstance(SearchByValueWindow.class);
    private final Project project;
    private final InsidiousService insidiousService;
    private final List<TracePoint> tracePointList = new LinkedList<>();
    DefaultTableCellRenderer centerRenderer;
    Vector<Object> headers = VectorUtils.convertToVector(new Object[]{"Class", "# Line", "# Thread", "Time"});
    private JPanel mainPanel;
    private JPanel searchControl;
    private JTable bugsTable;
    private JTextField searchValueInput;
    private JButton searchButton;
    private JButton fetchBackwardButton;
    private JScrollPane scrollpanel;
    private JTable searchQueryHistoryTable;
    private JPanel searchtablepanel;
    private JScrollPane searchtablescrollpane;
    private JPanel resultsPanel;
    private DefaultTableModel searchHistoryTableModel;
    private ReentrantLock lock;

    public SearchByValueWindow(Project project, InsidiousService insidiousService) {
        super(new Vector<>(List.of("Class", "# Line", "# Thread", "Time")), project);
        this.project = project;
        this.insidiousService = insidiousService;
        init();
        addComponentEventListeners();
        updateQueryList();

    }

    private void addComponentEventListeners() {
        fetchBackwardButton.addActionListener(actionEvent -> loadTracePoint(bugsTable.getSelectedRow()));

        searchButton.addActionListener(actionEvent -> doSearch());

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


    }

    private void doSearch() {

        if (searchValueInput.getText().equals("")) {
            Notifications.Bus.notify(
                    InsidiousNotification.balloonNotificationGroup
                            .createNotification("Cannot search with empty string", NotificationType.ERROR),
                    project);
            return;
        }
        this.clearResultsTable();
        try {
            updateQueryList();
            insidiousService.doSearch(SearchQuery.ByValue(searchValueInput.getText()));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to refresh sessions", e);
        }

    }

    private void init() {
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
        this.bugsTable.setModel(this.searchResultsTableModel);
        this.bugsTable.setDefaultRenderer(Object.class, centerRenderer);
        this.bugsTable.setAutoCreateRowSorter(true);

        this.searchQueryHistoryTable.setCellEditor(this.searchQueryHistoryTable.getDefaultEditor(String.class));
        this.searchQueryHistoryTable.setModel(searchHistoryTableModel);
        this.searchQueryHistoryTable.setDefaultRenderer(Object.class, centerRenderer);
        this.searchQueryHistoryTable.setAutoCreateRowSorter(true);

        lock = new ReentrantLock();

        this.searchQueryHistoryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public synchronized void valueChanged(ListSelectionEvent e) {
//                if (lock.isLocked()) {
//                    return;
//                }
//                if (!lock.tryLock()) {
//                    return;
//                }
//                List<SearchRecord> searchResults = insidiousService.getConfiguration().getSearchRecords();
//                try {
//                    int firstItemSelected = e.getFirstIndex();
//                    if (firstItemSelected < 0 || firstItemSelected >= searchResults.size()) {
//                        return;
//                    }
//                    SearchRecord selectedSearchResult = searchResults.get(firstItemSelected);
//                    searchValueInput.setText(selectedSearchResult.getQuery());
//                    doSearch();
//                } catch (Exception ex) {
//                    logger.error("failed to do search", ex);
//                } finally {
//                    lock.unlock();
//                }
            }
        });


    }

    public JPanel getContent() {
        return mainPanel;
    }

    public void updateQueryList() {

//        JTableHeader header = this.searchQueryHistoryTable.getTableHeader();
//        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
//        List<SearchRecord> searchQueries = insidiousService.getConfiguration().getSearchRecords();
//        Object[][] searchResultRows = new Object[searchQueries.size()][];
//        Object[] headers = {"Search query", "Time", "# Matched"};
//
//        int i = 0;
//        for (SearchRecord searchRecord : searchQueries) {
//
//            searchResultRows[i] = new String[]{
//                    searchRecord.getQuery(),
//                    searchRecord.getLastQueryDate().toString(),
//                    String.valueOf(searchRecord.getLastSearchResultCount())
//            };
//            i++;
//        }
//
//        searchHistoryTableModel.setDataVector(searchResultRows, headers);
//        this.searchQueryHistoryTable.setModel(searchHistoryTableModel);
//        this.searchQueryHistoryTable.setDefaultRenderer(Object.class, centerRenderer);

    }
}
