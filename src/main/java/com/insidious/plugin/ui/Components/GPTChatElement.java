package com.insidious.plugin.ui.Components;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.NotificationType;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

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
        if(user.equals("Alpaca"))
        {
            this.userInfoLabel.setIcon(UIUtils.TEST_TUBE_FILL);
            this.messageText.setBackground(UIUtils.black_custom);
            this.messageText.setForeground(UIUtils.teal);
        }
        else
        {
            this.userInfoLabel.setIcon(UIUtils.UNLOGGED_GPT_ICON_PINK);
            this.messageText.setBackground(UIUtils.black_custom);
            this.messageText.setForeground(UIUtils.teal);
        }
        copyButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                copyContents();
            }
        });
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

    public JTextArea getTextArea()
    {
        return this.messageText;
    }

    public void copyContents()
    {
        String message = this.messageText.getText();
        StringSelection selection = new StringSelection(message);
        Clipboard clipboard = Toolkit.getDefaultToolkit()
                .getSystemClipboard();
        clipboard.setContents(selection, selection);
        InsidiousNotification.notifyMessage("Copied contents.",
                NotificationType.INFORMATION);
    }

    public void enableCopyButton(boolean status)
    {
        this.copyButton.setVisible(status);
    }
}
