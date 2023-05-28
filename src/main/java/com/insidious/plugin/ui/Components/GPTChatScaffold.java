package com.insidious.plugin.ui.Components;

import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

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
    private JRadioButton chatGPTRadioButton;
    private JRadioButton selfHostedAlpacaRadioButton;
    private JPanel buttonPanel;
    private JPanel textPanel;
    private JButton modeSelectButton;
    private ScrollablePanel scrollablePanel;
    private UnloggedGptListener listener;
    private JScrollPane scrollPane;
    private boolean firstCall = false;
    private char defaultChar = apiKeyTextField.getEchoChar();
    private API_MODE apiMode = API_MODE.CHAT_GPT;
    public enum API_MODE {CHAT_GPT,ALPACA}

    ButtonGroup radioGroup = new ButtonGroup();
    public GPTChatScaffold(UnloggedGptListener listener) {

        radioGroup.add(chatGPTRadioButton);
        radioGroup.add(selfHostedAlpacaRadioButton);

        chatGPTRadioButton.setSelected(true);

        this.listener = listener;
        scrollablePanel = new ScrollablePanel();
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
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

                if (sendButton.isEnabled()) {
                    sendPrompt();
                }
            }
        });
        sendDefaultMessage();
        chatGPTRadioButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateDisplayForState();
            }
        });
        selfHostedAlpacaRadioButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateDisplayForState();
            }
        });
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void sendDefaultMessage() {
        addNewMessage(
                "Please add your openAPI API key to get started. If you don't have an account please sign up.",
                "ChatGPT", false);
        this.firstCall = true;
    }

    public void clearChatWindow() {
        scrollablePanel.removeAll();
    }

    public void sendPrompt() {
        String currentPrompt = this.promptTextArea.getText();
        if (!currentPrompt.trim().isEmpty()) {
            listener.makeApiCallForPrompt(currentPrompt);
        }
    }

    public void addNewMessage(String message, String user, boolean enableCopyButton) {
        if (this.firstCall) {
            clearChatWindow();
            this.firstCall = false;
        }
        GPTChatElement element = new GPTChatElement(message, user);
        element.enableCopyButton(enableCopyButton);
        JPanel contentPanel = element.getMainPanel();
        this.scrollablePanel.add(contentPanel);
        this.mainPanel.revalidate();
        this.mainPanel.repaint();
        listener.triggerUpdate();
        this.mainPanel.revalidate();
        this.mainPanel.repaint();
    }

    public String getAPIkey() {
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

    public void scrollToBottomV2() {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
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

    public void updateDisplayForState()
    {
        String selectionText = "GPT";
        for (Enumeration<AbstractButton> buttons = radioGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();

            if (button.isSelected()) {
                selectionText = button.getText();
            }
        }
        System.out.println("Selected : "+selectionText);
        if(selectionText.contains("GPT"))
        {
            this.apiMode = API_MODE.CHAT_GPT;
            this.apiKeyTextField.setEchoChar(defaultChar);

        }
        else
        {
            this.apiMode = API_MODE.ALPACA;
            this.apiKeyTextField.setEchoChar((char) 0);
        }

        if(this.apiMode.equals(API_MODE.CHAT_GPT))
        {
            this.apiKeyLabel.setText("ChatGPT API key");
        }
        else
        {
            this.apiKeyLabel.setText("Your Alpaca Endpoint");

        }
        this.topPanel.revalidate();
    }

    public API_MODE getApiMode()
    {
        return this.apiMode;
    }

    public String getTextFieldContent()
    {
        return this.apiKeyTextField.getText();
    }
}
