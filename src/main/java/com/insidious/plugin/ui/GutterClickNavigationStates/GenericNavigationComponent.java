package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.factory.InsidiousService;

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
    private InsidiousService.GUTTER_STATE currentState;
    public GenericNavigationComponent(InsidiousService.GUTTER_STATE state)
    {
        this.currentState = state;
        setTopText(state.toString());
        if(state.equals(InsidiousService.GUTTER_STATE.NO_AGENT))
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
        if(this.currentState.equals(InsidiousService.GUTTER_STATE.NO_AGENT))
        {
            System.out.println("Download Agent triggered");
        }
    }

}
