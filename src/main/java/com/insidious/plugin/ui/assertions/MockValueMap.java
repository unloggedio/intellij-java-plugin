package com.insidious.plugin.ui.assertions;

import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.record.AtomicRecordService;
import com.insidious.plugin.ui.highlighter.MockMethodLineHighlighter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MockValueMap {

    private HashMap<String, ArrayList<String>> dependencyMockMap = new HashMap<String, ArrayList<String>>();
    private HashMap<String, String> mockNameIdMap = new HashMap<String, String>();
    private HashMap<String, PsiMethodCallExpression> referenceToPsiMethodCallExpression = new HashMap<String, PsiMethodCallExpression>();

    public MockValueMap(Project project, MethodUnderTest methodUnderTest) {

        AtomicRecordService atomicRecordService = project.getService(AtomicRecordService.class);

        // add dependent methods without declared mock
        PsiClass classPsi = ApplicationManager.getApplication().runReadAction((Computable<PsiClass>) () -> JavaPsiFacade.getInstance(project)
                .findClass(methodUnderTest.getClassName(), GlobalSearchScope.projectScope(project)));

        PsiMethodCallExpression[] methodCallExpressions = ApplicationManager.getApplication()
                .runReadAction((Computable<PsiMethodCallExpression[]>) () ->
                        getChildrenOfTypeRecursive(classPsi, PsiMethodCallExpression.class));
        // todo: check if methodCallExpressions is null
        List<PsiMethodCallExpression> mockableCallExpressions = Arrays.stream(methodCallExpressions)
                .filter(e -> ApplicationManager.getApplication().runReadAction(
                        (Computable<Boolean>) () -> MockMethodLineHighlighter.isNonStaticDependencyCall(e)))
                .collect(Collectors.toList());

        for (PsiMethodCallExpression local : mockableCallExpressions) {
            String localMockMethodName = ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) () -> local.getMethodExpression().getReferenceName());
            if (!referenceToPsiMethodCallExpression.containsKey(localMockMethodName)) {
                referenceToPsiMethodCallExpression.put(localMockMethodName, local);
            }
            if (!dependencyMockMap.containsKey(localMockMethodName)) {
                dependencyMockMap.put(localMockMethodName, new ArrayList<String>());
            }
        }


        // add dependent methods with declared mock
        List<DeclaredMock> allMocks = atomicRecordService.getDeclaredMocksFor(methodUnderTest);

        for (int i = 0; i <= allMocks.size() - 1; i++) {
            DeclaredMock localMock = allMocks.get(i);
            String localMockId = localMock.getId();
            String localMockName = localMock.getName();
            String localMockMethodName = localMock.getMethodName();
            mockNameIdMap.put(localMockId, localMockName);

            if (dependencyMockMap.containsKey(localMockMethodName)) {
                dependencyMockMap.get(localMockMethodName).add(localMockId);
            } else {
                dependencyMockMap.put(localMockMethodName, new ArrayList<String>());
                dependencyMockMap.get(localMockMethodName).add(localMockId);
            }
        }
    }

    public static <T extends PsiElement> T[] getChildrenOfTypeRecursive(PsiElement element, Class<T> aClass) {
        if (element == null) return null;
        List<T> result = getChildrenOfTypeAsListRecursive(element, aClass);
        return result.isEmpty() ? null : ArrayUtil.toObjectArray(result, aClass);
    }

    public static <T extends PsiElement> List<T> getChildrenOfTypeAsListRecursive(PsiElement element, Class<? extends T> aClass) {
        List<T> result = new ArrayList<>();
        if (element != null) {
            for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (aClass.isInstance(child)) {
                    result.add(aClass.cast(child));
                }
                result.addAll(getChildrenOfTypeAsListRecursive(child, aClass));
            }
        }
        return result;
    }

    public HashMap<String, ArrayList<String>> getDependencyMockMap() {
        return this.dependencyMockMap;
    }

    public HashMap<String, String> getMockNameIdMap() {
        return this.mockNameIdMap;
    }

    public HashMap<String, PsiMethodCallExpression> getReferenceToPsiMethodCallExpression() {
        return this.referenceToPsiMethodCallExpression;
    }
}
