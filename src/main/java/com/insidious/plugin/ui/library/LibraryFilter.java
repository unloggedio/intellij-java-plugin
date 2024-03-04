package com.insidious.plugin.ui.library;

import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.methodscope.ComponentLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.uiDesigner.core.GridConstraints;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
import java.util.List;

import static com.intellij.uiDesigner.core.GridConstraints.ALIGN_FILL;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL;

public class LibraryFilter {
    private final LibraryFilterState originalFilterModel;
    private LibraryFilterState filterModel;
    private JTabbedPane mainPanel;
    private JCheckBox followEditorCheckBox;
    private JList<String> includedClassesList;
    private JPanel includedClassesPanel;
    private JList<String> excludedClassesList;
    private JList<String> includedMethodsList;
    private JList<String> excludedMethodsList;
    private JLabel addIncludedClassLabel;
    private JPanel includedClassesControlPanel;
    private JPanel excludedMethodsControlPanel;
    private JPanel includedMethodsControlPanel;
    private JPanel excludedClassesControlPanel;
    private JLabel removeIncludedClassLabel;
    private JLabel addExcludedClassLabel;
    private JLabel removeExcludedClassLabel;
    private JLabel addIncludedMethodLabel;
    private JLabel removeIncludedMethodLabel;
    private JLabel addExcludedMethodLabel;
    private JLabel removeExcludedMethodLabel;
    private JLabel followHelpLabel;
    private JLabel includedClassesFromClipboard;
    private JLabel excludedClassesFromClipboard;
    private JLabel includedMethodsFromClipboard;
    private JLabel excludedMethodsFromClipboard;
    private JPanel excludedClassesButtonPanel;
    private JPanel includedClassesButtonPanel;
    private JPanel includedMethodsButtonPanel;
    private JPanel excludedMethodsButtonPanel;
    private JPanel sourcePreferencesPanel;
    private JPanel classFiltersPanel;
    private JPanel performanceFiltersPanel;
    private JPanel mainPanelFilters;
    private JButton applyButton;
    private JButton cancelButton;
    private JButton resetToDefaultButton;
    private ComponentLifecycleListener<LibraryFilter> componentLifecycleListener;
    private DefaultListModel<String> modelIncludedClasses;
    private DefaultListModel<String> modelExcludedClasses;
    private DefaultListModel<String> modelIncludedMethods;
    private DefaultListModel<String> modelExcludedMethods;

    public LibraryFilter(LibraryFilterState filterModel, MethodUnderTest lastMethodFocussed) {
        originalFilterModel = new LibraryFilterState(filterModel);
        this.filterModel = new LibraryFilterState(originalFilterModel);
        int libraryFilterPanelWidth = 300;

        cancelButton.addActionListener(e -> {
            if (componentLifecycleListener != null) {
                componentLifecycleListener.onClose(LibraryFilter.this);
            }
        });

        applyButton.addActionListener(e -> {
            originalFilterModel.setFollowEditor(filterModel.followEditor);

            originalFilterModel.getIncludedClassNames().clear();
            originalFilterModel.getIncludedClassNames().addAll(filterModel.getIncludedClassNames());


            originalFilterModel.getIncludedMethodNames().clear();
            originalFilterModel.getIncludedMethodNames().addAll(filterModel.getIncludedMethodNames());


            originalFilterModel.getExcludedMethodNames().clear();
            originalFilterModel.getExcludedMethodNames().addAll(filterModel.getExcludedMethodNames());


            originalFilterModel.getExcludedClassNames().clear();
            originalFilterModel.getExcludedClassNames().addAll(filterModel.getExcludedClassNames());

            originalFilterModel.getExcludedClassNames().clear();
            originalFilterModel.getExcludedClassNames().addAll(filterModel.getExcludedClassNames());

            originalFilterModel.candidateFilterType = filterModel.candidateFilterType;
            componentLifecycleListener.onClose(LibraryFilter.this);
        });

        includedClassesList.setFixedCellWidth(libraryFilterPanelWidth);
        excludedClassesList.setFixedCellWidth(libraryFilterPanelWidth);
        includedMethodsList.setFixedCellWidth(libraryFilterPanelWidth);
        excludedMethodsList.setFixedCellWidth(libraryFilterPanelWidth);

        setUiModels(filterModel);
        resetToDefaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterModel.setFrom(originalFilterModel);
            }
        });


        excludedClassesFromClipboard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        excludedClassesFromClipboard.setToolTipText("Paste from clipboard, one class name each line");
        excludedClassesFromClipboard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        excludedClassesFromClipboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String fromClipboard = getFromClipboard();
                if (fromClipboard == null) return;

                String[] lines = fromClipboard.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    modelExcludedClasses.addElement(line);
                    filterModel.getExcludedClassNames().add(line);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                excludedClassesFromClipboard.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                excludedClassesFromClipboard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }
        });

        includedClassesFromClipboard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        includedClassesFromClipboard.setToolTipText("Paste from clipboard, one class name each line");
        includedClassesFromClipboard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        includedClassesFromClipboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String fromClipboard = getFromClipboard();
                if (fromClipboard == null) return;

                String[] lines = fromClipboard.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    modelIncludedClasses.addElement(line);
                    filterModel.getIncludedClassNames().add(line);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                includedClassesFromClipboard.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                includedClassesFromClipboard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }
        });

        excludedMethodsFromClipboard.setToolTipText("Paste from clipboard, one class name each line");
        excludedMethodsFromClipboard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        excludedMethodsFromClipboard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        excludedMethodsFromClipboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String fromClipboard = getFromClipboard();
                if (fromClipboard == null) return;

                String[] lines = fromClipboard.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    modelExcludedMethods.addElement(line);
                    filterModel.getExcludedMethodNames().add(line);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                excludedMethodsFromClipboard.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                excludedMethodsFromClipboard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }
        });

        includedMethodsFromClipboard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        includedMethodsFromClipboard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        includedMethodsFromClipboard.setToolTipText("Paste from clipboard, one class name each line");
        includedMethodsFromClipboard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String fromClipboard = getFromClipboard();
                if (fromClipboard == null) return;

                String[] lines = fromClipboard.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    modelIncludedMethods.addElement(line);
                    filterModel.getIncludedMethodNames().add(line);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                includedMethodsFromClipboard.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                includedMethodsFromClipboard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }
        });

        addExcludedClassLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addExcludedClassLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        addExcludedClassLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
                        filterModel.getExcludedClassNames().add(newName);
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
            public void mouseEntered(MouseEvent e) {
                addExcludedClassLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                addExcludedClassLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }

        });


        addIncludedClassLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addIncludedClassLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        addIncludedClassLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
                    filterModel.getIncludedClassNames().add(newName);
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
            public void mouseEntered(MouseEvent e) {
                addIncludedClassLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                addIncludedClassLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }

        });


        addIncludedMethodLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addIncludedMethodLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        addIncludedMethodLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
                    filterModel.getIncludedMethodNames().add(newName);
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

                newNameTextField.registerKeyboardAction(saveAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
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
            public void mouseEntered(MouseEvent e) {
                addIncludedMethodLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                addIncludedMethodLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }

        });


        addExcludedMethodLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addExcludedMethodLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        addExcludedMethodLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
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
                    filterModel.getExcludedMethodNames().add(newName);
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
            public void mouseEntered(MouseEvent e) {
                addExcludedMethodLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                addExcludedMethodLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }

        });


        removeExcludedClassLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeExcludedClassLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        removeExcludedClassLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                List<String> selectedValues = excludedClassesList.getSelectedValuesList();
                for (String selectedValue : selectedValues) {
                    filterModel.getExcludedClassNames().remove(selectedValue);
                    modelExcludedClasses.removeElement(selectedValue);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                removeExcludedClassLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                removeExcludedClassLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }

        });

        removeIncludedClassLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeIncludedClassLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        removeIncludedClassLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                List<String> selectedValues = includedClassesList.getSelectedValuesList();
                for (String selectedValue : selectedValues) {
                    filterModel.getIncludedClassNames().remove(selectedValue);
                    modelIncludedClasses.removeElement(selectedValue);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                removeIncludedClassLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                removeIncludedClassLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }

        });


        removeExcludedMethodLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeExcludedMethodLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        removeExcludedMethodLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                List<String> selectedValues = excludedMethodsList.getSelectedValuesList();
                for (String selectedValue : selectedValues) {
                    filterModel.getExcludedMethodNames().remove(selectedValue);
                    modelExcludedMethods.removeElement(selectedValue);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                removeExcludedMethodLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                removeExcludedMethodLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }

        });

        removeIncludedMethodLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeIncludedMethodLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        removeIncludedMethodLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                List<String> selectedValues = includedMethodsList.getSelectedValuesList();
                for (String selectedValue : selectedValues) {
                    filterModel.getIncludedMethodNames().remove(selectedValue);
                    modelIncludedMethods.removeElement(selectedValue);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                removeIncludedMethodLabel.setBorder(BorderFactory.createRaisedBevelBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                removeIncludedMethodLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }
        });

        followEditorCheckBox.addActionListener(e -> filterModel.setFollowEditor(followEditorCheckBox.isSelected()));


    }

	private void setUiModels(LibraryFilterState filterModel) {
        modelIncludedClasses = new DefaultListModel<>();
        modelIncludedClasses.addAll(filterModel.getIncludedClassNames());

        includedClassesList.setModel(modelIncludedClasses);
        includedClassesList.setBorder(BorderFactory.createEmptyBorder());

        modelExcludedClasses = new DefaultListModel<>();
        modelExcludedClasses.addAll(filterModel.getExcludedClassNames());
        excludedClassesList.setModel(modelExcludedClasses);

        modelIncludedMethods = new DefaultListModel<>();
        modelIncludedMethods.addAll(filterModel.getIncludedMethodNames());
        includedMethodsList.setModel(modelIncludedMethods);


        modelExcludedMethods = new DefaultListModel<>();
        modelExcludedMethods.addAll(filterModel.getExcludedMethodNames());
        excludedMethodsList.setModel(modelExcludedMethods);

        if (filterModel.isFollowEditor()) {
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

    public void setOnCloseListener(ComponentLifecycleListener<LibraryFilter> componentLifecycleListener) {
        this.componentLifecycleListener = componentLifecycleListener;
    }
}
