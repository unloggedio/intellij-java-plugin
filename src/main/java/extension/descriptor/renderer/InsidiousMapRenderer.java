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


