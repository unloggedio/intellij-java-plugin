package com.insidious.plugin.ui.Components;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class NavigationElement {
    private JPanel mainPanel;
    private JLabel navigationStageText;
    private JLabel rightIcon;

    private NavigationManager navigationManager;

    public NavigationElement(NavigationManager navigationManager)
    {
        this.navigationManager=navigationManager;
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                triggerNavigate();
            }
        });
    }

    void triggerNavigate()
    {
        this.navigationManager.NavigateToState(this.navigationStageText.getText());
    }

    public JPanel getComponent()
    {
        return mainPanel;
    }

    public void setNavigationStageText(String text)
    {
        this.navigationStageText.setText(text);
    }

    public void setNumberIcon(Icon icon)
    {
        this.navigationStageText.setIcon(icon);
    }
}
