package com.insidious.plugin.ui.testrunnerinjection.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Custom combo box with checkboxes.
 */
public class CheckComboBox extends JComboBox<JCheckBox> {
    private ArrayList<JCheckBox> items;

    /**
     * Constructor to initialize the CheckComboBox with an array of checkboxes.
     *
     * @param items array of checkboxes to be displayed in the combo box
     */
    public CheckComboBox(JCheckBox[] items) {
        super(items);
        this.items = new ArrayList<>();

        //add items to the list
        for (JCheckBox item : items) {
            this.items.add(item);
        }

        //setting custom renderer
        setRenderer(new CheckBoxListRenderer());

        //setting editor panel to show the message
        setEditor(new BasicComboBoxEditor() {
            @Override
            public Component getEditorComponent() {
                return new JLabel("Choose the modules where you want to inject this file");
            }
        });
        setEditable(true);

        //ActionListener to handle checkbox selection
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedItem = getSelectedItem();
                if (selectedItem instanceof JCheckBox) {
                    JCheckBox checkBox = (JCheckBox) selectedItem;
                    if (checkBox.isEnabled()) { // Check if the checkbox is enabled before toggling
                        checkBox.setSelected(!checkBox.isSelected());
                        refreshDisplay(); // Refresh the display to reflect selection changes
                    }
                }
            }
        });

        // Use a MouseListener to handle the selection state
        getEditor().getEditorComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                hidePopup();
            }
        });
    }

    /**
     * Override setPopupVisible to keep the popup open during selection.
     *
     * @param v true to set the popup visible, false otherwise
     */
    @Override
    public void setPopupVisible(boolean v) {
        // Override to keep the popup open on selection
        if (v) {
            super.setPopupVisible(v);
        }
    }

    /**
     * Override firePopupMenuCanceled to prevent the popup from closing.
     */
    @Override
    public void firePopupMenuCanceled() {
        // Override to prevent popup from closing
    }

    /**
     * Returns the list of checkboxes that are currently checked.
     *
     * @return an ArrayList of checked JCheckBox items
     */
    public ArrayList<JCheckBox> getCheckedItems() {
        ArrayList<JCheckBox> checkedItems = new ArrayList<>();
        for (JCheckBox item : items) {
            if (item.isSelected()) {
                checkedItems.add(item);
            }
        }
        return checkedItems;
    }

    /**
     * Hacky way to refresh the checkboxes quickly by forcing a repaint and revalidate.
     */
    private void refreshDisplay() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Temporarily select an invisible item to force refresh
                setSelectedItem(null);
                revalidate();
                repaint();
                setSelectedItem(getSelectedItem());
            }
        });
    }
}