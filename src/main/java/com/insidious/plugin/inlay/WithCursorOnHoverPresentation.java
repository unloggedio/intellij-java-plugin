package com.insidious.plugin.inlay;

import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.StaticDelegatePresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;

import java.awt.*;
import java.awt.event.MouseEvent;

class WithCursorOnHoverPresentation extends StaticDelegatePresentation {

    private final InlayPresentation presentation;
    private final Cursor cursor;
    private final Editor editor;

    public WithCursorOnHoverPresentation(InlayPresentation presentation, Cursor cursor, Editor editor) {
        super(presentation);
        this.presentation = presentation;
        this.cursor = cursor;
        this.editor = editor;
    }

    public void mouseMoved(MouseEvent event, Point translated) {
        super.mouseMoved(event, translated);
        ((EditorImpl) editor).setCustomCursor(this.getClass(), cursor);
    }

    public void mouseExited() {
        super.mouseExited();
        ((EditorImpl) editor).setCustomCursor(this.getClass(), null);
    }
}