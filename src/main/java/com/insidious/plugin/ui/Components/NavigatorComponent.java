package com.insidious.plugin.ui.Components;

import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

public class NavigatorComponent implements NavigationManager{
    private JPanel mainPanel;
    private JPanel borderParent;
    private OnboardingScaffold_v3 scaffold;
    private ArrayList<String> states = new ArrayList<>();

    private int currentState = 0;
    public NavigatorComponent(OnboardingScaffold_v3 onboardingScaffoldV3)
    {
        this.scaffold = onboardingScaffoldV3;
        states = new ArrayList<>();
        states.add("Module Selection");
        states.add("Project Config");
        states.add("Dependencies Check");
        states.add("Run Config");
        states.add("Run!");

        this.borderParent.removeAll();
        GridLayout gridLayout = new GridLayout(1, 30);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        for(int i=0;i<states.size();i++)
        {
            NavigationElement element = new NavigationElement(this);
            element.setNavigationStageText(states.get(i));
            GridConstraints constraints = new GridConstraints();
            constraints.setColumn(i);
            gridPanel.add(element.getComponent(), constraints);
        }
        this.borderParent.add(gridPanel, BorderLayout.CENTER);
        this.borderParent.revalidate();
    }

    public JPanel getComponent()
    {
        return mainPanel;
    }

    @Override
    public void NavigateToState(String state) {
        System.out.println("Navigating to "+state);
        loadState(state);
    }

    public void loadState(String state)
    {
        switch (state)
        {
            case "Module Selection":
                scaffold.loadModuleSection();
                currentState=0;
                break;
            case "Dependencies Check":
                scaffold.loadDependenciesManagementSection();
                currentState=2;
                break;
            case "Project Config":
                scaffold.loadProjectConfigSection();
                currentState=1;
                break;
            case "Run Config":
                scaffold.loadRunConfigSection();
                currentState=3;
                break;
            case "Run!":
                scaffold.loadRunSection(scaffold.checkIfLogsArePresent());
                currentState=4;
                break;
        }
    }

    public void loadNextState()
    {
        if(currentState+1<this.states.size())
        {
            currentState++;
            loadState(this.states.get(currentState));
        }
    }
}
