package com.insidious.plugin.ui;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.util.UIUtils;
import com.intellij.notification.NotificationType;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UnloggedSDKOnboarding {
    public static final Color CIRCLE_BLUE_FILL_COLOR = new Color(53, 116, 240);
    private final String UNLOGGED_SDK_VERSION = "0.1.48";
    private final String maven_default =
            "<dependency>\n" +
                    "  <artifactId>unlogged-sdk</artifactId>\n" +
                    "  <groupId>video.bug</groupId>\n" +
                    "  <version>" + UNLOGGED_SDK_VERSION + "</version>\n" +
                    "</dependency>";
    private final String maven_annotated =
            "<plugin>\n" +
                    "  <groupId>org.apache.maven.plugins</groupId>\n" +
                    "  <artifactId>maven-compiler-plugin</artifactId>\n" +
                    "  <configuration>\n" +
                    "      <annotationProcessorPaths>\n" +
                    "          <annotationProcessorPath>\n" +
                    "              <artifactId>unlogged-sdk</artifactId>\n" +
                    "              <groupId>video.bug</groupId>\n" +
                    "              <version>" + UNLOGGED_SDK_VERSION + "</version>\n" +
                    "          </annotationProcessorPath>\n" +
                    "      </annotationProcessorPaths>\n" +
                    "  </configuration>\n" +
                    "</plugin>";
    private final String gradle_dependency = "dependencies\n" +
            "{\n" +
            "    implementation 'video.bug:unlogged-sdk:" + UNLOGGED_SDK_VERSION + "'\n" +
            "    annotationProcessor 'video.bug:unlogged-sdk:" + UNLOGGED_SDK_VERSION + "'\n" +
            "}";
    private JPanel mainPanel;
    private JTextArea mavenDependencyArea;
    private JButton copyCodeButtonMaven;
    private JLabel discordButton;
    private JTabbedPane primaryTabbedPane;
    private JTextArea gradleTextArea;
    private JButton gradleCopyButton;
    private JTextArea mavenDependencyAreaAnnotation;
    private JPanel topAligner;
    private JPanel infoPanel;
    private JLabel headingLabel;
    private JPanel mainContent;
    private JPanel mavenPanel;
    private JPanel dependencyContents;
    private JPanel bottomControls;
    private JPanel gradlePanel;
    private JTextArea importIoUnloggedUnloggedTextArea;
    private JPanel step1TitlePanel;
    private JLabel step1Label;
    private JPanel bottomPanelContainer;
    private JLabel step2Label;
    private JPanel step2ContainerPanel;
    private JLabel step3Label;
    private JPanel step3ContainerPanel;
    private JLabel mvnOrGradleClean;
    private JLabel step4Label;
    private JPanel step4ContainerPanel;
    private JLabel emailButton;
    private JLabel githubButton;
    private JButton doneButton;
    private JPanel gradleDependencyContents;
    private JScrollPane extraMavenTextAreaScrollPanel;
    private JCheckBox usingMavenCompilerPluginCheckBox;
    private InsidiousService insidiousService;
    private String currentJDK = "JDK 1.8";

    public UnloggedSDKOnboarding(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        extraMavenTextAreaScrollPanel.setBorder(BorderFactory.createEmptyBorder());
        extraMavenTextAreaScrollPanel.setVisible(false);
        doneButton.setOpaque(true);
        doneButton.setBorderPainted(true);
        doneButton.setContentAreaFilled(true);
        copyCodeButtonMaven.addActionListener(e -> copyCode(PROJECT_TYPE.MAVEN));
        discordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        discordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });


        emailButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        emailButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToEmail();
            }
        });


        githubButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        githubButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToGithub();
            }
        });


        gradleCopyButton.addActionListener(e -> copyCode(PROJECT_TYPE.GRADLE));

        primaryTabbedPane.setIconAt(0, UIUtils.MAVEN_ICON);
        primaryTabbedPane.setIconAt(1, UIUtils.GRADLE_ICON);

        primaryTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedIndex = primaryTabbedPane.getSelectedIndex();
                if (selectedIndex == 0) {
                    mvnOrGradleClean.setText("mvn clean");
                } else {
                    mvnOrGradleClean.setText("gradle clean");
                }
            }
        });


        mavenDependencyArea.setText(maven_default);
        mavenDependencyAreaAnnotation.setText(maven_annotated);
        mavenDependencyAreaAnnotation.setVisible(false);
        mavenDependencyArea.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor.BLACK, 1, true),
                        mavenDependencyArea.getBorder()
                )
        );
        gradleTextArea.setText(gradle_dependency);
        gradleTextArea.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor.BLACK, 1, true),
                        gradleTextArea.getBorder()
                )
        );


        mavenDependencyAreaAnnotation.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor.BLACK, 1, true),
                        mavenDependencyAreaAnnotation.getBorder()
                )
        );

        usingMavenCompilerPluginCheckBox.addActionListener(
                e -> {
                    boolean selected = usingMavenCompilerPluginCheckBox.isSelected();
                    mavenDependencyAreaAnnotation.setVisible(selected);
                    extraMavenTextAreaScrollPanel.setVisible(selected);
                    extraMavenTextAreaScrollPanel.getParent().revalidate();
                    extraMavenTextAreaScrollPanel.getParent().repaint();
                });

//        CircleBorderJLabel step1label = new CircleBorderJLabel("1");
        JBColor CIRCLE_BLUE_JB_COLOR = new JBColor(CIRCLE_BLUE_FILL_COLOR, CIRCLE_BLUE_FILL_COLOR);
        CircularBorder circleBorder = new CircularBorder(CIRCLE_BLUE_JB_COLOR, 2, 10);
        step1Label.setBorder(
                BorderFactory.createCompoundBorder(
                        circleBorder,
                        BorderFactory.createEmptyBorder(0, 6, 0, 0)
                )
        );
        step1TitlePanel.add(step1Label, new GridConstraints());


        CircularBorder circleBorder2 = new CircularBorder(CIRCLE_BLUE_JB_COLOR, 2, 10);
        step2Label.setBorder(
                BorderFactory.createCompoundBorder(
                        circleBorder2,
                        BorderFactory.createEmptyBorder(0, 6, 0, 0)
                )
        );
        step2ContainerPanel.add(step2Label, new GridConstraints());


        CircularBorder circleBorder3 = new CircularBorder(CIRCLE_BLUE_JB_COLOR, 2, 10);
        step3Label.setBorder(
                BorderFactory.createCompoundBorder(
                        circleBorder3,
                        BorderFactory.createEmptyBorder(0, 5, 0, 0)
                )
        );
        step3ContainerPanel.add(step3Label, new GridConstraints());


        CircularBorder circleBorder4 = new CircularBorder(new JBColor(CIRCLE_BLUE_JB_COLOR, CIRCLE_BLUE_JB_COLOR
        ), 2, 10);
        step4Label.setBorder(
                BorderFactory.createCompoundBorder(
                        circleBorder4,
                        BorderFactory.createEmptyBorder(0, 5, 0, 0)
                )
        );
        step4ContainerPanel.add(step4Label, new GridConstraints());

        doneButton.addActionListener(e -> {
            mainPanel.removeAll();
            insidiousService.showCallYourApplicationScreen();
        });

    }

    public void copyCode(PROJECT_TYPE type) {
        if (type.equals(PROJECT_TYPE.MAVEN)) {
            String dependency = mavenDependencyArea.getText();
            if (!currentJDK.equals("JDK 1.8")) {
                dependency = dependency + "\n" + mavenDependencyAreaAnnotation.getText();
            }
            insidiousService.copyToClipboard(dependency);
            InsidiousNotification.notifyMessage("Copied to clipboard",
                    NotificationType.INFORMATION);
        } else {
            insidiousService.copyToClipboard(gradleTextArea.getText());
            InsidiousNotification.notifyMessage("Copied to clipboard",
                    NotificationType.INFORMATION);
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
                "routeToDiscord", null);
    }

    public void routeToEmail() {
        String link = "mailto:ssl@unlogged.io";
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
                "routeToEmail", null);
    }

    public void routeToGithub() {
        String link = "https://github.com/unloggedio/unlogged-sdk?tab=readme-ov-file#unlogged-java-sdk";
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
                "routeToGithub", null);
    }

    public JPanel getComponent() {
        return this.mainPanel;
    }

    public void showStep2(UnloggedOnboardingScreenV2 screen) {
        mainPanel.removeAll();
        mainPanel.add(screen.getComponent(), new GridConstraints());
    }

    private enum PROJECT_TYPE {MAVEN, GRADLE}
}
