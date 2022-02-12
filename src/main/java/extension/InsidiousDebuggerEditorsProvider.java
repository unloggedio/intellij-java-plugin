package extension;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaCodeFragmentFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousDebuggerEditorsProvider extends XDebuggerEditorsProviderBase {
    @Override
    protected PsiFile createExpressionCodeFragment(@NotNull Project project, @NotNull String text, @Nullable PsiElement context, boolean isPhysical) {
        return JavaCodeFragmentFactory.getInstance(project).createTypeCodeFragment(text, context, isPhysical);
    }

    @Override
    public @NotNull FileType getFileType() {
        return FileTypeManager.getInstance().getStdFileType("JAVA");
    }
}
