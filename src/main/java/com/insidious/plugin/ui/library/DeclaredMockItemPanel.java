package com.insidious.plugin.ui.library;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.*;
import com.insidious.plugin.ui.stomp.StompItem;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.UIUtils;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.insidious.plugin.ui.stomp.StompItem.TAG_LABEL_BACKGROUND_GREY;
import static com.insidious.plugin.ui.stomp.StompItem.TAG_LABEL_TEXT_GREY;

public class DeclaredMockItemPanel {
    private final DeclaredMock declaredMock;
    private final ActionToolbarImpl actionToolbar;
    private final InsidiousService insidiousService;
    private JLabel nameLabel;
    private JPanel mainPanel;
    private JPanel tagsContainerPanel;
    private JPanel controlContainer;
    private JCheckBox selectCandidateCheckbox;
    private JLabel deleteButton1;
    private JPanel mainPanelSub;
    private JPanel topContainerPanel;
    private JPanel nameContainerPanel;
    private JPanel actionPanel;
    private boolean isUnsaved;

    public DeclaredMockItemPanel(DeclaredMock declaredMock, ItemLifeCycleListener<DeclaredMock> itemLifeCycleListener
            , InsidiousService insidiousService) {
        this.declaredMock = declaredMock;
        this.insidiousService = insidiousService;
        boolean isMockActive = insidiousService.isMockEnabled(declaredMock);
        if (isMockActive) {
            mainPanel.setBorder(
                    BorderFactory.createTitledBorder(BorderFactory.createLineBorder(JBColor.GREEN))
            );
        }

        String methodName = declaredMock.getMethodName();
        if (methodName.length() > 25) {
            methodName = methodName.substring(0, 20) + "...";
        }
        this.nameLabel.setText(methodName);

        AnAction editAction = new AnAction(() -> "Edit", UIUtils.EDIT) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                itemLifeCycleListener.onEdit(declaredMock);
            }
        };

        java.util.List<AnAction> action11 = new ArrayList<>();
//        action11.add(deleteAction);
        action11.add(editAction);
        actionToolbar = new ActionToolbarImpl(
                "Live View", new DefaultActionGroup(action11), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);
        actionPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);


        nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                itemLifeCycleListener.onClick(declaredMock);
            }
        });
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        String simpleClassName = declaredMock.getFieldTypeName();
        if (simpleClassName.contains(".")) {
            simpleClassName = simpleClassName.substring(simpleClassName.lastIndexOf(".") + 1);
        }
        titledBorder.setTitle(declaredMock.getName());
        titledBorder.setTitleColor(JBColor.BLACK);


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


        int thenParameterSize = declaredMock.getThenParameter().size();
        ThenParameter thenParameter = declaredMock.getThenParameter().get(0);
        ReturnValue returnParameter = thenParameter.getReturnParameter();

        String className = returnParameter.getClassName();
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf("<"));
        }
        if (className.contains(".")) {
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

        returnsTag.addMouseListener(new MouseAdapter() {
            @Override
            public synchronized void mouseEntered(MouseEvent e) {
                if (returnsTag.getToolTipText() != null) {
                    return;
                }
                JsonNode jsonNode = null;
                try {
                    jsonNode = new ObjectMapper().readTree(returnParameter.getValue());
                    @Nullable String returnValueHover = StompItem.getPrettyPrintedArgumentsHtml(jsonNode);
                    returnsTag.setToolTipText(returnValueHover);
                } catch (JsonProcessingException e1) {
                    jsonNode = ObjectMapperInstance.getInstance().getNodeFactory().textNode(returnParameter.getValue());
                    returnsTag.setToolTipText(StompItem.getPrettyPrintedArgumentsHtml(jsonNode));
                }

                super.mouseEntered(e);
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
//        String finalSimpleClassName = simpleClassName;
//        Timer timer = new Timer(100, e -> {
//            String currentTitle = titledBorder.getTitle();
//            String targetTitle = finalSimpleClassName;
//
//            // Logic to animate from currentTitle to targetTitle
//            if (!currentTitle.equals(targetTitle)) {
//                StringBuilder newTitle = new StringBuilder(currentTitle);
//                int minLength = Math.min(currentTitle.length(), targetTitle.length());
//                for (int i = 0; i < minLength; i++) {
//                    if (currentTitle.charAt(i) != targetTitle.charAt(i)) {
//                        newTitle.setCharAt(i, targetTitle.charAt(i));
//                        break; // Change one character at a time
//                    }
//                }
//                if (currentTitle.length() < targetTitle.length()) {
//                    newTitle.append(targetTitle.charAt(currentTitle.length()));
//                } else if (currentTitle.length() > targetTitle.length()) {
//                    newTitle.deleteCharAt(currentTitle.length() - 1);
//                }
//
//                titledBorder.setTitle(newTitle.toString());
//                mainPanel.repaint();
//            } else {
//                ((Timer) e.getSource()).stop(); // Stop the timer when animation is complete
//            }
//        });

//        timer.start();


//        MouseAdapter showDeleteButtonListener = new MouseAdapter() {
//            @Override
//            public void mouseEntered(MouseEvent e) {
////                controlPanel.setVisible(true);
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e) {
////                controlPanel.setVisible(false);
//            }
//        };

//        controlPanel.setVisible(false);


//        mainPanel.addMouseListener(showDeleteButtonListener);
//        mainPanelSub.addMouseListener(showDeleteButtonListener);
//        nameContainerPanel.addMouseListener(showDeleteButtonListener);
//        actionPanel.addMouseListener(showDeleteButtonListener);
//        actionToolbar.getComponent().addMouseListener(showDeleteButtonListener);
//        nameLabel.addMouseListener(showDeleteButtonListener);
//        controlPanel.addMouseListener(showDeleteButtonListener);
//        controlContainer.addMouseListener(showDeleteButtonListener);
//        selectCandidateCheckbox.addMouseListener(showDeleteButtonListener);




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
        selectCandidateCheckbox.setVisible(b);
    }

    public boolean isUnsaved() {
        return isUnsaved;
    }

    public void setUnsaved(boolean isUnsaved) {
        this.isUnsaved = isUnsaved;
        if (isUnsaved) {
            TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
            String title = titledBorder.getTitle();
//            title = "[unsaved] " + title;
            TitledBorder titledBorder1 = BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(JBColor.ORANGE));
            titledBorder1.setTitle(title);
            mainPanel.setBorder(
                    titledBorder1
            );
        }
    }
}
