package com.insidious.plugin.ui.testrunnerinjection.util;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.testrunnerinjection.TestRunnerInjector;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.popup.*;

import javax.swing.*;
import java.awt.*;

/**
 * Utility class for creating and managing pop-up dialog for UnloggedTestRunner.
 */
public class PopUpUtil {

    // Holds the current instance of the popup
    private static JBPopup currentPopup;

    /**
     * Shows a pop-up dialog for configuring and injecting test runners into modules.
     *
     * @param insidiousService the service providing context for the plugin
     */
    public static void showTestRunnerPopUp(InsidiousService insidiousService) {

        // Close the existing popup if it is showing
        if (currentPopup != null && currentPopup.isVisible()) {
            currentPopup.cancel();
        }

        // Create the main panel for the test runner injector UI
        TestRunnerInjector testRunnerInjector = new TestRunnerInjector(insidiousService);
        JComponent testRunnerComponent = testRunnerInjector.getMainPanel();

        // Set maximum size for the popup component
        Dimension max = testRunnerComponent.getMaximumSize();
        testRunnerComponent.setMaximumSize(new Dimension((int) max.getWidth(), 800));

        // Create the popup builder and configure its properties
        ComponentPopupBuilder testRunnerPopUp = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(testRunnerComponent, null);
        currentPopup = testRunnerPopUp
                .setShowBorder(true)
                .setShowShadow(true)
                .setFocusable(true)
                .setCancelButton(new IconButton("Close", AllIcons.Actions.CloseDarkGrey))
                .setCancelKeyEnabled(true)
                .setMovable(true)
                .setRequestFocus(true)
                .setResizable(false)
                .setCancelOnClickOutside(false)
                .setCancelOnOtherWindowOpen(false)
                .setCancelOnWindowDeactivation(false)
                .setBelongsToGlobalPopupStack(true)
                .setTitle("Run Replay Tests")
                .setTitleIcon(new ActiveIcon(UIUtils.UNLOGGED_ICON_DARK_SVG))
                .createPopup();
        currentPopup.showCenteredInCurrentWindow(insidiousService.getProject());

        ApplicationManager.getApplication().invokeLater(() -> {
            Dimension size = currentPopup.getSize();
            currentPopup.setSize(new Dimension((int) size.getWidth(), (int) Math.min(800, size.getHeight())));
        });
    }
}
