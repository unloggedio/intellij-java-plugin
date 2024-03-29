package com.insidious.plugin.ui.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.insidious.plugin.callbacks.ExecutionRequestSourceType;
import com.insidious.plugin.callbacks.TestCandidateLifeListener;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.atomic.ClassUnderTest;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.InsidiousUtils;
import com.insidious.plugin.ui.methodscope.HighlightedRequest;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.insidious.plugin.util.UIUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class StompItem {
    public static final JBColor TAG_LABEL_BACKGROUND_GREY = new JBColor(new Color(235, 235, 238),
            new Color(235, 235, 238));
    public static final JBColor TAG_LABEL_TEXT_GREY = new JBColor(
            new Color(113, 128, 150, 255),
            new Color(113, 128, 150, 255));
    public static final int MAX_METHOD_NAME_LABEL_LENGTH = 25;
    public static final JBColor HOVER_HIGHLIGHT_COLOR = new JBColor(
            new Color(121, 64, 64),
            new Color(147, 125, 125)
    );
    private static final Logger logger = LoggerUtil.getInstance(StompItem.class);
    private final TestCandidateLifeListener testCandidateLifeListener;
    private final Color defaultPanelColor;
    private final InsidiousService insidiousService;
    private final JCheckBox selectCandidateCheckbox;
    private final ActionToolbarImpl actionToolbar;
    private TestCandidateMetadata candidateMetadata;
    private JPanel mainPanel;
    private JLabel statusLabel;
    //    private JLabel pinLabel;
    private JLabel timeTakenMsLabel;
    private JLabel lineCoverageLabel;
    private JLabel candidateTitleLabel;
    private JLabel parameterCountLabel;
    private JLabel callsCountLabel;
    private JPanel detailPanel;
    private JPanel infoPanel;
    private JPanel titleLabelContainer;
    private JPanel metadataPanel;
    private JPanel controlPanel;
    private JPanel controlContainer;
    private boolean isPinned = false;

    private boolean requestedHighlight = false;

    public StompItem(
            TestCandidateMetadata testCandidateMetadata,
            TestCandidateLifeListener testCandidateLifeListener,
            InsidiousService insidiousService, JCheckBox selectCandidateCheckbox) {
        this.selectCandidateCheckbox = selectCandidateCheckbox;
        this.candidateMetadata = testCandidateMetadata;
        this.testCandidateLifeListener = testCandidateLifeListener;
        this.insidiousService = insidiousService;
        defaultPanelColor = mainPanel.getBackground();
//        candidateTitleLabel.setIcon(AllIcons.Actions.RunToCursor);

        MouseAdapter methodNameClickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (!candidateMetadata.getLineNumbers().isEmpty()) {
                    Integer firstLine = candidateMetadata.getLineNumbers().get(0);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        InsidiousUtils.focusProbeLocationInEditor(firstLine,
                                candidateMetadata.getFullyQualifiedClassname(), insidiousService.getProject());
                    });
                }
            }
        };
        titleLabelContainer.addMouseListener(methodNameClickListener);

        candidateTitleLabel.addMouseListener(methodNameClickListener);

        mainPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                testCandidateLifeListener.onCandidateSelected(candidateMetadata, e);
                if (!candidateMetadata.getLineNumbers().isEmpty()) {
                    Integer firstLine = candidateMetadata.getLineNumbers().get(0);
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        InsidiousUtils.focusProbeLocationInEditor(firstLine,
                                candidateMetadata.getFullyQualifiedClassname(), insidiousService.getProject());
                    });
                }

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hoverOn();
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverOff();
                super.mouseExited(e);
            }
        });

        mainPanel.revalidate();

//        pinLabel.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (isPinned) {
//                    isPinned = false;
//                    pinLabel.setIcon(UIUtils.PUSHPIN_LINE);
//                } else {
//                    isPinned = true;
//                    pinLabel.setIcon(UIUtils.PUSHPIN_2_FILL);
//                }
//            }
//
//            @Override
//            public void mouseEntered(MouseEvent e) {
//                hoverOn();
//                if (isPinned) {
//                    pinLabel.setIcon(UIUtils.UNPIN_LINE);
//                } else {
//                    pinLabel.setIcon(UIUtils.PUSHPIN_2_LINE);
//                }
//                super.mouseEntered(e);
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e) {
//                hoverOff();
//                if (isPinned) {
//                    pinLabel.setIcon(UIUtils.PUSHPIN_2_FILL);
//                } else {
//                    pinLabel.setIcon(UIUtils.PUSHPIN_LINE);
//                }
//                super.mouseExited(e);
//            }
//        });


//        pinLabel.setIcon(UIUtils.PUSHPIN_LINE);
//        pinLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        new GotItTooltip("Unlogged.Stomp.Item.Replay",
//                "Replay a single method again using this button", insidiousService.getProject())
//                .withPosition(Balloon.Position.above)
//                .show(replaySingle, GotItTooltip.TOP_MIDDLE);


        AnAction replayAction = new AnAction(() -> "Replay", UIUtils.REPLAY_PINK) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    DumbService.getInstance(insidiousService.getProject())
                            .runReadActionInSmartMode(() -> {
                                testCandidateLifeListener.executeCandidate(Collections.singletonList(candidateMetadata),
                                        new ClassUnderTest(candidateMetadata.getFullyQualifiedClassname()),
                                        ExecutionRequestSourceType.Single,
                                        (testCandidate, agentCommandResponse, diffResult) -> {
                                            // do something with the response.
                                        });
                            });
                });
            }
        };

        ArrayList<AnAction> action11 = new ArrayList<>();
        action11.add(replayAction);
        actionToolbar = new ActionToolbarImpl(
                "Live View", new DefaultActionGroup(action11), true);
        actionToolbar.setMiniMode(false);
        actionToolbar.setForceMinimumSize(true);
        actionToolbar.setTargetComponent(mainPanel);


        controlContainer.add(actionToolbar.getComponent(), BorderLayout.CENTER);
//        replaySingle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//        replaySingle.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                ApplicationManager.getApplication().executeOnPooledThread(() -> {
//                    DumbService.getInstance(insidiousService.getProject())
//                            .runReadActionInSmartMode(() -> {
//                                testCandidateLifeListener.executeCandidate(Collections.singletonList(candidateMetadata),
//                                        new ClassUnderTest(candidateMetadata.getFullyQualifiedClassname()),
//                                        ExecutionRequestSourceType.Single,
//                                        (testCandidate, agentCommandResponse, diffResult) -> {
//                                            // do something with the response.
//                                        });
//                            });
//                });
//            }
//
//            @Override
//            public void mouseEntered(MouseEvent e) {
//                hoverOn();
//                super.mouseEntered(e);
//            }
//
//            @Override
//            public void mouseExited(MouseEvent e) {
//                hoverOff();
//                super.mouseExited(e);
//            }
//        });


        MethodCallExpression mainMethod = candidateMetadata.getMainMethod();
        MethodUnderTest methodUnderTest = new MethodUnderTest(
                mainMethod.getMethodName(), "",
                0, candidateMetadata.getFullyQualifiedClassname()
        );

        String className = methodUnderTest.getClassName();
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf(".") + 1);
        }
//        setTitledBorder("[" + candidateMetadata.getEntryProbeIndex() + "] " + className);
        long timeTakenMs = (mainMethod.getReturnValue().getProb().getRecordedAt() -
                mainMethod.getEntryProbe().getRecordedAt()) / (1000 * 1000);
        String itemLabel = String.format("%s", className);
        if (itemLabel.length() > MAX_METHOD_NAME_LABEL_LENGTH) {
            itemLabel = itemLabel.substring(0, MAX_METHOD_NAME_LABEL_LENGTH - 3) + "...";
        }


        ExecutionTimeCategory category = ExecutionTimeCategorizer.categorizeExecutionTime(timeTakenMs);
        String timeTakenMsString = ExecutionTimeCategorizer.formatTimePeriod(timeTakenMs);


        TitledBorder detailPanelBorder = (TitledBorder) mainPanel.getBorder();
        detailPanelBorder.setTitle(itemLabel);
        String methodNameText = methodUnderTest.getName() + "()";
        if (methodNameText.length() > 21) {
            methodNameText = methodNameText.substring(0, 17) + "..()";
        }
        candidateTitleLabel.setText("⬁" + methodNameText);
        candidateTitleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter labelMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoverOn();
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverOff();
                super.mouseExited(e);
            }
        };

        timeTakenMsLabel = createTagLabel("⏱ %s", new Object[]{timeTakenMsString}, Color.decode(category.getColorHex()),
                new JBColor(Gray._255, Gray._255),
                labelMouseAdapter);
        int tagCount = 1;
        metadataPanel.add(timeTakenMsLabel);

        if (candidateMetadata.getLineNumbers().size() > 1) {
            lineCoverageLabel = createTagLabel("+%d lines", new Object[]{candidateMetadata.getLineNumbers().size()},
                    TAG_LABEL_BACKGROUND_GREY, Color.decode(ExecutionTimeCategory.INSTANTANEOUS.getColorHex()),
                    labelMouseAdapter);
            tagCount++;
            lineCoverageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lineCoverageLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!requestedHighlight) {
                        requestedHighlight = true;
                        MethodUnderTest mut = MethodUnderTest.fromTestCandidateMetadata(candidateMetadata);
                        HighlightedRequest highlightRequest = new HighlightedRequest(
                                mut, new HashSet<>(candidateMetadata.getLineNumbers())
                        );
                        insidiousService.highlightLines(highlightRequest);
                    } else {
                        requestedHighlight = false;
                        insidiousService.removeCurrentActiveHighlights();
                    }
                }
            });
            metadataPanel.add(lineCoverageLabel);
        }
        ObjectMapper objectMapper = ObjectMapperInstance.getInstance();

        List<Parameter> argumentProbes = mainMethod.getArguments();
        if (!argumentProbes.isEmpty()) {


            parameterCountLabel = createTagLabel("%d Argument" + (
                            argumentProbes.size() > 1 ? "s" : ""
                    ), new Object[]{argumentProbes.size()}, TAG_LABEL_BACKGROUND_GREY,
                    TAG_LABEL_TEXT_GREY, labelMouseAdapter);

            ObjectNode parametersNode = objectMapper.getNodeFactory().objectNode();
            List<Parameter> arguments = mainMethod.getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                Parameter argument = arguments.get(i);
                JsonNode value = ClassTypeUtils.getValueForParameter(argument);
                String name = argument.getName();
                if (name == null) {
                    name = "Arg" + i;
                }
                parametersNode.set(name, value);
            }
            try {
                String prettyPrintedArguments = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(parametersNode);
                prettyPrintedArguments = prettyPrintedArguments.replaceAll("\\n", "<br/>");
                prettyPrintedArguments = prettyPrintedArguments.replaceAll(" ", "&nbsp;");
                String prettyPrintedArgumentsHtml = "<html>" + prettyPrintedArguments + "</html>";
                parameterCountLabel.setToolTipText(prettyPrintedArgumentsHtml);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                //
            }


            parameterCountLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                }
            });

            tagCount++;
            metadataPanel.add(parameterCountLabel);
        }

        if (tagCount < 4) {


            String returnValueClassName = mainMethod.getReturnValue().getType();
            if (returnValueClassName != null && returnValueClassName.contains(".")) {
                returnValueClassName = returnValueClassName.substring(returnValueClassName.lastIndexOf(".") + 1);
            }

            JLabel returnValueTag = createTagLabel("ᐊ " + returnValueClassName, new Object[]{argumentProbes.size()},
                    TAG_LABEL_BACKGROUND_GREY,
                    TAG_LABEL_TEXT_GREY, labelMouseAdapter);


            String prettyPrintedArgumentsHtml = getPrettyPrintedArgumentsHtml(
                    ClassTypeUtils.getValueForParameter(mainMethod.getReturnValue()));

            if (prettyPrintedArgumentsHtml != null) {
                returnValueTag.setToolTipText(prettyPrintedArgumentsHtml);
            }
            tagCount++;
            metadataPanel.add(returnValueTag);
        }

        if (tagCount < 4) {
            int size = candidateMetadata.getCallsList().size();
            if (size > 0) {
                callsCountLabel = createTagLabel("%d Downstream", new Object[]{size},
                        TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY, labelMouseAdapter);
//            callsCountLabel.setToolTipText("Click to show downstream calls");
//            callsCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
//            callsCountLabel.addMouseListener(new MouseAdapter() {
//                @Override
//                public void mouseClicked(MouseEvent e) {
//                    testCandidateLifeListener.onExpandChildren(candidateMetadata);
//                    super.mouseClicked(e);
//                }
//            });
                metadataPanel.add(callsCountLabel);
            }
        }


        selectCandidateCheckbox.addMouseListener(labelMouseAdapter);


        selectCandidateCheckbox.addActionListener(e -> {
            if (selectCandidateCheckbox.isSelected()) {
                testCandidateLifeListener.onSelected(candidateMetadata);
            } else {
                testCandidateLifeListener.unSelected(candidateMetadata);
            }
        });


    }

    @Nullable
    public static String getPrettyPrintedArgumentsHtml(JsonNode valueForParameter) {
        String prettyPrintedArgumentsHtml = null;


        try {
            String prettyPrintedArguments = ObjectMapperInstance.getInstance().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(valueForParameter);
            if (prettyPrintedArguments.length() > 500) {
                prettyPrintedArguments = prettyPrintedArguments.substring(0, 500);
            }
            prettyPrintedArguments = prettyPrintedArguments.replaceAll("\\n", "<br/>");
            prettyPrintedArguments = prettyPrintedArguments.replaceAll(" ", "&nbsp;");
            prettyPrintedArgumentsHtml = "<html>" + prettyPrintedArguments + "</html>";
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            //
        }
        return prettyPrintedArgumentsHtml;
    }

    public static JLabel createTagLabel(String tagText, Object[] value, Color backgroundColor, Color foreground,
                                        MouseAdapter mouseAdapter) {
        JLabel label = new JLabel();
        label.setText(String.format("<html><small>" + tagText + "</small></html>", value));

        label.setOpaque(true);
        JPopupMenu jPopupMenu = new JPopupMenu("Yay");
        jPopupMenu.add(new JMenuItem("item 1", UIUtils.GHOST_MOCK));
        label.setComponentPopupMenu(jPopupMenu);
        label.setBackground(backgroundColor);
        label.setForeground(foreground);

        // Creating a rounded border
        Border roundedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(backgroundColor, 3, true),
                BorderFactory.createEmptyBorder(0, 2, 0, 2)
        );
        label.setBorder(roundedBorder);
        label.addMouseListener(mouseAdapter);
        return label;
    }

    private void hoverOff() {
//        mainPanel.setBackground(defaultPanelColor);
//        detailPanel.setBackground(defaultPanelColor);
//        infoPanel.setBackground(defaultPanelColor);
//        titleLabelContainer.setBackground(defaultPanelColor);
//        controlPanel.setBackground(defaultPanelColor);
//        controlContainer.setBackground(defaultPanelColor);
//        selectCandidateCheckbox.setBackground(defaultPanelColor);
//        metadataPanel.setBackground(defaultPanelColor);

//        if (!selectCandidateCheckbox.isSelected()) {
//            selectCandidateCheckbox.setVisible(false);
//        }
//        if (!isPinned) {
//            pinLabel.setVisible(false);
//        }
    }

    private void hoverOn() {
//        mainPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
//        detailPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
//        infoPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
//        titleLabelContainer.setBackground(HOVER_HIGHLIGHT_COLOR);
//        controlPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
//        controlContainer.setBackground(HOVER_HIGHLIGHT_COLOR);
//        selectCandidateCheckbox.setBackground(HOVER_HIGHLIGHT_COLOR);
//        metadataPanel.setBackground(HOVER_HIGHLIGHT_COLOR);
        selectCandidateCheckbox.setVisible(true);
//        pinLabel.setVisible(true);

    }

    public JPanel getComponent() {
        return mainPanel;
    }

    public void setTitledBorder(String title) {
        TitledBorder titledBorder = (TitledBorder) mainPanel.getBorder();
        titledBorder.setTitle(title);
    }


    public void setStatus(String statusText) {
        statusLabel.setText(statusText);
    }

    public TestCandidateMetadata getTestCandidate() {
        return candidateMetadata;
    }

    public void setSelected(boolean b) {
        selectCandidateCheckbox.setSelected(b);
    }

    public boolean isPinned() {
        return isPinned;
    }
}
