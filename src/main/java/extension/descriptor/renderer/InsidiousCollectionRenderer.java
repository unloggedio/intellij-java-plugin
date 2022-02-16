package extension.descriptor.renderer;

public class InsidiousCollectionRenderer
        extends InsidiousCompoundReferenceRenderer {
    InsidiousCollectionRenderer(InsidiousArrayRenderer arrayRenderer) {
        super("Collection",

                RendererManager.createLabelRenderer(" size = ", "size()", null),
                RendererManager.createExpressionArrayChildrenRenderer("toArray()", "!isEmpty()", arrayRenderer));

        setClassName("java.util.Collection");
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\InsidiousCollectionRenderer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */