package com.insidious.plugin.UITests.Utils;

import com.intellij.remoterobot.fixtures.GutterIcon;

import java.util.Comparator;

public class CustomGutterIconComparator implements Comparator<GutterIcon> {
    public int compare(GutterIcon i1, GutterIcon i2) {
        return i1.getLineNumber() - i2.getLineNumber();
    }
}

