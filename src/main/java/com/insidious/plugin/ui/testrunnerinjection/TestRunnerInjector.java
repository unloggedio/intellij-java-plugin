package com.insidious.plugin.ui.testrunnerinjection;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.testrunnerinjection.components.CheckComboBox;
import com.insidious.plugin.ui.testrunnerinjection.util.MultiModuleManager;
import com.insidious.plugin.ui.testrunnerinjection.util.RunnerWriter;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.DumbService;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The TestRunnerInjector class provides a user interface for injecting test runner code
 * into multiple modules of a project.
 */
public class TestRunnerInjector {

    //UI elements
    private JPanel mainPanel;
    private JPanel mainContent;
    private JPanel step1ContainerPanel;
    private JPanel step1;
    private JPanel step2;
    private JPanel instructionText;
    private JLabel instructions;
    private JPanel copyPanel;
    private JPanel dropDownPanel;
    private JPanel injectFilePanel;
    private JTextArea testRunnerText;
    private JPanel documentationPanel;
    private JLabel heading;
    private JLabel content;
    private JLabel link;
    private JComboBox moduleSelector;
    private JButton injectButton;
    private JPanel step2ContainerPanel;
    private JPanel commands;
    private JTextArea mvnTestTextArea;
    private JTextArea gradleTestTextArea;
    private JButton mvnCopyButton;
    private JPanel mvn;
    private JPanel gradle;
    private JButton gradleCopyButton;
    private JButton copyRunnerCode;
    private JPanel mvnWrapPanel;
    private JPanel gradleWrapPanel;

    private String runnerCode = "import io.unlogged.runner.UnloggedTestRunner;\n" +
            "import org.junit.runner.RunWith;\n\n" +
            "@RunWith(UnloggedTestRunner.class)\n" +
            "public class UnloggedTest {\n" +
            "}";

    //Services
    private final InsidiousService insidiousService;
    private final MultiModuleManager multiModuleManager;


    /**
     * Constructor for TestRunnerInjector.
     * Initializes the UI components, sets up the module dropdown, and adds action listeners.
     *
     * @param insidiousService
     */
    public TestRunnerInjector(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.multiModuleManager = new MultiModuleManager(insidiousService.getProject());

        addBordersToTextBoxes();

        testRunnerText.setText(runnerCode);
        initializeModuleDropDown();
        addInjectButtonActionListener();

        //Copy Buttons

        copyRunnerCode.addActionListener(e -> copyCode(CopyType.RUNNER));
        mvnCopyButton.addActionListener(e -> copyCode(CopyType.MAVEN));
        gradleCopyButton.addActionListener(e -> copyCode(CopyType.GRADLE));

        //Open Documentation

        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToCiDocumentation();
            }
        });
    }

    /**
     * Returns the main panel of the UI.
     * @return the main JPanel
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Copies the code to the clipboard based on the specified CopyType.
     * @param type the type of code to copy (RUNNER, MAVEN, or GRADLE)
     */
    public void copyCode(CopyType type) {
        if (type.equals(CopyType.RUNNER)) {
            insidiousService.copyToClipboard(testRunnerText.getText());
            InsidiousNotification.notifyMessage("Copied to clipboard",
                    NotificationType.INFORMATION);
        } else if(type.equals(CopyType.MAVEN)){
            insidiousService.copyToClipboard(mvnTestTextArea.getText());
            InsidiousNotification.notifyMessage("Copied to clipboard",
                    NotificationType.INFORMATION);
        } else {
            insidiousService.copyToClipboard(gradleTestTextArea.getText());
            InsidiousNotification.notifyMessage("Copied to clipboard",
                    NotificationType.INFORMATION);
        }
    }

    /**
     * Opens the CI documentation link in the default browser.
     * If Desktop is not supported, shows a notification with the link.
     */
    public void routeToCiDocumentation() {
        String link = "https://read.unlogged.io/cirunner/";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
                //Handle this
            }
        } else {
            InsidiousNotification.notifyMessage(
                    "<a href='https://read.unlogged.io/cirunner/'>Documentation</a> for running unlogged replay tests from " +
                            "CLI/Maven/Gradle", NotificationType.INFORMATION);
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDocumentation", null);
    }

    /**
     * Adds borders to the text areas for better UI appearance.
     */
    private void addBordersToTextBoxes() {
        testRunnerText.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor.BLACK, 1, true),
                        testRunnerText.getBorder()
                )
        );

        mvnTestTextArea.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor.BLACK, 1, true),
                        mvnTestTextArea.getBorder()
                )
        );

        gradleTestTextArea.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(JBColor.BLACK, 1, true),
                        gradleTestTextArea.getBorder()
                )
        );
    }

    /**
     * Initializes the module dropdown with a custom CheckComboBox.
     * Replaces the default JComboBox with the custom CheckComboBox to support multiple selections.
     */
    private void initializeModuleDropDown() {

//        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
//        dumbService.runWhenSmart(() -> {
        CheckComboBox checkComboBox = new CheckComboBox(multiModuleManager.populateCheckComboBoxWithModules());
        checkComboBox.setPreferredSize(new Dimension(-1,40));

        // Remove the placeholder JComboBox and replace it with custom CheckComboBox
        dropDownPanel.remove(moduleSelector);
        moduleSelector = checkComboBox;
        GridConstraints gridConstraints = new GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK,
                GridConstraints.SIZEPOLICY_FIXED,
                null, null, null, 0, false
        );
        dropDownPanel.add(moduleSelector, gridConstraints);
//        });
    }

    /**
     * Adds an action listener to the inject button.
     * The listener injects runner code to the selected modules.
     */
    private void addInjectButtonActionListener() {
        injectButton.addActionListener(e -> {
            for (JCheckBox module : ((CheckComboBox) moduleSelector).getCheckedItems()) {
                RunnerWriter runnerWriter = new RunnerWriter(insidiousService.getProject());
                runnerWriter.writeFile(module.getText());
            }
            refreshModuleDropDown();
        });
    }

    private void refreshModuleDropDown() {
        // Reinitialize the module dropdown
        initializeModuleDropDown();
        // Repaint the panel to reflect changes
        dropDownPanel.revalidate();
        dropDownPanel.repaint();
    }

    /**
     * Enum representing the types of code that can be copied to the clipboard.
     */
    private enum CopyType {MAVEN, GRADLE, RUNNER}
}
