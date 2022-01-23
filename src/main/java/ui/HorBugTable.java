package ui;

import Network.GETCalls;
import actions.Constants;
import com.intellij.openapi.wm.ToolWindow;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.IOException;

public class HorBugTable {
    private JPanel panel1;
    private JTable bugs;
    private JPanel panel11;
    private JPanel panel12;
    private JScrollPane scrollpanel;
    private JPanel varpanel;
    private JLabel variables;
    private JLabel values;
    private JButton fetchSessionButton;
    private JButton refreshButton;

    public HorBugTable(ToolWindow toolWindow) {

    }

    public JPanel getContent() {
        return panel1;
    }

    public void setTableValues() {

        Object[] headers = {"Type", "FileName", "LineNum", "Time"};
        Object[][] objects = {
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:01"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Jan-2022 10:00:02"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:03"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Jan-2022 10:00:04"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:05"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Jan-2022 10:00:06"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:07"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19","12-Jan-2022 10:00:08"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:09"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Dec-2022 10:00:11"},
                {"Null Pointer Exception", "/org/io/video/Bug.java", "16", "12-Jan-2022 10:00:14"},
                {"Null Pointer Exception", "/org/io/video/Video.java", "19", "12-Jan-2022 00:00:01"}
        };
        JTableHeader header = this.bugs.getTableHeader();
        header.setFont(new Font("Fira Code", Font.PLAIN, 14));
        this.bugs.setCellEditor(this.bugs.getDefaultEditor( Boolean.class ) );
        DefaultTableModel defaultTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        defaultTableModel.setDataVector(objects, headers);
        this.bugs.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                //System.out.print(bugs.getValueAt(bugs.getSelectedRow(), 0).toString());
            }
        });

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        this.bugs.setModel(defaultTableModel);
        this.bugs.setDefaultRenderer(Object.class, centerRenderer);
        this.bugs.setAutoCreateRowSorter(true);
    }

    public void setVariable(String variablestr) {
        variables.setText(variablestr);
    }

    public void setVarValue(String value) {
        values.setText(value);
    }

    private void getErrors() {

    }

}
