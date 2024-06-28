package com.insidious.plugin.ui.testrunnerinjection.components;

import javax.swing.*;
import java.awt.*;

/**
 * Renderer to display checkboxes in a combo box.
 */
public class CheckBoxListRenderer extends JCheckBox implements ListCellRenderer<Object> {

    /**
     * Configures the renderer based on the properties of the checkbox being rendered.
     *
     * @param list         the JList being rendered
     * @param value        the value to display
     * @param index        the cell index
     * @param isSelected   true if the cell is selected
     * @param cellHasFocus true if the cell has the focus
     * @return the component used for rendering
     */
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) value;
            setEnabled(checkBox.isEnabled());
            setFont(list.getFont());
            setBackground(list.getBackground());
            setForeground(list.getForeground());
            setSelected(checkBox.isSelected());
            setText(checkBox.getText());
        }
        return this;
    }
}

