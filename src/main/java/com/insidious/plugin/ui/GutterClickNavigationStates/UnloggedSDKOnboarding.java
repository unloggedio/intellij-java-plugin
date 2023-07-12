package com.insidious.plugin.ui.GutterClickNavigationStates;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.intellij.pom.java.LanguageLevel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

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
    private JComboBox<LanguageLevel> jdkSelector;
    private JScrollPane mavenScroller;
    private JScrollPane gradleScroller;
    private JTextArea importIoUnloggedUnloggedTextArea;
    private InsidiousService insidiousService;
    private enum PROJECT_TYPE {MAVEN,GRADLE}

    private String maven_default =
            "<dependency>\n" +
            "  <artifactId>unlogged-sdk</artifactId>\n" +
            "  <groupId>video.bug</groupId>\n" +
            "  <version>0.0.11</version>\n" +
            "</dependency>";

    private String maven_annotated =
            "<dependency>\n" +
            "  <artifactId>unlogged-sdk</artifactId>\n" +
            "  <groupId>video.bug</groupId>\n" +
            "  <version>0.0.11</version>\n" +
            "</dependency>\n\n" +
                    "<plugin>\n" +
                    "  <groupId>org.apache.maven.plugins</groupId>\n" +
                    "  <artifactId>maven-compiler-plugin</artifactId>\n" +
                    "  <configuration>\n" +
                    "      <source>10</source>\n" +
                    "      <target>10</target>\n" +
                    "      <annotationProcessorPaths>\n" +
                    "          <annotationProcessorPath>\n" +
                    "              <groupId>org.projectlombok</groupId>\n" +
                    "              <artifactId>lombok</artifactId>\n" +
                    "              <version>1.18.24</version>\n" +
                    "              </annotationProcessorPath>\n" +
                    "              <annotationProcessorPath>\n" +
                    "                  <artifactId>unlogged-sdk</artifactId>\n" +
                    "                  <groupId>video.bug</groupId>\n" +
                    "                  <version>0.0.11</version>\n" +
                    "              </annotationProcessorPath>\n" +
                    "      </annotationProcessorPaths>\n" +
                    "  </configuration>\n" +
                    "</plugin>";

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
        for (LanguageLevel value : LanguageLevel.values()) {
            jdkSelector.addItem(value);
        }
        gradleCopyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyCode(PROJECT_TYPE.GRADLE);
            }
        });

        jdkSelector.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                LanguageLevel selectedLanguageLevel = (LanguageLevel) jdkSelector.getSelectedItem();
                int lastCaretPosition = mavenDependencyArea.getCaretPosition();
                if(selectedLanguageLevel.isLessThan(LanguageLevel.JDK_1_9))
                {
                    mavenDependencyArea.setText(maven_default);
                }
                else
                {
                    mavenDependencyArea.setText(maven_annotated);
                    mavenDependencyArea.setCaretPosition(lastCaretPosition);
                }
            }
        });
    }

    public void copyCode(PROJECT_TYPE type) {
        if(type.equals(PROJECT_TYPE.MAVEN)) {
            insidiousService.copyToClipboard(mavenDependencyArea.getText());
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
