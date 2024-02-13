package com.insidious.plugin.ui.library;

import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.mocking.ParameterMatcher;
import com.insidious.plugin.mocking.ParameterMatcherType;
import com.insidious.plugin.mocking.ThenParameter;
import com.insidious.plugin.ui.InsidiousUtils;
import com.insidious.plugin.ui.stomp.StompItem;
import com.insidious.plugin.util.ClassTypeUtils;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.stream.Collectors;

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
    private JLabel deleteButton;
    private JPanel mainPanelSub;
    private JPanel topContainerPanel;
    private JPanel nameContainerPanel;

    public DeclaredMockItemPanel(DeclaredMock declaredMock, ItemLifeCycleListener<DeclaredMock> itemLifeCycleListener, Project project) {
        this.declaredMock = declaredMock;
        this.nameLabel.setText(declaredMock.getMethodName());
        nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    InsidiousUtils.focusInEditor(declaredMock.getFieldTypeName(),
                            declaredMock.getMethodName(), project);
                });
            }
        });
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        String simpleClassName = declaredMock.getFieldTypeName();
        if (simpleClassName.contains(".")) {
            simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
        }
        titledBorder.setTitle("Declared mock definition");


        Map<ParameterMatcherType, java.util.List<ParameterMatcher>> countByType = declaredMock.getWhenParameter()
                .stream().collect(Collectors.groupingBy(ParameterMatcher::getType));
        int whenParametersSize = declaredMock.getWhenParameter().size();

        for (Map.Entry<ParameterMatcherType, java.util.List<ParameterMatcher>> parameterMatcherTypeListEntry : countByType.entrySet()) {

            ParameterMatcherType type = parameterMatcherTypeListEntry.getKey();
            java.util.List<ParameterMatcher> value = parameterMatcherTypeListEntry.getValue();
            int count = value.size();

            JLabel preConditionsTag = StompItem.createTagLabel(type.toString() + " " + value.get(0).getName(),
                    new Object[]{whenParametersSize},
                    TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY, new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            super.mouseClicked(e);
                        }
                    });
            tagsContainerPanel.add(preConditionsTag);


        }





        deleteButton.setVisible(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                itemLifeCycleListener.onDelete(declaredMock);
            }
        });

        int thenParameterSize = declaredMock.getThenParameter().size();
        ThenParameter thenParameter = declaredMock.getThenParameter().get(0);
        String className = thenParameter.getReturnParameter().getClassName();
        if(className.contains(".")) {
            className = className.substring(className.lastIndexOf(".") + 1);
        }
        JLabel returnsTag =
                StompItem.createTagLabel(thenParameter.getMethodExitType() + " " + className,
                new Object[]{thenParameterSize},
                TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY, new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                    }
                });


        tagsContainerPanel.add(returnsTag);


        selectCandidateCheckbox.addActionListener(e -> {
            if (selectCandidateCheckbox.isSelected()) {
                itemLifeCycleListener.onSelect(declaredMock);
            } else {
                itemLifeCycleListener.onUnSelect(declaredMock);
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

        MouseAdapter showDeleteButtonListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                deleteButton.setVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                deleteButton.setVisible(false);
            }
        };
        mainPanel.addMouseListener(showDeleteButtonListener);
        mainPanelSub.addMouseListener(showDeleteButtonListener);
        nameContainerPanel.addMouseListener(showDeleteButtonListener);
        nameLabel.addMouseListener(showDeleteButtonListener);
        controlPanel.addMouseListener(showDeleteButtonListener);
        controlContainer.addMouseListener(showDeleteButtonListener);
        selectCandidateCheckbox.addMouseListener(showDeleteButtonListener);
        deleteButton.addMouseListener(showDeleteButtonListener);

    }

    public void setSelected(boolean b) {
        selectCandidateCheckbox.setSelected(b);
    }


    public JComponent getComponent() {
        return mainPanel;
    }

    public DeclaredMock getDeclaredMock() {
        return declaredMock;
    }

    public void setIsSelectable(boolean b) {
        controlContainer.setVisible(b);
    }
}
