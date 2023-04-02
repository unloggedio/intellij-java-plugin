package com.insidious.plugin.ui.Components;

import com.google.common.collect.MapDifference;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Map;

public class CompareTableModel extends AbstractTableModel {

    private Map<String, MapDifference.ValueDifference<Object>> differences;

    private ArrayList<String> keys = new ArrayList<>();

    private final String[] columnNames = {
            "Key", "Old Value", "New Value"
    };

    public CompareTableModel(Map<String, MapDifference.ValueDifference<Object>> differences) {
        this.differences = differences;
        for(String key : differences.keySet())
        {
            keys.add(key);
        }
    }

    @Override
    public int getRowCount() {
        return keys.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        if(columnIndex==0)
        {
            String key = keys.get(rowIndex);
            key = key.replaceAll("/",".");
            key = "root"+key;
            return key;
        }
        MapDifference.ValueDifference difference = differences.get(keys.get(rowIndex));
        if(columnIndex==1)
        {
            return difference.leftValue();
        }
        else if(columnIndex==2)
        {
            return difference.rightValue();
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
}
