//package com.insidious.plugin.tester.creator;
//
//import com.insidious.plugin.adapter.MethodAdapter;
//import com.insidious.plugin.factory.CandidateSearchQuery;
//import com.insidious.plugin.factory.InsidiousService;
//import com.insidious.plugin.pojo.atomic.MethodUnderTest;
//import com.insidious.plugin.util.UIUtils;
//import com.intellij.navigation.ItemPresentation;
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.project.Project;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiFile;
//import com.intellij.psi.util.PsiTreeUtil;
//import com.intellij.testIntegration.JavaTestCreator;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.swing.*;
//import java.util.ArrayList;
//import java.util.Collection;
//
//public class UnloggedClassTestCreator extends JavaTestCreator implements ItemPresentation {
//
//    private static final Logger logger = LoggerFactory.getLogger(UnloggedClassTestCreator.class);
//
//    @Override
//    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
//        @NotNull Collection<PsiClass> definedClasses = PsiTreeUtil.findChildrenOfType(
//                file, PsiClass.class);
//
////        InsidiousService insidisousService = project.getService(InsidiousService.class);
////        for (PsiClass definedClass : definedClasses) {
////
////            MethodAdapter methodUnderTest = null;
////            CandidateSearchQuery csquery = CandidateSearchQuery.fromMethod(
////                    methodUnderTest, new ArrayList<>(), ""
////            );
////            insidisousService.getStoredCandidatesFor(csquery);
////
////        }
//
//        return file.getName().endsWith(".java") &&
//                !project.getService(InsidiousService.class)
//                        .getCurrentExecutionSession()
//                        .getSessionId()
//                        .equals("na");
//    }
//
//    @Override
//    public void createTest(Project project, Editor editor, PsiFile file) {
//        logger.warn("Create test: " + file.getName());
//    }
//
//
//    @Override
//    public @Nullable String getPresentableText() {
//        return "Create test using Unlogged";
//    }
//
//    @Override
//    public @Nullable Icon getIcon(boolean unused) {
//        return UIUtils.UNLOGGED_ICON_DARK;
//    }
//}
