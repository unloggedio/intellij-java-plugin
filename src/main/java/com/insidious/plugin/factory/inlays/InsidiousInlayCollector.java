package com.insidious.plugin.factory.inlays;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.InlayPresentationFactory;
import com.intellij.codeInsight.hints.presentation.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;

public class InsidiousInlayCollector implements InlayHintsCollector {
    public static final InlayPresentationFactory.Padding INLAY_PADDING = new InlayPresentationFactory.Padding(5, 5, 5,
            5);
    public static final InlayPresentationFactory.RoundedCorners ROUNDED_CORNERS = new InlayPresentationFactory.RoundedCorners(
            5, 5);
    public static final Color INLAY_BACKGROUND_COLOR = Color.BLUE;
    public static final float BACKGROUND_ALPHA = 0.2f;
    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayCollector.class);
    private final PresentationFactory presentationFactory;
    private final InsidiousService insidiousService;
    private PsiClass currentClass;

    public InsidiousInlayCollector(PresentationFactory inlayPresentationFactory, InsidiousService insidiousService) {
        this.presentationFactory = inlayPresentationFactory;
        this.insidiousService = insidiousService;
    }

    @Override
    public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
        return false;
//        int offset = psiElement.getTextOffset();
//
//        if (psiElement instanceof PsiClass) {
//            currentClass = (PsiClass) psiElement;
//        }
//
//        if (psiElement instanceof PsiMethod) {
//
//            Document document = editor.getDocument();
//            int elementLineNumber = document.getLineNumber(offset);
//            int lineStartOffset;
//            lineStartOffset = document.getLineStartOffset(elementLineNumber);
//            int line = document.getLineNumber(offset);
//            int startOffset = document.getLineStartOffset(line);
//            int column = offset - startOffset;
//
//
//            String elementTypeClass = ((PsiMethod) psiElement).getName();
//            InlayPresentation inlayWithOnClick = createInlayPresentation(psiElement.getClass().getSimpleName());
//            InlayPresentation inlayWithOnClick1 = createInlayPresentation(elementTypeClass);
//            SequencePresentation sequenceOfInlays = new SequencePresentation(
//                    Arrays.asList(inlayWithOnClick, inlayWithOnClick1));
//            InsetPresentation shiftedSequenceOfInlays = presentationFactory.inset(sequenceOfInlays, column, 0, 0,
//                    0);
//            logger.warn("PSIElement " +
//                    "[" + psiElement.getClass().getSimpleName() + "]" +
//                    "[" + elementLineNumber + "," + column + "]: "
//                    + currentClass.getQualifiedName() + "."
//                    + ((PsiMethod) psiElement).getName() + "()");
//            inlayHintsSink.addBlockElement(lineStartOffset, false, true, 0, shiftedSequenceOfInlays);
//        }
//        return true;
    }

    @NotNull
    private InlayPresentation createInlayPresentation(String elementTypeClass) {
//        InlayPresentation textInlay = presentationFactory.text(elementTypeClass);
//        InlayPresentation inlayContainer = presentationFactory.container(textInlay,
//                INLAY_PADDING, ROUNDED_CORNERS, INLAY_BACKGROUND_COLOR, BACKGROUND_ALPHA);
//        InlayPresentation textWithCursorOnHover = presentationFactory.withCursorOnHover(
//                inlayContainer, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
//        );
//        InlayPresentation inlayWithOnClick = presentationFactory.onClick(
//                textWithCursorOnHover,
//                MouseButton.Left, (mouseEvent, point) -> {
//                    logger.warn("clicked hint: " + elementTypeClass);
//                    return null;
//                }
//        );
//        return inlayWithOnClick;
        return null;
    }


}
