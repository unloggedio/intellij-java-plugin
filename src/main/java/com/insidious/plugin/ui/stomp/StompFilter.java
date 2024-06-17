package com.insidious.plugin.ui.stomp;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.uiDesigner.core.GridConstraints.ALIGN_FILL;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL;

public class StompFilter {
    private final StompFilterModel originalStompFilterModel;
    private RemoteSourceFilter remoteSourceFilter;
    private final StompFilterModel stompFilterModel;
    private JTabbedPane mainPanel;
    private JCheckBox followEditorCheckBox;
    private JList<String> includedClassesList;
    private JPanel includedClassesPanel;
    private JList<String> excludedClassesList;
    private JList<String> includedMethodsList;
    private JList<String> excludedMethodsList;
    private JPanel includedClassesControlPanel;
    private JPanel excludedMethodsControlPanel;
    private JPanel includedMethodsControlPanel;
    private JPanel excludedClassesControlPanel;
    private JLabel addExcludedClassLabel;
    private JLabel removeExcludedClassLabel;
    private JLabel followHelpLabel;
    private JLabel excludedClassesFromClipboard;
    private JPanel excludedClassesButtonPanel;
    private JPanel includedClassesButtonPanel;
    private JPanel includedMethodsButtonPanel;
    private JPanel excludedMethodsButtonPanel;
    private JPanel classFiltersPanel;
    private JPanel performanceFiltersPanel;
    private JPanel mainPanelFilters;
    private JButton applyButton;
    private JButton cancelButton;
    private JButton resetToDefaultButton;
    private JPanel sourcePreferencesPanel;
    //    private JRadioButton localhostRadio;
//    private JRadioButton remoteRadio;
//    private JPanel sourceModeOption;
//    private JTextField serverLinkField;
//    private JButton linkCancelButton;
//    private JButton linkSaveButton;
//    private JButton finalCancelButton;
//    private JButton finalSaveButton;
//    private JPanel sourcePreferencesPanel;
//    private JPanel remotePanel;
    private ComponentLifecycleListener<StompFilter> componentLifecycleListener;
    private DefaultListModel<String> modelIncludedClasses;
    private DefaultListModel<String> modelExcludedClasses;
    private DefaultListModel<String> modelIncludedMethods;
    private DefaultListModel<String> modelExcludedMethods;
    private InsidiousService insidiousService;
    private ButtonGroup serverModeButton;


    public StompFilter(InsidiousService insidiousService,
                       StompFilterModel stompFilterModel,
                       MethodUnderTest lastMethodFocussed,
                       int selectedTabIndex) {
        originalStompFilterModel = new StompFilterModel(stompFilterModel);
        this.stompFilterModel = new StompFilterModel(originalStompFilterModel);
        this.insidiousService = insidiousService;

        int stompFilterPanelWidth = 300;

        cancelButton.addActionListener(e -> {
            if (componentLifecycleListener != null) {
                componentLifecycleListener.onClose();
            }
        });

        mainPanel.addChangeListener(e -> {
            int selected = mainPanel.getSelectedIndex();
            if (selected == 1) {
                if (remoteSourceFilter == null) {
                    remoteSourceFilter = new RemoteSourceFilter(insidiousService);
                    remoteSourceFilter.setOnCloseListener(componentLifecycleListener);
                    sourcePreferencesPanel.add(remoteSourceFilter.getComponent(), BorderLayout.CENTER);
                }
            }
        });

        // sourceMode tab start code


        // sourceMode tab end code


//        new GotItTooltip("Unlogged.Stomp.Filter.Checkbox",
//                "Make the filter always sourceModePanel to the method focussed in your editor by enabling this",
//                project)
//                .withPosition(Balloon.Position.above)
//                .show((JPanel) followEditorCheckBox.getParent().getParent(), GotItTooltip.RIGHT_MIDDLE);


        applyButton.addActionListener(e -> {
            originalStompFilterModel.setFollowEditor(stompFilterModel.followEditor);

            originalStompFilterModel.getIncludedClassNames().clear();
            originalStompFilterModel.getIncludedClassNames().addAll(stompFilterModel.getIncludedClassNames());


            originalStompFilterModel.getIncludedMethodNames().clear();
            originalStompFilterModel.getIncludedMethodNames().addAll(stompFilterModel.getIncludedMethodNames());


            originalStompFilterModel.getExcludedMethodNames().clear();
            originalStompFilterModel.getExcludedMethodNames().addAll(stompFilterModel.getExcludedMethodNames());


            originalStompFilterModel.getExcludedClassNames().clear();
            originalStompFilterModel.getExcludedClassNames().addAll(stompFilterModel.getExcludedClassNames());

            originalStompFilterModel.getExcludedClassNames().clear();
            originalStompFilterModel.getExcludedClassNames().addAll(stompFilterModel.getExcludedClassNames());

            originalStompFilterModel.candidateFilterType = stompFilterModel.candidateFilterType;
            componentLifecycleListener.onClose();
        });

        includedClassesList.setFixedCellWidth(stompFilterPanelWidth);
        excludedClassesList.setFixedCellWidth(stompFilterPanelWidth);
        includedMethodsList.setFixedCellWidth(stompFilterPanelWidth);
        excludedMethodsList.setFixedCellWidth(stompFilterPanelWidth);

        setUiModels(stompFilterModel);
        resetToDefaultButton.addActionListener(e -> stompFilterModel.setFrom(originalStompFilterModel));


        // ExcludedClass

        ActionToolbarImpl excludedClassToolbar = createActionToolbar("Excluded Classes",
                new StompToolbarActionListener() {
                    @Override
                    public void onAdd() {
                        JPanel centerPanel = new JPanel();
                        centerPanel.setLayout(new BorderLayout());
                        JTextField newNameTextField = new JTextField();

                        if (lastMethodFocussed != null) {
                            String name = lastMethodFocussed.getClassName();
                            newNameTextField.setText(name);
                        }


                        Dimension current = newNameTextField.getMinimumSize();
                        newNameTextField.setMinimumSize(new Dimension(300, (int) current.getHeight()));
                        centerPanel.add(newNameTextField, BorderLayout.CENTER);


                        excludedClassesButtonPanel.setVisible(false);

                        GridConstraints constraints = new GridConstraints();
                        constraints.setFill(FILL_HORIZONTAL);
                        constraints.setVSizePolicy(ALIGN_FILL);
                        excludedClassesControlPanel.add(centerPanel, constraints);
                        excludedClassesControlPanel.setToolTipText("Press enter to submit, escape to cancel");

                        newNameTextField.registerKeyboardAction(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String newName = newNameTextField.getText().trim();
                                modelExcludedClasses.addElement(newName);
                                stompFilterModel.getExcludedClassNames().add(newName);
                                excludedClassesControlPanel.remove(centerPanel);
                                excludedClassesButtonPanel.setVisible(true);
                            }
                        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);

                        newNameTextField.registerKeyboardAction(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                excludedClassesControlPanel.remove(centerPanel);
                                excludedClassesButtonPanel.setVisible(true);
                            }
                        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
                        newNameTextField.addFocusListener(new FocusListener() {
                            @Override
                            public void focusGained(FocusEvent e) {

                            }

                            @Override
                            public void focusLost(FocusEvent e) {
                                excludedClassesControlPanel.remove(centerPanel);
                                excludedClassesButtonPanel.setVisible(true);
                            }
                        });

                        ApplicationManager.getApplication().invokeLater(() -> {
                            newNameTextField.requestFocus();
                            newNameTextField.select(0, newNameTextField.getText().length());
                        });


                    }

                    @Override
                    public void onRemove() {
                        List<String> selectedValues = excludedClassesList.getSelectedValuesList();
                        for (String selectedValue : selectedValues) {
                            stompFilterModel.getExcludedClassNames().remove(selectedValue);
                            modelExcludedClasses.removeElement(selectedValue);
                        }
                    }

                    @Override
                    public void onCopy() {
                        String fromClipboard = getFromClipboard();
                        if (fromClipboard == null) return;

                        String[] lines = fromClipboard.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            modelExcludedClasses.addElement(line);
                            stompFilterModel.getExcludedClassNames().add(line);
                        }
                    }
                });

        excludedClassesButtonPanel.add(excludedClassToolbar.getComponent(), BorderLayout.WEST);


        ActionToolbarImpl includedClassesToolbar = createActionToolbar("Included Classes",
                new StompToolbarActionListener() {
                    @Override
                    public void onAdd() {
                        JPanel centerPanel = new JPanel();
                        centerPanel.setLayout(new BorderLayout());
                        JTextField newNameTextField = new JTextField();

                        if (lastMethodFocussed != null) {
                            String name = lastMethodFocussed.getClassName();
                            newNameTextField.setText(name);
                        }


                        Dimension current = newNameTextField.getMinimumSize();
                        newNameTextField.setMinimumSize(new Dimension(300, (int) current.getHeight()));
                        centerPanel.add(newNameTextField, BorderLayout.CENTER);


                        includedClassesButtonPanel.setVisible(false);

                        GridConstraints constraints = new GridConstraints();
                        constraints.setFill(FILL_HORIZONTAL);
                        constraints.setVSizePolicy(ALIGN_FILL);
                        includedClassesControlPanel.add(centerPanel, constraints);
                        includedClassesControlPanel.setToolTipText("Press enter to submit, escape to cancel");

                        newNameTextField.registerKeyboardAction(e1 -> {
                            String newName = newNameTextField.getText().trim();
                            modelIncludedClasses.addElement(newName);
                            stompFilterModel.getIncludedClassNames().add(newName);
                            includedClassesControlPanel.remove(centerPanel);
                            includedClassesButtonPanel.setVisible(true);
                        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);

                        newNameTextField.registerKeyboardAction(e12 -> {
                            includedClassesControlPanel.remove(centerPanel);
                            includedClassesButtonPanel.setVisible(true);
                        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
                        newNameTextField.addFocusListener(new FocusListener() {
                            @Override
                            public void focusGained(FocusEvent e) {

                            }

                            @Override
                            public void focusLost(FocusEvent e) {
                                includedClassesControlPanel.remove(centerPanel);
                                includedClassesButtonPanel.setVisible(true);
                            }
                        });

                        ApplicationManager.getApplication().invokeLater(() -> {
                            newNameTextField.requestFocus();
                            newNameTextField.select(0, newNameTextField.getText().length());
                        });


                    }

                    @Override
                    public void onRemove() {
                        List<String> selectedValues = includedClassesList.getSelectedValuesList();
                        for (String selectedValue : selectedValues) {
                            stompFilterModel.getIncludedClassNames().remove(selectedValue);
                            modelIncludedClasses.removeElement(selectedValue);
                        }

                    }

                    @Override
                    public void onCopy() {
                        String fromClipboard = getFromClipboard();
                        if (fromClipboard == null) return;

                        String[] lines = fromClipboard.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            modelIncludedClasses.addElement(line);
                            stompFilterModel.getIncludedClassNames().add(line);
                        }
                    }
                });

        includedClassesButtonPanel.add(includedClassesToolbar.getComponent(), BorderLayout.WEST);

        ActionToolbarImpl includedMethodsToolbar = createActionToolbar("Included Methods",
                new StompToolbarActionListener() {
                    @Override
                    public void onAdd() {
                        JPanel centerPanel = new JPanel();
                        centerPanel.setLayout(new BorderLayout());
                        JTextField newNameTextField = new JTextField();

                        if (lastMethodFocussed != null) {
                            String name = lastMethodFocussed.getName();
                            newNameTextField.setText(name);
                        }

                        ActionListener saveAction = e13 -> {
                            String newName = newNameTextField.getText().trim();
                            modelIncludedMethods.addElement(newName);
                            stompFilterModel.getIncludedMethodNames().add(newName);
                            includedMethodsControlPanel.remove(centerPanel);
                            includedMethodsButtonPanel.setVisible(true);
                        };


                        Dimension current = newNameTextField.getMinimumSize();
                        newNameTextField.setMinimumSize(new Dimension(200, (int) current.getHeight()));
                        centerPanel.add(newNameTextField, BorderLayout.CENTER);
                        JButton addButton = new JButton();
                        addButton.setText("Add (↵)");
                        addButton.setMinimumSize(new Dimension(100, 30));
                        addButton.setMaximumSize(new Dimension(100, 30));
                        addButton.setPreferredSize(new Dimension(100, 30));
                        addButton.addActionListener(saveAction);
                        centerPanel.add(addButton, BorderLayout.EAST);


                        includedMethodsButtonPanel.setVisible(false);

                        GridConstraints constraints = new GridConstraints();
                        constraints.setFill(FILL_HORIZONTAL);
                        constraints.setVSizePolicy(ALIGN_FILL);
                        includedMethodsControlPanel.add(centerPanel, constraints);
                        includedMethodsControlPanel.setToolTipText("Press enter to submit, escape to cancel");

                        newNameTextField.registerKeyboardAction(saveAction,
                                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                                JComponent.WHEN_FOCUSED);

                        newNameTextField.registerKeyboardAction(e14 -> {
                            includedMethodsControlPanel.remove(centerPanel);
                            includedMethodsButtonPanel.setVisible(true);
                        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
                        newNameTextField.addFocusListener(new FocusListener() {
                            @Override
                            public void focusGained(FocusEvent e) {

                            }

                            @Override
                            public void focusLost(FocusEvent e) {
                                includedMethodsControlPanel.remove(centerPanel);
                                includedMethodsButtonPanel.setVisible(true);
                            }
                        });

                        ApplicationManager.getApplication().invokeLater(() -> {
                            newNameTextField.requestFocus();
                            newNameTextField.select(0, newNameTextField.getText().length());
                        });


                    }

                    @Override
                    public void onRemove() {
                        List<String> selectedValues = includedMethodsList.getSelectedValuesList();
                        for (String selectedValue : selectedValues) {
                            stompFilterModel.getIncludedMethodNames().remove(selectedValue);
                            modelIncludedMethods.removeElement(selectedValue);
                        }

                    }

                    @Override
                    public void onCopy() {
                        String fromClipboard = getFromClipboard();
                        if (fromClipboard == null) return;

                        String[] lines = fromClipboard.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            modelIncludedMethods.addElement(line);
                            stompFilterModel.getIncludedMethodNames().add(line);
                        }
                    }
                });

        includedMethodsButtonPanel.add(includedMethodsToolbar.getComponent(), BorderLayout.WEST);



        ActionToolbarImpl excludedMethodToolbar = createActionToolbar("Excluded Methods",
                new StompToolbarActionListener() {
                    @Override
                    public void onAdd() {
                        JPanel centerPanel = new JPanel();
                        centerPanel.setLayout(new BorderLayout());
                        JTextField newNameTextField = new JTextField();

                        if (lastMethodFocussed != null) {
                            String name = lastMethodFocussed.getName();
                            newNameTextField.setText(name);
                            newNameTextField.setSelectionEnd(0);
                            newNameTextField.setSelectionEnd(name.length());
                        }


                        Dimension current = newNameTextField.getMinimumSize();
                        newNameTextField.setMinimumSize(new Dimension(300, (int) current.getHeight()));
                        centerPanel.add(newNameTextField, BorderLayout.CENTER);


                        excludedMethodsButtonPanel.setVisible(false);

                        GridConstraints constraints = new GridConstraints();
                        constraints.setFill(FILL_HORIZONTAL);
                        constraints.setVSizePolicy(ALIGN_FILL);
                        excludedMethodsControlPanel.add(centerPanel, constraints);
                        excludedMethodsControlPanel.setToolTipText("Press enter to submit, escape to cancel");

                        newNameTextField.registerKeyboardAction(e15 -> {
                            String newName = newNameTextField.getText().trim();
                            modelExcludedMethods.addElement(newName);
                            stompFilterModel.getExcludedMethodNames().add(newName);
                            excludedMethodsControlPanel.remove(centerPanel);
                            excludedMethodsButtonPanel.setVisible(true);
                        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);

                        newNameTextField.registerKeyboardAction(e16 -> {
                            excludedMethodsControlPanel.remove(centerPanel);
                            excludedMethodsButtonPanel.setVisible(true);
                        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
                        newNameTextField.addFocusListener(new FocusListener() {
                            @Override
                            public void focusGained(FocusEvent e) {

                            }

                            @Override
                            public void focusLost(FocusEvent e) {
                                excludedMethodsControlPanel.remove(centerPanel);
                                excludedMethodsButtonPanel.setVisible(true);
                            }
                        });

                        ApplicationManager.getApplication().invokeLater(() -> {
                            newNameTextField.requestFocus();
                            newNameTextField.select(0, newNameTextField.getText().length());
                        });


                    }

                    @Override
                    public void onRemove() {
                        List<String> selectedValues = excludedMethodsList.getSelectedValuesList();
                        for (String selectedValue : selectedValues) {
                            stompFilterModel.getExcludedMethodNames().remove(selectedValue);
                            modelExcludedMethods.removeElement(selectedValue);
                        }

                    }

                    @Override
                    public void onCopy() {
                        String fromClipboard = getFromClipboard();
                        if (fromClipboard == null) return;

                        String[] lines = fromClipboard.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            modelExcludedMethods.addElement(line);
                            stompFilterModel.getExcludedMethodNames().add(line);
                        }
                    }
                });

        excludedMethodsButtonPanel.add(excludedMethodToolbar.getComponent(), BorderLayout.WEST);

        followEditorCheckBox.addActionListener(e -> {
            stompFilterModel.setFollowEditor(followEditorCheckBox.isSelected());
        });

    }

    public ActionToolbarImpl createActionToolbar(
            @NotNull String toolbarName,
            StompToolbarActionListener actionListener
    ) {


        List<AnAction> action11 = new ArrayList<>();


        action11.add(new AnAction(() -> "Add", AllIcons.General.Add) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                actionListener.onAdd();
            }
        });

        action11.add(new AnAction(() -> "Remove Selected", AllIcons.Actions.GC) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                actionListener.onRemove();
            }
        });

        action11.add(new AnAction(() -> "Paste from Clipboard (Classname by \\n)", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                actionListener.onCopy();
            }
        });

        ActionToolbarImpl actionToolbar = new ActionToolbarImpl(
                toolbarName, new DefaultActionGroup(action11), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);

        return actionToolbar;


    }

    private void setUiModels(StompFilterModel stompFilterModel) {
        modelIncludedClasses = new DefaultListModel<>();
        modelIncludedClasses.addAll(stompFilterModel.getIncludedClassNames());

        includedClassesList.setModel(modelIncludedClasses);
        includedClassesList.setBorder(BorderFactory.createEmptyBorder());

        modelExcludedClasses = new DefaultListModel<>();
        modelExcludedClasses.addAll(stompFilterModel.getExcludedClassNames());
        excludedClassesList.setModel(modelExcludedClasses);

        modelIncludedMethods = new DefaultListModel<>();
        modelIncludedMethods.addAll(stompFilterModel.getIncludedMethodNames());
        includedMethodsList.setModel(modelIncludedMethods);


        modelExcludedMethods = new DefaultListModel<>();
        modelExcludedMethods.addAll(stompFilterModel.getExcludedMethodNames());
        excludedMethodsList.setModel(modelExcludedMethods);

        if (stompFilterModel.isFollowEditor()) {
            followEditorCheckBox.setSelected(true);
        }

    }

    @Nullable
    private String getFromClipboard() {
        String fromClipboard;
        try {
            fromClipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException ex) {
            //
//                    throw new RuntimeException(ex);
            return null;
        }
        return fromClipboard;
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void setOnCloseListener(ComponentLifecycleListener<StompFilter> componentLifecycleListener) {
        this.componentLifecycleListener = componentLifecycleListener;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    public void setSelectedTab(int selectedTabIndex) {
        mainPanel.setSelectedIndex(selectedTabIndex);
    }
}
