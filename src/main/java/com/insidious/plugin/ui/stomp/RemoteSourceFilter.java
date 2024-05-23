package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.adapter.java.JavaClassAdapter;
import com.insidious.plugin.autoexecutor.AutoExecutionConsumer;
import com.insidious.plugin.autoexecutor.GlobalJavaSearchContext;
import com.insidious.plugin.client.UnloggedClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.SessionMode;
import com.insidious.plugin.ui.SessionInstanceChangeListener;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.insidious.plugin.upload.SourceFilter;
import com.insidious.plugin.upload.SourceModel;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class RemoteSourceFilter {

    private final SessionInstanceChangeListener insidiousService;
    private final SourceModel sourceModel;
    private ComponentLifecycleListener<StompFilter> componentLifecycleListener;
    private String localServerEndpoint;
    private List<String> localSessionId;
    private JPanel sourcePanel;
    private JPanel sourceModeOption;
    private JRadioButton localhostRadio;
    private JRadioButton remoteRadio;
    private JPanel remotePanel;
    private JPanel remoteServerPanel;
    private JTextField serverLinkField;
    private JPanel setupInfoPanel;
    //    private JButton linkCancelButton;
    private JButton linkSaveButton;
    private JScrollPane serverListScroll;
    private JPanel serverListButton;
    private JButton finalCancelButton;
    private JButton finalSaveButton;
    private JPanel mainPanel;
    private JPanel setupInfo;
    private JLabel remoteServerLink;
    private JLabel setupText;
    private JPanel serverListPanel;
    private UnloggedClientInterface client;
    private ButtonGroup buttonGroup;
    private HashMap<ButtonModel, ExecutionSession> modelToSessionMap;

    public RemoteSourceFilter(SourceModel sourceModel,
                              SessionInstanceChangeListener insidiousService) {
        this.sourceModel = sourceModel;
        serverListScroll.setVisible(false);
        this.insidiousService = insidiousService;

        // load the old state
        this.client = insidiousService.getUnloggedClient();
        if (this.sourceModel.getSessionMode() == SessionMode.LOCAL) {
            localhostRadio.setSelected(true);
            remotePanel.setVisible(false);
        }
        else {
            remoteRadio.setSelected(true);
            serverLinkField.setText(this.sourceModel.getServerEndpoint());
            remotePanel.setVisible(true);
            sessionDiscoveryBackground();
        }
        mainPanel.revalidate();
        mainPanel.repaint();

        // styling logic
        sourceModeOption.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.LIGHT_GRAY),
                "<html><b>Select source to scan</b></html>",
                TitledBorder.LEADING, TitledBorder.TOP));

        remotePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.LIGHT_GRAY),
                "<html><b>Remote Server Details</b></html>",
                TitledBorder.LEADING, TitledBorder.TOP));


        // radio button and remote panel logic
        remoteRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sourceModel.setSessionMode(SessionMode.REMOTE);
                client = insidiousService.setUnloggedClient(sourceModel);

                remotePanel.setVisible(true);
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
        localhostRadio.addActionListener(e -> {
            sourceModel.setSessionMode(SessionMode.LOCAL);
            sourceModel.setServerEndpoint(null);
            sourceModel.setSourceFilter(SourceFilter.MOST_RECENT);
            sourceModel.setSessionId(null);
            client = insidiousService.setUnloggedClient(sourceModel);

            remotePanel.setVisible(false);
            mainPanel.revalidate();
            mainPanel.repaint();
        });


        linkSaveButton.addActionListener(e -> {
            localServerEndpoint = serverLinkField.getText();
            try {
                URI uri = new URI(localServerEndpoint);
            } catch (URISyntaxException ex) {
                InsidiousNotification.notifyMessage("Please enter a valid URL",
                        NotificationType.ERROR);
                return;
            }
            this.sourceModel.setServerEndpoint(this.localServerEndpoint);
            client.setSourceModel(sourceModel);
            sessionDiscoveryBackground();
        });


        // final row button logic
        finalCancelButton.addActionListener(e -> {
            if (componentLifecycleListener != null) {
                componentLifecycleListener.onClose();
            }
        });

        finalSaveButton.addActionListener(e -> {

            if (this.sourceModel.getSessionMode() == SessionMode.REMOTE) {

                ButtonModel buttonModel = buttonGroup.getSelection();
                ExecutionSession executionSession = modelToSessionMap.get(buttonModel);

                // make the list of sessionId
                String sessionId = executionSession.getSessionId();
                List<String> listSessionId = new ArrayList<>();
                listSessionId.add(sessionId);

                this.sourceModel.setSourceFilter(SourceFilter.SELECTED_ONLY);
                this.sourceModel.setSessionId(listSessionId);
                this.client.setSourceModel(this.sourceModel);
            }

            componentLifecycleListener.onClose();
        });
    }

    private void createRemoteSessionList(List<ExecutionSession> executionSessionList) {
        // remove old session data
        this.buttonGroup = new ButtonGroup();
        this.modelToSessionMap = new HashMap<>();
        serverListPanel.removeAll();

        serverListScroll.setVisible(true);
        serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));
        List<String> prevSelectedSessionId = this.sourceModel.getSessionId();
        for (ExecutionSession executionSession : executionSessionList) {

            ExecutionSessionItemComponent esic = new ExecutionSessionItemComponent(executionSession, prevSelectedSessionId);
            serverListPanel.add(esic.getComponent());
            buttonGroup.add(esic.getRadioComponent());
            modelToSessionMap.put(esic.getRadioComponent().getModel(), executionSession);
        }
        if (executionSessionList.size() == 0) {
            serverListPanel.add(new JLabel("No sessions found, click \"Check\" to try again"));
        }

        serverListPanel.revalidate();
        serverListPanel.repaint();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void sessionDiscoveryBackground() {

        Task.Backgroundable executeAll = new Task.Backgroundable(this.insidiousService.getProject(), "Session discovery", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    List<ExecutionSession> executionSessionList = client.sessionDiscovery(false);
                    createRemoteSessionList(executionSessionList);
                } catch (Throwable th) {
                    InsidiousNotification.notifyMessage("Failed to connect to server: " + th.getMessage(),
                            NotificationType.ERROR);
                }
            }
        };
        ProgressManager.getInstance().run(executeAll);
    }


    public Component getComponent() {
        return mainPanel;
    }

    public void setOnCloseListener(ComponentLifecycleListener<StompFilter> componentLifecycleListener) {
        this.componentLifecycleListener = componentLifecycleListener;
    }
}
