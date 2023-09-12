package com.insidious.plugin.ui.mocking;

import com.intellij.ui.components.OnOffButton;

import javax.swing.*;
import java.awt.*;

public class SavedMockItemPanel {
    private JPanel mainPanel;
    private JPanel mockNamePanel;
    private JLabel mockNameLabel;
    private JPanel mockDefinitionMetadata;
    private JLabel mockMetadataLabel;
    private JPanel enableDisableSwitchPanel;
    private JPanel westPanel;
    private JPanel eastPanel;

    public SavedMockItemPanel() {

        enableDisableSwitchPanel.add(new OnOffButton(), BorderLayout.CENTER);
    }

    public Component getComponent() {
        return mainPanel;
    }
}
