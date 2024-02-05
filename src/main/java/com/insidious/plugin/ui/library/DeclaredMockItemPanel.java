package com.insidious.plugin.ui.library;

import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.ui.stomp.StompComponent;
import com.insidious.plugin.ui.stomp.StompItem;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.insidious.plugin.ui.stomp.StompItem.TAG_LABEL_BACKGROUND_GREY;
import static com.insidious.plugin.ui.stomp.StompItem.TAG_LABEL_TEXT_GREY;

public class DeclaredMockItemPanel {
    private final DeclaredMock declaredMock;
    private JLabel nameLabel;
    private JPanel mainPanel;
    private JPanel tagsContainerPanel;
    private JPanel controlPanel;
    private JPanel controlContainer;
    private JCheckBox selectCandidateCheckbox;

    public DeclaredMockItemPanel(DeclaredMock declaredMock, ItemLifeCycleListener<DeclaredMock> itemLifeCycleListener) {
        this.declaredMock = declaredMock;
        this.nameLabel.setText(declaredMock.getMethodName());
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        String simpleClassName = declaredMock.getFieldTypeName();
        if (simpleClassName.contains(".")) {
            simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
        }
        titledBorder.setTitle("Declared mock definition");

        JLabel preConditionsTag = StompItem.createTagLabel("%s pre conditions",
                new Object[]{declaredMock.getWhenParameter().size()},
                TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY, new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                    }
                });


        tagsContainerPanel.add(preConditionsTag);

        JLabel returnsTag = StompItem.createTagLabel("%s returns",
                new Object[]{declaredMock.getThenParameter().size()},
                TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY, new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                    }
                });


        tagsContainerPanel.add(returnsTag);

        // Timer to update the border title
        String finalSimpleClassName = simpleClassName;
        Timer timer = new Timer(100, e -> {
            String currentTitle = titledBorder.getTitle();
            String targetTitle = finalSimpleClassName;

            // Logic to animate from currentTitle to targetTitle
            if (!currentTitle.equals(targetTitle)) {
                StringBuilder newTitle = new StringBuilder(currentTitle);
                int minLength = Math.min(currentTitle.length(), targetTitle.length());
                for (int i = 0; i < minLength; i++) {
                    if (currentTitle.charAt(i) != targetTitle.charAt(i)) {
                        newTitle.setCharAt(i, targetTitle.charAt(i));
                        break; // Change one character at a time
                    }
                }
                if (currentTitle.length() < targetTitle.length()) {
                    newTitle.append(targetTitle.charAt(currentTitle.length()));
                } else if (currentTitle.length() > targetTitle.length()) {
                    newTitle.deleteCharAt(currentTitle.length() - 1);
                }

                titledBorder.setTitle(newTitle.toString());
                mainPanel.repaint();
            } else {
                ((Timer)e.getSource()).stop(); // Stop the timer when animation is complete
            }
        });

        timer.start();

    }

    public JComponent getComponent() {
        return mainPanel;
    }
}
