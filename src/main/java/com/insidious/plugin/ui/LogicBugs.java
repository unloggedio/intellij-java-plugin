package com.insidious.plugin.ui;

import com.insidious.plugin.util.LoggerUtil;
import org.slf4j.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.network.pojo.DebugPoint;
import com.insidious.plugin.pojo.DataEvent;
import com.insidious.plugin.pojo.TracePoint;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LogicBugs {
    private static final Logger logger = LoggerUtil.getInstance(LogicBugs.class);
    private final Project project;
    private final InsidiousService insidiousService;
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
    private JButton fetchBackwardButton;
    private JProgressBar progressBarfield;
    private JLabel errorLabel;
    private JPanel varpanel;
    private JProgressBar variableProgressbar;
    private JLabel varvalueErrorLabel;
    private JScrollPane scrollpanel;
    private JButton fetchForwardButton;
    private List<TracePoint> bugList;
    private DefaultTableModel defaultTableModelTraces, defaultTableModelvarsValues;

    public LogicBugs(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (traceIdfield.getText().equals("")) {
                    Messages.showInfoMessage("Empty String reference is not allowed", "Empty String");
                    return;
                }
                insidiousService.getTraces(0, traceIdfield.getText());
            }
        });


        fetchBackwardButton.addActionListener(actionEvent -> loadBug(bugsTable.getSelectedRow(), DirectionType.BACKWARDS));
//        fetchForwardButton.addActionListener(actionEvent -> loadBug(bugsTable.getSelectedRow(), DirectionType.FORWARDS));

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

    private void loadBug(int rowNum, DirectionType directionType) {

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

        TracePoint selectedTrace = bugList.get(rowNum);
        try {
            logger.info(String.format("Fetch by trace string for session [%s] on thread [%s]", selectedTrace.getExecutionSessionId(), selectedTrace.getThreadId()));
            insidiousService.setTracePoint(selectedTrace, directionType);
        } catch (Exception e) {
            Messages.showErrorDialog(project, e.getMessage(), "Failed to fetch session events");
        }

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

    public void setTracePoints(List<TracePoint> tracePointCollection) {
        scrollpanel.setVisible(true);
        parseTableItems(tracePointCollection);
    }
}
