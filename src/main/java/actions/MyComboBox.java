package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.StatusBar;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.UUID;

public class MyComboBox extends AnAction implements CustomComponentAction {
    List<String> ids;
    ComboBox<String> jComboBox;

    public MyComboBox() {
        jComboBox = new ComboBox<>();
        jComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String threadId = (String)jComboBox.getSelectedItem();

            }
        });

        //setDefaultIcon(false);
        Icon icon = new ImageIcon("");

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

    }

    @Override
    public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {

        if (ids == null) {
            jComboBox.addItem("Thread Id: ");
            jComboBox.setEnabled(true);
            return jComboBox;
        }

        //presentation.setIcon(new ImageIcon("/videobug/icon-16x16.png"));


        return jComboBox;
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void updateCustomComponent(@NotNull JComponent component, @NotNull Presentation presentation) {
        CustomComponentAction.super.updateCustomComponent(component, presentation);
    }


    public void updateUI(List<String> ids) {
        this.ids = ids;
        String[] array = this.ids.toArray(new String[0]);

        jComboBox.setModel(new DefaultComboBoxModel<>(array));

        jComboBox.setSelectedIndex(0);

        jComboBox.updateUI();
    }

    private void getVarsValues(String threadId) {

    }

}
