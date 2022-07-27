package com.insidious.plugin.ui;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.ObjectInfo;
import com.insidious.common.weaver.StringInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.ObjectsWithTypeInfo;
import com.insidious.plugin.pojo.SearchQuery;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SingleClassInfoWindow {
    private static final String SPAN_FORMAT = "<span style='color:%s;'>%s</span>";
    private final Project project;
    private final TreeClassInfoModel treeNode;
    private final InsidiousService insidiousService;
    private final List<ObjectsWithTypeInfo> objectResultList = new LinkedList<>();
    private JPanel containerPanel;
    private JTextArea resultTextArea;
    private JLabel headingLabel;
    private JPanel controlPanel;
    private JPanel headingPanel;
    private JButton searchObjects;
    private JButton loadObjectHistoryButton;

    public SingleClassInfoWindow(
            Project project,
            InsidiousService insidiousService,
            TreeClassInfoModel treeClassInfoModel
    ) {
        this.project = project;
        this.insidiousService = insidiousService;
        this.treeNode = treeClassInfoModel;
        headingLabel.setText(treeClassInfoModel.toString());


        loadObjectHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String initialValue = null;
                if (objectResultList.size() > 0) {
                    initialValue = String.valueOf(objectResultList.get(0).getObjectInfo().getObjectId());
                }
                String objectId = initialValue;
                if (objectResultList.size() > 1) {
                    objectId = Messages.showInputDialog("Object ID", "Unlogged", null,
                            initialValue, null);
                }
                if (objectId == null || objectId.length() == 0) {
                    return;
                }
                final Long objectLongId = Long.valueOf(objectId);

                try {
                    ProgressManager.getInstance().run(new Task.WithResult<ReplayData, Exception>(
                            project, "Unlogged", true
                    ) {
                        @Override
                        protected ReplayData compute(@NotNull ProgressIndicator indicator) throws Exception {
                            FilteredDataEventsRequest request = new FilteredDataEventsRequest();
                            request.setObjectId(objectLongId);
                            request.setPageInfo(new PageInfo(0, 1000));
                            ReplayData replayData = insidiousService.getClient().fetchObjectHistoryByObjectId(
                                    request
                            );

                            for (DataEventWithSessionId dataEvent : replayData.getDataEvents()) {
                                DataInfo probeInfo = replayData.getProbeInfoMap().get(String.valueOf(dataEvent.getDataId()));
                                ObjectInfo objectInfo = replayData.getObjectInfo().get(String.valueOf(dataEvent.getValue()));
                                String eventType = "<>";
                                if (probeInfo != null) {
                                    eventType = probeInfo.getEventType().toString();
                                }

                                String eventLogLine = "["
                                        + dataEvent.getRecordedAt()
                                        + "][" + dataEvent.getDataId()
                                        + "][" + eventType +
                                        "] value: " + Long.valueOf(dataEvent.getValue()).toString();

                                if (objectInfo != null) {
                                    TypeInfo typeInfo = replayData.getTypeInfo().get(String.valueOf(objectInfo.getTypeId()));
                                    eventLogLine =
                                            eventLogLine + ", Type: " + typeInfo.getTypeNameFromClass();

                                }
                                if (replayData.getStringInfoMap().containsKey(String.valueOf(dataEvent.getValue()))) {
                                    StringInfo stringValue = replayData.getStringInfoMap().get(String.valueOf(dataEvent.getValue()));
                                    eventLogLine =
                                            eventLogLine + ", Value: " + stringValue.getContent();
                                }


                                if (dataEvent.getValue() == objectLongId) {
                                    eventLogLine = " ** " + eventLogLine;
                                }

                                resultTextArea.append(eventLogLine + "\n");
                            }


                            return replayData;
                        }
                    });
                } catch (Exception ex) {
                    resultTextArea.append("Failed to load object history: " + ex.getMessage() +
                            "\n");
                }


            }
        });

        searchObjects.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                List<String> archiveList = insidiousService.getClient().getSessionArchiveList(treeClassInfoModel.getSessionId());

                String searchRange = null;
                if (archiveList.size() > 100) {
                    searchRange = Messages.showInputDialog(
                            "Search range, leave blank to search all",
                            "Unlogged", null);
                }

                resultTextArea.setText("Searching...\n");

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
                                            treeNode.getClassName().replaceAll("/", ".")
                                    ));
                            searchQuery.setRange(finalSearchRange);
                            insidiousService.getClient().getObjectsByType(
                                    searchQuery,
                                    treeNode.getSessionId(), new ClientCallBack<ObjectsWithTypeInfo>() {
                                        @Override
                                        public void error(ExceptionResponse errorResponse) {
                                            resultTextArea.setText("Failed - " + errorResponse.getMessage()
                                                    + "\n" + errorResponse.getError());
                                        }

                                        @Override
                                        public void success(Collection<ObjectsWithTypeInfo> tracePoints) {
                                            objectResultList.addAll(tracePoints);
                                            for (ObjectsWithTypeInfo tracePoint : tracePoints) {
                                                String newLine = "Trace point: " +
                                                        "ObjectId=" +
                                                        tracePoint.getObjectInfo().getObjectId() +
                                                        "\n";
                                                resultTextArea.append(newLine);
                                            }


                                        }

                                        @Override
                                        public void completed() {
                                            sync.offer(true);
                                        }
                                    }
                            );
                            sync.take();
                            resultTextArea.append("Finished searching\n");

                            return "ok";
                        }
                    });
                } catch (Exception ex) {
                    resultTextArea.append("Failed to search: " + ex.getMessage() + "\n");
                }


            }
        });

    }

    public JPanel getContent() {
        return containerPanel;
    }
}
