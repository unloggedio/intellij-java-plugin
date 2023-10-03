package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.mocking.MethodExitType;
import com.insidious.plugin.mocking.ReturnValue;
import com.insidious.plugin.mocking.ThenParameter;
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

    public SavedMockItemPanel(DeclaredMock declaredMock,
                              DeclaredMockLifecycleListener declaredMockLifecycleListener,
                              boolean mockEnabled) {
        this.declaredMock = declaredMock;
        OnOffButton onOffButton = new OnOffButton();
        enableDisableSwitchPanel.add(onOffButton, BorderLayout.CENTER);
        if (mockEnabled) {
            onOffButton.setSelected(true);
        }

        onOffButton.addActionListener(e -> {
            if (onOffButton.isSelected()) {
                declaredMockLifecycleListener.onEnable(declaredMock);
            } else {
                declaredMockLifecycleListener.onDisable(declaredMock);
            }
        });

        ThenParameter thenParameter = declaredMock.getThenParameter().get(0);
        ReturnValue returnParameter = thenParameter.getReturnParameter();
        if (thenParameter.getMethodExitType() == MethodExitType.NORMAL) {
        }
        switch (thenParameter.getMethodExitType()) {
            case NORMAL:
                mockMetadataLabel.setText("Returns " + returnParameter.getClassName());
                break;
            case EXCEPTION:
                mockMetadataLabel.setText("Throws " + returnParameter.getClassName());
                break;
            case NULL:
                mockMetadataLabel.setText("Returns null");
                break;
        }

        mockNameLabel.setText(declaredMock.getName());
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                declaredMockLifecycleListener.onDeleteRequest(declaredMock);
            }
        });
        editButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                declaredMockLifecycleListener.onUpdateRequest(declaredMock);
            }
        });
    }

    public Component getComponent() {
        return mainPanel;
    }
}
