package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.VMoptionsConstructionService;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.ui.GutterClickNavigationStates.configpanels.CliConfigPanel;
import com.insidious.plugin.ui.GutterClickNavigationStates.configpanels.GradleConfigPanel;
import com.insidious.plugin.ui.GutterClickNavigationStates.configpanels.IntellijRunConfig;
import com.insidious.plugin.ui.GutterClickNavigationStates.configpanels.MavenConfigPanel;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.DumbService;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.util.PsiUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

public class AgentConfigComponent {
    public static final String CALENDLY_UNLOGGED_LINK_STRING = "https://calendly.com/unlogged/unlogged-onboarding";
    private final InsidiousService insidiousService;
    private final VMoptionsConstructionService vmoptsConstructionService = new VMoptionsConstructionService();
    private LanguageLevel currentSelectedLanguageLevel;
    private URI calendlyUri;
    private JPanel mainPanel;
    private JPanel aligner;
    private JPanel topPanel;
    private JLabel iconLabel;
    private JPanel selectionsParent;
    private JPanel javaVersionSelectorPanel;
    private JLabel jvsplabel;
    private JComboBox<LanguageLevel> javaComboBox;
    private JPanel supportPanel;
    private JButton discordButton;
    private JPanel calendlyLinkPanel;
    private JButton startApplicationWithUnloggedButton;
    private JPanel manualConfigurationStepsPanel;
    private JButton showManualConfigurationStepsButton;
    private JTabbedPane configTabsPanel;
    private JPanel tabbedConfigPanelContainer;

    public AgentConfigComponent(InsidiousService insidiousService) {
        try {
            this.calendlyUri = new URI(CALENDLY_UNLOGGED_LINK_STRING);
        } catch (URISyntaxException e) {
            this.calendlyUri = null;
            // should never happen
        }
        this.insidiousService = insidiousService;
        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        dumbService.runWhenSmart(() -> {
            @NotNull LanguageLevel projectLanguageLevel = PsiUtil.getLanguageLevel(
                    insidiousService.getProject());
            currentSelectedLanguageLevel = projectLanguageLevel;
            javaComboBox.getModel().setSelectedItem(currentSelectedLanguageLevel);
            updateConfigTabs(currentSelectedLanguageLevel);
        });


        manualConfigurationStepsPanel.setVisible(false);

        showManualConfigurationStepsButton.addActionListener(e -> {
            UsageInsightTracker.getInstance().RecordEvent("SHOW_MANUAL_CONFIGURATION", new JSONObject());
            manualConfigurationStepsPanel.setVisible(true);
        });


        startApplicationWithUnloggedButton.addActionListener(
                e -> {
                    if (currentSelectedLanguageLevel == null) {
                        InsidiousNotification.notifyMessage(
                                "Please wait for project indexing to complete.", NotificationType.WARNING);
                        return;
                    }
                    JSONObject eventProperties = new JSONObject();
                    eventProperties.put("package", insidiousService.fetchBasePackage());
                    eventProperties.put("languageLevel", currentSelectedLanguageLevel.toString());
                    UsageInsightTracker.getInstance().RecordEvent("START_WITH_UNLOGGED", eventProperties);
                    String currentJVMOpts = getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION,
                            currentSelectedLanguageLevel);
                    insidiousService.startProjectWithUnloggedAgent(currentJVMOpts);
                });


        for (LanguageLevel value : LanguageLevel.values()) {
            javaComboBox.addItem(value);
        }


//        javaComboBox.addItem(LanguageLevel.JDK_1_8);
//        javaComboBox.addItem(LanguageLevel.JDK_11);
//        javaComboBox.addItem(LanguageLevel.JDK_17);
//        javaComboBox.addItem(LanguageLevel.JDK_18);

        javaComboBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                LanguageLevel selectedLanguageLevel = (LanguageLevel) javaComboBox.getSelectedItem();
                currentSelectedLanguageLevel = selectedLanguageLevel;
                updateConfigTabs(selectedLanguageLevel);
            }
        });

        discordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord("https://discord.gg/Hhwvay8uTa");
            }
        });


        JButton button = new JButton();
        button.setText("<HTML>" +
                "<a href=\"" + CALENDLY_UNLOGGED_LINK_STRING + "\">" + CALENDLY_UNLOGGED_LINK_STRING + "</a>" +
                "</HTML>");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setBackground(JBColor.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setToolTipText(calendlyUri != null ? calendlyUri.toString() : CALENDLY_UNLOGGED_LINK_STRING);
        button.setMargin(JBUI.insets(5));
        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Desktop.isDesktopSupported() && calendlyUri != null) {
                    try {
                        Desktop.getDesktop().browse(calendlyUri);
                    } catch (IOException e1) { /* TODO: error handling */ }
                } else { /* TODO: error handling */ }
            }
        });
        calendlyLinkPanel.add(button, BorderLayout.CENTER);
    }

    private void updateConfigTabs(LanguageLevel projectLanguageLevel) {

        configTabsPanel.removeAll();

        MavenConfigPanel mavenConfigPanel =
                new MavenConfigPanel(getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.MAVEN_CLI, projectLanguageLevel),
                        () -> {
                            UsageInsightTracker.getInstance().RecordEvent("COPY_VM_PARAMS_MAVEN", new JSONObject());
                            insidiousService.copyToClipboard(
                                    getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.MAVEN_CLI, projectLanguageLevel));
                        });

        GradleConfigPanel gradleConfigPanel = new GradleConfigPanel(
                getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.GRADLE_CLI, projectLanguageLevel),
                () -> {
                    UsageInsightTracker.getInstance().RecordEvent("COPY_VM_PARAMS_GRADLE", new JSONObject());
                    insidiousService.copyToClipboard(
                            getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.GRADLE_CLI, projectLanguageLevel));
                    InsidiousNotification.notifyMessage("Copied Gradle Configuration to clipboard",
                            NotificationType.INFORMATION);

                });

        IntellijRunConfig intellijRunConfigPanel = new IntellijRunConfig(
                getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION, projectLanguageLevel),
                () -> {
                    UsageInsightTracker.getInstance().RecordEvent("COPY_VM_PARAMS_INTELLIJ", new JSONObject());
                    insidiousService.copyToClipboard(
                            getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION, projectLanguageLevel));
                    InsidiousNotification.notifyMessage("Copied VM param to clipboard", NotificationType.INFORMATION);

                },
                () -> {
                    UsageInsightTracker.getInstance().RecordEvent("ADD_AGENT_TO_RUN_CONFIG", new JSONObject());
                    insidiousService.addAgentToRunConfig(
                            getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION, projectLanguageLevel));

                });

        CliConfigPanel cliConfigPanel = new CliConfigPanel(
                getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.JAVA_JAR_CLI, projectLanguageLevel),
                () -> {
                    UsageInsightTracker.getInstance().RecordEvent("COPY_VM_PARAMS_CLI", new JSONObject());
                    insidiousService.copyToClipboard(
                            getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES.JAVA_JAR_CLI, projectLanguageLevel));
                    InsidiousNotification.notifyMessage("Copied to clipboard", NotificationType.INFORMATION);
                });


        configTabsPanel.addTab("IntelliJ", UIUtils.INTELLIJ_ICON, intellijRunConfigPanel.getComponent());
        configTabsPanel.addTab("Maven", UIUtils.MAVEN_ICON, mavenConfigPanel.getComponent());
        configTabsPanel.addTab("Gradle", UIUtils.GRADLE_ICON, gradleConfigPanel.getComponent());
        configTabsPanel.addTab("CLI", UIUtils.JAVA_ICON, cliConfigPanel.getComponent());

    }

    public JPanel getComponent() {
        return this.mainPanel;
    }


    public String getCurrentJVMOpts(ProjectTypeInfo.RUN_TYPES currentType1, LanguageLevel languageLevel) {
        return vmoptsConstructionService.getVMOptionsForRunType(currentType1, languageLevel,
                Collections.singletonList(insidiousService.fetchBasePackage()));
    }

    private void routeToDiscord(String link) {
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDiscord_EXE", null);
    }

}
