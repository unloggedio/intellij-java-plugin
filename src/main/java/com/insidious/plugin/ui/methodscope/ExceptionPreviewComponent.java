package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.Components.AtomicRecord.AtomicRecordListener;
import com.insidious.plugin.ui.Components.AtomicRecord.SaveForm;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.testFramework.LightVirtualFile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.*;

public class ExceptionPreviewComponent {
    private final String message;
    private final String stackTrace;
    private final InsidiousService service;
    private JPanel mainPanel;
    private JPanel buttonControlPanel;
    private JButton showfulltrace;
    private JButton accept;
    private JTextArea exceptionArea;
    private JLabel iconLabel;
    private JPanel topPanel;
    private JPanel topAligner;
    private JButton deleteButton;
    private SaveForm saveForm;
    private StoredCandidate candidate;
    public ExceptionPreviewComponent(String message, String stacktrace, InsidiousService insidiousService,
                                     AtomicRecordListener listener, boolean showSave, boolean showDelete,
                                     StoredCandidate candidate) {
        this.message = message;
        this.stackTrace = stacktrace;
        this.service = insidiousService;
        this.candidate=candidate;
        
        this.exceptionArea.setText(message);
        if(!showSave)
        {
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
                if(saveForm!=null){
                    saveForm.dispose();
                }
                saveForm = new SaveForm(listener);
                saveForm.setStoredCandidate(candidate);
                saveForm.setVisible(true);
            }
        });

        if(!showDelete)
        {
            this.deleteButton.setVisible(false);
        }
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.deleteCandidateRecord();
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
        titledBorder.setTitle(title);
    }
}
