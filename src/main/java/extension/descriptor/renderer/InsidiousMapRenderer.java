package extension.descriptor.renderer;

public class InsidiousMapRenderer
        extends InsidiousCompoundReferenceRenderer {
    InsidiousMapRenderer(InsidiousArrayRenderer arrayRenderer) {
        super("Map",

                RendererManager.createLabelRenderer(" size = ", "size()", null),
                RendererManager.createExpressionArrayChildrenRenderer("entrySet().toArray()", "!isEmpty()", arrayRenderer));

        setClassName("java.util.Map");
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\InsidiousMapRenderer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */