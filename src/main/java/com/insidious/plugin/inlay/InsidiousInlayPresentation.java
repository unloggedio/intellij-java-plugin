package com.insidious.plugin.inlay;

import com.intellij.codeInsight.hints.presentation.BasePresentation;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class InsidiousInlayPresentation extends BasePresentation {
    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public void paint(@NotNull Graphics2D graphics2D, @NotNull TextAttributes textAttributes) {

    }
}
