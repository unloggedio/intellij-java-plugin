package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.DeclaredMock;
import com.intellij.ui.components.OnOffButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SavedMockItemPanel {
    private final DeclaredMock declaredMock;
    private JPanel mainPanel;
    private JPanel mockNamePanel;
    private JLabel mockNameLabel;
    private JPanel mockDefinitionMetadata;
    private JLabel mockMetadataLabel;
    private JPanel enableDisableSwitchPanel;
    private JPanel westPanel;
    private JPanel eastPanel;
    private JLabel deleteButton;
    private JLabel editButton;

    public SavedMockItemPanel(DeclaredMock declaredMock, UpdateDeleteListener<DeclaredMock> updateDeleteListener) {
        this.declaredMock = declaredMock;
        enableDisableSwitchPanel.add(new OnOffButton(), BorderLayout.CENTER);
        mockNameLabel.setText(declaredMock.getName());
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateDeleteListener.onDeleteRequest(declaredMock);
            }
        });
        editButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateDeleteListener.onUpdateRequest(declaredMock);
            }
        });
    }

    public Component getComponent() {
        return mainPanel;
    }
}
