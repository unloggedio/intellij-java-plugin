package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.client.UnloggedClientInterface;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.SessionMode;
import com.insidious.plugin.ui.SessionInstanceChangeListener;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.insidious.plugin.upload.SourceModel;
import com.intellij.notification.NotificationType;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class RemoteSourceFilter {

    private final SessionInstanceChangeListener insidiousService;
    private final SourceModel sourceModel;
    private ComponentLifecycleListener<StompFilter> componentLifecycleListener;
    private SessionMode localSessionMode;
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
    private JPanel serverListPanel;
    private JPanel serverListButton;
    private JButton finalCancelButton;
    private JButton finalSaveButton;
    private JPanel mainPanel;
    private JPanel setupInfo;
    private JLabel remoteServerLink;
    private JLabel setupText;
    private UnloggedClientInterface client;

    public RemoteSourceFilter(SourceModel sourceModel,
                              SessionInstanceChangeListener insidiousService) {
        this.sourceModel = sourceModel;
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
                client = insidiousService.setUnloggedClient(sourceModel);

                remotePanel.setVisible(true);
                mainPanel.revalidate();
                mainPanel.repaint();
            }
        });
        localhostRadio.addActionListener(e -> {
            localSessionMode = SessionMode.LOCAL;
            sourceModel.setSessionMode(localSessionMode);
            client = insidiousService.setUnloggedClient(sourceModel);

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

            // remove old session data
            serverListPanel.removeAll();

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
            List<ExecutionSession> executionSessionList;
            try {
                executionSessionList = client.sessionDiscovery(false);
            } catch (Throwable th) {
                InsidiousNotification.notifyMessage("Failed to connect to server: " + th.getMessage(),
                        NotificationType.ERROR);
                return;

            }

            ButtonGroup buttonGroup = new ButtonGroup();
            serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));
            for (ExecutionSession executionSession : executionSessionList) {

                ExecutionSessionItemComponent esic = new ExecutionSessionItemComponent(executionSession);
                serverListPanel.add(esic.getComponent());
                buttonGroup.add(esic.getRadioComponent());
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

//            if (this.sourceModel.getSessionMode() == SessionMode.REMOTE) {
//                // get selected execution session
//                ButtonModel selectedButton = remoteButtonGroup.getSelection();
//                int buttonIndex = -1;
//                for (int i=0;i<=listButtonModel.size()-1;i++) {
//                    if (listButtonModel.get(i) == selectedButton) {
//                        buttonIndex = i;
//                        break;
//                    }
//                }
//
//                // make the list of sessionId
//                String sessionId = this.executionSessionList.get(buttonIndex).getSessionId();
//                List<String> listSessionId = new ArrayList<>();
//                listSessionId.add(sessionId);
//
//                this.sourceModel.setSourceFilter(SourceFilter.SELECTED_ONLY);
//                this.sourceModel.setSessionId(listSessionId);
//                this.client.setSourceModel(this.sourceModel);
//            }

            componentLifecycleListener.onClose();
        });


    }

    public Component getComponent() {
        return mainPanel;
    }

    public void setOnCloseListener(ComponentLifecycleListener<StompFilter> componentLifecycleListener) {
        this.componentLifecycleListener = componentLifecycleListener;
    }
}
