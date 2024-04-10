package com.insidious.plugin.ui.stomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.insidious.plugin.MethodSignatureParser;
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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
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
    private static final Logger logger = LoggerUtil.getInstance(StompItem.class);
    private final TestCandidateLifeListener testCandidateLifeListener;
    private final Color defaultPanelColor;
    private final InsidiousService insidiousService;
    private final JCheckBox selectCandidateCheckbox;
    //    private final ActionToolbarImpl actionToolbar;
    private final MethodUnderTest methodUnderTest;
    private TestCandidateBareBone candidateMetadata;
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
    private JPanel stompRowItem;
    private TestCandidateMetadata loadedCandidate;

    public StompItem(
            TestCandidateBareBone testCandidateMetadata,
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
                                candidateMetadata.getMethodUnderTest().getClassName(), insidiousService.getProject());
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
                                candidateMetadata.methodUnderTest.getClassName(), insidiousService.getProject());
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


        AnAction replayAction = new AnAction(() -> "Replay", UIUtils.REPLAY_PINK) {

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    DumbService.getInstance(insidiousService.getProject())
                            .runReadActionInSmartMode(() -> {
                                testCandidateLifeListener.executeCandidate(Collections.singletonList(candidateMetadata),
                                        new ClassUnderTest(candidateMetadata.getMethodUnderTest().getClassName()),
                                        ExecutionRequestSourceType.Single,
                                        (testCandidate, agentCommandResponse, diffResult) -> {
                                            // do something with the response.
                                        });
                            });
                });
            }
        };

//        ArrayList<AnAction> action11 = new ArrayList<>();
//        action11.add(replayAction);
//        actionToolbar = new ActionToolbarImpl(
//                "Live View", new DefaultActionGroup(action11), true);
//        actionToolbar.setMiniMode(false);
//        actionToolbar.setForceMinimumSize(true);
//        actionToolbar.setTargetComponent(mainPanel);
//
//
//        controlContainer.add(actionToolbar.getComponent(), BorderLayout.CENTER);
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


//        MethodCallExpression mainMethod = candidateMetadata.getMainMethod();
//        Pair<PsiMethod, PsiSubstitutor> targetPsiMethod = ClassTypeUtils.getPsiMethod(
//                mainMethod, insidiousService.getProject());
        methodUnderTest = candidateMetadata.getMethodUnderTest();
//        if (targetPsiMethod != null) {
//            methodUnderTest = MethodUnderTest.fromPsiCallExpression(targetPsiMethod.getFirst());
//        } else {
//        }

        String className = methodUnderTest.getClassName();
        if (className.contains(".")) {
            className = className.substring(className.lastIndexOf(".") + 1);
        }
//        setTitledBorder("[" + candidateMetadata.getEntryProbeIndex() + "] " + className);
//        long timeTakenMs = (mainMethod.getReturnValue().getProb().getRecordedAt() -
//                mainMethod.getEntryProbe().getRecordedAt()) / (1000 * 1000);
        long timeTakenMs = candidateMetadata.getTimeSpentNano() / (1000 * 1000);
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

        timeTakenMsLabel = createTagLabel("⏱ %s", new Object[]{timeTakenMsString},
                category.getJbColor(),
                new JBColor(Gray._255, Gray._255),
                labelMouseAdapter);
        timeTakenMsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));


        timeTakenMsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                highlight();
            }
        });

        int tagCount = 1;
        metadataPanel.add(timeTakenMsLabel);

        if (candidateMetadata.getLineNumbers().size() > 1) {
            lineCoverageLabel = createTagLabel("+%d lines", new Object[]{candidateMetadata.getLineNumbers().size()},
                    TAG_LABEL_BACKGROUND_GREY, ExecutionTimeCategory.INSTANTANEOUS.getJbColor(),
                    labelMouseAdapter);
            tagCount++;
            lineCoverageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lineCoverageLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    highlight();
                }
            });
            metadataPanel.add(lineCoverageLabel);
        }
        ObjectMapper objectMapper = ObjectMapperInstance.getInstance();

        List<String> descriptorName = MethodSignatureParser.parseMethodSignature(
                candidateMetadata.getMethodUnderTest().getSignature());
        String returnValueType = descriptorName.remove(descriptorName.size() - 1);
        String returnValueClassName = ClassTypeUtils.getDottedClassName(returnValueType);

        if (!descriptorName.isEmpty()) {


            parameterCountLabel = createTagLabel("%d Argument" + (
                            descriptorName.size() > 1 ? "s" : ""
                    ), new Object[]{descriptorName.size()}, TAG_LABEL_BACKGROUND_GREY,
                    TAG_LABEL_TEXT_GREY, labelMouseAdapter);


            parameterCountLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {

                    if (loadedCandidate == null) {
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            checkLoadedCandidate();
                            ApplicationManager.getApplication().invokeLater(() -> {
                                setArgumentTagHoverText(parameterCountLabel);
                            });
                        });
                    } else {
                        setArgumentTagHoverText(parameterCountLabel);
                    }

                    super.mouseEntered(e);


                }
            });

            tagCount++;
            metadataPanel.add(parameterCountLabel);
        }

        if (tagCount < 4) {


            if (returnValueClassName != null && returnValueClassName.contains(".")) {
                returnValueClassName = returnValueClassName.substring(returnValueClassName.lastIndexOf(".") + 1);
            }

            JLabel returnValueTag = createTagLabel("ᐊ " + returnValueClassName, new Object[]{descriptorName.size()},
                    TAG_LABEL_BACKGROUND_GREY,
                    TAG_LABEL_TEXT_GREY, labelMouseAdapter);


            returnValueTag.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (loadedCandidate == null) {
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            checkLoadedCandidate();
                            ApplicationManager.getApplication().invokeLater(() -> {
                                setReturnTagHoverValue(returnValueTag);
                            });
                        });
                    } else {
                        setReturnTagHoverValue(returnValueTag);
                    }
                    super.mouseEntered(e);
                }
            });
            tagCount++;
            metadataPanel.add(returnValueTag);
        }

//        if (tagCount < 4) {
//            int size = candidateMetadata.getCallsList().size();
//            if (size > 0) {
//                callsCountLabel = createTagLabel("%d Downstream", new Object[]{size},
//                        TAG_LABEL_BACKGROUND_GREY, TAG_LABEL_TEXT_GREY, labelMouseAdapter);
////            callsCountLabel.setToolTipText("Click to show downstream calls");
////            callsCountLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
////            callsCountLabel.addMouseListener(new MouseAdapter() {
////                @Override
////                public void mouseClicked(MouseEvent e) {
////                    testCandidateLifeListener.onExpandChildren(candidateMetadata);
////                    super.mouseClicked(e);
////                }
////            });
//                metadataPanel.add(callsCountLabel);
//            }
//        }


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
            int remaining = prettyPrintedArguments.length() - 500;
            if (prettyPrintedArguments.length() > 500) {
                prettyPrintedArguments = prettyPrintedArguments.substring(0, 500);
            }
            prettyPrintedArguments = prettyPrintedArguments.replaceAll("\\n", "<br/>");
            prettyPrintedArguments = prettyPrintedArguments.replaceAll(" ", "&nbsp;");
            if (remaining > 1) {
                prettyPrintedArguments += "  .... <b>+" + remaining + " more characters</b>";
            }
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

        String hex = "#" + Integer.toHexString(foreground.getRGB()).substring(2);
        label.setText(String.format("<html><font size=-1 color=" + hex + ">" + tagText + "</font></html>",
                value));

        label.setOpaque(true);
        JPopupMenu jPopupMenu = new JPopupMenu("Yay");
        jPopupMenu.add(new JMenuItem("item 1", UIUtils.GHOST_MOCK));
        label.setComponentPopupMenu(jPopupMenu);
        label.setBackground(backgroundColor);

        // Creating a rounded border
        Border roundedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(backgroundColor, 3, true),
                BorderFactory.createEmptyBorder(0, 2, 0, 2)
        );
        label.setBorder(roundedBorder);
        label.addMouseListener(mouseAdapter);
        return label;
    }

    private void setArgumentTagHoverText(JLabel parameterCountLabel) {
        if (loadedCandidate == null) {
            return;
        }
        ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
        if (parameterCountLabel.getToolTipText() == null ||
                parameterCountLabel.getToolTipText().isEmpty()) {
            ObjectNode parametersNode = objectMapper.getNodeFactory().objectNode();
            List<Parameter> arguments = loadedCandidate.getMainMethod().getArguments();
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
            } catch (JsonProcessingException e1) {
                e1.printStackTrace();
                //
            }
        }
    }

    private void setReturnTagHoverValue(JLabel returnValueTag) {
        if (loadedCandidate == null) {
            return;
        }
        MethodCallExpression mainMethod = loadedCandidate.getMainMethod();

        if (returnValueTag.getToolTipText() == null
                || returnValueTag.getToolTipText().isEmpty()) {
            JsonNode valueForParameter = ClassTypeUtils.getValueForParameter(mainMethod.getReturnValue());
            String prettyPrintedArgumentsHtml = "{}";
            if (valueForParameter.isNumber()
                    && (mainMethod.getReturnValue().getType() == null ||
                    (mainMethod.getReturnValue().getType().length() != 1 &&
                            !mainMethod.getReturnValue().getType().startsWith("java.lang")))
            ) {
                // not a serializable value
//                            valueForParameter = objectMapper.createObjectNode();
            } else {
                prettyPrintedArgumentsHtml = getPrettyPrintedArgumentsHtml(valueForParameter);
            }

            if (prettyPrintedArgumentsHtml != null) {
                returnValueTag.setToolTipText(prettyPrintedArgumentsHtml);
            }

        }
    }

    private synchronized void checkLoadedCandidate() {
        if (loadedCandidate == null) {
            loadedCandidate = insidiousService.getTestCandidateById(
                    candidateMetadata.getId(), true);
        }
    }

    private void highlight() {
        if (!requestedHighlight) {
            requestedHighlight = true;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                checkLoadedCandidate();
                Pair<PsiMethod, PsiSubstitutor> targetPsiMethod =
                        ApplicationManager.getApplication().runReadAction(
                                (Computable<Pair<PsiMethod, PsiSubstitutor>>) () -> ClassTypeUtils.getPsiMethod(
                                        loadedCandidate.getMainMethod(), insidiousService.getProject()));
                if (targetPsiMethod != null) {
                    MethodUnderTest methodUnderTest1 = ApplicationManager.getApplication().runReadAction(
                            (Computable<MethodUnderTest>) () -> MethodUnderTest.fromPsiCallExpression(
                                    targetPsiMethod.getFirst()));
                    insidiousService.highlightTimingInformation(loadedCandidate,
                            methodUnderTest1);
                } else {
                    insidiousService.highlightTimingInformation(loadedCandidate, methodUnderTest);
                }


            });
            HighlightedRequest highlightRequest = new HighlightedRequest(
                    methodUnderTest, new HashSet<>(candidateMetadata.getLineNumbers())
            );
            insidiousService.highlightLines(highlightRequest);

        } else {
            requestedHighlight = false;
            insidiousService.removeCurrentActiveHighlights();
            insidiousService.removeTimingInformation();
        }
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
        selectCandidateCheckbox.setVisible(true);

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

    public TestCandidateBareBone getTestCandidate() {
        return candidateMetadata;
    }

    public void setSelected(boolean b) {
        selectCandidateCheckbox.setSelected(b);
    }

    public boolean isPinned() {
        return isPinned;
    }

    public JPanel getStompRowItem() {
        return stompRowItem;
    }

    public void setStompRowItem(JPanel stompRowItem) {
        this.stompRowItem = stompRowItem;
    }
}
