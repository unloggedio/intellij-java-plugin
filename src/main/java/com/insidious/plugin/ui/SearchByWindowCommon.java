package com.insidious.plugin.ui;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.VectorUtils;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import javax.swing.table.DefaultTableModel;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class SearchByWindowCommon {
    protected final List<TracePoint> tracePointList = new LinkedList<>();
    protected final DefaultTableModel searchResultsTableModel;
    private final Vector<Object> tableColumnNames;
    private final Project project;
    private final InsidiousService insidiousService;
    private Logger logger = LoggerUtil.getInstance(SearchByWindowCommon.class);

    public SearchByWindowCommon(Vector<Object> tableColumnNames, Project project) {
        this.tableColumnNames = tableColumnNames;
        this.project = project;
        this.insidiousService = ServiceManager.getService(InsidiousService.class);
        this.searchResultsTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public void addTracePoints(List<TracePoint> tracePointCollection) {
        this.tracePointList.addAll(tracePointCollection);
        updateResultsTable();
    }

    protected void clearResultsTable() {
        this.tracePointList.clear();
        updateResultsTable();
    }


    protected void loadTracePoint(int rowNum) {
        logger.info("load trace point by index: " + rowNum);
        if (rowNum >= tracePointList.size() || rowNum < 0) {
            logger.info("selected by index out of size " + rowNum + " -> " + tracePointList.size());
            Notifications.Bus.notify(InsidiousNotification.balloonNotificationGroup
                    .createNotification("Please select a trace point to fetch",
                            NotificationType.ERROR), project);
            return;
        }
        TracePoint selectedTrace = tracePointList.get(rowNum);
        try {

            logger.info(String.format("Fetch by exception for session [%s] on thread [%s]",
                    selectedTrace.getExecutionSession()
                            .getSessionId(), selectedTrace.getThreadId()));

            insidiousService.loadTracePoint(selectedTrace);

        } catch (Exception e) {

            logger.error("failed to fetch session events", e);
            if (InsidiousNotification.balloonNotificationGroup != null) {
                Notifications.Bus.notify(InsidiousNotification.balloonNotificationGroup
                        .createNotification("Failed to fetch session events - " + e.getMessage(),
                                NotificationType.ERROR), project);
            }
        }
    }

    protected void updateResultsTable() {
        Vector<Vector<Object>> tableItemsVector = new Vector<>(tracePointList.size());

        for (TracePoint tracePoint : tracePointList) {
            tableItemsVector.add(VectorUtils.tracePointToRowVector(tracePoint));
        }
        searchResultsTableModel.setDataVector(tableItemsVector, tableColumnNames);
    }

}
