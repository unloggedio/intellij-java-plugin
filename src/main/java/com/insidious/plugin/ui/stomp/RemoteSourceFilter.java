package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.SessionMode;
import com.insidious.plugin.ui.SessionInstanceChangeListener;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.insidious.plugin.upload.SourceModel;
import com.intellij.notification.NotificationType;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RemoteSourceFilter {

    private final ComponentLifecycleListener<StompFilter> componentLifecycleListener;
    private final SessionInstanceChangeListener insidiousService;
    private SourceModel sourceModel;
    private SessionMode localSessionMode;
    private String localServerEndpoint;
    private List<String> localSessionId;

    private JPanel sourcePanel;
    private JPanel sourceModeOption;
    private JRadioButton localhostRadio;
    private JRadioButton remoteRadio;
    private JPanel remotePanel;
    private JPanel remoteServerPanel;
    private JLabel remoteServerLink;
    private JTextField serverLinkField;
    private JPanel setupInfoPanel;
    //    private JButton linkCancelButton;
    private JButton linkSaveButton;
    private JPanel serverListPanel;
    private JPanel serverListButton;
    private JButton finalCancelButton;
    private JButton finalSaveButton;
    private JPanel mainPanel;
    private JLabel setupText;
    private JPanel setupInfo;
    private VideobugClientInterface client;

    public RemoteSourceFilter(SourceModel sourceModel,
                              ComponentLifecycleListener<StompFilter> componentLifecycleListener,
                              SessionInstanceChangeListener insidiousService) {
        this.sourceModel = sourceModel;
        this.componentLifecycleListener = componentLifecycleListener;
        this.insidiousService = insidiousService;
        sourceModeOption.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.LIGHT_GRAY),
                "<html><b>Select source to scan</b></html>",
                TitledBorder.LEADING, TitledBorder.TOP));

        remotePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.LIGHT_GRAY),
                "<html><b>Remote Server Details</b></html>",
                TitledBorder.LEADING, TitledBorder.TOP));


        // radio button and remote panel logic
        remotePanel.setVisible(false);
        remoteRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                localSessionMode = SessionMode.REMOTE;
                sourceModel.setSessionMode(localSessionMode);
                client = insidiousService.modifySessionInstance(sourceModel);

                remotePanel.setVisible(true);
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
        localhostRadio.addActionListener(e -> {
            localSessionMode = SessionMode.LOCAL;
            sourceModel.setSessionMode(localSessionMode);
            client = insidiousService.modifySessionInstance(sourceModel);

            remotePanel.setVisible(false);
            mainPanel.revalidate();
            mainPanel.repaint();
        });


        // server link logic
        String placeholderText = "Enter your server URL here";
        serverLinkField.setText(placeholderText);
        serverLinkField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (serverLinkField.getText().equals(placeholderText)) {
                    serverLinkField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (serverLinkField.getText().isEmpty()) {
                    serverLinkField.setText(placeholderText);
                }
            }
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
            List<ExecutionSession> executionSessionList = client.sessionDiscovery();

            serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));
            for (ExecutionSession executionSession : executionSessionList) {

                Date date = executionSession.getCreatedAt();
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE dd-MM-yyyy HH:mm");
                String timeVal = dateFormat.format(date);

                String hostname = executionSession.getHostname();
                String projectId = executionSession.getProjectId();

                String radioButtonText = "<html> <small>" + timeVal + "</small> " + hostname + "<br>" + projectId + "</html>";
                JRadioButton localButton = new JRadioButton(radioButtonText);
                localButton.setMargin(JBUI.insets(10, 20));
                localButton.setBorder(BorderFactory.createCompoundBorder(
                        localButton.getBorder(),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                serverListPanel.add(localButton);
            }

            serverListPanel.revalidate();
            serverListPanel.repaint();
            mainPanel.repaint();
            mainPanel.revalidate();
        });


        // final row button logic
        finalCancelButton.addActionListener(e -> {
            if (componentLifecycleListener != null) {
                componentLifecycleListener.onClose();
            }
        });

        finalSaveButton.addActionListener(e -> {
            sourceModel.setSessionId(this.localSessionId);
            componentLifecycleListener.onClose();
        });


    }

    public Component getComponent() {
        return mainPanel;
    }
}
