package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.UIUtils;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class NavigatorComponent implements NavigationManager {
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel BorderParentPanel;
    private JPanel actionsPanel;
    private JButton supportButton;
    private OnboardingScaffoldV3 scaffold;
    private ArrayList<String> states = new ArrayList<>();
    private ArrayList<NavigationElement> elements = new ArrayList<>();
    private int currentState = 0;

    public NavigatorComponent(OnboardingScaffoldV3 onboardingScaffoldV3) {
        this.scaffold = onboardingScaffoldV3;
        states = new ArrayList<>();
        states.add("Module Selection");
        states.add("JDK and Json serializer");
        states.add("Required Dependencies");
        states.add("Run!");

        this.borderParent.removeAll();
        this.elements = new ArrayList<>();
        GridLayout gridLayout = new GridLayout(1, 30);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        for (int i = 0; i < states.size(); i++) {
            NavigationElement element = new NavigationElement(this);
            element.setNavigationStageText(states.get(i));
            element.setNumberIcon(UIUtils.getNumberedIconFor(i + 1));
            elements.add(element);
            GridConstraints constraints = new GridConstraints();
            constraints.setColumn(i);
            gridPanel.add(element.getComponent(), constraints);
        }
        this.borderParent.add(gridPanel, BorderLayout.CENTER);
        this.borderParent.revalidate();
        supportButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });
        supportButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    @Override
    public void NavigateToState(String state) {
        System.out.println("Navigating to " + state);
        loadState(state);
    }

    public void loadState(String state) {
        switch (state) {
            case "Module Selection":
                currentState = 0;
                scaffold.setDividerLocation(440);
                scaffold.loadModuleSection();
                break;
            case "Required Dependencies":
                currentState = 2;
                scaffold.setDividerLocation(1020);
                scaffold.loadDependenciesManagementSection();
                break;
            case "JDK and Json serializer":
                currentState = 1;
                scaffold.setDividerLocation(440);
                scaffold.loadProjectConfigSection();
                break;
            case "Run!":
                currentState = 3;
                scaffold.setDividerLocation(400);
                scaffold.loadRunSection(scaffold.checkIfLogsArePresent());
                break;
        }
    }

    public void loadNextState() {
        if (currentState + 1 < this.states.size()) {
            currentState++;
            loadState(this.states.get(currentState));
        }
    }

    public void refreshOptions() {
        for (int i = 0; i < this.elements.size(); i++) {
            if (i < currentState) {
                elements.get(i)
                        .getComponent()
                        .setVisible(false);
            } else {
                elements.get(i)
                        .getComponent()
                        .setVisible(true);
            }
        }
    }

    public void setNavElementVisibleStates(boolean state) {
        for (int i = 0; i < this.elements.size(); i++) {
            elements.get(i)
                    .getComponent()
                    .setVisible(state);
        }
    }

    public void restartOnboarding() {
        this.currentState = 0;
        setNavElementVisibleStates(true);
        loadState(this.states.get(currentState));
        UsageInsightTracker.getInstance()
                .RecordEvent("StartOverTriggered", null);
    }

    public boolean shouldReloadDocumentation() {
        if (currentState == 0) {
            return true;
        }
        return false;
    }

    public boolean shouldReloadRun() {
        if (currentState == 3) {
            return true;
        }
        return false;
    }

    private void routeToDiscord() {
        String link = "https://discord.gg/Hhwvay8uTa";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
    }
}
