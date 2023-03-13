package com.insidious.plugin.ui.Components;

import com.insidious.plugin.ui.UIUtils;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.concurrency.EdtExecutorService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GPTChatScaffold {
    private JPanel mainPanel;
    private JPanel bottomPanel;
    private JPanel bottomControls;
    private JTextArea promptTextArea;
    private JButton sendButton;
    private JPanel borderParent;
    private JPanel centerPanel;
    private JPanel topPanel;
    private JPasswordField apiKeyTextField;
    private JLabel apiKeyLabel;
    private JScrollPane parentScroller;
    private JScrollPane TextScrollPanel;

    public JPanel getComponent()
    {
        return this.mainPanel;
    }
    private ScrollablePanel scrollablePanel;
    private UnloggedGptListener listener;
    private JScrollPane scrollPane;
    private boolean firstCall=false;
    public GPTChatScaffold(UnloggedGptListener listener)
    {
        this.listener = listener;
        scrollablePanel = new ScrollablePanel();
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
        scrollPane = new JBScrollPane();
        scrollPane = new JBScrollPane();
        //scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(scrollablePanel);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);

        this.borderParent.removeAll();
        borderParent.add(scrollPane, BorderLayout.CENTER);
        this.borderParent.revalidate();

        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if(sendButton.isEnabled())
                {
                    sendPrompt();
                }
            }
        });
        sendDefaultMessage();
    }

    public void sendDefaultMessage()
    {
        addNewMessage(
                "Please add your openAPI API key to get started. If you don't have an account please sign up.",
                "ChatGPT",false);
        this.firstCall=true;
    }

    public void clearChatWindow()
    {
        scrollablePanel.removeAll();
    }

    public void sendPrompt()
    {
        String currentPrompt = this.promptTextArea.getText();
        if(!currentPrompt.trim().isEmpty())
        {
            listener.makeApiCallForPrompt(currentPrompt);
        }
    }

    public void addNewMessage(String message, String user, boolean enableCopyButton)
    {
        if(this.firstCall)
        {
            clearChatWindow();
            this.firstCall=false;
        }
        GPTChatElement element = new GPTChatElement(message,user);
        element.enableCopyButton(enableCopyButton);
        JPanel contentPanel = element.getMainPanel();
        this.scrollablePanel.add(contentPanel);
        this.mainPanel.revalidate();
        this.mainPanel.repaint();
        listener.triggerUpdate();
        this.mainPanel.revalidate();
        this.mainPanel.repaint();
    }

    public String getAPIkey()
    {
        return this.apiKeyTextField.getText().trim();
    }

    public void scrollToBottom() {
        JScrollBar verticalBar = this.scrollPane.getVerticalScrollBar();
        verticalBar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                Adjustable adjustable = e.getAdjustable();
                adjustable.setValue(adjustable.getMaximum());
                verticalBar.removeAdjustmentListener(this);
            }
        });
    }

    public void scrollToBottomV2()
    {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue( vertical.getMaximum());
    }

    public void setLoadingButtonState() {
        this.sendButton.setEnabled(false);
        UIUtils.setGifIconForButton(this.sendButton, "loading-def.gif", UIUtils.SEND_TEAL_ICON);
    }

    public void setReadyButtonState() {
        this.sendButton.setEnabled(true);
        this.sendButton.setIcon(UIUtils.SEND_TEAL_ICON);
    }

    public void resetPrompt() {
        this.promptTextArea.setText("");
    }
}
