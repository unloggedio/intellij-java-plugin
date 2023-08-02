package com.insidious.plugin.ui.methodscope;


import com.insidious.plugin.util.UIUtils;
import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class CompareTableModel extends AbstractTableModel {

    private final String[] columnNames = {
            "Key", "Expected", "Actual"
    };
    private final List<DifferenceInstance> differences;
    private final JTable table;
    private boolean headerSetup = false;
    private Color defaultColor = null;
    private Color defaultForeground = null;

    public CompareTableModel(List<DifferenceInstance> differences, JTable table) {
        this.differences = differences;
        this.table = table;
        Color color = (Color) UIManager.get("Table.background");
        this.defaultForeground = (Color) UIManager.get("Table.foreground");
        this.defaultColor = color;
        this.table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                if (differences != null && differences.size() > 0) {
                    DifferenceInstance i = differences.get(row);
                    if (i.getDifferenceType().equals(DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY)) {
                        if (column == 1 || column == 2) {
                            c.setBackground(UIUtils.green);
                            c.setForeground(JBColor.WHITE);
                            return c;
                        }
                    }
                }
                c.setBackground(defaultColor);
                c.setForeground(defaultForeground);
                return c;
            }
        });

    }

    @Override
    public int getRowCount() {
        return differences.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        if (!headerSetup) {
            try {
                //setupHeader();
            } catch (Exception e) {
                System.out.println("Failed to set header");
            }
        }
        DifferenceInstance instance = differences.get(rowIndex);
        if (columnIndex == 0) {
            String key = instance.getKey();
//            key = key.replaceAll("/", ".");
//            key = key.replaceFirst("\\.", "");
            return key;
        }
        if (columnIndex == 1) {
            return instance.getLeftValue();
        } else if (columnIndex == 2) {
            return instance.getRightValue();
        }
        return null;
    }

    public void setupHeader() {
        Border headerBorder = UIManager.getBorder("TableHeader.cellBorder");

        JLabel keyHeader = new JLabel(columnNames[0], UIUtils.JSON_KEY, JLabel.CENTER);
        keyHeader.setBorder(headerBorder);

        JLabel oldHeader = new JLabel(columnNames[1], UIUtils.OLD_KEY, JLabel.CENTER);
        oldHeader.setBorder(headerBorder);

        JLabel newHeader = new JLabel(columnNames[2], UIUtils.NEW_KEY, JLabel.CENTER);
        newHeader.setBorder(headerBorder);

        TableCellRenderer renderer = new JComponentTableCellRenderer();
        TableColumnModel columnModel = this.table.getColumnModel();

        TableColumn column0 = columnModel.getColumn(0);
        TableColumn column1 = columnModel.getColumn(1);
        TableColumn column2 = columnModel.getColumn(2);

        column0.setHeaderRenderer(renderer);
        column0.setHeaderValue(keyHeader);

        column1.setHeaderRenderer(renderer);
        column1.setHeaderValue(oldHeader);

        column2.setHeaderRenderer(renderer);
        column2.setHeaderValue(newHeader);
        headerSetup = true;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    static class JComponentTableCellRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            return (Component) value;
        }
    }
}
