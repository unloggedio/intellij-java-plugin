package com.insidious.plugin.inlay;

import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.StaticDelegatePresentation;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;

public class WithCursorOnHoverPresentation extends StaticDelegatePresentation {
    public WithCursorOnHoverPresentation(@NotNull InlayPresentation presentation) {
        super(presentation);
    }

    public void mouseMoved(MouseEvent event, Point translated) {
//        super.mouseMoved(event, translated);
//        (editor as? EditorImpl)?.setCustomCursor(this::class, cursor)
    }

    public void mouseExited() {
//        super.mouseExited()
//        (editor as? EditorImpl)?.setCustomCursor(this::class, null)
    }
}
