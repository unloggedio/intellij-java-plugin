package com.insidious.plugin.ui;

import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.pojo.ExceptionResponse;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.ObjectWithTypeInfo;
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
    private final List<ObjectWithTypeInfo> objectResultList = new LinkedList<>();
    private JPanel containerPanel;
    private JTextArea resultTextArea;
    private JLabel headingLabel;
    private JPanel controlPanel;
    private JPanel headingPanel;
    private JButton searchObjects;
    private JButton loadObjectHistoryButton;
    private LoadEventHistoryListener eventHistoryListener;

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
                resultTextArea.append("Loading object history: " + objectId);
                eventHistoryListener.loadEventHistory(objectLongId);

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

                String fullyClassifiedClassName = treeNode.getClassName().replaceAll("/", ".");
                resultTextArea.setText("Searching for objects of type " + fullyClassifiedClassName + "\n");

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
                                    treeNode.getSessionId(), new ClientCallBack<ObjectWithTypeInfo>() {
                                        @Override
                                        public void error(ExceptionResponse errorResponse) {
                                            resultTextArea.setText("Failed - " + errorResponse.getMessage()
                                                    + "\n" + errorResponse.getError());
                                        }

                                        @Override
                                        public void success(Collection<ObjectWithTypeInfo> tracePoints) {
                                            objectResultList.addAll(tracePoints);
                                            for (ObjectWithTypeInfo tracePoint : tracePoints) {
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

    public void addEventHistoryLoadRequestListener(LoadEventHistoryListener loadEventHistoryListener) {
        this.eventHistoryListener = loadEventHistoryListener;
    }
}
