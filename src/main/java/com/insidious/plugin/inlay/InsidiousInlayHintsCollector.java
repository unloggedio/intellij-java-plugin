package com.insidious.plugin.inlay;

import com.insidious.plugin.client.ClassMethodAggregates;
import com.insidious.plugin.client.MethodCallAggregate;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.ui.highlighter.MethodMockGutterNavigationHandler;
import com.insidious.plugin.ui.highlighter.MockItemClickListener;
import com.insidious.plugin.ui.highlighter.MockMethodLineHighlighter;
import com.insidious.plugin.ui.mocking.MockDefinitionListPanel;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.UIUtils;
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.presentation.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
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
    public static final Integer UNLOGGED_REQUEST_GROUP = 101;
    public static final Integer UNLOGGED_RESPONSE_GROUP = 102;
    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayHintsCollector.class);
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

    @Override
    public boolean collect(PsiElement element, Editor editor, InlayHintsSink inlayHintsSink) {
        if (element instanceof PsiClass) {
            currentClass = (PsiClass) element;
            classMethodAggregates = insidiousService.getClassMethodAggregates(currentClass.getQualifiedName());
//            return true;
        }
        if (classMethodAggregates == null) {
            if (currentClass != null) {
                logger.warn("we dont have any class method aggregates for class: " + currentClass.getQualifiedName());
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


            createInlinePresentationsForMethod((PsiMethod) element, editor, inlayHintsSink);
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

    private void createInlinePresentationsForCallExpression(
            PsiMethodCallExpression methodCallExpression, Editor editor, InlayHintsSink inlayHintsSink) {

        Document document = editor.getDocument();

        TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(methodCallExpression);


        int savedMockCount = 0;
        List<PsiMethodCallExpression> mockableCalls = getMockableCalls(methodCallExpression);

        Map<PsiMethodCallExpression, List<DeclaredMock>> declaredMockMap
                = new HashMap<>();
        for (PsiMethodCallExpression mockableCall : mockableCalls) {
            MethodUnderTest methodUnderTest = MethodUnderTest.fromCallExpression(mockableCall);
            List<DeclaredMock> declaredMocks = insidiousService.getDeclaredMocksOf(methodUnderTest);
            savedMockCount += declaredMocks.size();
            declaredMockMap.put(mockableCall, declaredMocks);
        }


        int mockableCallCount = mockableCalls.size();
        if (mockableCallCount == 0) {
            return;
        }
        if (mockableCallCount == 1) {
            PsiMethodCallExpression theCall = mockableCalls.get(0);
            PsiExpression qualifierTextExpression = theCall.getMethodExpression()
                    .getQualifierExpression();
            if (qualifierTextExpression == null) {
                return;
            }
            String qualifierText = qualifierTextExpression.getText();
            String typeCanonicalName = qualifierTextExpression.getType()
                    .getCanonicalText();
            if (qualifierText.equals("log") || qualifierText.equals("logger")) {
                return;
            }
            if (
                    typeCanonicalName.startsWith("com.fasterxml.jackson")
            ) {
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


        inlayPresentations.add(createMockInlayPresentation(mockableCalls, savedMockCount,
                declaredMockMap.keySet().size()));

        SequencePresentation sequenceOfInlays = new SequencePresentation(inlayPresentations);


        inlayHintsSink.addBlockElement(startOffset, true, true, UNLOGGED_APM_GROUP, sequenceOfInlays);
    }

    private List<PsiMethodCallExpression> getMockableCalls(PsiMethodCallExpression methodCallExpression) {
        List<PsiMethodCallExpression> mockableCalls = new ArrayList<>();
        int savedMockCount = 0;
        Map<PsiMethodCallExpression, List<DeclaredMock>> declaredMockMap = new HashMap<>();

        if (MockMethodLineHighlighter.isNonStaticDependencyCall(methodCallExpression)) {
            mockableCalls.add(methodCallExpression);
        }

        PsiMethodCallExpression[] allCalls = MethodMockGutterNavigationHandler.getChildrenOfTypeRecursive(
                methodCallExpression,
                PsiMethodCallExpression.class);

        if (allCalls != null) {
            for (PsiMethodCallExpression callExpression : allCalls) {
                if (MockMethodLineHighlighter.isNonStaticDependencyCall(callExpression)) {
                    mockableCalls.add(callExpression);
                }
            }
        }
        return mockableCalls;
    }

    private void createInlinePresentationsForMethod(PsiMethod methodPsiElement, Editor editor, InlayHintsSink inlayHintsSink) {
        MethodCallAggregate methodAggregate = classMethodAggregates.getMethodAggregate(methodPsiElement.getName());
        if (methodAggregate == null) {
            return;
        }

        Document document = editor.getDocument();
        int elementLineNumber = document.getLineNumber(methodPsiElement.getTextOffset());

        TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(methodPsiElement);


        Integer count = methodAggregate.getCount();
        InlayPresentation inlayShowingCount = createInlayPresentation(count + (count < 2 ? " call" : " calls"),
                "click to filter in timeline");
        String avgStringText = String.format(formatTimeDuration(methodAggregate.getAverage()));
        InlayPresentation inlayShowingAverage = createInlayPresentation(avgStringText, "mean");
        String stdDevStringText = String.format(formatTimeDuration(methodAggregate.getStdDev()));
        InlayPresentation inlayShowingStdDev = createInlayPresentation(stdDevStringText, "stdDev");


        int line = editor.getDocument().getLineNumber(range.getStartOffset());
        int offset = getAnchorOffset(methodPsiElement);
        int columnWidth = EditorUtil.getPlainSpaceWidth(editor);
        int startOffset = document.getLineStartOffset(line);
        int column = offset - startOffset;

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
        inlayHintsSink.addBlockElement(startOffset, true, true, UNLOGGED_APM_GROUP, sequenceOfInlays);

    }

    private TextRange getTextRangeWithoutLeadingCommentsAndWhitespaces(PsiElement element) {

        JBIterable<? extends PsiElement> elementChildren = SyntaxTraverser.psiApi()
                .children(element);
        PsiElement start = elementChildren.filter(e -> !(e instanceof PsiComment) && !(e instanceof PsiWhiteSpace))
                .first();
        return TextRange.create(start.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
    }

    private InlayPresentation createCommaInlayPresentation() {

        PresentationFactory factory = getFactory();
        InlayPresentation text;

        text = factory.smallText(", ");
        return text;
    }


    private InlayPresentation createMockInlayPresentation(List<PsiMethodCallExpression> mockableCallExpressions,
                                                          int savedMockCount, int callExpressionCount) {

        PresentationFactory factory = getFactory();
        InlayPresentation text;

        boolean mockingEnabled = insidiousService.isMockingEnabled();

        StringBuilder inlayTextBuilder = new StringBuilder();

        TextAttributesKey inlayAttributes;
        if (savedMockCount == 0) {
            inlayTextBuilder.append("create mock");
            inlayAttributes = TextAttributesKey
                    .createTextAttributesKey("INSIDIOUS_CREATE_MOCK",
                            new TextAttributes(new JBColor(
                                    new Color(204, 154, 137),
                                    new Color(204, 154, 137)
                            ), new JBColor(
                                    new Color(44, 161, 184),
                                    new Color(44, 161, 184)
                            ),
                                    new JBColor(
                                            new Color(0, 245, 31),
                                            new Color(0, 245, 31)
                                    ), EffectType.LINE_UNDERSCORE, Font.PLAIN));

        } else {
            inlayAttributes = TextAttributesKey
                    .createTextAttributesKey("INSIDIOUS_BROWSE_MOCK",
                            new TextAttributes(new JBColor(
                                    new Color(0, 238, 74),
                                    new Color(0, 238, 74)
                            ), new JBColor(
                                    new Color(44, 161, 184),
                                    new Color(44, 161, 184)
                            ),
                                    new JBColor(
                                            new Color(0, 245, 31),
                                            new Color(0, 245, 31)
                                    ), EffectType.LINE_UNDERSCORE, Font.PLAIN));

            inlayTextBuilder.append(savedMockCount).append(" saved mocks");
        }


        text = factory.smallText(inlayTextBuilder.toString());


        text = new WithAttributesPresentation(text, inlayAttributes, editor,
                (new WithAttributesPresentation.AttributesFlags()).withSkipEffects(true));
//        text = factory.withReferenceAttributes(text);


        text = new OnClickPresentation(text, (mouseEvent, point) -> {
            logger.warn("inlay clicked create mock");
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
                    methodItemPanel.addMouseListener(new MockItemClickListener(methodItemPanel, methodCallExpression));

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
                        .setTitle("Mock Method Calls")
                        .setTitleIcon(new ActiveIcon(UIUtils.GHOST_MOCK))
                        .createPopup()
                        .show(new RelativePoint(mouseEvent));

            } else if (mockableCallExpressions.size() == 1) {

                // there is only a single mockable call on this line

                PsiMethodCallExpression methodCallExpression = mockableCallExpressions.get(0);
                MockDefinitionListPanel gutterMethodPanel = new MockDefinitionListPanel(methodCallExpression);

                JComponent gutterMethodComponent = gutterMethodPanel.getComponent();

                ComponentPopupBuilder gutterMethodComponentPopup = JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(gutterMethodComponent, null);

                JBPopup componentPopUp = gutterMethodComponentPopup
                        .setProject(methodCallExpression.getProject())
                        .setShowBorder(true)
                        .setShowShadow(true)
                        .setFocusable(true)
                        .setMinSize(new Dimension(600, -1))
                        .setRequestFocus(true)
                        .setResizable(true)
                        .setCancelOnClickOutside(true)
                        .setCancelOnOtherWindowOpen(true)
                        .setCancelKeyEnabled(true)
                        .setBelongsToGlobalPopupStack(false)
                        .setTitle("Manage Mocks")
                        .setTitleIcon(new ActiveIcon(UIUtils.GHOST_MOCK))
                        .addListener(new JBPopupListener() {
                            @Override
                            public void onClosed(@NotNull LightweightWindowEvent event) {
//                                finalText.updateState(finalText);
                            }
                        })
                        .createPopup();
                componentPopUp.show(new RelativePoint(mouseEvent));
                gutterMethodPanel.setPopupHandle(componentPopUp);


            }

        });

        text = factory.withTooltip("<html>Click to browse mocks\n\nMultiline<br /> <b>bold</b></html>", text);

        text = new WithCursorOnHoverPresentation(text, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), editor);

        return text;
    }


    private InlayPresentation createInlayPresentation(final String inlayText, String hoverText) {

        PresentationFactory factory = getFactory();
        InlayPresentation text;

        text = factory.smallText(inlayText);
        text = factory.withReferenceAttributes(text);

        text = new OnClickPresentation(text, (mouseEvent, point) -> {
            logger.warn("inlay clicked: " + inlayText);
        });

        InlayPresentation onHover = factory.roundWithBackground(text);

        text = new ChangeOnHoverPresentation(text, () -> onHover, mouseEvent -> true);


        text = factory.withTooltip(hoverText, text);

        text = new WithCursorOnHoverPresentation(text, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR), editor);

        return text;
    }


}
