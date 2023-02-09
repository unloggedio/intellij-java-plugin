package com.insidious.plugin.ui.Components;

import com.insidious.plugin.util.Strings;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class Obv3_CardParent implements CardSelectionActionListener {
    private final List<Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String>> actions = new ArrayList<>();
    private final CardActionListener actionListener;
    private JPanel mainPanel;
    private JPanel borderParenlPanel;
    private JPanel topPanel;
    private JPanel bottomPanel;
    private JPanel centerPanel;
    private JPanel actionButtonGroup;
    private JButton actionButton;
    private JPanel cardContainer;
    private JButton skipButton;
    private Set<String> dependenciesToAdd = new TreeSet<>();

    public Obv3_CardParent(List<DropdownCardInformation> cards, CardActionListener actionListener) {
        this.actionListener = actionListener;
        this.cardContainer.removeAll();
        GridLayout gridLayout = new GridLayout(cards.size(), 1);
        gridLayout.setVgap(16);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        for (int i = 0; i < cards.size(); i++) {
            DropDownCard_OBV3 card = new DropDownCard_OBV3(cards.get(i), this);
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            gridPanel.add(card.getComponent(), constraints);
        }
        JScrollPane scrollPane = new JBScrollPane(gridPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.cardContainer.setPreferredSize(scrollPane.getSize());
        this.cardContainer.add(scrollPane, BorderLayout.CENTER);
        this.cardContainer.revalidate();
        actionButton.setText("Continue");
        skipButton.setVisible(false);
        actionButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                proceedToAction();
            }
        });
        actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        skipButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public Obv3_CardParent(List<DependencyCardInformation> cards, boolean changeButton, CardActionListener actionListener) {
        this.actionListener = actionListener;
        this.cardContainer.removeAll();
        GridLayout gridLayout = new GridLayout(cards.size(), 1);
        gridLayout.setVgap(16);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        for (int i = 0; i < cards.size(); i++) {
            ListCard_OBV3 card = new ListCard_OBV3(cards.get(i), this);
            GridConstraints constraints = new GridConstraints();
            constraints.setRow(i);
            gridPanel.add(card.getComponent(), constraints);
        }
        this.skipButton.setVisible(cards.get(0)
                .isShowSkipButton());
        this.cardContainer.add(gridPanel, BorderLayout.CENTER);
        this.cardContainer.revalidate();

        if (changeButton) {
            actionButton.setText(cards.get(0)
                    .getPrimaryButtonText());
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

    private void proceedToAction() {
        Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String> action = new TreeMap<>();
        action.put(OnboardingScaffoldV3.ONBOARDING_ACTION.NEXT_STATE, "");
        if (shouldAddAction(action)) {
            actions.add(action);
        }
        actionListener.performActions(this.actions);
    }

    private void proceedToNextState() {
        Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String> action = new TreeMap<>();
        action.put(OnboardingScaffoldV3.ONBOARDING_ACTION.NEXT_STATE, "");
        if (shouldAddAction(action)) {
            actions.add(action);
        }
        actionListener.performActions(this.actions);
    }

    private void proceedToUpdateModule(Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String> action) {
        List<Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String>> acts = new ArrayList<>();
        acts.add(action);
        actionListener.performActions(acts);
    }

    private void proceedToAddDependency() {
        Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String> action = new TreeMap<>();
        action.put(OnboardingScaffoldV3.ONBOARDING_ACTION.ADD_DEPENDENCIES, Strings.join(this.dependenciesToAdd, ","));
        if (shouldAddAction(action)) {
            actions.add(action);
        }
        actionListener.performActions(this.actions);
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    @Override
    public void selectedOption(String selection, OnboardingScaffoldV3.DROP_TYPES type) {
        System.out.println("Selected : " + selection + " Type : " + type.toString());
        switch (type) {
            case JAVA_VERSION:
                Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String> action = new TreeMap<>();
                action.put(OnboardingScaffoldV3.ONBOARDING_ACTION.UPDATE_SELECTION, "jdk:" + selection);
                if (shouldAddAction(action)) {
                    actions.add(action);
                }
                break;
            case SERIALIZER:
                action = new TreeMap<>();
                boolean hasDownloadTask = false;
                for (Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String> item : actions) {
                    OnboardingScaffoldV3.ONBOARDING_ACTION action_type = new ArrayList<>(item.keySet()).get(0);
                    if (action_type.equals(OnboardingScaffoldV3.ONBOARDING_ACTION.DOWNLOAD_AGENT)) {
                        hasDownloadTask = true;
                        item.replace(OnboardingScaffoldV3.ONBOARDING_ACTION.DOWNLOAD_AGENT, selection);
                    }
                }

                if (!hasDownloadTask) {
                    action = new TreeMap<>();
                    action.put(OnboardingScaffoldV3.ONBOARDING_ACTION.DOWNLOAD_AGENT, "" + (selection));
                    actions.add(action);
                }
                break;
            case MODULE:
                action = new TreeMap<>();
                action.put(OnboardingScaffoldV3.ONBOARDING_ACTION.UPDATE_SELECTION, "module:" + selection);
                proceedToUpdateModule(action);
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

    @Override
    public void refreshSerializerSelection() {
        actionListener.refreshSerializers();
    }

    private boolean shouldAddAction(Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String> action) {
        OnboardingScaffoldV3.ONBOARDING_ACTION actionType = action.keySet()
                .iterator()
                .next();
        String parameter = action.get(actionType);
        for (Map<OnboardingScaffoldV3.ONBOARDING_ACTION, String> entry : this.actions) {
            if (entry.containsKey(actionType)) {
                if (entry.get(actionType)
                        .equals(parameter)) {
                    return false;
                }
            }
        }
        return true;
    }
}
