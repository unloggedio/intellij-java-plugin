package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.MethodDisplayComponent;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RouterPanel implements ComponentProvider {
    private final MethodDisplayComponent methodDisplayComponent;
    private JLabel executeMethodRouteLabel;
    private JLabel runReplayTests;
    private JLabel filterInTimeline;
    private JLabel mockCallsLabel;
    private JLabel boilerplateLabel;
    private JLabel loadTestLabel;
    private JLabel fuzzyTestLabel;
    private JPanel mainPanel;
    private JLabel replayJunitTest;
    private JLabel setupInstructionsLabel;
    private JLabel requiredSdkInfoLabel;
    private JPanel methodInfoContainer;
    private JPanel withSdkPanel;
    private JPanel withoutSdkPanel;
    private MethodAdapter method;
    private boolean miniMode;

    public RouterPanel(RouterListener routerListener, InsidiousService insidiousService) {

        setupInstructionsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setupInstructionsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routerListener.showOnboardingInstructions();
            }
        });

        methodDisplayComponent = new MethodDisplayComponent(insidiousService);
        methodInfoContainer.add(methodDisplayComponent.getComponent(), BorderLayout.CENTER);

        loadTestLabel.setEnabled(false);
        loadTestLabel.setToolTipText("Coming soon");
        fuzzyTestLabel.setEnabled(false);
        fuzzyTestLabel.setToolTipText("Coming soon");


        executeMethodRouteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        executeMethodRouteLabel.setIcon(AllIcons.Actions.Execute);
        filterInTimeline.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        filterInTimeline.setIcon(AllIcons.General.Filter);
        runReplayTests.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runReplayTests.setIcon(AllIcons.Actions.Restart);

        mockCallsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mockCallsLabel.setIcon(UIUtils.GHOST_MOCK);

        replayJunitTest.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        replayJunitTest.setIcon(AllIcons.Scope.Tests);

        boilerplateLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boilerplateLabel.setIcon(AllIcons.Scope.Tests);
        loadTestLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadTestLabel.setIcon(AllIcons.Scope.Tests);
        fuzzyTestLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fuzzyTestLabel.setIcon(AllIcons.Scope.Tests);


        // 4
        executeMethodRouteLabel.setBorder(BorderFactory.createCompoundBorder(executeMethodRouteLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        mockCallsLabel.setBorder(BorderFactory.createCompoundBorder(mockCallsLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        filterInTimeline.setBorder(BorderFactory.createCompoundBorder(filterInTimeline.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        runReplayTests.setBorder(BorderFactory.createCompoundBorder(runReplayTests.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));


        // 3

        boilerplateLabel.setBorder(BorderFactory.createCompoundBorder(boilerplateLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        replayJunitTest.setBorder(BorderFactory.createCompoundBorder(replayJunitTest.getBorder(),
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

        mockCallsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routerListener.showMockCreator();
            }
        });

        replayJunitTest.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routerListener.showJunitFromRecordedCreator();
            }
        });


        filterInTimeline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routerListener.showStompAndFilterForMethod();
            }
        });

        boilerplateLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routerListener.showJunitCreator();
            }

        });

        runReplayTests.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routerListener.runReplayTests();
            }

        });

    }

    public void setMethod(MethodAdapter method) {
        this.method = method;
        methodDisplayComponent.setMethod(method);
        methodInfoContainer.removeAll();
        methodInfoContainer.add(methodDisplayComponent.getComponent(), BorderLayout.CENTER);
        setMiniMode(false);
    }


    public JPanel getComponent() {
        return mainPanel;
    }

    @Override
    public String getTitle() {
        return "";
    }

    public void setMiniMode(boolean b) {
        this.miniMode = b;
        if (this.miniMode) {
            withSdkPanel.setVisible(false);
            withoutSdkPanel.setVisible(false);
            methodDisplayComponent.showBackButtonVisible(true);
            mainPanel.setMaximumSize(new Dimension(-1, -1));
        } else {
            withSdkPanel.setVisible(true);
            withoutSdkPanel.setVisible(true);
            methodDisplayComponent.showBackButtonVisible(false);
            mainPanel.setMaximumSize(new Dimension(-1, 100));
        }
    }

    public void setTitle(String title) {
        methodDisplayComponent.setDescription(title);
    }
}
