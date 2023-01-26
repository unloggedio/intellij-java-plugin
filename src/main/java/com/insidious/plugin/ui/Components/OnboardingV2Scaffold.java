package com.insidious.plugin.ui.Components;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.OnboardingService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

public class OnboardingV2Scaffold implements OnboardingStateManager {
    private static final Logger logger = LoggerUtil.getInstance(OnboardingV2Scaffold.class);
    private final OnboardingService onboardingService;
    private final InsidiousService insidiousService;
    private JPanel basePanel;
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel topParentPanel;
    private JPanel moduleSelectionPanel;
    private JComboBox moduleSelectionBox;
    private JLabel modulesHeading;
    private JPanel JavaVersionSelectionPanel;
    private JLabel javaVersionText;
    private JComboBox javaSelectionBox;
    private JPanel includePanel;
    private JLabel includeHeadingLabel;
    private JPanel BasePackagePanel;
    private JLabel basePackageLabel;
    private JPanel bottomPanelparent;
    private JPanel centerPanel;
    private JPanel rightPanel;
    private JButton supportButton;
    private DocumentationOnboardingComponent documentation_instance;
    private DependencyManagementComponent dependency_instance;

    public OnboardingV2Scaffold(InsidiousService insidiousService, WaitingStateComponent.WAITING_COMPONENT_STATES state, OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
        this.insidiousService = insidiousService;
        if (state.equals(WaitingStateComponent.WAITING_COMPONENT_STATES.WAITING_FOR_LOGS)) {
            //go to docs
            loadDocumentationComponent();
        } else if (state.equals(WaitingStateComponent.WAITING_COMPONENT_STATES.SWITCH_TO_LIVE_VIEW)) {
            loadDocumentationComponent();
        }

        setupProjectInformationSection();
        loadWaitingStateComponent(state);
    }

    public OnboardingV2Scaffold(InsidiousService insidiousService, WaitingStateComponent.WAITING_COMPONENT_STATES state, Map<String, String> missingDependencies, OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
        this.insidiousService = insidiousService;
        loadDependencyComponent(missingDependencies, onboardingService);
        setupProjectInformationSection();
        loadWaitingStateComponent(state);
    }

    public JPanel getComponent() {
        return mainPanel;
    }

    @Override
    public void transistionToState(WaitingStateComponent.WAITING_COMPONENT_STATES state) {
        switch (state) {
            case AWAITING_DEPENDENCY_ADDITION:
                loadDependencyComponent(onboardingService.fetchMissingDependencies(), onboardingService);
                //add dep mgmt comp
                break;
            case WAITING_FOR_LOGS:
                loadDocumentationComponent();
                //add docs comp
                break;
            case SWITCH_TO_DEPENDENCY_MANAGEMENT:
                loadDependencyComponent(onboardingService.fetchMissingDependencies(), onboardingService);
                //add dep mgmt comp
                break;
            case SWITCH_TO_DOCUMENTATION:
                //add docs comp
                //loadDocumentationComponent();
                break;
            case SWITCH_TO_LIVE_VIEW:
                //add liveview, switch windows.
                insidiousService.addLiveView();
                break;
        }
    }

    @Override
    public void checkForSelogs() {
        //System.out.println("Checking for SE logs");
        recursiveFileCheck();
    }

    private void recursiveFileCheck() {
        ApplicationManager.getApplication()
                .runReadAction(new Runnable() {
                    public void run() {
                        runSelogCheck();
                    }
                });
    }

    private void runSelogCheck() {
        //System.out.println("In check for selogs");
        if (insidiousService.areLogsPresent()) {
            //switch state
            logger.info("Can Switch to LIVE VIEW");
            loadWaitingStateComponent(WaitingStateComponent.WAITING_COMPONENT_STATES.SWITCH_TO_LIVE_VIEW);
            UsageInsightTracker.getInstance()
                    .RecordEvent("LogsReady", null);
        } else {
            //System.out.println("TIMER CHECK FOR SELOGS");
            //run till you have selogs (5 seconds at a time)
            Timer timer = new Timer(5000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    runSelogCheck();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    @Override
    public boolean canGoToDocumentation() {
        return onboardingService.canGoToDocumention();
    }

    public void loadDocumentationComponent() {
        this.centerPanel.removeAll();
        DocumentationOnboardingComponent documentationOnboardingComponent = new DocumentationOnboardingComponent(
                insidiousService);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(documentationOnboardingComponent.getComponent(), constraints);
        this.centerPanel.add(gridPanel, BorderLayout.CENTER);
        this.centerPanel.revalidate();
        this.documentation_instance = documentationOnboardingComponent;
    }

    public void loadDependencyComponent(Map<String, String> missingDependencies, OnboardingService onboardingService) {
        this.centerPanel.removeAll();
        DependencyManagementComponent dependencyManagementComponent = new DependencyManagementComponent(
                missingDependencies, onboardingService, insidiousService);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(dependencyManagementComponent.getComponent(), constraints);
        this.centerPanel.add(gridPanel, BorderLayout.CENTER);
        this.centerPanel.revalidate();
        this.dependency_instance = dependencyManagementComponent;
    }

    public void loadWaitingStateComponent(WaitingStateComponent.WAITING_COMPONENT_STATES state) {
        this.rightPanel.removeAll();
        WaitingStateComponent waitingStateComponent = new WaitingStateComponent(state, this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(waitingStateComponent.getComponent(), constraints);
        this.rightPanel.add(gridPanel, BorderLayout.CENTER);
        this.rightPanel.revalidate();
    }

    private void setupProjectInformationSection() {
        populateModules(onboardingService.fetchModules());
        javaSelectionBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    String version = event.getItem()
                            .toString();
                    boolean add = version.startsWith(">");
                    if (documentation_instance != null) {
                        documentation_instance.setAddOpens(add);
                    }
                }
            }
        });

        supportButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });

        moduleSelectionPanel.setBorder(new LineBorder(new Color(32, 32, 32)));
        JavaVersionSelectionPanel.setBorder(new LineBorder(new Color(32, 32, 32)));
        includePanel.setBorder(new LineBorder(new Color(32, 32, 32)));
    }

    public void populateModules(List<String> modules) {
        DefaultComboBoxModel module_model = new DefaultComboBoxModel();
        module_model.addAll(modules);
        moduleSelectionBox.setModel(module_model);
//        moduleSelectionBox.removeAll();
        moduleSelectionBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                String moduleName = event.getItem()
                        .toString();
                String basePackage = onboardingService.fetchBasePackageForModule(moduleName);
                this.basePackageLabel.setText(basePackage);
                if (documentation_instance != null) {
                    documentation_instance.setBasePackage(basePackage);
                    documentation_instance.triggerUpdate();
                }
            }
        });
        moduleSelectionBox.setSelectedIndex(0);
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
