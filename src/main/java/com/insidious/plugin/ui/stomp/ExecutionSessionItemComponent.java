package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.client.pojo.ExecutionSession;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ExecutionSessionItemComponent {
    private final ExecutionSession executionSession;
    private JRadioButton radioButton;
    private JPanel mainPanel;
    private JLabel dateTimeLabel;
    private JLabel hostnameLabel;
    private JLabel packageNameLabel;

    public ExecutionSessionItemComponent(ExecutionSession executionSession, List<String> prevSelectedSessionId ) {

        this.executionSession = executionSession;
        Date date = executionSession.getCreatedAt();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm");
        String timeVal = dateFormat.format(date);

        String hostname = executionSession.getHostname();
        String projectId = executionSession.getProjectId();

        packageNameLabel.setText(projectId);
        hostnameLabel.setText(hostname);
        dateTimeLabel.setText(timeVal);

//        String radioButtonText = "<html> <small>" + timeVal + "</small> " + hostname + "<br>" + projectId + "</html>";
//        radioButton.setText(radioButtonText);
        radioButton.setMargin(JBUI.insets(10, 20));
        radioButton.setBorder(BorderFactory.createCompoundBorder(
                radioButton.getBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        if ((prevSelectedSessionId!=null) && (prevSelectedSessionId.contains(executionSession.getSessionId()))) {
            radioButton.setSelected(true);
        }
    }

    public Component getComponent() {
        return mainPanel;
    }

    public AbstractButton getRadioComponent() {
        return radioButton;
    }
}
