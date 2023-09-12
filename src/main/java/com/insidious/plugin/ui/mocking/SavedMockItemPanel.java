package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.mocking.DeclaredMock;
import com.intellij.ui.components.OnOffButton;

import javax.swing.*;
import java.awt.*;

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

    public SavedMockItemPanel(DeclaredMock declaredMock) {
        this.declaredMock = declaredMock;
        enableDisableSwitchPanel.add(new OnOffButton(), BorderLayout.CENTER);
    }

    public Component getComponent() {
        return mainPanel;
    }
}
