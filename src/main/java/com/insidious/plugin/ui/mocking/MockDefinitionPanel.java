package com.insidious.plugin.ui.mocking;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.components.OnOffButton;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.Random;

import static com.intellij.uiDesigner.core.GridConstraints.*;

public class MockDefinitionPanel {
    private static final Logger logger = LoggerUtil.getInstance(MockDefinitionPanel.class);
    private JPanel mockDefinitionTitlePanel;
    private JLabel mockedMethodText;
    private JLabel mockEnableSwitchLabel;
    private JPanel mockSwitchContainer;
    private JPanel mockSwitchPanel;
    private JButton addNewMockButton;
    private JPanel savedMocksListParent;
    private JPanel savedMocksTitlePanel;
    private JPanel newMockButtonPanel;
    private JPanel titleEastPanel;
    private JPanel titleWestPanel;
    private JPanel mainPanel;
    private JPanel titlePanelParent;
    private JPanel methodTextPanel;
    private JScrollPane savedItemScrollPanel;
    private JPanel scrollParent;

    public MockDefinitionPanel() {

        mockSwitchPanel.add(new OnOffButton(), BorderLayout.EAST);

        int savedCandidateCount = new Random(new Date().getTime()).nextInt(0, 10);

        JPanel itemListPanel = new JPanel();

        itemListPanel.setBorder(BorderFactory.createEmptyBorder());
        itemListPanel.setLayout(new GridLayout(savedCandidateCount, 1));
        itemListPanel.setAlignmentY(0);
        for (int i = 0; i < savedCandidateCount; i++) {
            SavedMockItemPanel savedMockItem = new SavedMockItemPanel();
            GridConstraints constraints = new GridConstraints(
                    i, 0, 1, 1, ANCHOR_NORTH,
                    GridConstraints.FILL_HORIZONTAL, SIZEPOLICY_CAN_GROW | SIZEPOLICY_CAN_SHRINK,
                    SIZEPOLICY_FIXED,
                    new Dimension(-1, 75),
                    new Dimension(-1, 75),
                    new Dimension(-1, 75)
            );
            Component component = savedMockItem.getComponent();
//            component.setMaximumSize(new Dimension(-1, 75));
//            component.setPreferredSize(new Dimension(-1, 75));
//            component.setMinimumSize(new Dimension(-1, 75));
            itemListPanel.add(component, constraints);
        }

//        JBScrollPane savedItemScrollPanel = new JBScrollPane(itemListPanel);
        savedItemScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        int containerHeight = Math.min(300, savedCandidateCount * 75);
        logger.warn("set container height: " + containerHeight);
        savedItemScrollPanel.getViewport().setSize(new Dimension(-1, containerHeight));
        savedItemScrollPanel.getViewport().setPreferredSize(new Dimension(-1, containerHeight));
        savedItemScrollPanel.setPreferredSize(new Dimension(-1, containerHeight));
        savedItemScrollPanel.setSize(new Dimension(-1, containerHeight));
//        savedItemScrollPanel.setMaximumSize(new Dimension(-1, containerHeight));
        savedItemScrollPanel.setViewportView(itemListPanel);
//        savedItemScrollPanel.revalidate();
//        savedItemScrollPanel.repaint();
//        if (containerHeight < 300) {
//            itemListPanel.setSize(new Dimension(-1, containerHeight));
//            itemListPanel.setPreferredSize(new Dimension(-1, containerHeight));
//            itemListPanel.setMaximumSize(new Dimension(-1, containerHeight));
//        }
//        scrollParent.add(savedItemScrollPanel, new GridConstraints());
//        scrollParent.setSize(new Dimension(-1, 500));


        // close by clicking something else
//        closeButton.addActionListener(e -> onClickListener.onClick());

    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
