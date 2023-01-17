package com.insidious.plugin.ui.Components;

import com.insidious.plugin.ui.UI_Utils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class WaitingStateComponent {
    private JPanel mainPanel;
    private JPanel containerCenter;
    private JPanel topPanel;
    private JLabel iconLabel;
    private JLabel headingLabel;
    private JButton proceedButton;
    private JLabel bodyLabel;
    private OnboardingStateManager stateManager;
    private WAITING_COMPONENT_STATES currentState;

    public enum WAITING_COMPONENT_STATES
    {
        WAITING_FOR_LOGS,
        SWITCH_TO_LIVE_VIEW,
        SWITCH_TO_DOCUMENTATION,
        SWITCH_TO_DEPENDENCY_MANAGEMENT,
        AWAITING_DEPENDENCY_ADDITION
    }

    public JPanel getComponent()
    {
        return mainPanel;
    }

    public WaitingStateComponent(WAITING_COMPONENT_STATES state, OnboardingStateManager stateManager)
    {
        this.stateManager=stateManager;
        this.currentState=state;
        setState(state);
        this.proceedButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        proceedButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleTransition();
            }
        });
    }

    public void handleTransition()
    {
        switch (this.currentState)
        {
            case WAITING_FOR_LOGS:
                System.out.println("Nothing to do");
                break;
            case AWAITING_DEPENDENCY_ADDITION:
                if(stateManager.canGoToDocumentation())
                {
                    this.currentState=WAITING_COMPONENT_STATES.SWITCH_TO_DOCUMENTATION;
                    setState(WAITING_COMPONENT_STATES.SWITCH_TO_DOCUMENTATION);
                }
                break;
            case SWITCH_TO_DOCUMENTATION:
                //transitionToDocs
                System.out.println("Switching to Documentation view, waiting for logs");
                this.currentState=WAITING_COMPONENT_STATES.WAITING_FOR_LOGS;
                setState(WAITING_COMPONENT_STATES.WAITING_FOR_LOGS);
                stateManager.transistionToState(WAITING_COMPONENT_STATES.WAITING_FOR_LOGS);
                break;
            case SWITCH_TO_DEPENDENCY_MANAGEMENT:
                //switch to dep mgmt
                break;
            case SWITCH_TO_LIVE_VIEW:
                System.out.println("Switching to live view");
                stateManager.transistionToState(WAITING_COMPONENT_STATES.SWITCH_TO_LIVE_VIEW);
        }
    }

    public void setState(WAITING_COMPONENT_STATES state)
    {
        switch (state)
        {
            case WAITING_FOR_LOGS:
                UI_Utils.setGifIconForLabel(this.iconLabel,"clock_animated.gif",UI_Utils.WAITING_COMPONENT_WAITING);
                this.headingLabel.setText("Waiting for logs");
                this.bodyLabel.setText("<html><body style='text-align: center'>After agent is added, send data to your application using <br>Postman, Swagger or UI.</body></html>");
                setButtonState(false);
                setMainPanelBorder(UI_Utils.yellow_alert);
                stateManager.checkForSelogs();
                return;
            case SWITCH_TO_LIVE_VIEW:
                UI_Utils.setGifIconForLabel(this.iconLabel,"checkmark_animated.gif",UI_Utils.WAITING_COMPONENT_SUCCESS);
                this.headingLabel.setText("Ready to Generate!");
                this.bodyLabel.setText("We have a few cases ready to generate.");
                setButtonState(true);
                setMainPanelBorder(UI_Utils.teal);
                return;
            case AWAITING_DEPENDENCY_ADDITION:
                UI_Utils.setGifIconForLabel(this.iconLabel,"clock_animated.gif",UI_Utils.WAITING_COMPONENT_WAITING);
                this.headingLabel.setText("Missing dependencies");
                this.bodyLabel.setText("Please add the dependencies to proceed.");
                setButtonState(false);
                setMainPanelBorder(UI_Utils.yellow_alert);
                return;
            case SWITCH_TO_DOCUMENTATION:
                UI_Utils.setGifIconForLabel(this.iconLabel,"checkmark_animated.gif",UI_Utils.WAITING_COMPONENT_SUCCESS);
                this.headingLabel.setText("Proceed to Documentation");
                this.bodyLabel.setText("<html><body style='text-align: center'>Dependencies added, <br>please sync your project and proceed</body></html>");
                setButtonState(true);
                setMainPanelBorder(UI_Utils.teal);
        }
    }

    private void setMainPanelBorder(Color color)
    {
        this.mainPanel.setBorder(new LineBorder(color));
    }
    private void setButtonState(boolean status)
    {
        this.proceedButton.setEnabled(status);
        if(!status)
        {
            this.proceedButton.setBorderPainted(true);
            this.proceedButton.setContentAreaFilled(true);
            this.proceedButton.setOpaque(false);
        }
        else
        {
            this.proceedButton.setBorderPainted(false);
            this.proceedButton.setContentAreaFilled(false);
            this.proceedButton.setOpaque(true);
        }
    }

}
