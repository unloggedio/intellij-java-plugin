package com.insidious.plugin.extension.descriptor.renderer;

public class InsidiousCollectionRenderer
        extends InsidiousCompoundReferenceRenderer {
    InsidiousCollectionRenderer(InsidiousArrayRenderer arrayRenderer) {
        super("Collection",

                RendererManager.createLabelRenderer(" size = ", "size()", null),
                RendererManager.createExpressionArrayChildrenRenderer("toArray()", "!isEmpty()", arrayRenderer));

        setClassName("java.com.insidious.plugin.util.Collection");
    }
}

