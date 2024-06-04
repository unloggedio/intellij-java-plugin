package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.client.UnloggedClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.ExecutionSessionSourceMode;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.insidious.plugin.upload.ExecutionSessionSource;
import com.insidious.plugin.upload.SourceFilter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.AnimatedIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

public class RemoteSourceFilter {

    private static final Logger logger = LoggerUtil.getInstance(RemoteSourceFilter.class);
    //    private final InsidiousService insidiousService;
    private final ButtonGroup buttonGroup;
    AnimatedIcon icon = new AnimatedIcon(
            125,
            AllIcons.Process.Step_1,
            AllIcons.Process.Step_2,
            AllIcons.Process.Step_3,
            AllIcons.Process.Step_4,
            AllIcons.Process.Step_5,
            AllIcons.Process.Step_6,
            AllIcons.Process.Step_7,
            AllIcons.Process.Step_8
    );
    private ExecutionSessionSource executionSessionSource;
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
    private JButton checkNewSessionButton;
    private JScrollPane serverListScroll;
    private JPanel serverListButton;
    private JButton finalCancelButton;
    private JButton confirmSaveApplyButton;
    private JPanel mainPanel;
    private JPanel setupInfo;
    private JLabel remoteServerLink;
    private JLabel howToSetupLabel;
    private JPanel serverListPanel;
    private UnloggedClientInterface independentClientInstance;
    private HashMap<ButtonModel, ExecutionSession> modelToSessionMap;

    public void routeToServerReadme() {
        String link = "https://read.unlogged.io/server/";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToServerReadme", null);
    }



    public RemoteSourceFilter(InsidiousService insidiousService) {
        this.executionSessionSource = insidiousService.getSessionSource();
        this.buttonGroup = new ButtonGroup();
        serverListScroll.setVisible(false);
        serverListScroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));

        howToSetupLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        howToSetupLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToServerReadme();
            }
        });

        // load the old state
        this.independentClientInstance = UnloggedClientFactory.createClient(insidiousService.getSessionSource());
        if (this.executionSessionSource.getSessionMode() == ExecutionSessionSourceMode.LOCAL) {
            localhostRadio.setSelected(true);
            remotePanel.setVisible(false);
        } else {
            remoteRadio.setSelected(true);
            serverLinkField.setText(this.executionSessionSource.getServerEndpoint());
            remotePanel.setVisible(true);
            serverListScroll.setVisible(true);
            showLoading();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                List<ExecutionSession> list = sessionDiscoveryBackground(independentClientInstance);
                ApplicationManager.getApplication().invokeLater(() -> {
                    createRemoteSessionList(list);
                });
            });
        }
        mainPanel.revalidate();
        mainPanel.repaint();


        // radio button and remote panel logic
        remoteRadio.addActionListener(e -> {
            this.executionSessionSource = new ExecutionSessionSource(ExecutionSessionSourceMode.REMOTE);
            independentClientInstance = UnloggedClientFactory.createClient(executionSessionSource);

            confirmSaveApplyButton.setEnabled(false);
            setSessionListPanelMessage("Click \"Check for sessions\" to list and select a session");
            serverListScroll.setVisible(true);

            remotePanel.setVisible(true);
            mainPanel.revalidate();
            mainPanel.repaint();
        });
        localhostRadio.addActionListener(e -> {
            this.executionSessionSource = new ExecutionSessionSource(ExecutionSessionSourceMode.LOCAL);
            executionSessionSource.setSourceFilter(SourceFilter.MOST_RECENT);
            independentClientInstance = UnloggedClientFactory.createClient(executionSessionSource);
            confirmSaveApplyButton.setEnabled(true);
            remotePanel.setVisible(false);
            mainPanel.revalidate();
            mainPanel.repaint();
        });


        checkNewSessionButton.addActionListener(e -> {
            localServerEndpoint = getServerLinkFieldText();
            try {
                URI uri = new URI(localServerEndpoint);
            } catch (URISyntaxException ex) {
                InsidiousNotification.notifyMessage("Please enter a valid URL",
                        NotificationType.ERROR);
                return;
            }
            showLoading();
            this.executionSessionSource = new ExecutionSessionSource(ExecutionSessionSourceMode.REMOTE);
            this.executionSessionSource.setServerEndpoint(localServerEndpoint);
            independentClientInstance = UnloggedClientFactory.createClient(executionSessionSource);

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                List<ExecutionSession> list = sessionDiscoveryBackground(independentClientInstance);
                createRemoteSessionList(list);
            });

        });


        // final row button logic
        finalCancelButton.addActionListener(e -> {
            if (componentLifecycleListener != null) {
                componentLifecycleListener.onClose();
            }
        });

        confirmSaveApplyButton.addActionListener(e -> {

            if (!this.executionSessionSource.equals(insidiousService.getSessionSource())) {
                insidiousService.clearSession();
                independentClientInstance = UnloggedClientFactory.createClient(executionSessionSource);
                ExecutionSession executionSession = null;
                if (executionSessionSource.getSessionMode() == ExecutionSessionSourceMode.REMOTE) {
                    ButtonModel buttonModel = buttonGroup.getSelection();
                    executionSession = modelToSessionMap.get(buttonModel);

                    // make the list of sessionId
                    String sessionId = executionSession.getSessionId();
                    List<String> listSessionId = new ArrayList<>();
                    listSessionId.add(sessionId);

                    executionSessionSource.setSourceFilter(SourceFilter.SELECTED_ONLY);
                    executionSessionSource.setSessionId(listSessionId);
                }
                independentClientInstance.setSourceModel(this.executionSessionSource);
                insidiousService.setSessionSource(executionSessionSource);
                insidiousService.setUnloggedClient(independentClientInstance);
                if (executionSession != null) {
                    insidiousService.setSession(executionSession);
                }
            }


            componentLifecycleListener.onClose();
        });
    }

    private String getServerLinkFieldText() {
        String text = serverLinkField.getText();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private void showLoading() {
        serverListPanel.removeAll();
        serverListPanel.add(new JLabel("Loading sessions", icon, JLabel.CENTER));
    }


    private void createRemoteSessionList(List<ExecutionSession> executionSessionList) {
        // remove old session data
        Enumeration<AbstractButton> elements = buttonGroup.getElements();
        serverListScroll.setVisible(true);
        while (elements.hasMoreElements()) {
            AbstractButton button = elements.nextElement();
            buttonGroup.remove(button);
        }


        confirmSaveApplyButton.setEnabled(false);
        this.modelToSessionMap = new HashMap<>();
        serverListPanel.removeAll();

        List<String> prevSelectedSessionId = this.executionSessionSource.getSessionId();
        for (ExecutionSession executionSession : executionSessionList) {

            ExecutionSessionItemComponent esic = new ExecutionSessionItemComponent(executionSession);
            serverListPanel.add(esic.getComponent());
            AbstractButton radioComponent = esic.getRadioComponent();
            if ((prevSelectedSessionId != null) && (prevSelectedSessionId.contains(executionSession.getSessionId()))) {
                radioComponent.setSelected(true);
            }

            radioComponent.addActionListener(e -> confirmSaveApplyButton.setEnabled(true));

            buttonGroup.add(radioComponent);
            modelToSessionMap.put(radioComponent.getModel(), executionSession);
        }
        if (executionSessionList.isEmpty()) {
            setSessionListPanelMessage("No sessions found, click \"Check for sessions\" to try again");
        }

        serverListPanel.revalidate();
        serverListPanel.repaint();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void setSessionListPanelMessage(String text) {
        JLabel comp = new JLabel(text);
        comp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        serverListPanel.removeAll();
        serverListPanel.add(comp);
    }

    private List<ExecutionSession> sessionDiscoveryBackground(UnloggedClientInterface clientInterface) {
        try {
            return clientInterface.sessionDiscovery(false);
        } catch (Throwable th) {
            logger.warn("Failed to connect to remote server", th);
            InsidiousNotification.notifyMessage("Failed to connect to server: " + th.getMessage(),
                    NotificationType.ERROR);
        }
        return new ArrayList<>();
    }


    public Component getComponent() {
        return mainPanel;
    }

    public void setOnCloseListener(ComponentLifecycleListener<StompFilter> componentLifecycleListener) {
        this.componentLifecycleListener = componentLifecycleListener;
    }
}
