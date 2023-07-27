package com.insidious.plugin.ui.Components.AtomicRecord;

public class AssertionElement {
    private AssertionBlock block;
    private AssertionRule rule;

    public AssertionElement(AssertionRule rule, AssertionBlock block) {
        this.rule = rule;
        this.block = block;
    }

    public AssertionBlock getBlock() {
        return block;
    }

    public void setBlock(AssertionBlock block) {
        this.block = block;
    }

    public AssertionRule getRule() {
        return rule;
    }

    public void setRule(AssertionRule rule) {
        this.rule = rule;
    }


    @Override
    public String toString() {
        return "AssertionElement{" +
                "block=" + block +
                ", rule=" + rule +
                '}';
    }
}
