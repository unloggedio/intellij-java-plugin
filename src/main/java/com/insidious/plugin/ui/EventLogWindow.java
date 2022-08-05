package com.insidious.plugin.ui;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.InsidiousService;
import com.intellij.ide.DataManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.impl.text.TextEditorState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;

public class EventLogWindow {
    private final InsidiousService service;
    private final int paginationSize = 100;
    private ReplayData replayData;
    private DefaultTableModel tableModel;
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
    private int currentPage = 0;
    private long currentObjectId;

    public EventLogWindow(InsidiousService insidiousService) {

        this.service = insidiousService;


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

        this.searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        });


        eventsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                int selectedColumn = eventsTable.getSelectedColumn();
                String selectedColumnName = tableModel.getColumnName(selectedColumn);

                int selectedRowIndex = eventsTable.getSelectedRow();
                Object clickedValue = tableModel.getValueAt(selectedRowIndex, selectedColumn);

                DataEventWithSessionId selectedEvent = replayData.getDataEvents().get(selectedRowIndex);
                DataInfo probeInfo = replayData.getProbeInfoMap().get(String.valueOf(selectedEvent.getDataId()));

                ClassInfo classInfo = replayData.getClassInfoMap().get(String.valueOf(probeInfo.getClassId()));
                String fileName = classInfo.getFilename();

                String fileLocation = "src/main/java/" + classInfo.getClassName() + ".java";


                @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                        .refreshAndFindFileByUrl(
                                Path.of(service.getProject().getBasePath(), fileLocation).toUri().toString());

                if (newFile == null) {
                    return;
                }

                FileEditor[] fileEditor = FileEditorManager.getInstance(service.getProject()).openFile(newFile,
                        true, true);



                Editor editor =
                        (Editor) DataManager.getInstance()
                                .getDataContext(fileEditor[0].getComponent())
                                .getData(CommonDataKeys.EDITOR);


                @Nullable Document newDocument = FileDocumentManager.getInstance().getDocument(newFile);
                if (probeInfo.getLine() > 0) {
                    int lineOffsetStart = newDocument.getLineStartOffset(probeInfo.getLine() - 1);
                    editor.getCaretModel().getCurrentCaret().moveToOffset(lineOffsetStart);
                    editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
                }

            }

        });

        this.firstPage.addActionListener(e -> {
            currentPage = 0;
            loadHistory(currentObjectId, currentPage);
        });
        this.previousPage.addActionListener(e -> {
            currentPage -= 1;
            if (currentPage < 0) {
                currentPage = 0;
            }
            loadHistory(currentObjectId, currentPage);
        });
        this.nextPage.addActionListener(e -> {
            currentPage += 1;
            loadHistory(currentObjectId, currentPage);
        });
    }

    public void loadObject(long objectId) {
        currentPage = 0;
        currentObjectId = objectId;

        queryTextField.setText(String.valueOf(objectId));
        try {
            ProgressManager.getInstance().run(new Task.WithResult<ReplayData, Exception>(
                    service.getProject(), "Unlogged", true
            ) {
                @Override
                protected ReplayData compute(@NotNull ProgressIndicator indicator) throws Exception {
                    return loadHistory(objectId, currentPage);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            InsidiousNotification.notifyMessage("failed to search: " + e.getMessage(),
                    NotificationType.ERROR);
        }

    }

    private ReplayData loadHistory(long objectId, int pageNumber) {
        infoLabel.setText("Loading page: " + (pageNumber + 1));
        FilteredDataEventsRequest filterRequest = new FilteredDataEventsRequest();
        filterRequest.setObjectId(objectId);
        PageInfo pageInfo = new PageInfo(pageNumber, paginationSize, PageInfo.Order.ASC);

        pageInfo.setBufferSize(Integer.valueOf(String.valueOf(bufferSize.getValue())));

        filterRequest.setPageInfo(pageInfo);
        ReplayData replayData1 = service.getClient().fetchObjectHistoryByObjectId(filterRequest);
        updateTableData(replayData1);
        infoLabel.setText("[Page " + (pageNumber + 1) + "] [" + replayData1.getDataEvents().size() + " events]");
        return replayData1;
    }

    private void updateTableData(ReplayData replayData1) {
        this.replayData = replayData1;
        Vector<Object> columnVector = new Vector<>(List.of(
                "Event", "#Time", "#Line", "Value", "Attributes", "Value type", "String"
        ));
        Vector<Vector<Object>> dataVector = new Vector<>(replayData.getDataEvents().size());


        int rowIndex = 0;
        for (DataEventWithSessionId dataEvent : replayData.getDataEvents()) {
            Vector<Object> rowVector = new Vector<>(6);

            DataInfo probeInfo = replayData.getProbeInfoMap().get(String.valueOf(dataEvent.getDataId()));
            ObjectInfo objectInfo = replayData.getObjectInfo().get(String.valueOf(dataEvent.getValue()));
            String eventType = probeInfo.getEventType().toString();


//            rowVector.add(dataEvent.getRecordedAt());
            rowVector.add(eventType);
            rowVector.add(dataEvent.getNanoTime());
            rowVector.add(probeInfo.getLine());
            rowVector.add(Long.valueOf(dataEvent.getValue()).toString());
            rowVector.add(probeInfo.getAttributes());

            if (objectInfo != null) {
                TypeInfo typeInfo = replayData.getTypeInfo().get(String.valueOf(objectInfo.getTypeId()));
                rowVector.add(typeInfo.getTypeNameFromClass());
            }
            if (replayData.getStringInfoMap().containsKey(String.valueOf(dataEvent.getValue()))) {
                StringInfo stringValue = replayData.getStringInfoMap().get(String.valueOf(dataEvent.getValue()));
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


//        //  "Event", "#Time", "#Line", "Value", "Attributes", "Value type", "String"
//        eventsTable.getColumn("Event").setPreferredWidth(130);
//        eventsTable.getColumn("#Time").setPreferredWidth(25);
//        eventsTable.getColumn("#Line").setPreferredWidth(5);
//        eventsTable.getColumn("Value").setPreferredWidth(40);
//        eventsTable.getColumn("Attributes").setPreferredWidth(200);
//        eventsTable.getColumn("String").setPreferredWidth(100);


    }

    public JPanel getContent() {
        return containerPanel;
    }

}
