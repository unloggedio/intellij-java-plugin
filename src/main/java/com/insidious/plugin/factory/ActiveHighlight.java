package com.insidious.plugin.factory;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;

import java.util.List;

public class ActiveHighlight {
    private final Editor editor;
    private final List<RangeHighlighter> rangeHighlighterList;

    public ActiveHighlight(List<RangeHighlighter> rangeHighlighterList, Editor editor) {
        this.rangeHighlighterList = rangeHighlighterList;
        this.editor = editor;
    }

    public Editor getEditor() {
        return editor;
    }

    public List<RangeHighlighter> getRangeHighlighterList() {
        return rangeHighlighterList;
    }
}
