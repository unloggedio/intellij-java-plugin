package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class UnloggedSDKOnboarding {
    private JPanel mainPanel;
    private JPanel topAligner;
    private JPanel infoPanel;
    private JLabel headingLabel;
    private JPanel mainContent;
    private JTextArea mavenDependencyArea;
    private JButton copyCodeButtonMaven;
    private JButton discordButton;
    private JTabbedPane primaryTabbedPane;
    private JPanel mavenPanel;
    private JPanel gradlePanel;
    private JPanel dependencyContents;
    private JPanel gradleDependencyContents;
    private JTextArea gradleTextArea;
    private JButton gradleCopyButton;
    private JPanel bottomControls;
    private JComboBox jdkSelector;
    private JTextArea importIoUnloggedUnloggedTextArea;
    private JTextArea mavenDependencyAreaAnnotation;
    private InsidiousService insidiousService;
    private enum PROJECT_TYPE {MAVEN,GRADLE}
    private String currentJDK = "JDK 1.8";

    private final String UNLOGGED_SDK_VERSION = "0.0.12";

    private String maven_default =
            "<dependency>\n" +
            "  <artifactId>unlogged-sdk</artifactId>\n" +
            "  <groupId>video.bug</groupId>\n" +
            "  <version>"+UNLOGGED_SDK_VERSION+"</version>\n" +
            "</dependency>";

    private String maven_annotated =
            "<plugin>\n" +
            "  <groupId>org.apache.maven.plugins</groupId>\n" +
            "  <artifactId>maven-compiler-plugin</artifactId>\n" +
            "  <configuration>\n" +
            "      <annotationProcessorPaths>\n" +
            "          <annotationProcessorPath>\n" +
            "              <artifactId>unlogged-sdk</artifactId>\n" +
            "              <groupId>video.bug</groupId>\n" +
            "              <version>"+UNLOGGED_SDK_VERSION+"</version>\n" +
            "          </annotationProcessorPath>\n" +
            "      </annotationProcessorPaths>\n" +
            "  </configuration>\n" +
            "</plugin>";

    private String gradle_dependency = "dependencies\n" +
            "{\n" +
            "    implementation 'video.bug:unlogged-sdk:0.0.11'\n" +
            "    annotationProcessor 'video.bug:unlogged-sdk:0.0.11'\n" +
            "}";

    public UnloggedSDKOnboarding(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        copyCodeButtonMaven.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyCode(PROJECT_TYPE.MAVEN);
            }
        });
        discordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                routeToDiscord();
            }
        });
        gradleCopyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyCode(PROJECT_TYPE.GRADLE);
            }
        });
        primaryTabbedPane.setIconAt(0, UIUtils.MAVEN_ICON);
        primaryTabbedPane.setIconAt(1, UIUtils.GRADLE_ICON);
        mavenDependencyArea.setText(maven_default);
        mavenDependencyAreaAnnotation.setText(maven_annotated);
        mavenDependencyAreaAnnotation.setVisible(false);
        gradleTextArea.setText(gradle_dependency);

        jdkSelector.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                String selectedItem = (String) jdkSelector.getSelectedItem();
                currentJDK = selectedItem;
                String version = selectedItem.split(" ")[1];
                if(version.equals("1.8"))
                {
                    mavenDependencyAreaAnnotation.setVisible(false);
                }
                else
                {
                    mavenDependencyAreaAnnotation.setVisible(true);
                }
            }
        });
    }

    public void copyCode(PROJECT_TYPE type) {
        if(type.equals(PROJECT_TYPE.MAVEN)) {
            String dependency = mavenDependencyArea.getText();
            if(!currentJDK.equals("JDK 1.8"))
            {
                dependency = dependency + "\n" + mavenDependencyAreaAnnotation.getText();
            }
            insidiousService.copyToClipboard(dependency);
        }
        else {
            insidiousService.copyToClipboard(gradleTextArea.getText());
        }
    }

    public void routeToDiscord() {
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
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDiscord_GPT", null);
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }
}
