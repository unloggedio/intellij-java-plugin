package com.insidious.plugin.ui.eventviewer;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.ObjectInfo;
import com.insidious.common.weaver.StringInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.exception.SessionNotSelectedException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.ui.InsidiousUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;

public class EventLogWindow {
    private final int paginationSize = 500;
    private final Project project;
    private final VideobugClientInterface clientInterface;
    private ReplayData replayData;
    private DefaultTableModel tableModel = null;
    private JPanel filterPanel;
    private JTable eventsTable;
    private JTextField queryTextField;
    private JButton searchButton;
    private JScrollPane eventsPanel;
    private JPanel containerPanel;
    private JSpinner bufferSize;
    private JButton firstPage;
    private JButton previousPage;
    private JButton nextPage;
    private JPanel infoPanelContainer;
    private JLabel infoLabel;
    private JPanel paginationPanel;
    private int currentPage = 0;
    private long currentObjectId;
    private ReplayData replayData1;
    private Logger logger = LoggerUtil.getInstance(EventLogWindow.class);

    public EventLogWindow(Project project, VideobugClientInterface clientInterface) {
        this.project = project;
        this.clientInterface = clientInterface;


        queryTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String newObjectId = queryTextField.getText();
                    if (newObjectId == null || newObjectId.length() < 1) {
                        return;
                    }
                    long objectId;
                    try {
                        objectId = Long.parseLong(newObjectId);
                    } catch (Exception e3) {
                        InsidiousNotification.notifyMessage("Invalid object id to search", NotificationType.ERROR);
                        return;
                    }
                    loadObject(objectId);
                }
            }
        });

        this.searchButton.addActionListener(event -> {
            String queryString = queryTextField.getText();

            if (!queryString.contains("=") && !queryString.contains(" ")) {
                loadObject(Long.parseLong(queryString));
                return;
            }

            String[] queryStringParts = queryString.trim().split(",");
            FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();

            filterRequest.setPageInfo(new PageInfo(0, paginationSize, PageInfo.Order.ASC));


            for (String queryStringPart : queryStringParts) {

                String[] queryStringPartTriplet = queryStringPart.trim().split(" ");

                String operator = queryStringPartTriplet[1];
                String key = queryStringPartTriplet[0];
                String value = queryStringPartTriplet[2];

                switch (key) {
                    case "probe":
                        filterRequest.setProbeId(Integer.parseInt(value));
                        break;
                    case "thread":
                        filterRequest.setThreadId(Long.valueOf(value));
                        break;
                    case "time":
                        filterRequest.setNanotime(Long.parseLong(value));

                        switch (operator) {
                            case "<":
                                filterRequest.setPageInfo(new PageInfo(0, paginationSize, PageInfo.Order.DESC));
                                break;
                            case ">":
                                filterRequest.setPageInfo(new PageInfo(0, paginationSize, PageInfo.Order.ASC));
                                break;
                        }

                        break;
                    case "value":
                        filterRequest.setValueId(List.of(Long.valueOf(value)));
                        break;
                    case "object":
                        filterRequest.setObjectId(Long.valueOf(value));
                        break;

                }

            }

            try {
                loadHistory(filterRequest);
            } catch (SessionNotSelectedException ex) {
                InsidiousNotification.notifyMessage("Failed to fetch events: " + ex.getMessage(),
                        NotificationType.ERROR);
            }
        });


        eventsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
//                int selectedColumn = eventsTable.getSelectedColumn();
//                String selectedColumnName = tableModel.getColumnName(selectedColumn);

                int selectedRowIndex = eventsTable.getSelectedRow();
//                Object clickedValue = tableModel.getValueAt(selectedRowIndex, selectedColumn);

                DataEventWithSessionId selectedEvent = replayData.getDataEvents().get(selectedRowIndex);
                DataInfo probeInfo = replayData.getProbeInfo(selectedEvent.getProbeId());

                InsidiousUtils.focusProbeLocationInEditor(probeInfo.getLine(),
                        replayData.getClassInfo(probeInfo.getClassId()).getClassName(), project);

            }

        });

        eventsTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && (e.getKeyCode() == 'c' || e.getKeyCode() == 'C')) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    toExcel(eventsTable, outputStream);

                    StringSelection selection = new StringSelection(
                            outputStream.toString()
                    );
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            selection, selection
                    );
                } else {
                    super.keyPressed(e);
                }
            }
        });

        this.firstPage.addActionListener(e -> {
            try {
                replayData = replayData.getPage(0);
            } catch (SessionNotSelectedException ex) {
                InsidiousNotification.notifyMessage("Failed to load events: " + ex.getMessage(),
                        NotificationType.ERROR);
                return;
            }
            updateTableData(replayData);
        });
        this.previousPage.addActionListener(e -> {
            try {
                replayData = replayData.getPreviousPage();
            } catch (SessionNotSelectedException ex) {
                InsidiousNotification.notifyMessage("Failed to load events: " + ex.getMessage(),
                        NotificationType.ERROR);
                return;
            }
            updateTableData(replayData);
        });
        this.nextPage.addActionListener(e -> {
            try {
                replayData = replayData.getNextPage();
            } catch (SessionNotSelectedException ex) {
                InsidiousNotification.notifyMessage(
                        "Failed to load events: " + ex.getMessage(), NotificationType.ERROR);
                return;
            }
            updateTableData(replayData);
        });
    }

    public void loadObject(long objectId) {
        currentPage = 0;
        currentObjectId = objectId;

        queryTextField.setText(String.valueOf(objectId));
        try {
            ProgressManager.getInstance().run(new Task.WithResult<ReplayData, Exception>(
                    project, "Unlogged", true
            ) {
                @Override
                protected ReplayData compute(ProgressIndicator indicator) throws Exception {
                    try {
                        return loadHistory(objectId, currentPage);
                    } catch (SessionNotSelectedException e) {
                        InsidiousNotification.notifyMessage("Failed to fetch events: " + e.getMessage(),
                                NotificationType.ERROR);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            InsidiousNotification.notifyMessage("failed to search: " + e.getMessage(),
                    NotificationType.ERROR);
        }

    }

    private ReplayData loadHistory(long objectId, int pageNumber) throws SessionNotSelectedException {
        infoLabel.setText("Loading page: " + (pageNumber + 1));
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
        filterRequest.setObjectId(objectId);
        PageInfo pageInfo = new PageInfo(pageNumber, paginationSize, PageInfo.Order.ASC);

        pageInfo.setBufferSize(Integer.valueOf(String.valueOf(bufferSize.getValue())));

        filterRequest.setPageInfo(pageInfo);
        ReplayData replayData = null;
//        replayData = service.getClient().fetchObjectHistoryByObjectId(filterRequest);
        updateTableData(replayData);
        infoLabel.setText("[Page " + (pageNumber + 1) + "] [" + replayData.getDataEvents().size() + " events]");
        return replayData;
    }

    private ReplayData loadHistory(FilteredDataEventsRequest filterRequest) throws SessionNotSelectedException {

        PageInfo pageInfo = filterRequest.getPageInfo();

        pageInfo.setBufferSize(Integer.parseInt(String.valueOf(bufferSize.getValue())));

        filterRequest.setPageInfo(pageInfo);
        ReplayData replayData1 = clientInterface.fetchObjectHistoryByObjectId(filterRequest);
        updateTableData(replayData1);
        infoLabel.setText(" [Page " +
                (filterRequest.getPageInfo().getNumber() + 1) +
                "] [" + replayData1.getDataEvents().size() + " events]");
        return replayData1;
    }

    private void updateTableData(ReplayData replayData1) {
        this.replayData = replayData1;
        Vector<Object> columnVector = new Vector<>(List.of(
                "Event", "Nano", "#EventId", "#Line", "Value", "Attributes", "Value type", "String", "Serialized"
        ));
        Vector<Vector<Object>> dataVector = new Vector<>(replayData.getDataEvents().size());


        int rowIndex = 0;
        if (replayData.getDataEvents().size() == 0) {
            InsidiousNotification.notifyMessage("No events found", NotificationType.INFORMATION);
            return;
        }
        long previousRecordedAt = replayData.getDataEvents().get(0).getRecordedAt();
        for (DataEventWithSessionId dataEvent : replayData.getDataEvents()) {
            Vector<Object> rowVector = new Vector<>(6);

            DataInfo probeInfo = replayData.getProbeInfo(dataEvent.getProbeId());
            ObjectInfo objectInfo = replayData.getObjectInfoMap().get(dataEvent.getValue());
            String eventType = probeInfo.getEventType().toString();


//            rowVector.add(dataEvent.getRecordedAt());
            rowVector.add(eventType);
            rowVector.add(dataEvent.getRecordedAt() + " (" + (dataEvent.getRecordedAt() - previousRecordedAt) + ")");
            previousRecordedAt = dataEvent.getRecordedAt();
            rowVector.add(dataEvent.getEventId());
            rowVector.add(probeInfo.getLine());
            rowVector.add(Long.valueOf(dataEvent.getValue()).toString());
            rowVector.add(probeInfo.getAttributes());

            if (objectInfo != null) {
                TypeInfo typeInfo = replayData.getTypeInfoMap().get(objectInfo.getTypeId());
                rowVector.add(typeInfo.getTypeNameFromClass());
            } else {
                rowVector.add("");
            }
            if (replayData.getStringInfoMap().containsKey(dataEvent.getValue())) {
                StringInfo stringValue = replayData.getStringInfoMap().get(dataEvent.getValue());
                rowVector.add(stringValue.getContent());
            } else {
                rowVector.add("");
            }
            rowVector.add(new String(dataEvent.getSerializedValue()));


            dataVector.add(rowVector);

        }


        if (tableModel == null) {
            tableModel = new DefaultTableModel(dataVector, columnVector) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return true;
                }
            };

            eventsTable.setModel(tableModel);

            try {
                eventsTable.getColumn("Event").setPreferredWidth(130);
                eventsTable.getColumn("#Time").setPreferredWidth(25);
                eventsTable.getColumn("#Line").setPreferredWidth(5);
                eventsTable.getColumn("Value").setPreferredWidth(40);
                eventsTable.getColumn("Attributes").setPreferredWidth(200);
                eventsTable.getColumn("Value type").setPreferredWidth(100);
            } catch (Exception e) {
                logger.info("failed to set column width: " + e.getMessage());
            }

        } else {
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.removeRow(0);
            }

            for (Vector<Object> objects : dataVector) {
                tableModel.addRow(objects);
            }

        }


    }

    public JPanel getContent() {
        return containerPanel;
    }


    public void toExcel(JTable table, OutputStream excel) {
        try {
            TableModel model = table.getModel();

            for (int i = 0; i < model.getColumnCount(); i++) {
                excel.write((model.getColumnName(i).toString() + "\t").getBytes());
            }

            excel.write("\n".getBytes());

            for (int i = 0; i < model.getRowCount(); i++) {
                for (int j = 0; j < model.getColumnCount(); j++) {
                    Object valueAt = model.getValueAt(i, j);
                    String valueString = "";
                    if (valueAt != null) {
                        valueString = valueAt.toString();
                    }
                    excel.write((valueString + "\t").getBytes());
                }
                excel.write("\n".getBytes());
            }

        } catch (IOException e) {
            e.printStackTrace();
            // should never happen
        }
    }

}
