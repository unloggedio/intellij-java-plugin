package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.callbacks.CandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.Components.AtomicRecord.SaveForm;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.testFramework.LightVirtualFile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;

public class ExceptionPreviewComponent {
    private final String message;
    private final String stackTrace;
    private final InsidiousService service;
    private JPanel mainPanel;
    private JPanel scrollGrid;
    private JButton showfulltrace;
    private JButton accept;
    private JTextArea exceptionArea;
    private JLabel iconLabel;
    private JButton deleteButton;
    private JPanel topAligner;
    private JPanel topPanel;
    private JPanel buttonControlPanel;
    private JPanel bottomPanel;
    private SaveForm saveForm;
    private StoredCandidate candidate;
    private AgentCommandResponse<String> agentCommandResponse;

    public ExceptionPreviewComponent(String message, String stacktrace, InsidiousService insidiousService,
                                     CandidateLifeListener listener, boolean showSave, boolean showDelete,
                                     StoredCandidate candidate, AgentCommandResponse<String> agentCommandResponse) {
        this.message = message;
        this.stackTrace = stacktrace;
        this.service = insidiousService;
        this.candidate = candidate;
        this.agentCommandResponse = agentCommandResponse;

        this.exceptionArea.setText(message);
        if (!showSave) {
            this.accept.setVisible(false);
        }

        this.iconLabel.setIcon(UIUtils.EXCEPTION_CASE);
        this.iconLabel.setIcon(UIUtils.ORANGE_EXCEPTION);
        showfulltrace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayFullStackTrace();
            }
        });
        accept.addKeyListener(new KeyAdapter() {
        });
        accept.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.onSaveRequest(candidate, agentCommandResponse);
            }
        });

        accept.setIcon(UIUtils.SAVE_CANDIDATE_GREEN_SVG);
        deleteButton.setIcon(UIUtils.DELETE_CANDIDATE_RED_SVG);
        if (!showDelete) {
            this.deleteButton.setVisible(false);
        }
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.onDeleted(candidate);
            }
        });
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void displayFullStackTrace() {
        if (stackTrace == null) {
            return;
        }
        LightVirtualFile file = new LightVirtualFile("Exception trace", PlainTextFileType.INSTANCE, stackTrace);
        file.setWritable(false);
        FileEditorManager.getInstance(service.getProject()).openFile(file, true);
    }

    public void setBorderTitle(String title) {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        Font currentFont = titledBorder.getTitleFont();
        Font currentFontBold = currentFont.deriveFont(Font.BOLD);
        titledBorder.setTitleFont(currentFontBold);
        titledBorder.setTitle(title);
    }
}
