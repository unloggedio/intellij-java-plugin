package com.insidious.plugin.util;

import java.util.HashSet;
import java.util.List;

import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;

public class MockIntersection {
    
    public static HashSet<String> enabledStoredMock (InsidiousService insidiousService, HashSet<String> enabledMockDefination) { 
       
        HashSet<String> enabledStoredMock = new HashSet<>();
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(insidiousService.getCurrentMethod());
        List<DeclaredMock> allMock = insidiousService.getDeclaredMocksFor(methodUnderTest);

        for (DeclaredMock localMock: allMock) {
            if (enabledMockDefination.contains(localMock.getId())) {
                enabledStoredMock.add(localMock.getId());
            }
        }

        return enabledStoredMock;
    }
}
