package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.factory.GutterState;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GenericNavigationComponent {
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel topTextPanel;
    private JLabel iconLabel;
    private JTextArea topStatusText;
    private JButton actionButton;
    private JTextArea mainContentText;
    private JPanel demoImageHolder;
    private GutterState currentState;
    public GenericNavigationComponent(GutterState state)
    {
        this.currentState = state;
        setTopText(state.toString());
        if(state.equals(GutterState.NO_AGENT))
        {
            //display button
            this.actionButton.setVisible(true);
            this.actionButton.setText("Download Agent");
            actionButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    triggerAgentDownload();
                }
            });
        }
    }

    public JPanel getComponent()
    {
        return this.mainPanel;
    }

    public void setTopText(String text)
    {
        this.topStatusText.setText(text);
    }

    public void triggerAgentDownload()
    {
        if(this.currentState.equals(GutterState.NO_AGENT))
        {
            System.out.println("Download Agent triggered");
        }
    }

}
