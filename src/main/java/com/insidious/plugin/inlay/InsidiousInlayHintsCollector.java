package com.insidious.plugin.inlay;

import com.insidious.plugin.adapter.java.JavaMethodAdapter;
import com.insidious.plugin.client.ClassMethodAggregates;
import com.insidious.plugin.client.MethodCallAggregate;
import com.insidious.plugin.client.UnloggedTimingTag;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.highlighter.MockItemClickListener;
import com.insidious.plugin.ui.mocking.OnSaveListener;
import com.insidious.plugin.ui.stomp.ExecutionTimeCategorizer;
import com.insidious.plugin.ui.stomp.ExecutionTimeCategory;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.InlayPresentationFactory;
import com.intellij.codeInsight.hints.presentation.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.popup.ActiveIcon;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.GotItTooltip;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.List;
import java.util.*;

public class InsidiousInlayHintsCollector extends FactoryInlayHintsCollector {
    public static final Color INLAY_BACKGROUND_COLOR = JBColor.BLUE;
    public static final float BACKGROUND_ALPHA = 0.2f;
    public static final Integer UNLOGGED_APM_GROUP = -500;
    public static final Integer UNLOGGED_TPM_GROUP = 500;
    public static final Integer UNLOGGED_REQUEST_GROUP = 101;
    public static final Integer UNLOGGED_RESPONSE_GROUP = 102;
    public static final @NotNull TextAttributesKey INSIDIOUS_CREATE_MOCK_ATTRIBUTES = TextAttributesKey
            .createTextAttributesKey("INSIDIOUS_CREATE_MOCK",
                    new TextAttributes(new JBColor(
                            new Color(213, 186, 172),
                            new Color(213, 186, 172)
                    ), new JBColor(
                            new Color(44, 161, 184),
                            new Color(44, 161, 184)
                    ), new JBColor(
                            new Color(48, 121, 38),
                            new Color(48, 121, 38)
                    ), EffectType.LINE_UNDERSCORE, Font.PLAIN));
    public static final @NotNull TextAttributesKey INSIDIOUS_ACTIVE_MOCK_ATTRIBUTES = TextAttributesKey
            .createTextAttributesKey("INSIDIOUS_MOCK_ACTIVE",
                    new TextAttributes(new JBColor(
                            new Color(37, 152, 0),
                            new Color(37, 152, 0)
                    ), new JBColor(
                            new Color(44, 161, 184),
                            new Color(44, 161, 184)
                    ),
                            new JBColor(
                                    new Color(48, 121, 38),
                                    new Color(48, 121, 38)
                            ), EffectType.LINE_UNDERSCORE, Font.PLAIN));
    public static final @NotNull TextAttributesKey INSIDIOUS_BROWSE_MOCK_ATTRIBUTES = TextAttributesKey
            .createTextAttributesKey("INSIDIOUS_BROWSE_MOCK",
                    new TextAttributes(new JBColor(
                            new Color(171, 121, 224),
                            new Color(171, 121, 224)
                    ), new JBColor(
                            new Color(44, 161, 184),
                            new Color(44, 161, 184)
                    ),
                            new JBColor(
                                    new Color(48, 121, 38),
                                    new Color(48, 121, 38)
                            ), EffectType.LINE_UNDERSCORE, Font.PLAIN));
    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayHintsCollector.class);
    private static final Map<ExecutionTimeCategory, TextAttributesKey> exectionTimeCategoryttributeMap =
            new HashMap<>();

    static {
        for (ExecutionTimeCategory value : ExecutionTimeCategory.values()) {
            TextAttributesKey timeTakenColorAttrib =
                    TextAttributesKey.createTextAttributesKey("CTRL_CLICKABLE_" + value.toString(),
                            new TextAttributes(
                                    value.getJbColor(),
                                    null,
                                    value.getJbColor(),
                                    EffectType.LINE_UNDERSCORE, Font.PLAIN));
            exectionTimeCategoryttributeMap.put(value, timeTakenColorAttrib);

        }
    }

    private final InsidiousService insidiousService;
    private final Editor editor;
    private PsiClass currentClass;
    private ClassMethodAggregates classMethodAggregates;


    public InsidiousInlayHintsCollector(Editor editor) {
        super(editor);
        this.editor = editor;
        this.insidiousService = editor.getProject().getService(InsidiousService.class);


    }

    private static String formatTimeDuration(Float duration) {
        if (duration > 1000000) {
            return String.format("%.2f s", duration / (1000 * 1000));
        }
        if (duration > 1000) {
            return String.format("%.2f ms", duration / 1000);
        }
        return String.format("%.2f µs", duration);
    }

    private static int getAnchorOffset(PsiElement element) {
        for (PsiElement child : element.getChildren()) {
            if (!(child instanceof PsiDocComment) && !(child instanceof PsiWhiteSpace)) {
                return child.getTextRange().getStartOffset();
            }
        }
        return element.getTextRange().getStartOffset();
    }

    public static String padString(String str, int length, char padChar) {
        if (str.length() >= length) {
            return str;
        }

        StringBuilder padded = new StringBuilder(str);
        while (padded.length() < length) {
            padded.append(padChar);
        }
        return padded.toString();
    }

    @Override
    public boolean collect(PsiElement element, Editor editor, InlayHintsSink inlayHintsSink) {
        if (!(editor instanceof EditorImpl)) {
            return true;
        } else {
            VirtualFile vf = ((EditorImpl) editor).getVirtualFile();
            if (vf instanceof LightVirtualFile) {
                // no inlay hints for light virtual files, which are in memory files like snippets
                return true;
            }
        }

        if (element instanceof PsiClass) {
            currentClass = (PsiClass) element;
            classMethodAggregates = insidiousService.getClassMethodAggregates(currentClass.getQualifiedName());
//            return true;
        }
        if (classMethodAggregates == null) {
            if (currentClass != null) {
                logger.debug("we dont have any class method aggregates for class: " + currentClass.getQualifiedName());
            }
//            return false;
        }

        if (element instanceof PsiMethod) {

            if (currentClass == null) {
                currentClass = ((PsiMethodImpl) element).getContainingClass();
                if (currentClass == null) {
                    return true;
                }
            }

            PsiMethod methodPsiElement = (PsiMethod) element;
            List<UnloggedTimingTag> timingTags = insidiousService.getTimingInformation(
                    MethodUnderTest.fromPsiCallExpression(methodPsiElement));

            if (timingTags != null && timingTags.size() > 0) {
                createInlinePresentationsForTimingTags(methodPsiElement, editor, inlayHintsSink, timingTags);
            }

            createInlinePresentationsForMethod(methodPsiElement, editor, inlayHintsSink);
        } else if (element instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) element;
            PsiMethodCallExpression hasParent = PsiTreeUtil.getParentOfType(
                    methodCallExpression, PsiMethodCallExpression.class);
            if (hasParent != null) {
                // we only deal with top level calls
                return true;
            }
            createInlinePresentationsForCallExpression(methodCallExpression, editor, inlayHintsSink);
        }

        return true;
    }

    private void createInlinePresentationsForTimingTags(
            PsiMethod methodPsiElement,
            Editor editor,
            InlayHintsSink inlayHintsSink,
            List<UnloggedTimingTag> timingTags) {


        Document document = editor.getDocument();
//        int elementLineNumber = document.getLineNumber(methodPsiElement.getTextOffset());

        TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(methodPsiElement);


        int offset = range.getStartOffset();
        int line = document.getLineNumber(offset);
//        int columnWidth = EditorUtil.getPlainSpaceWidth(editor);
//        int startOffset = document.getLineStartOffset(line);
//        int column = offset - startOffset;


        int totalCount = timingTags.size();
        for (int i = 0; i < totalCount; i++) {
            UnloggedTimingTag timingTag = timingTags.get(i);
            int inlayHintOffset = document.getLineStartOffset(timingTag.getLineNumber() - 1);
            long timeTakenInMs;
            UnloggedTimingTag nextTime = null;
            if (i < totalCount - 1) {
                nextTime = timingTags.get(i + 1);
            }
            if (nextTime != null) {
                timeTakenInMs = (nextTime.getNanoSecondTimestamp() - timingTag.getNanoSecondTimestamp()) / (1000 * 1000);
            } else {
                timeTakenInMs = 0;
            }
//            if (timeTakenInMs == 0) {
//                continue;
//            }

//            ExecutionTimeCategory category = ExecutionTimeCategorizer.categorizeExecutionTime(timeTakenInMs);
//            String timeTakenMsString = ExecutionTimeCategorizer.formatTimePeriod(timeTakenInMs);


            InlayPresentation inlayShowingCount = createTimeTagInlayPresentation(
                    "time spent on line " + timingTag.getLineNumber(), (mouseEvent, point) -> {
//                        insidiousService.showStompAndFilterForMethod(new JavaMethodAdapter(methodPsiElement));
//                        logger.warn("inlay clicked: " + timeTakenInMs + (" ms"));
                    }, timeTakenInMs);

            int width = inlayShowingCount.getWidth();

            SequencePresentation sequenceOfInlays = new SequencePresentation(
                    Arrays.asList(
                            inlayShowingCount,
                            new SpacePresentation(50 - width, 80)
                    )
            );

            logger.warn(
                    "Add timing tag: " + timeTakenInMs + " ms for " + methodPsiElement.getName() + " at line " + timingTag.getLineNumber() + " at offset " + inlayHintOffset);
            inlayHintsSink.addInlineElement(inlayHintOffset, false, sequenceOfInlays, false);

        }


    }

    private void createInlinePresentationsForCallExpression(
            PsiMethodCallExpression methodCallExpression, Editor editor, InlayHintsSink inlayHintsSink) {

        Document document = editor.getDocument();

        TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(methodCallExpression);


        int activeMockCount = 0;
        int savedMockCount = 0;
        List<PsiMethodCallExpression> mockableCalls = getMockableCalls(methodCallExpression);

        Map<PsiMethodCallExpression, List<DeclaredMock>> declaredMockMap
                = new HashMap<>();
        for (PsiMethodCallExpression mockableCall : mockableCalls) {
            MethodUnderTest methodUnderTest = MethodUnderTest.fromPsiCallExpression(mockableCall);
            List<DeclaredMock> declaredMocks = insidiousService.getDeclaredMocksOf(methodUnderTest);
            for (DeclaredMock declaredMock : declaredMocks) {
                if (insidiousService.isMockEnabled(declaredMock)) {
                    activeMockCount++;
                }
            }
            savedMockCount += declaredMocks.size();
            declaredMockMap.put(mockableCall, declaredMocks);
        }


        int mockableCallCount = mockableCalls.size();
        if (mockableCallCount == 0) {
            return;
        }
        if (mockableCallCount == 1) {
            PsiMethodCallExpression theCall = mockableCalls.get(0);
            PsiExpression qualifierTextExpression = theCall.getMethodExpression().getQualifierExpression();
            if (qualifierTextExpression == null) {
                return;
            }
            String qualifierText = qualifierTextExpression.getText();
            String typeCanonicalName = qualifierTextExpression.getType().getCanonicalText();
            if (qualifierText.equals("log") || qualifierText.equals("logger")) {
                return;
            }
            if (typeCanonicalName.startsWith("com.fasterxml.jackson")) {
                return;
            }
        }


        int line = editor.getDocument().getLineNumber(range.getStartOffset());
        int offset = getAnchorOffset(methodCallExpression);
        int columnWidth = EditorUtil.getPlainSpaceWidth(editor);
        int startOffset = document.getLineStartOffset(line);
        int column = offset - startOffset;

        List<InlayPresentation> inlayPresentations = new ArrayList<>();

        inlayPresentations.add(new SpacePresentation(column * columnWidth - 2, 0));


        int callExpressionCount = declaredMockMap.keySet().size();
        if (activeMockCount > 0) {
            InlayPresentation mockInlayPresentation = createMockInlayPresentation(mockableCalls,
                    savedMockCount, activeMockCount,
                    callExpressionCount);
            inlayPresentations.add(getFactory().icon(
                    IconUtil.toSize(UIUtils.CHECK_ICON, 10, 10)
            ));
            inlayPresentations.add(new SpacePresentation(4, 0));
            inlayPresentations.add(mockInlayPresentation);
            inlayPresentations.add(new SpacePresentation(1 * columnWidth, 0));
        } else if (savedMockCount > 0) {
            InlayPresentation mockInlayPresentation = createMockInlayPresentation(mockableCalls, savedMockCount,
                    activeMockCount, callExpressionCount);
            inlayPresentations.add(mockInlayPresentation);
            inlayPresentations.add(new SpacePresentation(1 * columnWidth, 0));
        } else {
            InlayPresentation createMock = createMockInlayPresentation(mockableCalls, 0, 0, callExpressionCount);
            inlayPresentations.add(createMock);
        }

        SequencePresentation sequenceOfInlays = new SequencePresentation(inlayPresentations);

        GotItTooltip go = new GotItTooltip("Unlogged.Inlay.Mock",
                "Mock downstream call by creating mock responses. " +
                        "Enable mocks in live application",
                insidiousService.getProject())
                .withHeader("Mock Downstream Call")
                .withLink("Go to library", insidiousService::showLibrary)
                .andShowCloseShortcut()
                .withPosition(Balloon.Position.above);
        ApplicationManager.getApplication().invokeLater(() -> {
            go.show(editor.getContentComponent(), (component, balloon) -> {
                Point point = editor.offsetToXY(startOffset, true, true);
                return new Point((int) (point.getX() + column * columnWidth),
                        (int) point.getY() - editor.getLineHeight());
            });
        });


        inlayHintsSink.addBlockElement(startOffset, true, true, UNLOGGED_APM_GROUP, sequenceOfInlays);
    }

    private List<PsiMethodCallExpression> getMockableCalls(PsiMethodCallExpression methodCallExpression) {
        List<PsiMethodCallExpression> mockableCalls = new ArrayList<>();
        int savedMockCount = 0;
        Map<PsiMethodCallExpression, List<DeclaredMock>> declaredMockMap = new HashMap<>();

        if (ClassTypeUtils.isNonStaticDependencyCall(methodCallExpression)) {
            mockableCalls.add(methodCallExpression);
        }

        PsiMethodCallExpression[] allCalls = ClassTypeUtils.getChildrenOfTypeRecursive(
                methodCallExpression,
                PsiMethodCallExpression.class);

        if (allCalls != null) {
            for (PsiMethodCallExpression callExpression : allCalls) {
                if (ClassTypeUtils.isNonStaticDependencyCall(callExpression)) {
                    mockableCalls.add(callExpression);
                }
            }
        }
        return mockableCalls;
    }

    private void createInlinePresentationsForMethod(PsiMethod methodPsiElement, Editor editor, InlayHintsSink inlayHintsSink) {
        if (classMethodAggregates == null) {
            return;
        }
        MethodCallAggregate methodAggregate = classMethodAggregates.getMethodAggregate(methodPsiElement.getName());
        if (methodAggregate == null) {
            return;
        }

        Document document = editor.getDocument();
//        int elementLineNumber = document.getLineNumber(methodPsiElement.getTextOffset());

        TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(methodPsiElement);


        Integer count = methodAggregate.getCount();
        InlayPresentation inlayShowingCount = createInlayPresentation(count + (count < 2 ? " call" : " calls"),
                "<html>Show mocks in Unlogged Library</html>", (mouseEvent, point) -> {
                    insidiousService.showStompAndFilterForMethod(new JavaMethodAdapter(methodPsiElement));
                    logger.warn("inlay clicked: " + count + (count < 2 ? " call" : " calls"));
                });

        String avgStringText = String.format(formatTimeDuration(methodAggregate.getAverage()));
        InlayPresentation inlayShowingAverage = createInlayPresentation(avgStringText, "mean",
                (mouseEvent, point) -> logger.warn("inlay clicked: " + avgStringText));
        String stdDevStringText = String.format(formatTimeDuration(methodAggregate.getStdDev()));
        InlayPresentation inlayShowingStdDev = createInlayPresentation(stdDevStringText, "stdDev",
                (mouseEvent, point) -> logger.warn("inlay clicked: " + stdDevStringText));


        int offset = range.getStartOffset();
        int line = document.getLineNumber(offset);
        int columnWidth = EditorUtil.getPlainSpaceWidth(editor);
        int startOffset = document.getLineStartOffset(line);
        int column = offset - startOffset;

        int inlayHintOffset = document.getLineStartOffset(document.getLineNumber(methodPsiElement.getTextOffset()));
        SequencePresentation sequenceOfInlays = new SequencePresentation(
                Arrays.asList(
                        new SpacePresentation(column * columnWidth, 0),
                        inlayShowingCount,
                        getFactory().textSpacePlaceholder(1, false),
                        inlayShowingAverage,
                        getFactory().textSpacePlaceholder(1, false),
                        inlayShowingStdDev
                )
        );

//        logger.warn("PSIElement " +
//                "[" + methodPsiElement.getClass().getSimpleName() + "]" +
//                "[" + elementLineNumber + "," + column + "]: "
//                + currentClass.getQualifiedName() + "."
//                + methodElement.getName() + "()");

//            inlayHintsSink.addBlockElement(line, true, root, constraints);
        inlayHintsSink.addBlockElement(inlayHintOffset, true, true, UNLOGGED_APM_GROUP, sequenceOfInlays);

    }

    private TextRange getTextRangeWithoutLeadingCommentsAndWhitespaces(PsiElement element) {

        JBIterable<? extends PsiElement> elementChildren = SyntaxTraverser.psiApi()
                .children(element);
        PsiElement start = elementChildren.filter(e -> !(e instanceof PsiComment) && !(e instanceof PsiWhiteSpace))
                .first();
        return TextRange.create(start.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
    }

    private InlayPresentation createMockInlayPresentation(List<PsiMethodCallExpression> mockableCallExpressions,
                                                          int savedMockCount, int activeMockCount,
                                                          int callExpressionCount) {

        PresentationFactory factory = getFactory();
        InlayPresentation text;

        StringBuilder inlayTextBuilder = new StringBuilder();

        TextAttributesKey inlayAttributes;
        String inlayText;
        if (activeMockCount > 0) {
            inlayText = activeMockCount + " active mock" + (savedMockCount == 1 ? "" : "s");
            inlayAttributes = INSIDIOUS_ACTIVE_MOCK_ATTRIBUTES;
        } else if (savedMockCount == 0) {
            inlayText = "create mock";
            inlayAttributes = INSIDIOUS_CREATE_MOCK_ATTRIBUTES;
        } else {
            inlayAttributes = INSIDIOUS_BROWSE_MOCK_ATTRIBUTES;
            inlayText = savedMockCount + " saved mock" + (savedMockCount == 1 ? "" : "s");
        }
        inlayTextBuilder.append(inlayText);


        text = factory.smallText(inlayTextBuilder.toString());


        text = new WithAttributesPresentation(text, inlayAttributes, editor,
                (new WithAttributesPresentation.AttributesFlags()).withSkipEffects(true));
//        text = factory.withReferenceAttributes(text);


        text = new OnClickPresentation(text, (mouseEvent, point) -> {
            if (savedMockCount == 0) {
                if (mockableCallExpressions.size() > 1) {

                    JPanel gutterMethodPanel = new JPanel();
                    gutterMethodPanel.setLayout(new GridLayout(0, 1));
                    gutterMethodPanel.setMinimumSize(new Dimension(400, 300));

                    for (PsiMethodCallExpression methodCallExpression : mockableCallExpressions) {
                        PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
                        String methodCallText = methodExpression.getText();
                        JPanel methodItemPanel = new JPanel();
                        methodItemPanel.setLayout(new BorderLayout());

                        methodItemPanel.add(new JLabel(methodCallText), BorderLayout.CENTER);
                        JLabel iconLabel = new JLabel(UIUtils.CHECK_GREEN_SMALL);
                        Border border = iconLabel.getBorder();
                        CompoundBorder borderWithMargin;
                        borderWithMargin = BorderFactory.createCompoundBorder(border,
                                BorderFactory.createEmptyBorder(0, 5, 0, 5));
                        iconLabel.setBorder(borderWithMargin);
                        methodItemPanel.add(iconLabel, BorderLayout.EAST);

                        Border currentBorder = methodItemPanel.getBorder();
                        borderWithMargin = BorderFactory.createCompoundBorder(currentBorder,
                                BorderFactory.createEmptyBorder(5, 10, 5, 5));
                        methodItemPanel.setBorder(borderWithMargin);
                        methodItemPanel.addMouseListener(
                                new MockItemClickListener(methodItemPanel, methodCallExpression));

                        gutterMethodPanel.add(methodItemPanel);

                    }


                    ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                            .createComponentPopupBuilder(gutterMethodPanel, null);

                    gutterMethodComponentPopup
                            .setProject(insidiousService.getProject())
                            .setShowBorder(true)
                            .setShowShadow(true)
                            .setFocusable(true)
                            .setRequestFocus(true)
                            .setCancelOnClickOutside(true)
                            .setCancelOnOtherWindowOpen(true)
                            .setCancelKeyEnabled(true)
                            .setBelongsToGlobalPopupStack(false)
                            .setTitle("Select Call Expression")
                            .setTitleIcon(new ActiveIcon(UIUtils.GHOST_MOCK))
                            .createPopup()
                            .show(new RelativePoint(mouseEvent));

                } else {
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        DumbService.getInstance(insidiousService.getProject())
                                .runReadActionInSmartMode(() -> {
                                    PsiMethodCallExpression methodCallExpression = mockableCallExpressions.get(0);
                                    PsiMethod psiMethod = (PsiMethod) methodCallExpression.getMethodExpression()
                                            .resolve();
                                    insidiousService.showMockCreator(new JavaMethodAdapter(psiMethod),
                                            methodCallExpression, new OnSaveListener() {
                                                @Override
                                                public void onSaveDeclaredMock(DeclaredMock declaredMock) {
                                                }
                                            });
                                });
                    });
                }
            } else {
                DumbService.getInstance(insidiousService.getProject())
                        .smartInvokeLater(() -> {
                            insidiousService.onMethodCallExpressionInlayClick(mockableCallExpressions, mouseEvent,
                                    point);
                        });
            }
        });

        text = factory.withTooltip("<html>Show mocks in Unlogged Library</html>", text);

        text = new WithCursorOnHoverPresentation(text, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), editor);

        return text;
    }

    private InlayPresentation createInlayPresentation(final String inlayText, String hoverText, InlayPresentationFactory.ClickListener clickListener) {

        PresentationFactory factory = getFactory();
        InlayPresentation text;

        text = factory.smallText(inlayText);
        text = factory.withReferenceAttributes(text);

        text = new OnClickPresentation(text, clickListener);

//        InlayPresentation onHover = factory.roundWithBackground(text);
//        text = new ChangeOnHoverPresentation(text, () -> onHover, mouseEvent -> true);


        text = factory.withTooltip(hoverText, text);

        text = new WithCursorOnHoverPresentation(text, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), editor);

        return text;
    }

    private InlayPresentation createTimeTagInlayPresentation(String hoverText,
                                                             InlayPresentationFactory.ClickListener clickListener,
                                                             long timeTakenMs) {


        PresentationFactory factory = getFactory();
        InlayPresentation text;

        String inlayText = ExecutionTimeCategorizer.formatTimePeriod(timeTakenMs);
        text = factory.smallText(inlayText);
        WithAttributesPresentation.AttributesFlags flags = (new WithAttributesPresentation.AttributesFlags()).withSkipEffects(
                true);

        text = new WithAttributesPresentation(text, exectionTimeCategoryttributeMap.get(
                ExecutionTimeCategorizer.categorizeExecutionTime(timeTakenMs)
        ), this.editor, flags);

        text = new OnClickPresentation(text, clickListener);

//        InlayPresentation onHover = factory.roundWithBackground(text);
//        text = new ChangeOnHoverPresentation(text, () -> onHover, mouseEvent -> true);


        text = factory.withTooltip(hoverText, text);

//        text = new WithCursorOnHoverPresentation(text, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), editor);

        return text;
    }


}
