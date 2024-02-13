package com.insidious.plugin.ui.library;

import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.InsidiousUtils;
import com.insidious.plugin.ui.stomp.StompItem;
import com.insidious.plugin.util.AtomicAssertionUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.insidious.plugin.ui.stomp.StompItem.TAG_LABEL_BACKGROUND_GREY;
import static com.insidious.plugin.ui.stomp.StompItem.TAG_LABEL_TEXT_GREY;

public class StoredCandidateItemPanel {
    private final StoredCandidate storedCandidate;
    private JLabel nameLabel;
    private JPanel mainPanel;
    private JPanel tagsContainerPanel;
    private JPanel controlPanel;
    private JPanel controlContainer;
    private JCheckBox selectCandidateCheckbox;
    private JLabel deleteButton;

    public StoredCandidateItemPanel(StoredCandidate storedCandidate,
                                    ItemLifeCycleListener<StoredCandidate> itemLifeCycleListener, Project project) {
        this.storedCandidate = storedCandidate;
        this.nameLabel.setText(storedCandidate.getMethod().getName());
        nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!storedCandidate.getLineNumbers().isEmpty()) {
                    Integer firstLine = storedCandidate.getLineNumbers().get(0);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        InsidiousUtils.focusProbeLocationInEditor(firstLine,
                                storedCandidate.getMethod().getClassName(), project);
                    });
                } else {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        InsidiousUtils.focusInEditor(storedCandidate.getMethod().getClassName(),
                                storedCandidate.getMethod().getName(), project);
                    });
                }
            }
        });
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        String simpleClassName = storedCandidate.getMethod().getClassName();
        if (simpleClassName.contains(".")) {
            simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
        }
        titledBorder.setTitle("Saved replay test");


        int lineCount = storedCandidate.getLineNumbers().size();
        JLabel preConditionsTag = StompItem.createTagLabel("%s line" + (lineCount == 1 ? "" : "s"),
                new Object[]{lineCount},
                TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY, new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                    }
                });


        tagsContainerPanel.add(preConditionsTag);

        int assertionCount = AtomicAssertionUtils.countAssertions(storedCandidate.getTestAssertions());
        JLabel returnsTag = StompItem.createTagLabel("%s assertion" + (assertionCount == 1 ? "" : "s"),
                new Object[]{assertionCount},
                TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY, new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                    }
                });


        tagsContainerPanel.add(returnsTag);


        selectCandidateCheckbox.addActionListener(e -> {
            if (selectCandidateCheckbox.isSelected()) {
                itemLifeCycleListener.onSelect(storedCandidate);
            } else {
                itemLifeCycleListener.onUnSelect(storedCandidate);
            }
        });
        deleteButton.setVisible(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                itemLifeCycleListener.onDelete(storedCandidate);
            }
        });


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
                ((Timer) e.getSource()).stop(); // Stop the timer when animation is complete
            }
        });

        timer.start();

    }

    public void setSelected(boolean b) {
        selectCandidateCheckbox.setSelected(b);
    }


    public JComponent getComponent() {
        return mainPanel;
    }

    public StoredCandidate getStoredCandidate() {
        return storedCandidate;
    }

    public void setIsSelectable(boolean b) {
        controlContainer.setVisible(b);
    }
}
