package com.insidious.plugin.extension.model;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;


public class InsidiousBookmark
        implements Comparable<InsidiousBookmark> {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousBookmark.class);

    private final long basicBlock;

    private final String name;

    private final String psiClass;
    private final int lineNum;

    public InsidiousBookmark(long bb, String name, String psiClass, int lineNum) {
        this.basicBlock = bb;
        this.name = name;
        this.psiClass = psiClass;
        this.lineNum = lineNum;
        logger.debug("Inserting bookmark to map: " + name + ":" + bb);
    }

    public long getBasicBlock() {
        return this.basicBlock;
    }

    public String getName() {
        return this.name;
    }

    public String getPsiClass() {
        return this.psiClass;
    }

    public int getLineNum() {
        return this.lineNum;
    }


    public String toString() {
        return "InsidiousBookmark{basicBlock=" + this.basicBlock + ", name='" + this.name + '\'' + ", psiClass='" + this.psiClass + '\'' + ", lineNum=" + this.lineNum + '}';
    }


    public int compareTo( InsidiousBookmark that) {
        return this.name.compareTo(that.name);
    }
}


