package extension.descriptor.renderer;

import com.intellij.debugger.engine.evaluation.CodeFragmentKind;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jdi.Type;
import extension.descriptor.InsidiousValueDescriptor;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

public class RendererManager {
    private static final Logger logger = Logger.getInstance(RendererManager.class);
    private static final RendererManager instance = new RendererManager();
    private final InsidiousPrimitiveRenderer myDefaultPrimitiveRenderer;
    private final InsidiousArrayRenderer myDefaultArrayRenderer;
    private final InsidiousClassRenderer myDefaultClassRenderer;
    private final InsidiousToStringRenderer myToStringRenderer;
    private final InsidiousHexRenderer myHexRenderer;
    private final InsidiousListObjectRenderer myListObjectRenderer;
    private final InsidiousMapRenderer myMapRenderer;
    private final InsidiousMapEntryRenderer myMapEntryRenderer;
    private final InsidiousCollectionRenderer myCollectionRenderer;
    private final List<InsidiousNodeRenderer> allRenderers = new ArrayList<>();

    private RendererManager() {
        this.myDefaultPrimitiveRenderer = new InsidiousPrimitiveRenderer();
        this.myDefaultArrayRenderer = new InsidiousArrayRenderer();
        this.myDefaultClassRenderer = new InsidiousClassRenderer();
        this.myToStringRenderer = new InsidiousToStringRenderer();
        this.myHexRenderer = new InsidiousHexRenderer();
        this.myListObjectRenderer = new InsidiousListObjectRenderer(this.myDefaultArrayRenderer);
        this.myMapRenderer = new InsidiousMapRenderer(this.myDefaultArrayRenderer);
        this.myMapEntryRenderer = new InsidiousMapEntryRenderer(this.myDefaultArrayRenderer);
        this.myCollectionRenderer = new InsidiousCollectionRenderer(this.myDefaultArrayRenderer);

        this.allRenderers.add(this.myDefaultPrimitiveRenderer);


        this.allRenderers.add(this.myListObjectRenderer);
        this.allRenderers.add(this.myMapRenderer);
        this.allRenderers.add(this.myMapEntryRenderer);
        this.allRenderers.add(this.myCollectionRenderer);

        this.allRenderers.add(this.myToStringRenderer);
        this.allRenderers.add(this.myDefaultArrayRenderer);
        this.allRenderers.add(this.myDefaultClassRenderer);
        this.allRenderers.add(this.myHexRenderer);
    }

    public static RendererManager getInstance() {
        return instance;
    }

    public static InsidiousLabelRenderer createLabelRenderer(@NonNls final String prefix, @NonNls String expressionText, @NonNls final String postfix) {
        InsidiousLabelRenderer labelRenderer = new InsidiousLabelRenderer() {


            public String calcLabel(InsidiousValueDescriptor descriptor, EvaluationContext evaluationContext, DescriptorLabelListener labelListener) throws EvaluateException {
                String evaluated = super.calcLabel(descriptor, evaluationContext, labelListener);
                if (prefix == null && postfix == null) {
                    return evaluated;
                }
                if (prefix != null && postfix != null) {
                    return prefix + evaluated + postfix;
                }
                if (prefix != null) {
                    return prefix + evaluated;
                }
                return evaluated + postfix;
            }
        };
        labelRenderer.setLabelExpression(new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expressionText, "", JavaFileType.INSTANCE));


        return labelRenderer;
    }

    public static InsidiousExpressionChildrenRenderer createExpressionArrayChildrenRenderer(String expressionText, String childrenExpandableText, InsidiousArrayRenderer arrayRenderer) {
        InsidiousExpressionChildrenRenderer renderer = createExpressionChildrenRenderer(expressionText, childrenExpandableText);
        renderer.setPredictedRenderer(arrayRenderer);
        return renderer;
    }

    public static InsidiousExpressionChildrenRenderer createExpressionChildrenRenderer(@NonNls String expressionText, @NonNls String childrenExpandableText) {
        InsidiousExpressionChildrenRenderer childrenRenderer = new InsidiousExpressionChildrenRenderer();

        childrenRenderer.setChildrenExpression(new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expressionText, "", JavaFileType.INSTANCE));


        if (childrenExpandableText != null) {
            childrenRenderer.setChildrenExpandable(new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, childrenExpandableText, "", JavaFileType.INSTANCE));
        }


        return childrenRenderer;
    }

    public static InsidiousEnumerationChildrenRenderer createEnumerationChildrenRenderer(@NonNls String[][] expressions) {
        InsidiousEnumerationChildrenRenderer childrenRenderer = new InsidiousEnumerationChildrenRenderer();
        if (expressions != null && expressions.length > 0) {
            ArrayList<InsidiousEnumerationChildrenRenderer.ChildInfo> childrenList = new ArrayList<>(expressions.length);

            for (String[] expression : expressions) {
                childrenList.add(new InsidiousEnumerationChildrenRenderer.ChildInfo(expression[0], new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expression[1], "", JavaFileType.INSTANCE), false));
            }


            childrenRenderer.setChildren(childrenList);
        }
        return childrenRenderer;
    }

    public InsidiousNodeRenderer getRenderer(Type type) {
        return getDefaultRenderer(type);
//        return this.allRenderers.stream()
//                .filter(r -> DebuggerUtilsImpl.suppressExceptions((), Boolean.valueOf(false), true, ClassNotPreparedException.class))
//                .findFirst()
//                .orElseGet(() -> getDefaultRenderer(type));
    }

    public InsidiousNodeRenderer getDefaultRenderer(Type type) {
        if (this.myDefaultPrimitiveRenderer.isApplicable(type)) {
            return this.myDefaultPrimitiveRenderer;
        }

        if (this.myDefaultArrayRenderer.isApplicable(type)) {
            return this.myDefaultArrayRenderer;
        }

        logger.assertTrue(this.myDefaultClassRenderer.isApplicable(type), type.name());
        return this.myDefaultClassRenderer;
    }

    public InsidiousToStringRenderer getToStringRenderer() {
        return this.myToStringRenderer;
    }

    public InsidiousHexRenderer getHexRenderer() {
        return this.myHexRenderer;
    }

    public InsidiousPrimitiveRenderer getPrimitiveRenderer() {
        return this.myDefaultPrimitiveRenderer;
    }

    public InsidiousArrayRenderer getArrayRenderer() {
        return this.myDefaultArrayRenderer;
    }

    public InsidiousClassRenderer getClassRenderer() {
        return this.myDefaultClassRenderer;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\RendererManager.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */