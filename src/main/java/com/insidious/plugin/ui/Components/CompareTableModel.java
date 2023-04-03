package com.insidious.plugin.ui.Components;


import javax.swing.table.AbstractTableModel;
import java.util.List;

public class CompareTableModel extends AbstractTableModel {

    private List<DifferenceInstance> differences;
    private final String[] columnNames = {
            "Key", "Old Value", "New Value"
    };

    public CompareTableModel(List<DifferenceInstance> differences) {
        this.differences = differences;
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

        DifferenceInstance instance = differences.get(rowIndex);
        if(columnIndex==0)
        {
            String key = instance.getKey();
            key = key.replaceAll("/",".");
            key = key.replaceFirst("\\.","");
            return key;
        }
        if(columnIndex==1)
        {
            return instance.getLeftValue();
        }
        else if(columnIndex==2)
        {
            return instance.getRightValue();
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
}
