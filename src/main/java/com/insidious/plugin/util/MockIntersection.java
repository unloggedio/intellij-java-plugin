package com.insidious.plugin.util;

import com.insidious.plugin.adapter.MethodAdapter;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MockIntersection {

    public static HashSet<String> enabledStoredMock(InsidiousService insidiousService, HashSet<String> mockIds) {

        HashSet<String> enabledStoredMock = new HashSet<>();
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(insidiousService.getCurrentMethod());
        List<DeclaredMock> allMock = insidiousService.getDeclaredMocksFor(methodUnderTest);

        for (DeclaredMock localMock : allMock) {
            if (mockIds.contains(localMock.getId())) {
                enabledStoredMock.add(localMock.getId());
            }
        }

        return enabledStoredMock;
    }

    public static ArrayList<DeclaredMock> enabledStoredMockDefination(InsidiousService insidiousService, HashSet<String> mockIds, MethodAdapter currentMethod) {

        ArrayList<DeclaredMock> enabledStoredMockDefination = new ArrayList<>();
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(currentMethod);
        List<DeclaredMock> allMock = insidiousService.getDeclaredMocksFor(methodUnderTest);

        for (DeclaredMock localMock : allMock) {
            if (mockIds.contains(localMock.getId())) {
                enabledStoredMockDefination.add(localMock);
            }
        }

        return enabledStoredMockDefination;
    }
}
