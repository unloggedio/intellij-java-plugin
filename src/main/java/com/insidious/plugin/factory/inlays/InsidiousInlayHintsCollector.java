package com.insidious.plugin.factory.inlays;

import com.insidious.plugin.client.ClassMethodAggregates;
import com.insidious.plugin.client.MethodCallAggregate;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.hints.BlockConstraints;
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.RecursivelyUpdatingRootPresentation;
import com.intellij.codeInsight.hints.presentation.SequencePresentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;

public class InsidiousInlayHintsCollector extends FactoryInlayHintsCollector {
    public static final Color INLAY_BACKGROUND_COLOR = Color.BLUE;
    public static final float BACKGROUND_ALPHA = 0.2f;
    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayHintsCollector.class);
    private final InsidiousService insidiousService;
    private PsiClass currentClass;
    private ClassMethodAggregates classMethodAggregates;

    public InsidiousInlayHintsCollector(Editor editor) {
        super(editor);
        this.insidiousService = editor.getProject().getService(InsidiousService.class);
    }

    @Override
    public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
        return false;
//        if (element instanceof PsiClass) {
//            currentClass = (PsiClass) element;
//
//            classMethodAggregates = insidiousService.getClassMethodAggregates(
//                    currentClass.getQualifiedName());
//        }
//
//        if (element instanceof PsiMethod) {
//            if (classMethodAggregates == null) {
//                logger.warn("we dont have any class method aggregates for class: " + currentClass.getQualifiedName());
//                return false;
//            }
//            PsiMethod methodElement = (PsiMethod) element;
//            MethodCallAggregate methodAggregate = classMethodAggregates.getMethodAggregate(methodElement.getName());
//            if (methodAggregate == null) {
//                logger.warn(
//                        "no aggregate found for method [" + currentClass.getQualifiedName() + "." + methodElement.getName() + "()]");
//                return true;
//            }
//
//            Document document = editor.getDocument();
//            int elementLineNumber = document.getLineNumber(element.getTextOffset());
//
//            TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(element);
//
//
//            String elementTypeClass = methodElement.getName();
//            InlayPresentation inlayShowingCount = createInlayPresentation(methodAggregate.getCount() + " calls");
//            InlayPresentation inlayShowingAverage =
//                    createInlayPresentation(String.format(", avg: %.2f µs", methodAggregate.getAverage()));
//            InlayPresentation inlayShowingStdDev =
//                    createInlayPresentation(String.format(", stdDev: %.2f µs", methodAggregate.getStdDev()));
//            SequencePresentation sequenceOfInlays = new SequencePresentation(
//                    Arrays.asList(inlayShowingCount, inlayShowingAverage, inlayShowingStdDev));
//
//            int line = editor.getDocument().getLineNumber(range.getStartOffset());
//            int column = range.getStartOffset() - editor.getDocument().getLineStartOffset(line);
//
//            RecursivelyUpdatingRootPresentation root = new RecursivelyUpdatingRootPresentation(sequenceOfInlays);
//
//            BlockConstraints constraints = new BlockConstraints(false, 0, 0, column);
//
//            logger.warn("PSIElement " +
//                    "[" + element.getClass().getSimpleName() + "]" +
//                    "[" + elementLineNumber + "," + column + "]: "
//                    + currentClass.getQualifiedName() + "."
//                    + methodElement.getName() + "()");
//
//            inlayHintsSink.addBlockElement(line, true, root, constraints);
//        }
//        return true;
    }

    private TextRange getTextRangeWithoutLeadingCommentsAndWhitespaces(PsiElement element) {

        JBIterable<? extends PsiElement> elementChildren = SyntaxTraverser.psiApi()
                .children(element);
        PsiElement start = elementChildren.filter(e -> !(e instanceof PsiComment) && !(e instanceof PsiWhiteSpace))
                .first();
        return TextRange.create(start.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
    }

    @NotNull
    private InlayPresentation createInlayPresentation(String inlayText) {

        PresentationFactory factory = getFactory();

        InlayPresentation text = factory.smallTextWithoutBackground(inlayText);
        InlayPresentation withIcon = text;

        return factory.referenceOnHover(withIcon, (mouseEvent, point) -> {

        });
    }


}
