package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.testFramework.LightVirtualFile;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ExceptionOptionsComponent {
    private JPanel mainPanel;
    private JPanel buttonControlPanel;
    private JButton showfulltrace;
    private JButton accept;
    private JTextArea exceptionArea;
    private JLabel iconLabel;
    private JPanel topPanel;
    private JPanel topAligner;
    private String message;
    private String stackTrace;

    private InsidiousService service;

    public ExceptionOptionsComponent(String message, String stacktrace, InsidiousService insidiousService) {
        this.message = message;
        this.stackTrace = stacktrace;
        this.service = insidiousService;
        this.exceptionArea.setText(message);
        this.iconLabel.setIcon(UIUtils.EXCEPTION_CASE);
        showfulltrace.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                displayFullStackTrace();
            }
        });
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void displayFullStackTrace() {
        LightVirtualFile file = new LightVirtualFile("Exception trace", PlainTextFileType.INSTANCE, stackTrace);
        file.setWritable(false);
        FileEditorManager.getInstance(service.getProject()).openFile(file, true);
    }
}
