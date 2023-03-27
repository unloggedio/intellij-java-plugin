package com.insidious.plugin.factory;

import com.insidious.plugin.factory.inlays.InsidiousInlayCollector;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class InsidiousInlayHintsProvider {

//    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayHintsProvider.class);
//    private final SettingsKey<NoSettings> noSettingsSettingsKey = new SettingsKey<>("com.indisious.java");
//    private final NoSettings noSettings = new NoSettings();
//    private PresentationFactory inlayPresentationFactory;
//
//    @NotNull
//    @Override
//    public SettingsKey<NoSettings> getKey() {
//        return noSettingsSettingsKey;
//    }
//
//    @Nls(capitalization = Nls.Capitalization.Sentence)
//    @NotNull
//    @Override
//    public String getName() {
//        return "Unlogged recorded metrics";
//    }
//
//    @Nullable
//    @Override
//    public String getPreviewText() {
//        return "Unlogged recorded metrics";
//    }
//
//    @NotNull
//    @Override
//    public ImmediateConfigurable createConfigurable(@NotNull NoSettings noSettings) {
//        return new ImmediateConfigurable() {
//            @NotNull
//            @Override
//            public JComponent createComponent(@NotNull ChangeListener changeListener) {
//                logger.warn("create component: " + noSettings + " => " + changeListener);
//                return null;
//            }
//        };
//    }
//
//    @Nullable
//    @Override
//    public InlayHintsCollector getCollectorFor(
//            @NotNull PsiFile psiFile,
//            @NotNull Editor editor,
//            @NotNull NoSettings noSettings,
//            @NotNull InlayHintsSink inlayHintsSink) {
//        inlayPresentationFactory = new PresentationFactory((EditorImpl) editor);
//        final InsidiousService insidiousService = editor.getProject().getService(InsidiousService.class);
//        return new InsidiousInlayCollector(inlayPresentationFactory, insidiousService);
//    }
//
//
//    @NotNull
//    @Override
//    public NoSettings createSettings() {
//        return noSettings;
//    }
}
