package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssertionRuleEditPanel {
    private final Project project;
    private JPanel mainPanel;
    private JTextField keyTextField;
    private JTextField valueTextArea;
    private JLabel operationTypeLabel;
    private JLabel dropDownIconLabel;
    private JPanel actionPanelContainer;

    public AssertionRuleEditPanel(
            AtomicAssertion atomicAssertion,
            ItemLifeCycleListener<AtomicAssertion> componentLifecycleListener, Project project) {
        this.project = project;
        AtomicAssertion originalCopy = new AtomicAssertion(atomicAssertion);
        keyTextField.setText(originalCopy.getKey());
        valueTextArea.setText(originalCopy.getExpectedValue());
        operationTypeLabel.setText(originalCopy.getAssertionType().toString());
        dropDownIconLabel.setIcon(AllIcons.Actions.InlayDropTriangle);
        dropDownIconLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        operationTypeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        dropDownIconLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                @NotNull JBPopup gutterMethodComponentPopup = JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(Arrays.asList(AssertionType.values()))
                        .createPopup();

                gutterMethodComponentPopup.showUnderneathOf(operationTypeLabel);
            }
        });

        List<AnAction> actionList = new ArrayList<>();

        actionList.add(new AnAction(() -> "Save", AllIcons.Actions.MenuSaveall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                componentLifecycleListener.onEdit(originalCopy);
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        });

        actionList.add(new AnAction(() -> "Delete", AllIcons.General.Remove) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                componentLifecycleListener.onDelete(originalCopy);
            }

            @Override
            public boolean displayTextInToolbar() {
                return false;
            }
        });

        actionList.add(new AnAction(() -> "Cancel", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                componentLifecycleListener.onUnSelect(originalCopy);
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        });

        ActionToolbarImpl actionToolbar = new ActionToolbarImpl(
                "JUnit Test Generator", new DefaultActionGroup(actionList), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);
        actionPanelContainer.add(actionToolbar.getComponent(), BorderLayout.WEST);


    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
