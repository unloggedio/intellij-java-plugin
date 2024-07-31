package com.insidious.plugin.UITests.wrapper;

public class TerminalCommandOption {
    private String text;
    private int durationBeforeOption;
    private int durationAfterOption;

    public TerminalCommandOption(String text, int durationBeforeOption, int durationAfterOption) {
        this.text = text;
        this.durationBeforeOption = durationBeforeOption;
        this.durationAfterOption = durationAfterOption;
    }

    public String getText() {
        return text;
    }

    public int getDurationBeforeOption() {
        return durationBeforeOption;
    }

    public int getDurationAfterOption() {
        return durationAfterOption;
    }
}
