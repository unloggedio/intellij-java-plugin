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
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class LiveViewWindow {
    private final Project project;
    private final InsidiousService insidiousService;
    private JButton selectSession;
    private JPanel bottomPanel;
    private JPanel leftSplitContainer;
    private JTree classHierarchyContainer;
    private JPanel bottomLeftResultPanel;
    private JPanel leftControlPanel;
    private JPanel mainPanel;


    public LiveViewWindow(Project project, InsidiousService insidiousService) {
        this.project = project;
        this.insidiousService = insidiousService;

        this.selectSession.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileChooserDescriptor goingToChooseASingleFolder = FileChooserDescriptorFactory.createSingleFolderDescriptor();

                VideobugLocalClient localClient = (VideobugLocalClient) insidiousService.getClient();


                String sessionRootDirectory = localClient.getRootDirectory();
//                VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(
//                        sessionRootDirectory
//                );
                VirtualFileSystem vfs = VirtualFileManager.getInstance().getFileSystem("C:");
//                vfs.
//                goingToChooseASingleFolder.setRoots(fileByUrl);
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


            }
        });

    }

    public JComponent getContent() {
        return mainPanel;
    }


}
