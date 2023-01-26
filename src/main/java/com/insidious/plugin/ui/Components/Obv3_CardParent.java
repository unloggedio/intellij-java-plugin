package com.insidious.plugin.ui.Components;

import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class Obv3_CardParent implements CardSelectionActionListener{
    private JPanel mainPanel;
    private JPanel borderParenlPanel;
    private JPanel topPanel;
    private JPanel bottomPanel;
    private JPanel centerPanel;
    private JPanel actionButtonGroup;
    private JButton actionButton;
    private JPanel cardContainer;
    private JButton skipButton;
    private List<Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String>> actions = new ArrayList<>();
    private CardActionListener actionListener;
    private Set<String> dependenciesToAdd = new TreeSet<>();
    public Obv3_CardParent(List<DropdownCardInformation> cards, CardActionListener actionListener)
    {
        this.actionListener = actionListener;
        this.cardContainer.removeAll();
        GridLayout gridLayout = new GridLayout(1, cards.size());
        gridLayout.setHgap(16);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        for(int i=0;i<cards.size();i++)
        {
            DropDownCard_OBV3 card = new DropDownCard_OBV3(cards.get(i),this);
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            gridPanel.add(card.getComponent(), constraints);
        }
        this.cardContainer.add(gridPanel, BorderLayout.CENTER);
        this.cardContainer.revalidate();
        actionButton.setText("Continue");
        skipButton.setVisible(false);
        actionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                proceedToAction();
            }
        });
    }

    private void proceedToAction()
    {
        Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> action = new TreeMap<>();
        action.put(OnboardingScaffold_v3.ONBOARDING_ACTION.NEXT_STATE,"");
        if(shouldAddAction(action))
        {
            actions.add(action);
        }
        actionListener.performActions(this.actions);
    }

    public Obv3_CardParent(List<DependencyCardInformation> cards, boolean changeButton, CardActionListener actionListener)
    {
        this.actionListener = actionListener;
        this.cardContainer.removeAll();
        GridLayout gridLayout = new GridLayout(1, cards.size());
        gridLayout.setHgap(16);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        for(int i=0;i<cards.size();i++)
        {
            ListCard_OBV3 card = new ListCard_OBV3(cards.get(i),this);
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            gridPanel.add(card.getComponent(), constraints);
        }
        this.cardContainer.add(gridPanel, BorderLayout.CENTER);
        this.cardContainer.revalidate();

        if(changeButton)
        {
            actionButton.setText("Add dependencies");
            skipButton.setVisible(true);
            skipButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    proceedToNextState();
                }
            });
        }
        actionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                proceedToAddDependency();
            }
        });
    }

    private void proceedToNextState()
    {
        Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> action = new TreeMap<>();
        action.put(OnboardingScaffold_v3.ONBOARDING_ACTION.NEXT_STATE,"");
        if(shouldAddAction(action))
        {
            actions.add(action);
        }
        actionListener.performActions(this.actions);
    }

    private void proceedToAddDependency() {

        if(this.dependenciesToAdd.size()==0)
        {
            return;
        }
        Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> action = new TreeMap<>();
        action.put(OnboardingScaffold_v3.ONBOARDING_ACTION.ADD_DEPENDENCIES,this.dependenciesToAdd.toString());
        if(shouldAddAction(action))
        {
            actions.add(action);
        }
        actionListener.performActions(this.actions);
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    @Override
    public void selectedOption(String selection, OnboardingScaffold_v3.DROP_TYPES type) {
        System.out.println("Selected : "+selection+" Type : "+type.toString());
        switch (type)
        {
            case JAVA_VERSION:
                Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> action = new TreeMap<>();
                action.put(OnboardingScaffold_v3.ONBOARDING_ACTION.UPDATE_SELECTION,"addopens:"+(selection.startsWith(">")?true:false));
                if(shouldAddAction(action))
                {
                    actions.add(action);
                }
                break;
            case SERIALIZER:
                action = new TreeMap<>();
                boolean hasDownloadTask=false;
                for(Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> item : actions)
                {
                    OnboardingScaffold_v3.ONBOARDING_ACTION action_type = new ArrayList<>(item.keySet()).get(0);
                    if(action_type.equals(OnboardingScaffold_v3.ONBOARDING_ACTION.DOWNLOAD_AGENT))
                    {
                        hasDownloadTask=true;
                        item.replace(OnboardingScaffold_v3.ONBOARDING_ACTION.DOWNLOAD_AGENT,selection);
                    }
                }

                if(!hasDownloadTask)
                {
                    action = new TreeMap<>();
                    action.put(OnboardingScaffold_v3.ONBOARDING_ACTION.DOWNLOAD_AGENT,""+(selection));
                    actions.add(action);
                }
                break;
            case MODULE:
                action = new TreeMap<>();
                action.put(OnboardingScaffold_v3.ONBOARDING_ACTION.UPDATE_SELECTION,"module:"+selection);
                if(shouldAddAction(action))
                {
                    actions.add(action);
                }
                break;
        }
    }

    @Override
    public void setSelectionsForDependencyAddition(Set<String> dependencies) {
        this.dependenciesToAdd = dependencies;
    }

    @Override
    public void refreshModules() {
        //refresh
        actionListener.refreshModules();
    }

    @Override
    public void refreshDependencies() {
        actionListener.refreshDependencies();
    }

    private boolean shouldAddAction(Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> action)
    {
        OnboardingScaffold_v3.ONBOARDING_ACTION actionType = action.keySet().iterator().next();
        String parameter = action.get(actionType);
        for(Map<OnboardingScaffold_v3.ONBOARDING_ACTION,String> entry : this.actions)
        {
            if(entry.containsKey(actionType))
            {
                if(entry.get(actionType).equals(parameter))
                {
                    return false;
                }
            }
        }
        return true;
    }
}
