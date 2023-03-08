package com.insidious.plugin.ui.Components;

import com.insidious.plugin.ui.UIUtils;

import javax.swing.*;

public class GPTChatElement {
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel topPanel;
    private JPanel centerPanel;
    private JPanel rightPanel;
    private JTextArea messageText;
    private JButton copyButton;
    private JLabel userInfoLabel;

    public GPTChatElement(String message, String user)
    {
        this.messageText.setText(message);
        this.userInfoLabel.setText(user);
        if(!user.equals("You"))
        {
            this.userInfoLabel.setIcon(UIUtils.UNLOGGED_GPT_ICON_PINK);
            this.messageText.setBackground(UIUtils.NeutralGrey);
        }
    }

    public JPanel getMainPanel()
    {
        return this.mainPanel;
    }

    public void setMessageText(String text)
    {
        this.messageText.setText(text);
    }

    public void setUser(String text)
    {
        this.userInfoLabel.setText(text);
    }
}
