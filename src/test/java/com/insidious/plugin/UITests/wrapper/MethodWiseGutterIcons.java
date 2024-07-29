package com.insidious.plugin.UITests.wrapper;

import com.intellij.remoterobot.fixtures.GutterIcon;

import java.util.List;

public class MethodWiseGutterIcons {

    GutterIcon mainGutterIcon;
    List<GutterIcon> ghostMockGutterIcons;

    public MethodWiseGutterIcons(GutterIcon mainGutterIcon, List<GutterIcon> ghostMockGutterIcons) {
        this.mainGutterIcon = mainGutterIcon;
        this.ghostMockGutterIcons = ghostMockGutterIcons;
    }

    public GutterIcon getMainGutterIcon() {
        return mainGutterIcon;
    }

    public List<GutterIcon> getGhostMockGutterIcons() {
        return ghostMockGutterIcons;
    }
}
