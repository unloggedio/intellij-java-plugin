package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.UIUtils;

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
                UsageInsightTracker.getInstance()
                        .RecordEvent("ProceedToUnitTest", null);
                stateManager.transistionToState(WAITING_COMPONENT_STATES.SWITCH_TO_LIVE_VIEW);
        }
    }

    public void setState(WAITING_COMPONENT_STATES state)
    {
        switch (state)
        {
            case WAITING_FOR_LOGS:
                //UIUtils.setGifIconForLabel(this.iconLabel,"clock_animated.gif",UIUtils.WAITING_COMPONENT_WAITING);
                this.iconLabel.setIcon(UIUtils.WAITING_COMPONENT_WAITING);
                this.headingLabel.setText("Waiting for logs");
                this.bodyLabel.setText("<html><body style='text-align: center'>After the agent is added, send data to your application using <br>Postman, Swagger or UI.</body></html>");
                setButtonState(false);
                setMainPanelBorder(UIUtils.yellow_alert);
                stateManager.checkForSelogs();
                return;
            case SWITCH_TO_LIVE_VIEW:
                //UIUtils.setGifIconForLabel(this.iconLabel,"checkmark_animated.gif",UIUtils.WAITING_COMPONENT_SUCCESS);
                this.iconLabel.setIcon(UIUtils.WAITING_COMPONENT_SUCCESS);
                this.headingLabel.setText("Ready to Generate!");
                this.bodyLabel.setText("We have a few cases ready to generate.");
                setButtonState(true);
                setMainPanelBorder(UIUtils.teal);
                return;
            case AWAITING_DEPENDENCY_ADDITION:
                //UIUtils.setGifIconForLabel(this.iconLabel,"clock_animated.gif",UIUtils.WAITING_COMPONENT_WAITING);
                this.iconLabel.setIcon(UIUtils.WAITING_COMPONENT_WAITING);
                this.headingLabel.setText("Missing dependencies");
                this.bodyLabel.setText("Please add the dependencies to proceed.");
                setButtonState(false);
                setMainPanelBorder(UIUtils.yellow_alert);
                return;
            case SWITCH_TO_DOCUMENTATION:
                //UIUtils.setGifIconForLabel(this.iconLabel,"checkmark_animated.gif",UIUtils.WAITING_COMPONENT_SUCCESS);
                this.iconLabel.setIcon(UIUtils.WAITING_COMPONENT_SUCCESS);
                this.headingLabel.setText("Proceed to Documentation");
                this.bodyLabel.setText("<html><body style='text-align: center'>Dependencies added, <br>please sync your project and proceed</body></html>");
                setButtonState(true);
                setMainPanelBorder(UIUtils.teal);
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
