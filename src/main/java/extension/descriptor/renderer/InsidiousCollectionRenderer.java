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

