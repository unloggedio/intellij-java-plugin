package com.insidious.plugin.ui.methodscope;

import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Iterator;

public class ResponseMapTable extends AbstractTableModel {
    private final String[] columnNames = {
            "Key", "Value"
    };
    private final ArrayList<String> keys = new ArrayList<>();
    private final ObjectNode response;

    public ResponseMapTable(ObjectNode rightOnly) {
        this.response = rightOnly;
        for (Iterator<String> it = response.fieldNames(); it.hasNext(); ) {
            String key = it.next();
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
        if (columnIndex == 0) {
            String key = keys.get(rowIndex);
            key = key.replaceAll("/", ".");
            return key;
        }
        Object value = response.get(keys.get(rowIndex));
        if (columnIndex == 1) {
            return value;
        }
        return null;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
}
