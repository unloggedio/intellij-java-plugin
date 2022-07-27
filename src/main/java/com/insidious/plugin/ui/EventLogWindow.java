package com.insidious.plugin.ui;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.ObjectInfo;
import com.insidious.common.weaver.StringInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.InsidiousService;

import javax.swing.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

public class EventLogWindow {
    private final InsidiousService service;
    private final ReplayData replayData;
    private final DefaultTableModel tableModel;
    private JPanel filterPanel;
    private JTable eventsTable;
    private JTextField queryTextField;
    private JButton filterButton;
    private JScrollPane eventsPanel;
    private JPanel containerPanel;

    public EventLogWindow(InsidiousService service, ReplayData replayData) {
        this.service = service;
        this.replayData = replayData;

        TableColumnModel eventsTableColumnModel = new DefaultTableColumnModel();

        TableColumn nanoTimeColumn = new TableColumn();
        nanoTimeColumn.setHeaderValue("#Seq");
        eventsTableColumnModel.addColumn(nanoTimeColumn);

        TableColumn eventTypeColumn = new TableColumn();
        eventTypeColumn.setHeaderValue("EventType");
        eventsTableColumnModel.addColumn(eventTypeColumn);

        TableColumn probeIdColumn = new TableColumn();
        probeIdColumn.setHeaderValue("ProbeId");
        eventsTableColumnModel.addColumn(probeIdColumn);

        TableColumn valueColumn = new TableColumn();
        valueColumn.setHeaderValue("Value");
        eventsTableColumnModel.addColumn(valueColumn);

        eventsTable.setColumnModel(eventsTableColumnModel);

        Vector<Object> columnVector = new Vector<>(List.of(
                "#seq", "Event", "#Probe", "Value", "Value Class", "String"
        ));
        Vector<Vector<Object>> dataVector = new Vector<>(replayData.getDataEvents().size());


        int rowIndex = 0;
        for (DataEventWithSessionId dataEvent : replayData.getDataEvents()) {
            Vector<Object> rowVector = new Vector<>(6);

            DataInfo probeInfo = replayData.getProbeInfoMap().get(String.valueOf(dataEvent.getDataId()));
            ObjectInfo objectInfo = replayData.getObjectInfo().get(String.valueOf(dataEvent.getValue()));
            String eventType = "<>";
            if (probeInfo != null) {
                eventType = probeInfo.getEventType().toString();
            }

            rowVector.add(dataEvent.getRecordedAt());
            rowVector.add(eventType);
            rowVector.add(dataEvent.getDataId());
            rowVector.add(Long.valueOf(dataEvent.getValue()).toString());

            String eventLogLine = "["
                    + dataEvent.getRecordedAt()
                    + "][" + dataEvent.getDataId()
                    + "][" + eventType +
                    "] value: " + Long.valueOf(dataEvent.getValue()).toString();

            if (objectInfo != null) {
                TypeInfo typeInfo = replayData.getTypeInfo().get(String.valueOf(objectInfo.getTypeId()));
                eventLogLine =
                        eventLogLine + ", Type: " + typeInfo.getTypeNameFromClass();
                rowVector.add(typeInfo.getTypeNameFromClass());
            }
            if (replayData.getStringInfoMap().containsKey(String.valueOf(dataEvent.getValue()))) {
                StringInfo stringValue = replayData.getStringInfoMap().get(String.valueOf(dataEvent.getValue()));
                eventLogLine =
                        eventLogLine + ", Value: " + stringValue.getContent();
                rowVector.add(stringValue.getContent());
            }


            dataVector.add(rowVector);

        }


        tableModel = new DefaultTableModel(dataVector, columnVector) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        eventsTable.setModel(tableModel);
        eventsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                int selectedColumn = eventsTable.getSelectedColumn();
                String selectedColumnName = tableModel.getColumnName(selectedColumn);

                int selectedRowIndex = eventsTable.getSelectedRow();
                Object clickedValue = tableModel.getValueAt(selectedRowIndex, selectedColumn);

                switch (selectedColumnName) {
                    case "Probe":

                        break;
                }

            }
        });
    }

    public JPanel getContent() {
        return containerPanel;
    }

}
