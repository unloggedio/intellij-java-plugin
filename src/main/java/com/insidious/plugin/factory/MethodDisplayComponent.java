package com.insidious.plugin.factory;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.util.UIUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MethodDisplayComponent {
    private MethodAdapter methodAdapter;
    private JPanel mainPanel;
    private JLabel methodNameLabel;
    private JLabel selectedRouterInfoPanel;
    private JButton backButton;

    public MethodDisplayComponent(InsidiousService insidiousService) {
        methodNameLabel.setIcon(UIUtils.EXECUTE);
        backButton.setVisible(false);
        backButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                backButton.setVisible(false);
                selectedRouterInfoPanel.setText("");
                insidiousService.showRouter();
            }
        });
    }

    public void showBackButtonVisible(boolean visible) {
        backButton.setVisible(visible);
    }

    public void setMethod(MethodAdapter methodAdapter) {
        methodNameLabel.setIcon(null);
        this.methodAdapter = methodAdapter;
        String labelText = methodAdapter.getName();
        if (labelText.length() > 30) {
            labelText = labelText.substring(0, 27) + "...";
        }
        methodNameLabel.setText("<html>" + labelText + "</html>");
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void setDescription(String description) {
        selectedRouterInfoPanel.setText(description);
    }
}
