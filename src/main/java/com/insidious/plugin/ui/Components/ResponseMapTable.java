package com.insidious.plugin.ui.Components;

import com.google.common.collect.MapDifference;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Map;

public class ResponseMapTable extends AbstractTableModel {
    private ArrayList<String> keys = new ArrayList<>();

    private final String[] columnNames = {
            "Key", "Old Value", "New Value"
    };

    private Map<String,Object> response;


    public ResponseMapTable(Map<String, Object> rightOnly) {
        this.response=rightOnly;
        for(String key : response.keySet())
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
        Object value = response.get(keys.get(rowIndex));
        if(columnIndex==1)
        {
            return null;
        }
        else if(columnIndex==2)
        {
            return value;
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
}
