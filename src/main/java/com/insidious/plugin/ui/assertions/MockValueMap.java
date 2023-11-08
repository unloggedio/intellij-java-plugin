package com.insidious.plugin.ui.assertions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.insidious.plugin.atomicrecord.AtomicRecordService;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;

public class MockValueMap {

    private HashMap<String, ArrayList<String>> dependencyMockMap = new HashMap<String, ArrayList<String>> ();
    private HashMap<String, String> mockNameIdMap = new HashMap<String, String>();

    public MockValueMap(InsidiousService insidiousService) {

        AtomicRecordService atomicRecordService = insidiousService.getAtomicRecordService();
        MethodUnderTest methodUnderTest = MethodUnderTest.fromMethodAdapter(insidiousService.getCurrentMethod());

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

    public HashMap<String, ArrayList<String>> getDependencyMockMap(){
        return this.dependencyMockMap;
    }

    public HashMap<String, String> getMockNameIdMap(){
        return this.mockNameIdMap;
    }
}
