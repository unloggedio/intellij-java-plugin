package com.insidious.plugin.atomicrecord;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.record.AtomicRecordService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.*;

public class AtomicRecordServiceTest {

    private AtomicRecordService atomicRecordService;
    private InsidiousService insidiousService;

    @BeforeEach
    public void setup() {
        insidiousService = Mockito.mock(InsidiousService.class);
        UsageInsightTracker.getInstance().close();

        String currentDir = System.getProperty("user.dir");
        Project project = Mockito.mock(Project.class);
        ModuleManager mockedModuleManager = Mockito.mock(ModuleManager.class);

        Mockito.when(mockedModuleManager.getModules()).thenReturn(new Module[0]);
        Mockito.when(project.getService(ModuleManager.class)).thenReturn(mockedModuleManager);
        Mockito.when(project.getService(InsidiousService.class)).thenReturn(insidiousService);

        Mockito.when(insidiousService.getProject()).thenReturn(project);
        Mockito.when(insidiousService.getProject().getBasePath()).thenReturn(currentDir);
        String saveLocation = currentDir + File.separator + getResourcePath() + "unlogged";
        File directory = new File(saveLocation);

        if (directory.exists() && directory.isDirectory()) {
            deleteDirectoryAndFiles(directory);
        }

        atomicRecordService = new AtomicRecordService(project);
        atomicRecordService.checkPreRequisites();

        atomicRecordService.setUseNotifications(false);
    }

    @Test
    public void testCRUDflow() {
        String classname = "com.test.classA";
        String methodName = "methodA";
        String methodSignature = "SignA";
        List<String> arguments = new ArrayList<>();
        arguments.add("1");

        TestCandidateMetadata candidateMetadata = new TestCandidateMetadata();
        Parameter returnValue = new Parameter();
        returnValue.setProbeAndProbeInfo(new DataEventWithSessionId(), null);
        MethodCallExpression methodCallExpression = new MethodCallExpression("method",
                new Parameter(), new ArrayList<>(), returnValue, 0);
        methodCallExpression.setReturnDataEvent(new DataEventWithSessionId());
        methodCallExpression.setEntryProbe(new DataEventWithSessionId());
        candidateMetadata.setMainMethod(methodCallExpression);
        StoredCandidate candidate = new StoredCandidate(candidateMetadata);
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate1");
        candidate.setDescription("Description 1");
        candidate.setMethodArguments(arguments);
        candidate.setMethod(new MethodUnderTest(methodName, methodSignature, 123, classname));
        StoredCandidateMetadata metadata = new StoredCandidateMetadata("unlogged", "unlogged",
                System.currentTimeMillis());

        candidate.setMetadata(metadata);

        MethodUnderTest methodUnderTest3 = new MethodUnderTest(methodName, methodSignature, 0, classname);
        atomicRecordService.saveCandidate(methodUnderTest3, candidate);
        //make sure new record is added
        Assertions.assertEquals("Candidate1",
                atomicRecordService.getCandidatesByClass(classname).get(methodUnderTest3.getMethodHashKey()).get(0)
                        .getName());
        //get map from file
        Assertions.assertEquals("Candidate1",
                atomicRecordService.updateMap().get(classname)
                        .getStoredCandidateMap().get(methodUnderTest3.getMethodHashKey()).get(0).getName());

        //replace existing candidate
        TestCandidateMetadata candidateMetadata1 = new TestCandidateMetadata();
        Parameter returnValue1 = new Parameter();
        returnValue.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());
        MethodCallExpression methodCallExpression1 = new MethodCallExpression("method",
                new Parameter(), new ArrayList<>(), returnValue1, 0);
        methodCallExpression1.setReturnDataEvent(new DataEventWithSessionId());
        methodCallExpression1.setEntryProbe(new DataEventWithSessionId());
        candidateMetadata1.setMainMethod(methodCallExpression);

        candidate = new StoredCandidate(candidateMetadata1);
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate1_New");
        candidate.setDescription("Description 1 new");
        candidate.setMethodArguments(arguments);
        candidate.setMethod(new MethodUnderTest(methodName, null, 123, classname));
        metadata = new StoredCandidateMetadata("unlogged", "unlogged",
                System.currentTimeMillis());

        candidate.setMetadata(metadata);

        MethodUnderTest methodUnderTest4 = new MethodUnderTest(methodName, methodSignature, 0, classname);
        atomicRecordService.saveCandidate(methodUnderTest4, candidate);

        //length should be 1
        Assertions.assertEquals(1,
                atomicRecordService.getCandidatesByClass(classname).get(methodUnderTest4.getMethodHashKey()).size());

        //add new method map to same class
        methodName = "methodB";
        methodSignature = "SignB";

        TestCandidateMetadata candidateMetadata2 = new TestCandidateMetadata();
        Parameter returnValue2 = new Parameter();
        MethodCallExpression methodCallExpression2 = new MethodCallExpression("method",
                new Parameter(), new ArrayList<>(), returnValue2, 0);
        methodCallExpression2.setReturnDataEvent(new DataEventWithSessionId());
        methodCallExpression2.setEntryProbe(new DataEventWithSessionId());
        Parameter returnValue3 = new Parameter();
        returnValue3.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());
        methodCallExpression2.setReturnValue(returnValue3);
        candidateMetadata2.setMainMethod(methodCallExpression2);


        candidate = new StoredCandidate(candidateMetadata2);
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate2");
        candidate.setDescription("Description 2 new");
        candidate.setMethodArguments(arguments);
        candidate.setMethod(new MethodUnderTest(methodName, null, 1235, classname));

        metadata = new StoredCandidateMetadata("unlogged", "unlogged",
                System.currentTimeMillis());

        candidate.setMetadata(metadata);

        MethodUnderTest methodUnderTest = new MethodUnderTest(methodName, methodSignature, 0, classname);
        atomicRecordService.saveCandidate(methodUnderTest, candidate);
        Assertions.assertEquals(2,
                atomicRecordService.getCandidatesByClass(classname).size());

        CandidateSearchQuery query = new CandidateSearchQuery(methodUnderTest, "", List.of());

        //test hasStoredCandidates
        boolean hasCandidates = atomicRecordService.hasStoredCandidateForMethod(methodUnderTest);
        //true case
        Assertions.assertTrue(hasCandidates);

        //false case
        MethodUnderTest methodUnderTest2 = new MethodUnderTest(methodName, "1" + methodSignature, 0, classname);
        hasCandidates = atomicRecordService.hasStoredCandidateForMethod(methodUnderTest2);
        Assertions.assertFalse(hasCandidates);

        //get candidates case

        //positive case
        List<StoredCandidate> candidateList = atomicRecordService.getStoredCandidatesForMethod(methodUnderTest);
        Assertions.assertEquals(1, candidateList.size());

        //negative case
        candidateList = atomicRecordService.getStoredCandidatesForMethod(methodUnderTest2);
        Assertions.assertEquals(List.of(), candidateList);

        //compute gutter status
        //data available state
        MethodUnderTest method = new MethodUnderTest(methodName, methodSignature, 1235, classname);
        GutterState state = atomicRecordService.computeGutterState(method);
        Assertions.assertEquals(GutterState.DATA_AVAILABLE, state);

        //test update gutter status flow
        atomicRecordService.setCandidateStateForCandidate(candidate.getCandidateId(), classname,
                method.getMethodHashKey(), StoredCandidateMetadata.CandidateStatus.PASSING);

        StoredCandidateMetadata.CandidateStatus savedStatus = atomicRecordService.
                getCandidatesByClass(classname).get(method.getMethodHashKey()).get(0).getMetadata()
                .getCandidateStatus();

        Assertions.assertEquals(StoredCandidateMetadata.CandidateStatus.PASSING, savedStatus);

        //gutter state - same case
        state = atomicRecordService.computeGutterState(new MethodUnderTest(methodName, methodSignature,
                1235, classname));
        Assertions.assertEquals(GutterState.NO_DIFF, state);

        //diff state
        atomicRecordService.setCandidateStateForCandidate(candidate.getCandidateId(), classname,
                method.getMethodHashKey(), StoredCandidateMetadata.CandidateStatus.FAILING);
        state = atomicRecordService.computeGutterState(new MethodUnderTest(methodName, methodSignature,
                1235, classname));
        Assertions.assertEquals(GutterState.DIFF, state);

        //code changed state
        MethodUnderTest method1 = new MethodUnderTest(methodName, methodSignature,
                12345, classname);
        state = atomicRecordService.computeGutterState(method1);
        Assertions.assertEquals(GutterState.EXECUTE, state);

        //test writeall sync
        //should be updated in file
        atomicRecordService.writeAll();
        Assertions.assertEquals(StoredCandidateMetadata.CandidateStatus.FAILING,
                atomicRecordService.updateMap().get(classname)
                        .getStoredCandidateMap().get(method1.getMethodHashKey()).get(0).getMetadata()
                        .getCandidateStatus());

        //test delete flow
        atomicRecordService.deleteStoredCandidate(classname, method1.getMethodHashKey(),
                candidate.getCandidateId());
        Assertions.assertEquals(0,
                atomicRecordService.getCandidatesByClass(classname).get(method1.getMethodHashKey()).size());

        //add new candidate to existing method
        methodName = "methodA";
        methodSignature = "SignA";

        arguments = new ArrayList<>();
        arguments.add("2");


        TestCandidateMetadata candidateMetadata4 = new TestCandidateMetadata();
        Parameter returnValue4 = new Parameter();
        MethodCallExpression methodCallExpression4 = new MethodCallExpression("method",
                new Parameter(), new ArrayList<>(), returnValue4, 0);
        methodCallExpression4.setReturnDataEvent(new DataEventWithSessionId());
        methodCallExpression4.setEntryProbe(new DataEventWithSessionId());
        Parameter returnValue5 = new Parameter();
        returnValue5.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());
        methodCallExpression4.setReturnValue(returnValue5);
        candidateMetadata4.setMainMethod(methodCallExpression4);

        candidate = new StoredCandidate(candidateMetadata4);
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate1_New");
        candidate.setDescription("Description 1 new");
        candidate.setMethodArguments(arguments);
        candidate.setMethod(new MethodUnderTest(methodName, null, 123, classname));

        metadata = new StoredCandidateMetadata("unlogged", "unlogged",
                System.currentTimeMillis());

        candidate.setMetadata(metadata);

        MethodUnderTest methodUnderTest1 = new MethodUnderTest(methodName, methodSignature, 0, classname);
        atomicRecordService.saveCandidate(methodUnderTest1, candidate);

        //length should be 2
        Assertions.assertEquals(2,
                atomicRecordService.getCandidatesByClass(classname).get(methodUnderTest1.getMethodHashKey()).size());
    }

    @Test
    public void testExceptionMessage() {
        //add flow
        Assertions.assertEquals("Added test candidate",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.ADD_CANDIDATE, null, true));
        Assertions.assertEquals("Failed to add test candidate" +
                        "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.ADD_CANDIDATE, null, false));

        //update flow
        Assertions.assertEquals("Updated test candidate",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.UPDATE_CANDIDATE, null, true));
        Assertions.assertEquals("Failed to update test candidate" +
                        "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.UPDATE_CANDIDATE, null, false));

        //delete flow
        Assertions.assertEquals("Deleted test candidate",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.DELETE_CANDIDATE, null, true));
        Assertions.assertEquals("Failed to delete test candidate" +
                        "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.DELETE_CANDIDATE, null, false));
    }

    @Test
    public void testFilterCandidates() {
        Map<String, List<StoredCandidate>> candidates = new TreeMap<>();
        String key1 = "A#a";
        List<StoredCandidate> storedCandidateList = new ArrayList<>();
        TestCandidateMetadata candidateMetadata = new TestCandidateMetadata();
        Parameter returnValue = new Parameter();
        returnValue.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());

        MethodCallExpression methodCallExpression = new MethodCallExpression("method",
                new Parameter(), new ArrayList<>(), returnValue, 0);

        methodCallExpression.setReturnDataEvent(new DataEventWithSessionId());
        methodCallExpression.setEntryProbe(new DataEventWithSessionId());
        candidateMetadata.setMainMethod(methodCallExpression);


        StoredCandidate candidate = new StoredCandidate(candidateMetadata);
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate1");
        candidate.setDescription("Description 1");
        candidate.setMethodArguments(new ArrayList<>());
        candidate.setMethod(new MethodUnderTest("a", null, 123, null));

        StoredCandidateMetadata metadata = new StoredCandidateMetadata("unlogged", "unlogged",
                System.currentTimeMillis());

        candidate.setMetadata(metadata);

        storedCandidateList.add(candidate);

        TestCandidateMetadata candidateMetadata1 = new TestCandidateMetadata();
        Parameter returnValue1 = new Parameter();
        MethodCallExpression methodCallExpression1 = new MethodCallExpression("method",
                new Parameter(), new ArrayList<>(), returnValue1, 0);
        methodCallExpression1.setReturnDataEvent(new DataEventWithSessionId());
        methodCallExpression1.setEntryProbe(new DataEventWithSessionId());
        candidateMetadata1.setMainMethod(methodCallExpression);


        candidate = new StoredCandidate(candidateMetadata1);
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate11");
        candidate.setDescription("Description 11");
        candidate.setMethodArguments(new ArrayList<>());
        candidate.setMethod(new MethodUnderTest("a", null, 123, null));

        metadata = new StoredCandidateMetadata("unlogged", "unlogged",
                System.currentTimeMillis());


        candidate.setMetadata(metadata);

        storedCandidateList.add(candidate);
        candidates.put(key1, storedCandidateList);

        String key2 = "B#b";


        TestCandidateMetadata candidateMetadata4 = new TestCandidateMetadata();
        Parameter returnValue4 = new Parameter();
        MethodCallExpression methodCallExpression4 = new MethodCallExpression("method",
                new Parameter(), new ArrayList<>(), returnValue4, 0);
        methodCallExpression4.setReturnDataEvent(new DataEventWithSessionId());
        methodCallExpression4.setEntryProbe(new DataEventWithSessionId());
        Parameter returnValue5 = new Parameter();
        returnValue5.setProbeAndProbeInfo(new DataEventWithSessionId(), new DataInfo());
        methodCallExpression4.setReturnValue(returnValue5);
        candidateMetadata4.setMainMethod(methodCallExpression4);

        candidate = new StoredCandidate(candidateMetadata4);
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate11");
        candidate.setDescription("Description 11");
        candidate.setMethodArguments(new ArrayList<>());
        candidate.setMethod(new MethodUnderTest("b", null, 123, null));

        metadata = new StoredCandidateMetadata("unlogged", "unlogged",
                System.currentTimeMillis());


        candidate.setMetadata(metadata);

        storedCandidateList = new ArrayList<>();
        storedCandidateList.add(candidate);
        candidates.put(key2, storedCandidateList);

        Map<String, List<StoredCandidate>> filtered = atomicRecordService.filterCandidates(candidates);
        Assertions.assertEquals(2, filtered.size());

        //null check
        Assertions.assertEquals(null, atomicRecordService.filterCandidates(null));
    }

    @Test
    public void testUseNotificationToggle() {
        atomicRecordService.setUseNotifications(true);
        Assertions.assertEquals(true, atomicRecordService.isUseNotifications());
        atomicRecordService.setUseNotifications(false);
    }

//    @Test
//    public void testFailedToFetchFromFile() {
//        AtomicRecord record = atomicRecordService.getAtomicRecordFromFile(
//                new File(atomicRecordService.getSaveLocation() + "test.json"));
//        Assertions.assertEquals(null, record);
//    }

    @Test
    public void testCandidateFetchForNonMehtodsnotStored() {
        MethodUnderTest methodUnderTest = new MethodUnderTest("some", "signature", 123, "someclass");

        boolean hasCandidates = atomicRecordService.hasStoredCandidateForMethod(methodUnderTest);
        Assertions.assertEquals(false, hasCandidates);

        List<StoredCandidate> candidates = atomicRecordService.getStoredCandidatesForMethod(methodUnderTest);
        Assertions.assertEquals(List.of(), candidates);
    }

    private void deleteDirectoryAndFiles(File unlogged) {
        for (File subfile : unlogged.listFiles()) {
            subfile.delete();
        }
    }

    public String getResourcePath() {
        return "src" + File.separator + "test" + File.separator + "resources" + File.separator;
    }
}
