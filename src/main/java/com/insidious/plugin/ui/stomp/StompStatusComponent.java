package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class StompStatusComponent {
    private final Map<String, JLabel> rightPanelMapById = new HashMap<>();
    private JPanel connectionStatusPanel;
    private JPanel mainPanel;
    private JPanel rightPanelContainer;
    private JLabel connectionStatusLabel;

    public StompStatusComponent(FilterModel filterModel) {

        // Create an EmptyBorder with desired margins (top, left, bottom, right)
        int topMargin = 7;
        int leftMargin = 0;
        int bottomMargin = 7;
        int rightMargin = 7;
        EmptyBorder marginBorder = JBUI.Borders.empty(topMargin, leftMargin, bottomMargin, rightMargin);

        // Apply the margin border to the panel
        rightPanelContainer.setBorder(marginBorder);

        setDisconnected();
    }

    AnimatedIcon connectedIconAnimated = new AnimatedIcon(
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


    public synchronized void setConnected() {
        connectionStatusLabel.setText("Connected");
        connectionStatusLabel.setForeground(new Color(31, 138, 60));
        connectionStatusLabel.setIcon(connectedIconAnimated);
    }

    public synchronized void setDisconnected() {
        connectionStatusLabel.setText("Disconnected");
        connectionStatusLabel.setForeground(new Color(160, 174, 192));
        connectionStatusLabel.setIcon(UIUtils.DISCONNECTED_ICON);
    }

    public synchronized void addRightStatus(String id, String value) {
        if (rightPanelMapById.containsKey(id)) {
            JLabel existingPanel = rightPanelMapById.get(id);
            existingPanel.setText(value);
        } else {
            JLabel newPanel = new JLabel(value);
            Font currentFont = newPanel.getFont();
            Font newFont = new Font(currentFont.getName(), Font.PLAIN, 12); // 20 is the desired font size
            newPanel.setForeground(new JBColor(Gray._140, Gray._140));
            rightPanelMapById.put(id, newPanel);
            if (rightPanelContainer.getComponentCount() > 0) {
                JSeparator separator = new JSeparator(JSeparator.VERTICAL);
                separator.setPreferredSize(new Dimension(2, 20)); // Adjust the width and height as needed
                rightPanelContainer.add(separator);
            }
            rightPanelContainer.add(newPanel);
        }
    }

    public synchronized void removeRightStatus(String id) {
        if (rightPanelMapById.containsKey(id)) {
            JLabel existingPanel = rightPanelMapById.get(id);
            rightPanelContainer.remove(existingPanel);
            rightPanelContainer.revalidate();
            rightPanelContainer.repaint();
        }
    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
