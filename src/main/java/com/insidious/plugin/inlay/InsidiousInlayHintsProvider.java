package com.insidious.plugin.inlay;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.hints.*;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;

import javax.swing.*;

public class InsidiousInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayHintsProvider.class);
    private final SettingsKey<NoSettings> noSettingsSettingsKey = new SettingsKey<>("com.indisious.java");
    private final NoSettings noSettings = new NoSettings();


    @Override
    public SettingsKey<NoSettings> getKey() {
        return noSettingsSettingsKey;
    }


    @Override
    public String getName() {
        return "Unlogged recorded metrics";
    }


    @Override
    public String getPreviewText() {
        return "Unlogged recorded metrics";
    }


    @Override
    public ImmediateConfigurable createConfigurable(NoSettings noSettings) {
        return new ImmediateConfigurable() {

            @Override
            public JComponent createComponent(ChangeListener changeListener) {
                logger.warn("create component: " + noSettings + " => " + changeListener);
                return null;
            }
        };
    }


    @Override
    public InlayHintsCollector getCollectorFor(
            PsiFile psiFile,
            Editor editor,
            NoSettings noSettings,
            InlayHintsSink inlayHintsSink) {
        return new InsidiousInlayHintsCollector(editor);
    }

    @Override
    public boolean isVisibleInSettings() {
        return false;
    }

    @Override
    public boolean isLanguageSupported(Language language) {
        return true;
    }


    @Override
    public NoSettings createSettings() {
        return noSettings;
    }
}
