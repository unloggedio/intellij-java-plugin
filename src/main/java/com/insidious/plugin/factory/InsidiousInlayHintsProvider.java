package com.insidious.plugin.factory;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.MouseButton;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
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

public class InsidiousInlayHintsProvider implements InlayHintsProvider<NoSettings> {

    private static final Logger logger = LoggerUtil.getInstance(InsidiousInlayHintsProvider.class);
    private final SettingsKey<NoSettings> noSettingsSettingsKey = new SettingsKey<>("com.indisious.java");
    private final NoSettings noSettings = new NoSettings();
    private PresentationFactory inlayPresentationFactory;

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
        inlayPresentationFactory = new PresentationFactory((EditorImpl) editor);
        final InsidiousService insidiousService = editor.getProject().getService(InsidiousService.class);
        return new InlayHintsCollector() {
            @Override
            public boolean collect(@NotNull PsiElement psiElement, @NotNull Editor editor, @NotNull InlayHintsSink inlayHintsSink) {
//                if (psiElement instanceof PsiReferenceParameterList
//                        || psiElement instanceof PsiWhiteSpace
//                        || psiElement instanceof PsiJavaToken
//                ) {
//                    return false;
//                }
                if (psiElement instanceof PsiClass) {
                    logger.warn("Found class: " + ((PsiClass) psiElement).getQualifiedName());
                }
                if (psiElement instanceof PsiMethod) {
                    int elementOffset = psiElement.getTextOffset();
                    int elementLineNumber = editor.getDocument().getLineNumber(elementOffset);
                    String elementTypeClass = psiElement.getClass().getSimpleName();
                    inlayHintsSink.addBlockElement(elementOffset, true, true, 0,
                            inlayPresentationFactory.onClick(
                                    inlayPresentationFactory.withCursorOnHover(
                                            inlayPresentationFactory.text(elementTypeClass),
                                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                    ),
                                    MouseButton.Left, new Function2<MouseEvent, Point, Unit>() {
                                        @Override
                                        public Unit invoke(MouseEvent mouseEvent, Point point) {
                                            logger.warn("clicked hint: " + elementTypeClass);
                                            return null;
                                        }
                                    }
                            ));
                }
                return true;
            }
        };
    }

    @NotNull
    @Override
    public NoSettings createSettings() {
        return noSettings;
    }
}
