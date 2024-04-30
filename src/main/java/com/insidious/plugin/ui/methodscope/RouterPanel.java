package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RouterPanel {
    private JLabel executeMethodRouteLabel;
    private JLabel runReplayTests;
    private JLabel filterInTimeline;
    private JLabel mockCallsLabel;
    private JLabel boilerplateLabel;
    private JLabel loadTestLabel;
    private JLabel fuzzyTestLabel;
    private JPanel mainPanel;

    public RouterPanel(RouterListener routerListener) {
        executeMethodRouteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        executeMethodRouteLabel.setIcon(AllIcons.Actions.Execute);
        filterInTimeline.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterInTimeline.setIcon(AllIcons.General.Filter);
        runReplayTests.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runReplayTests.setIcon(AllIcons.Actions.Restart);

        mockCallsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mockCallsLabel.setIcon(UIUtils.GHOST_MOCK);

        boilerplateLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boilerplateLabel.setIcon(AllIcons.Scope.Tests);
        loadTestLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadTestLabel.setIcon(AllIcons.Scope.Tests);
        fuzzyTestLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fuzzyTestLabel.setIcon(AllIcons.Scope.Tests);


        // 4
        executeMethodRouteLabel.setBorder(BorderFactory.createCompoundBorder(executeMethodRouteLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        mockCallsLabel.setBorder(BorderFactory.createCompoundBorder(executeMethodRouteLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        filterInTimeline.setBorder(BorderFactory.createCompoundBorder(filterInTimeline.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        runReplayTests.setBorder(BorderFactory.createCompoundBorder(runReplayTests.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));


        // 3

        boilerplateLabel.setBorder(BorderFactory.createCompoundBorder(boilerplateLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        loadTestLabel.setBorder(BorderFactory.createCompoundBorder(loadTestLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        fuzzyTestLabel.setBorder(BorderFactory.createCompoundBorder(fuzzyTestLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));


        executeMethodRouteLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routerListener.showDirectInvoke();

            }
        });


        filterInTimeline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routerListener.showStompAndFilterForMethod();
            }
        });

    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
