package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.assertions.AssertionResult;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.ui.assertions.AssertionBlock;
import com.insidious.plugin.ui.assertions.AssertionBlockManager;
import com.insidious.plugin.ui.assertions.AssertionRule;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AtomicAssertionItemPanel {
    private final AtomicAssertion atomicAssertion;
    private final ItemLifeCycleListener<AtomicAssertion> atomicAssertionLifeListener;
    private final Project project;
    private final AssertionBlock assertionBlock;
    private final ActionToolbarImpl readModeActionToolbar;
    private final ActionToolbarImpl editModeActionToolbar;
    private JPanel mainPanel;
    private JPanel assertionPanel;

    public AtomicAssertionItemPanel(AtomicAssertion atomicAssertion, ItemLifeCycleListener<AtomicAssertion> atomicAssertionLifeListener, Project project, Supplier<List<KeyValue>> keyValueSupplier) {


        assertionBlock = new AssertionBlock(atomicAssertion, new AssertionBlockManager() {
            @Override
            public void addNewRule() {

            }

            @Override
            public void addNewGroup() {

            }

            @Override
            public AssertionResult executeAssertion(AtomicAssertion atomicAssertion) {
                return null;
            }

            @Override
            public void deleteAssertionRule(AssertionRule assertionRule) {

            }

            @Override
            public void removeAssertionGroup() {

            }

            @Override
            public KeyValue getCurrentTreeKey() {
                return null;
            }

            @Override
            public void removeAssertionGroup(AssertionBlock block) {

            }
        }, true, project);

        this.atomicAssertion = atomicAssertion;
        this.atomicAssertionLifeListener = atomicAssertionLifeListener;
        this.project = project;

//        assertionPanel.setViewportView(assertionBlock.getContent());
        assertionPanel.add(assertionBlock.getContent(), BorderLayout.CENTER);
        assertionPanel.setBorder(BorderFactory.createEmptyBorder());


        List<AnAction> readModeActionList = new ArrayList<>();
        List<AnAction> editModeActionList = new ArrayList<>();

        readModeActionList.add(new AnAction(() -> "Edit", AllIcons.Actions.Edit) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                assertionBlock.showEditForm(keyValueSupplier);
                ApplicationManager.getApplication().invokeLater(() -> {
                    assertionPanel.remove(readModeActionToolbar.getComponent());
                    assertionPanel.add(editModeActionToolbar.getComponent(), BorderLayout.NORTH);
                    mainPanel.repaint();
                    mainPanel.revalidate();
                    assertionPanel.revalidate();
                    assertionPanel.repaint();
                });

            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        });

        AnAction saveAction = new AnAction(() -> "Save", AllIcons.Actions.MenuSaveall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
//                componentLifecycleListener.onEdit(originalCopy);
                assertionPanel.remove(editModeActionToolbar.getComponent());
                assertionPanel.add(readModeActionToolbar.getComponent(), BorderLayout.NORTH);
                assertionBlock.saveEdit();
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }

        };

        editModeActionList.add(new AnAction(() -> "Delete", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
//                componentLifecycleListener.onDelete(originalCopy);
            }

            @Override
            public boolean displayTextInToolbar() {
                return false;
            }
        });

        editModeActionList.add(new AnAction(() -> "Cancel", AllIcons.Actions.Cancel) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
//                componentLifecycleListener.onUnSelect(originalCopy);
                assertionBlock.hideEditForm();
                assertionPanel.remove(editModeActionToolbar.getComponent());
                assertionPanel.add(readModeActionToolbar.getComponent(), BorderLayout.NORTH);
                assertionPanel.revalidate();
                assertionPanel.repaint();
            }

            @Override
            public boolean displayTextInToolbar() {
                return true;
            }
        });
        editModeActionList.add(saveAction);

        readModeActionToolbar = new ActionToolbarImpl(
                "Replay Editor Action Toolbar", new DefaultActionGroup(readModeActionList), true);
        readModeActionToolbar.setMiniMode(false);
        readModeActionToolbar.setForceMinimumSize(true);
        readModeActionToolbar.setTargetComponent(mainPanel);


        editModeActionToolbar = new ActionToolbarImpl(
                "Replay Editor Action Toolbar", new DefaultActionGroup(editModeActionList), true);
        editModeActionToolbar.setMiniMode(false);
        editModeActionToolbar.setForceMinimumSize(true);
        editModeActionToolbar.setTargetComponent(mainPanel);


        assertionPanel.add(readModeActionToolbar.getComponent(), BorderLayout.NORTH);

    }

    public void setTitle(String text) {
        ((TitledBorder) mainPanel.getBorder()).setTitle(text);
    }

    public JPanel getComponent() {
        return mainPanel;
    }
}
