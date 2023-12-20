package com.insidious.plugin.autoexecutor;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class GlobalJavaSearchContext extends GlobalSearchScope {
    private ProjectFileIndex index;

    public GlobalJavaSearchContext(Project project) {
        super(project);
        this.index = ProjectRootManager.getInstance(project).getFileIndex();
    }

    @Override
    public boolean isSearchInModuleContent(Module aModule) {
        return false;
    }

    @Override
    public boolean isSearchInLibraries() {
        return false;
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        return index.isInSourceContent(file);
    }
}
