package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.assertions.AssertionType;
import com.insidious.plugin.assertions.AtomicAssertion;
import com.insidious.plugin.assertions.Expression;
import com.insidious.plugin.assertions.KeyValue;
import com.insidious.plugin.ui.library.ItemLifeCycleListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.JBColor;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AssertionRuleEditPanel {
    public static final Cursor PREDEFINED_HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Project project;
    private final Supplier<List<KeyValue>> keyValueSupplier;
    private JPanel mainPanel;
    private JTextField keyTextField;
    private JTextField valueTextArea;
    private JLabel operationTypeLabel;
    private JLabel dropDownIconLabel;
    private JPanel operationContainer;
    private JPanel gridPanel;
    private boolean justPresentedKeyOptions = false;
    private AssertionType assertionType;

    public AssertionRuleEditPanel(
            AtomicAssertion atomicAssertion,
            ItemLifeCycleListener<AtomicAssertion> componentLifecycleListener,
            Supplier<List<KeyValue>> keyValueSupplier,
            Project project) {
        this.project = project;
        this.keyValueSupplier = keyValueSupplier;
        assertionType = atomicAssertion.getAssertionType();
        AtomicAssertion originalCopy = new AtomicAssertion(atomicAssertion);
        keyTextField.setText(originalCopy.getKey());
        valueTextArea.setText(originalCopy.getExpectedValue());
        operationTypeLabel.setText(originalCopy.getAssertionType().toString());
        dropDownIconLabel.setIcon(AllIcons.Actions.InlayDropTriangle);
        dropDownIconLabel.setCursor(PREDEFINED_HAND_CURSOR);
        operationTypeLabel.setCursor(PREDEFINED_HAND_CURSOR);
        operationContainer.setCursor(PREDEFINED_HAND_CURSOR);
        MouseAdapter showOperationSelectionDropdown = showOperationSelectionDropdown();

        gridPanel.setBackground(JBColor.WHITE);
        mainPanel.setBackground(JBColor.WHITE);
        keyTextField.setBackground(JBColor.WHITE);
        valueTextArea.setBackground(JBColor.WHITE);
        operationContainer.setBackground(JBColor.WHITE);
        operationTypeLabel.setBackground(JBColor.WHITE);


        operationContainer.addMouseListener(showOperationSelectionDropdown);
        operationTypeLabel.addMouseListener(showOperationSelectionDropdown);
        dropDownIconLabel.addMouseListener(showOperationSelectionDropdown);



        keyTextField.addFocusListener(new FocusListener() {

            private JBPopup keyChooserPopup;

            @Override
            public void focusGained(FocusEvent e) {

                if (keyChooserPopup != null || justPresentedKeyOptions) {
                    justPresentedKeyOptions = false;
                    return;
                }

                List<KeyValue> keyValuePairs = keyValueSupplier.get();


//                Vector<Vector<String>> rowVector = keyValuePairs.stream()
//                        .map(e1 -> new Vector<>(List.of(e1.getKey(), e1.getValue())))
//                        .collect(Collectors.toCollection(Vector::new));
//
//
//                Vector<String> columnVector = new Vector<>();
//                DefaultTableModel model = new DefaultTableModel(
//                        rowVector, columnVector
//                );
                List<String> table = keyValuePairs.stream()
                        .map(KeyValue::getKey)
                        .sorted()
                        .collect(Collectors.toList());


                justPresentedKeyOptions = true;
                keyChooserPopup = JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(table)
                        .setCancelOnClickOutside(true)
                        .setCancelOnWindowDeactivation(true)
                        .setCancelKeyEnabled(true)
                        .setRenderer(new DefaultListCellRenderer(){
                            @Override
                            public Border getBorder() {
                                return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)
                                        , super.getBorder());
                            }
                        })
                        .setItemChosenCallback(new Consumer<>() {
                            @Override
                            public void consume(String o) {
                                keyTextField.setText(o);
                                for (KeyValue keyValuePair : keyValuePairs) {
                                    if (keyValuePair.getKey().equals(o)) {
                                        valueTextArea.setText(keyValuePair.getValue());
                                        break;
                                    }
                                }
                            }
                        })
                        .addListener(new JBPopupListener() {
                            @Override
                            public void onClosed(@NotNull LightweightWindowEvent event) {
                                justPresentedKeyOptions = true;
                                keyChooserPopup = null;
                            }
                        })
                        .createPopup();
                keyChooserPopup.showUnderneathOf(keyTextField);
            }

            @Override
            public void focusLost(FocusEvent e) {
                justPresentedKeyOptions = false;
            }
        });


    }

    @NotNull
    private MouseAdapter showOperationSelectionDropdown() {
        return new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                @NotNull JBPopup gutterMethodComponentPopup = JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(Arrays.asList(AssertionType.values()))
                        .setItemChosenCallback(new Consumer<AssertionType>() {
                            @Override
                            public void consume(AssertionType assertionType) {
                                AssertionRuleEditPanel.this.assertionType = assertionType;
                                operationTypeLabel.setText(assertionType.toString());
                            }
                        })
                        .createPopup();

                gutterMethodComponentPopup.showUnderneathOf(operationTypeLabel);
            }
        };
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public AtomicAssertion getUpdatedValue() {
        return new AtomicAssertion(Expression.SELF, assertionType, keyTextField.getText().strip(),
                valueTextArea.getText());
    }
}
