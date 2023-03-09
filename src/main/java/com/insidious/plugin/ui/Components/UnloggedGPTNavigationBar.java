package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.UIUtils;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UnloggedGPTNavigationBar {
    private JPanel mainPanel;
    private JPanel centerPanel;
    private JButton findBugsButton;
    private JButton optimizeButton;
    private JButton refactorButton;
    private JButton explainButton;
    private JPanel elementsParent;
    private JPanel selectedClassDetailsPanel;
    private JLabel selectedMethodNameLabel;
    private JButton refreshButton;
    private JPanel controlPanel;
    private JButton backButton;
    private UnloggedGptListener listener;

    public UnloggedGPTNavigationBar(UnloggedGptListener listener) {
        this.listener=listener;
        findBugsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(findBugsButton.isEnabled()) {
                    sendUpdateRequest(findBugsButton.getText());
                }
            }
        });
        findBugsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        optimizeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(optimizeButton.isEnabled()) {
                    sendUpdateRequest(optimizeButton.getText());
                }
            }
        });
        optimizeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refactorButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(refactorButton.isEnabled()) {
                    sendUpdateRequest(refactorButton.getText());
                }
            }
        });
        refactorButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        explainButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(explainButton.isEnabled()) {
                    sendUpdateRequest(explainButton.getText());
                }
            }
        });
        explainButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                refreshPage();
            }
        });
        refactorButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                goBack();
            }
        });
    }

    public JPanel getComponent()
    {
        return mainPanel;
    }

    public void sendUpdateRequest(String type)
    {
        JSONObject eventProperties = new JSONObject();
        eventProperties.put("option_type", type);
        UsageInsightTracker.getInstance().RecordEvent(
                "GPTOptionClicked",eventProperties);
        listener.triggerCallTypeForCurrentMethod(type);
    }

    public void updateSelection(String s) {
        this.selectedMethodNameLabel.setText(s);
    }

    public void refreshPage()
    {
        listener.refreshPage();
    }

    public void goBack()
    {
        listener.goBack();
    }

    public void setControlPanelVisibility(boolean status)
    {
        this.controlPanel.setVisible(status);
    }

    public void setActionButtonLoadingState(String type) {
        JButton button = getButtonForType(type);
        if(button!=null)
        {
            UIUtils.setGifIconForButton(button, "loading-def.gif", button.getIcon());
        }
    }

    private JButton getButtonForType(String type)
    {
        switch (type)
        {
            case "Find Bugs":
                return findBugsButton;
            case "Optimize":
                return optimizeButton;
            case "Refactor":
                return refactorButton;
            case "Explain":
                return explainButton;
        }
        return null;
    }

    private Icon findIconForType(String type)
    {
        switch (type)
        {
            case "Find Bugs":
                return UIUtils.FIND_BUGS_GREY;
            case "Optimize":
                return UIUtils.OPTIMIZE_GREY;
            case "Refactor":
                return UIUtils.REFACTOR_GREY;
            case "Explain":
                return UIUtils.EXPLAIN_GREY;
        }
        return null;
    }
    public void setActionButtonReadyState(String type) {
        JButton button = getButtonForType(type);
        if(button!=null)
        {
            button.setIcon(findIconForType(type));
        }
    }
}
