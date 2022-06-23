package com.insidious.plugin.ui;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class HorBugTable {
    private static final Logger logger = LoggerUtil.getInstance(HorBugTable.class);
    private final InsidiousService insidiousService;
    DefaultTableModel defaultTableModel, varsDefaultTableModel, bugTypeTableModel, searchHistoryTableModel;
    Project project;
    String basepath;
    DefaultTableCellRenderer centerRenderer;
    private Map<String, Boolean> exceptionMap = new HashMap<>();
    private JPanel panel1;
    private JTable bugs;
    private JPanel panel11;
    private JPanel panel12;
    private JScrollPane scrollpanel;
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
    private JTextArea commandTextArea;
    private JButton applybutton;
    private JTextField traceIdfield;
    private JLabel someLable;
    private List<TracePoint> bugList;

    public HorBugTable(Project project, InsidiousService insidiousService) {
        this.project = project;
        basepath = this.project.getBasePath();

        this.insidiousService = insidiousService;
        fetchSessionButton.addActionListener(actionEvent -> loadBug(bugs.getSelectedRow()));

        varsDefaultTableModel = new DefaultTableModel() {
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
                    setTracePoints(List.of());
                    scrollpanel.setVisible(true);
                    List<String> exceptionClassnameList = exceptionMap.entrySet()
                            .stream()
                            .filter(Map.Entry::getValue)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());
                    insidiousService.refreshSession();


                    insidiousService.getTracesByType(exceptionClassnameList, 0);
                } catch (Exception e) {
                    logger.error("failed to load sessions for module", e);
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
        setTableValues();
    }

    public void setCommandText(String text) {
        commandTextArea.setText(text);
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

        JTableHeader header = this.bugs.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        this.bugs.setCellEditor(this.bugs.getDefaultEditor(Boolean.class));


        centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.bugs.setModel(defaultTableModel);
        this.bugs.setDefaultRenderer(Object.class, centerRenderer);
        this.bugs.setAutoCreateRowSorter(true);

    }


    private void setTableData(List<TracePoint> tracePointCollection) {
        this.bugList = tracePointCollection;
        Object[][] sampleObject = new Object[bugList.size()][];
        Object[] headers = {"Type of Crash", "ClassName", "LineNum", "ThreadId", "Timestamp"};

        int i = 0;
        for (TracePoint tracePoint : bugList) {
            sampleObject[i] = tracePointToStringObject(tracePoint);
            i++;
        }

        defaultTableModel.setDataVector(sampleObject, headers);
    }

    private String[] tracePointToStringObject(TracePoint tracePoint) {
        String className = tracePoint.getClassname().substring(
                tracePoint.getClassname().lastIndexOf('/') + 1);
        return new String[]{
                tracePoint.getExceptionClass().substring(
                        tracePoint.getExceptionClass().lastIndexOf('.') + 1),
                className,
                String.valueOf(tracePoint.getLinenum()),
                String.valueOf(tracePoint.getThreadId()),
                new Date(tracePoint.getRecordedAt()).toString()
        };
    }

    private Vector<String> tracePointToStringVector(TracePoint tracePoint) {
        String className = tracePoint.getClassname().substring(
                tracePoint.getClassname().lastIndexOf('/') + 1);
        return new Vector<>(
                Arrays.asList(
                        tracePoint.getExceptionClass().substring(
                                tracePoint.getExceptionClass().lastIndexOf('.') + 1),
                        className,
                        String.valueOf(tracePoint.getLinenum()),
                        String.valueOf(tracePoint.getThreadId()),
                        new Date(tracePoint.getRecordedAt()).toString())
        );
    }

    private void loadBug(int rowNum) {
        logger.info("load trace point by index: " + rowNum);
        if (rowNum >= bugList.size() || rowNum < 0) {
            logger.info("selected by index out of size "+rowNum+" -> " + bugList.size());
            Notifications.Bus.notify(InsidiousNotification.balloonNotificationGroup
                    .createNotification("Please select a trace point to fetch",
                            NotificationType.ERROR), project);
            return;
        }
        TracePoint selectedTrace = bugList.get(rowNum);
        try {
            logger.info(String.format("Fetch by exception for session [%s] on thread [%s]",
                    selectedTrace.getExecutionSessionId(), selectedTrace.getThreadId()));
            insidiousService.setTracePoint(selectedTrace);
        } catch (Exception e) {

            logger.error("failed to fetch session events", e);
            if (InsidiousNotification.balloonNotificationGroup != null) {
                Notifications.Bus.notify(InsidiousNotification.balloonNotificationGroup
                                .createNotification("Failed to fetch session events - " + e.getMessage(),
                                        NotificationType.ERROR), project);
            }
        }
    }

    private void initBugTypeTable() {
        JTableHeader header = this.bugTypes.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));


        exceptionMap = insidiousService.getDefaultExceptionClassList();
        updateBugsTable();


        bugTypes.getModel().addTableModelListener(tableModelEvent -> {
            if (tableModelEvent.getFirstRow() == -1) {
                return;
            }
            String exceptionClassName = (String) bugTypes.getModel().getValueAt(tableModelEvent.getFirstRow(), 0);
            Boolean isSelected = (Boolean) bugTypes.getModel().getValueAt(tableModelEvent.getFirstRow(), 1);
            exceptionMap.put(exceptionClassName, isSelected);
        });
    }

    private void updateBugsTable() {
        Object[] headers = {"Error Type", "Track it?"};

        Set<Map.Entry<String, Boolean>> entries = exceptionMap.entrySet();
        Object[][] errorTypes = new Object[entries.size()][];

        int i = 0;
        for (Map.Entry<String, Boolean> stringBooleanEntry : entries) {
            Object[] errorType = new Object[]{stringBooleanEntry.getKey(), stringBooleanEntry.getValue()};
            errorTypes[i] = errorType;
            i++;
        }


        bugTypeTableModel.setDataVector(errorTypes, headers);
        bugTypes.setModel(bugTypeTableModel);


    }

    private void showDialog() {
        String value = Messages.showInputDialog("Error Class Name (fully qualified)", "What's the Error Name?", null);

        if (value == null || value.equals("")) {
            return;
        }

        exceptionMap.put(value.trim(), true);
        insidiousService.setExceptionClassList(exceptionMap);
        updateBugsTable();
    }


    private void hideTable(String table) {
        if (table.equals("bugs")) {
            progressBarfield.setVisible(true);
            scrollpanel.setVisible(false);
        } else if (table.equals("varsvalues")) {
            varsvaluePane.setVisible(false);
        }
    }


    private void updateErrorLabel(String text) {
        errorLabel.setText(text);
    }

    public void setTracePoints(List<TracePoint> tracePointCollection) {
        scrollpanel.setVisible(true);
        setTableData(tracePointCollection);
    }

    public void addTracePoints(List<TracePoint> tracePointCollection) {
        scrollpanel.setVisible(true);
        tracePointCollection.forEach(e -> defaultTableModel.getDataVector().add(tracePointToStringVector((e))));
    }

    public void clearTracePoints() {
        scrollpanel.setVisible(true);
        setTracePoints(List.of());
    }
}
