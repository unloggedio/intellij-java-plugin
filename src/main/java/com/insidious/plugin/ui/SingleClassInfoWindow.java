package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.ObjectWithTypeInfo;
import com.insidious.plugin.pojo.SearchQuery;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SingleClassInfoWindow {
    private static final String SPAN_FORMAT = "<span style='color:%s;'>%s</span>";
    private static final Vector<?> OBJECT_TABLE_COLUMNS = new Vector<>(
            List.of(
                    "Object Id",
                    "Type",
                    "Load",
                    "Test"
            )
    );
    private final Project project;
    private final InsidiousService insidiousService;
    private final List<ObjectWithTypeInfo> objectResultList = new LinkedList<>();
    private JPanel containerPanel;
    private JLabel headingLabel;
    private JPanel controlPanel;
    private JPanel headingPanel;
    private JButton searchObjects;
    private JButton loadObjectHistoryButton;
    private JTextField searchQueryTextField;
    private JButton searchTypeButton;
    private JTable objectListTable;
    private JScrollPane resultContainerPanel;
    private LoadEventHistoryListener eventHistoryListener;
    private TableModel tableDataModel;

    public SingleClassInfoWindow(
            Project project,
            InsidiousService insidiousService
    ) {
        this.project = project;
        this.insidiousService = insidiousService;
//        this.treeNode = treeClassInfoModel;
//        searchQueryTextField.setText(treeClassInfoModel.toString());
        tableDataModel = new DefaultTableModel(
                new Vector<>(0),
                OBJECT_TABLE_COLUMNS
        );

        searchTypeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchQuery = searchQueryTextField.getText();
                if (searchQuery != null && searchQuery.length() > 0) {
                    doSearch(searchQuery);
                }
            }
        });

//        loadObjectHistoryButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                String initialValue = null;
//                if (objectResultList.size() > 0) {
//                    initialValue = String.valueOf(objectResultList.get(0).getObjectInfo().getObjectId());
//                }
//                String objectId = initialValue;
//                if (objectResultList.size() > 1) {
//                    objectId = Messages.showInputDialog("Object ID", "Unlogged", null,
//                            initialValue, null);
//                }
//                if (objectId == null || objectId.length() == 0) {
//                    return;
//                }
//                final Long objectLongId = Long.valueOf(objectId);
////                resultTextArea.append("Loading object history: " + objectId + "\n");
//                eventHistoryListener.loadEventHistory(objectLongId);
//
//            }
//        });

//        searchObjects.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                doSearch(searchQueryTextField.getText());
//            }
//        });

    }

    public void doSearch(String searchQuery) {
        setRows(List.of());
        searchQueryTextField.setText(searchQuery);


        List<String> archiveList = insidiousService.getClient()
                .getSessionArchiveList(insidiousService.getClient()
                        .getCurrentSession().getSessionId());

        String searchRange = null;
        if (archiveList.size() > 100) {
            searchRange = Messages.showInputDialog(
                    "Search range, leave blank to search all",
                    "Unlogged", null);
        }

        String fullyClassifiedClassName = searchQuery;
//        fullyClassifiedClassName = treeNode.getClassName().replaceAll("/", ".");
//        resultTextArea.setText("Searching for objects of type " + fullyClassifiedClassName + "\n");

        try {
            String finalSearchRange = searchRange;
            objectResultList.clear();
            ProgressManager.getInstance().run(new Task.WithResult<Object, Exception>(
                    project, "Unlogged", true
            ) {
                @Override
                protected Object compute(@NotNull ProgressIndicator indicator) throws Exception {

                    BlockingQueue<Boolean> sync = new ArrayBlockingQueue<>(1);
                    SearchQuery searchQuery = SearchQuery
                            .ByType(List.of(
                                    fullyClassifiedClassName
                            ));
                    searchQuery.setRange(finalSearchRange);
                    insidiousService.getClient().getObjectsByType(
                            searchQuery,
                            insidiousService.getClient().getCurrentSession().getSessionId(),
                            new ClientCallBack<>() {
                                @Override
                                public void error(ExceptionResponse errorResponse) {

                                    InsidiousNotification.notifyMessage(
                                            "Failed to search by type - " + errorResponse.getMessage(),
                                            NotificationType.ERROR
                                    );

                                }

                                @Override
                                public void success(Collection<ObjectWithTypeInfo> tracePoints) {
                                    addTracePointsToTable(tracePoints);


                                }

                                @Override
                                public void completed() {
                                    sync.offer(true);
                                }
                            }
                    );
                    sync.take();
//                    resultTextArea.append("Finished searching\n");

                    return "ok";
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
//            resultTextArea.append("Failed to search: " + ex.getMessage() + "\n");
        }
    }

    private void addTracePointsToTable(Collection<ObjectWithTypeInfo> tracePoints) {
        objectResultList.addAll(tracePoints);
        setRows(objectResultList);

    }

    private void setRows(List<ObjectWithTypeInfo> tracePoints) {
        Vector<Vector<Object>> tracePointRowVectors = new Vector<>(tracePoints.size());
        for (ObjectWithTypeInfo tracePoint : tracePoints) {
//            JButton loadObjectButton = new JButton();
//            loadObjectButton.addActionListener(e -> eventHistoryListener
//                    .loadEventHistory(
//                            tracePoint.getObjectInfo().getObjectId()
//                    )
//            );
            tracePointRowVectors.add(

                    new Vector<>(List.of(
                            tracePoint.getObjectInfo().getObjectId(),
                            tracePoint.getTypeInfo().getTypeNameFromClass(),
                            "Load",
                            "Test"
                    ))
            );
        }

        tableDataModel = new DefaultTableModel(tracePointRowVectors, OBJECT_TABLE_COLUMNS);


        objectListTable.setPreferredScrollableViewportSize(
                objectListTable.getPreferredSize());
        //thanks mKorbel +1 http://stackoverflow.com/questions/10551995/how-to-set-jscrollpane-layout-to-be-the-same-as-jtable


        objectListTable.setModel(tableDataModel);
        objectListTable.getColumn("Load").setCellRenderer(new ButtonRenderer());
        ActionListener actionLoadListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                eventHistoryListener.loadEventHistory(
                        tracePoints.get(objectListTable.getSelectedRow()).getObjectInfo().getObjectId()
                );
            }
        };
        ActionListener actionTestListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ObjectWithTypeInfo selectedTracePoint = tracePoints.get(objectListTable.getSelectedRow());
                try {
                    insidiousService.generateTestCases(
                            selectedTracePoint
                    );
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        objectListTable.getColumn("Load").setCellEditor(new ButtonEditor(new JCheckBox(),
                actionLoadListener));
        objectListTable.getColumn("Load").setPreferredWidth(30);


        objectListTable.getColumn("Test").setCellRenderer(new ButtonRenderer());
        objectListTable.getColumn("Test").setCellEditor(new ButtonEditor(new JCheckBox(),
                actionTestListener));
        objectListTable.getColumn("Test").setPreferredWidth(30);
    }


    public JPanel getContent() {
        return containerPanel;
    }

    public void addEventHistoryLoadRequestListener(LoadEventHistoryListener loadEventHistoryListener) {
        this.eventHistoryListener = loadEventHistoryListener;
    }


    class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(UIManager.getColor("Button.background"));
            }
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {

        protected JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox, ActionListener actionListener) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(actionListener);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(table.getBackground());
            }
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
//            if (isPushed) {
//                JOptionPane.showMessageDialog(button, label + ": Ouch!");
//            }
//            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
//            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
