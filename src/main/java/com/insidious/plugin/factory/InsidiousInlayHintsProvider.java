package com.insidious.plugin.factory;

import com.insidious.plugin.factory.inlays.InsidiousInlayHintsCollector;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.hints.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InsidiousInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayHintsProvider.class);
    private final SettingsKey<NoSettings> noSettingsSettingsKey = new SettingsKey<>("com.indisious.java");
    private final NoSettings noSettings = new NoSettings();

    @NotNull
    @Override
    public SettingsKey<NoSettings> getKey() {
        return noSettingsSettingsKey;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        return "Unlogged recorded metrics";
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return "Unlogged recorded metrics";
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings noSettings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener changeListener) {
                logger.warn("create component: " + noSettings + " => " + changeListener);
                return null;
            }
        };
    }

    @Nullable
    @Override
    public InlayHintsCollector getCollectorFor(
            @NotNull PsiFile psiFile,
            @NotNull Editor editor,
            @NotNull NoSettings noSettings,
            @NotNull InlayHintsSink inlayHintsSink) {
        return new InsidiousInlayHintsCollector(editor);
    }


    @NotNull
    @Override
    public NoSettings createSettings() {
        return noSettings;
    }
}
