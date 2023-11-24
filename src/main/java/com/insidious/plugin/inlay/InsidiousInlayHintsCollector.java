package com.insidious.plugin.inlay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.client.ClassMethodAggregates;
import com.insidious.plugin.client.MethodCallAggregate;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.presentation.SequencePresentation;
import com.intellij.codeInsight.hints.presentation.SpacePresentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.JBIterable;


import java.awt.*;
import java.util.Arrays;
import java.util.Map;

public class InsidiousInlayHintsCollector extends FactoryInlayHintsCollector {
    public static final Color INLAY_BACKGROUND_COLOR = JBColor.BLUE;
    public static final float BACKGROUND_ALPHA = 0.2f;
    public static final Integer UNLOGGED_APM_GROUP = -500;
    public static final Integer UNLOGGED_REQUEST_GROUP = 101;
    public static final Integer UNLOGGED_RESPONSE_GROUP = 102;
    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayHintsCollector.class);
    private final InsidiousService insidiousService;
    private final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private PsiClass currentClass;
    private ClassMethodAggregates classMethodAggregates;

    public InsidiousInlayHintsCollector(Editor editor) {
        super(editor);
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
    public boolean collect( PsiElement element,  Editor editor,  InlayHintsSink inlayHintsSink) {
        if (element instanceof PsiClass) {
            currentClass = (PsiClass) element;
            classMethodAggregates = insidiousService.getClassMethodAggregates(currentClass.getQualifiedName());
            return true;
        }
        if (!(element instanceof PsiMethod)) {
            return true;
        }


//        if (element instanceof PsiMethod) {
        if (classMethodAggregates == null) {
            logger.warn("we dont have any class method aggregates for class: " + currentClass.getQualifiedName());
            return false;
        }
        PsiMethod methodElement = (PsiMethod) element;
        MethodCallAggregate methodAggregate = classMethodAggregates.getMethodAggregate(methodElement.getName());
        if (methodAggregate == null) {
//                logger.warn(
//                        "no aggregate found for method [" + currentClass.getQualifiedName() + "." + methodElement.getName() + "()]");
            return true;
        }

        Document document = editor.getDocument();
        int elementLineNumber = document.getLineNumber(element.getTextOffset());

        TextRange range = getTextRangeWithoutLeadingCommentsAndWhitespaces(element);


        InlayPresentation inlayShowingCount = createInlayPresentation(methodAggregate.getCount() + " calls");
        String avgStringText = String.format(", avg: " + formatTimeDuration(methodAggregate.getAverage()));
        InlayPresentation inlayShowingAverage = createInlayPresentation(avgStringText);
        String stdDevStringText = String.format(", stdDev: " + formatTimeDuration(methodAggregate.getStdDev()));
        InlayPresentation inlayShowingStdDev = createInlayPresentation(stdDevStringText);


        int line = editor.getDocument().getLineNumber(range.getStartOffset());
        int offset = getAnchorOffset(element);
        int columnWidth = EditorUtil.getPlainSpaceWidth(editor);
        int startOffset = document.getLineStartOffset(line);
        int column = offset - startOffset;

        SequencePresentation sequenceOfInlays = new SequencePresentation(
                Arrays.asList(new SpacePresentation(column * columnWidth, 0), inlayShowingCount,
                        inlayShowingAverage, inlayShowingStdDev));

//        logger.warn("PSIElement " +
//                "[" + element.getClass().getSimpleName() + "]" +
//                "[" + elementLineNumber + "," + column + "]: "
//                + currentClass.getQualifiedName() + "."
//                + methodElement.getName() + "()");

//            inlayHintsSink.addBlockElement(line, true, root, constraints);
        inlayHintsSink.addBlockElement(startOffset, true, true, UNLOGGED_APM_GROUP, sequenceOfInlays);

//            String executionPairKey = currentClass.getQualifiedName() + "#" + ((PsiMethod) element).getName();
//            Pair<AgentCommandRequest, AgentCommandResponse> executionPairList = insidiousService.getExecutionPairs(
//                    executionPairKey);
//            if (executionPairList != null) {
//                AgentCommandRequest agentRequest = executionPairList.getFirst();
//                AgentCommandResponse agentResponse = executionPairList.getSecond();
//
//                BlockConstraints requestInlayConstraints = new BlockConstraints(false, 101, UNLOGGED_REQUEST_GROUP,
//                        column);
//                BlockConstraints responseInlayConstraints = new BlockConstraints(false, 102, UNLOGGED_RESPONSE_GROUP,
//                        column);
//                StringBuilder methodParameterText = new StringBuilder();
//                List<String> methodParameters = agentRequest.getMethodParameters();
//                for (int i = 0; i < methodParameters.size(); i++) {
//                    JvmParameter parameter = methodElement.getParameters()[i];
//                    String methodParameter = methodParameters.get(i);
//                    if (i > 0) {
//                        methodParameterText.append(", ");
//                    }
//                    methodParameterText.append(parameter.getName()).append(": ").append(methodParameter);
//                }
//
//                String inputInlayText = String.format("Input: " + methodParameterText);
//                InlayPresentation executionRequestInlay = createInlayPresentation(inputInlayText);
//
//                InlayPresentation executionResponseInlay = createInlayPresentation(
//                        (Map) agentResponse.getMethodReturnValue());
//
//                RecursivelyUpdatingRootPresentation requestInlayRoot =
//                        new RecursivelyUpdatingRootPresentation(executionRequestInlay);
//                inlayHintsSink.addBlockElement(line, true, requestInlayRoot, requestInlayConstraints);
//
//                RecursivelyUpdatingRootPresentation responseInlayRoot =
//                        new RecursivelyUpdatingRootPresentation(executionResponseInlay);
//                inlayHintsSink.addBlockElement(line, true, responseInlayRoot, responseInlayConstraints);
//
//            }

        return true;
    }

    private TextRange getTextRangeWithoutLeadingCommentsAndWhitespaces(PsiElement element) {

        JBIterable<? extends PsiElement> elementChildren = SyntaxTraverser.psiApi()
                .children(element);
        PsiElement start = elementChildren.filter(e -> !(e instanceof PsiComment) && !(e instanceof PsiWhiteSpace))
                .first();
        return TextRange.create(start.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
    }

    
    private InlayPresentation createInlayPresentation(String inlayText) {

        PresentationFactory factory = getFactory();
        InlayPresentation text;
        try {
            Map valueInMap = objectMapper.readValue(inlayText, Map.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(valueInMap);
            text = factory.smallText(prettyJson);
        } catch (Exception e) {
            // not a json value
            text = factory.smallText(inlayText);
        }

        InlayPresentation withIcon = text;


        return withIcon;

//        return factory.referenceOnHover(withIcon, (mouseEvent, point) -> {
//            logger.warn("inlay on hover: " + mouseEvent.getPoint());
//        });
    }

    
    private InlayPresentation createInlayPresentation(Map valueInMap) {

        PresentationFactory factory = getFactory();
        InlayPresentation text;
        try {
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(valueInMap);
            text = factory.smallText(prettyJson);
        } catch (Exception e) {
            // not a json value
            text = factory.smallText(String.valueOf(valueInMap));
        }

        InlayPresentation withIcon = text;

        return withIcon;

//        return factory.referenceOnHover(withIcon, (mouseEvent, point) -> {
//            logger.warn("inlay on hover: " + mouseEvent.getPoint());
//        });
    }


}
