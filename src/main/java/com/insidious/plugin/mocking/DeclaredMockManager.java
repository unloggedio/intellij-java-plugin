package com.insidious.plugin.mocking;

import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

public class DeclaredMockManager {
    private Project project;

    public DeclaredMockManager(Project project) {
        this.project = project;
    }

    public List<DeclaredMock> getDeclaredMocks(MethodUnderTest methodUnderTest) {
        return new ArrayList<>();
    }
}
