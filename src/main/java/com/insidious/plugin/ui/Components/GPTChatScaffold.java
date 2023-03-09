package com.insidious.plugin.ui.Components;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.openapi.ui.playback.commands.ActionCommand;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;

public class GPTChatScaffold {
    private JPanel mainPanel;
    private JPanel bottomPanel;
    private JPanel bottomControls;
    private JTextArea promptTextArea;
    private JButton sendButton;
    private JPanel borderParent;
    private JPanel centerPanel;
    private JPanel topPanel;
    private JTextField apiKeyTextField;
    private JLabel apiKeyLabel;
    private JScrollPane parentScroller;
    private JScrollPane TextScrollPanel;

    public JPanel getComponent()
    {
        return this.mainPanel;
    }
    private ScrollablePanel scrollablePanel;
    private UnloggedGptListener listener;
    public GPTChatScaffold(UnloggedGptListener listener)
    {
        this.listener = listener;
        scrollablePanel = new ScrollablePanel();
        scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JBScrollPane();
        scrollPane = new JBScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(scrollablePanel);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);

        this.borderParent.removeAll();
        borderParent.add(scrollPane, BorderLayout.CENTER);
        this.borderParent.revalidate();

        sendButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendPrompt();
            }
        });
    }

    public void staticmockadd()
    {
        this.borderParent.removeAll();
        int GridRows = 15;
        GridLayout gridLayout = new GridLayout(GridRows, 1);
        gridLayout.setVgap(8);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        for (int i = 0; i < GridRows; i++) {
            GPTChatElement element = new GPTChatElement("Test message","User");
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            gridPanel.add(element.getMainPanel(), constraints);
        }
        JScrollPane scrollPane = new JBScrollPane(gridPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        borderParent.setPreferredSize(scrollPane.getSize());
        borderParent.add(scrollPane, BorderLayout.CENTER);
        this.borderParent.revalidate();
    }
    public void sendPrompt()
    {
        String currentPrompt = this.promptTextArea.getText();
        if(currentPrompt.trim().isEmpty())
        {
            System.out.println("Empty prompt");
        }
        else
        {
            //send upwards
            System.out.println("Adding new message");
            addNewMessage(currentPrompt,"You");
            String responseMessage = listener.makeApiCallForPrompt(currentPrompt);
            addNewMessage(responseMessage,"ChatGPT");
        }
    }

    //run this on edt thread to get it to display instantly.
    //edt thread updation doesn't work
    public void addNewMessage(String message, String user)
    {
        GPTChatElement element = new GPTChatElement(message,user);
        JPanel contentPanel = element.getMainPanel();
        this.scrollablePanel.add(contentPanel);
        this.mainPanel.revalidate();
        this.mainPanel.repaint();
        listener.triggerUpdate();
//        JScrollBar vertical = parentScroller.getVerticalScrollBar();
//        vertical.setValue(vertical.getMaximum());
    }

    public void runUpdateOnEDTthread(String prompt, String user)
    {
        EdtExecutorService.getInstance().execute(new Runnable() {

            @Override
            public void run() {
                addNewMessage(prompt,user);
            }
        });
    }

    public String getAPIkey()
    {
        return this.apiKeyTextField.getText().trim();
    }
}
