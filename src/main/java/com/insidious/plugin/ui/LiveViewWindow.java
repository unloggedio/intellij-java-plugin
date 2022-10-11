package com.insidious.plugin.ui;

import com.insidious.plugin.client.VideobugLocalClient;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.IOException;

public class LiveViewWindow {
    private final Project project;
    private final InsidiousService insidiousService;
    private final NewVideobugTreeModel treeModel;
    private final VideobugTreeCellRenderer cellRenderer;
    private JButton selectSession;
    private JPanel bottomPanel;
    private JPanel leftSplitContainer;
    private JTree mainTree;
    private JPanel mainPanel;


    public LiveViewWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;

        this.selectSession.addActionListener(selectSessionActionListener(project, insidiousService));

        treeModel = new NewVideobugTreeModel(insidiousService);
        cellRenderer = new VideobugTreeCellRenderer(insidiousService);

        mainTree.setModel(treeModel);
        mainTree.setCellRenderer(cellRenderer);

        TreeUtil.installActions(mainTree);

    }

    @NotNull
    private static ActionListener selectSessionActionListener(Project project, InsidiousService insidiousService) {
        return e -> {
            FileChooserDescriptor goingToChooseASingleFolder = FileChooserDescriptorFactory.createSingleFolderDescriptor();

            VideobugLocalClient localClient = (VideobugLocalClient) insidiousService.getClient();


            String sessionRootDirectory = localClient.getRootDirectory();
            VirtualFileSystem vfs = VirtualFileManager.getInstance().getFileSystem("C:");
            goingToChooseASingleFolder.setTitle("Unlogged");
            goingToChooseASingleFolder.setDescription("Select session");

            @Nullable VirtualFile choosenFile = FileChooser.chooseFile(goingToChooseASingleFolder, project, null);

            ExecutionSession session = new ExecutionSession();
            session.setSessionId(choosenFile.getName());
            try {
                insidiousService.getClient().setSession(session);
            } catch (IOException ex) {
                InsidiousNotification.notifyMessage("Failed to open session: " + ex.getMessage(), NotificationType.ERROR);
            }


        };
    }

    public JComponent getContent() {
        return mainPanel;
    }


}
