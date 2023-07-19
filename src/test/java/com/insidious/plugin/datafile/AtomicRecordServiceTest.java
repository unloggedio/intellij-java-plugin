package com.insidious.plugin.datafile;

import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.atomic.AtomicRecord;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
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

        String currentDir = System.getProperty("user.dir");
        Project project = Mockito.mock(Project.class);
        Mockito.when(insidiousService.getProject()).thenReturn(project);
        Mockito.when(insidiousService.getProject().getBasePath()).thenReturn(currentDir);
        String saveLocation = currentDir + File.separator + getResourcePath() + "unlogged";
        File directory = new File(saveLocation);

        if (directory.exists() && directory.isDirectory()) {
            deleteDirectoryAndFiles(directory);
        }

        atomicRecordService = new AtomicRecordService(insidiousService);
        atomicRecordService.checkPreRequisits();

        atomicRecordService.setUseNotifications(false);
    }

    @Test
    public void testCRUDflow() {
        String classname = "com.test.classA";
        String methodName = "methodA";
        String methodSignature = "SignA";
        List<String> arguments = new ArrayList<>();
        arguments.add("1");

        StoredCandidate candidate = new StoredCandidate();
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate1");
        candidate.setDescription("Description 1");
        candidate.setMethodArguments(arguments);
        candidate.setMethod(new MethodUnderTest(methodName, methodSignature, 123, classname));
        StoredCandidateMetadata metadata = new StoredCandidateMetadata();
        metadata.setRecordedBy("unlogged");
        metadata.setHostMachineName("unlogged");
        metadata.setTimestamp(System.currentTimeMillis());
        candidate.setMetadata(metadata);

        atomicRecordService.saveCandidate(new MethodUnderTest(methodName, methodSignature, 0, classname), candidate);
        //make sure new record is added
        Assertions.assertEquals("Candidate1",
                atomicRecordService.getStoredRecords().get(classname)
                        .getStoredCandidateMap().get(methodName + "#" + methodSignature).get(0).getName());
        //get map from file
        Assertions.assertEquals("Candidate1",
                atomicRecordService.updateMap().get(classname)
                        .getStoredCandidateMap().get(methodName + "#" + methodSignature).get(0).getName());

        //replace existing candidate
        candidate = new StoredCandidate();
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate1_New");
        candidate.setDescription("Description 1 new");
        candidate.setMethodArguments(arguments);
        candidate.setMethod(new MethodUnderTest(methodName, null, 123, classname));
        metadata = new StoredCandidateMetadata();
        metadata.setRecordedBy("unlogged");
        metadata.setHostMachineName("unlogged");
        metadata.setTimestamp(System.currentTimeMillis());
        candidate.setMetadata(metadata);

        atomicRecordService.saveCandidate(new MethodUnderTest(methodName, methodSignature, 0, classname), candidate);

        //length should be 1
        Assertions.assertEquals(1,
                atomicRecordService.getStoredRecords().get(classname)
                        .getStoredCandidateMap().get(methodName + "#" + methodSignature).size());

        //add new method map to same class
        methodName = "methodB";
        methodSignature = "SignB";
        candidate = new StoredCandidate();
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate2");
        candidate.setDescription("Description 2 new");
        candidate.setMethodArguments(arguments);
        candidate.setMethod(new MethodUnderTest(methodName, null, 1235, classname));
        metadata = new StoredCandidateMetadata();
        metadata.setRecordedBy("unlogged");
        metadata.setHostMachineName("unlogged");
        metadata.setTimestamp(System.currentTimeMillis());
        candidate.setMetadata(metadata);

        atomicRecordService.saveCandidate(new MethodUnderTest(methodName, methodSignature, 0, classname), candidate);
        Assertions.assertEquals(2,
                atomicRecordService.getStoredRecords().get(classname)
                        .getStoredCandidateMap().size());

        //test hasStoredCandidates
        boolean hasCandidates = atomicRecordService.hasStoredCandidateForMethod(classname,
                methodName + "#" + methodSignature);
        //true case
        Assertions.assertEquals(true, hasCandidates);

        //false case
        hasCandidates = atomicRecordService.hasStoredCandidateForMethod(classname,
                methodName + "#1" + methodSignature);
        Assertions.assertEquals(false, hasCandidates);

        //get candidates case

        //positive case
        List<StoredCandidate> candidateList = atomicRecordService.getStoredCandidatesForMethod(classname,
                methodName + "#" + methodSignature);
        Assertions.assertEquals(1, candidateList.size());

        //negative case
        candidateList = atomicRecordService.getStoredCandidatesForMethod(classname,
                methodName + "#1" + methodSignature);
        Assertions.assertEquals(List.of(), candidateList);

        //compute gutter status
        //data available state
        GutterState state = atomicRecordService.computeGutterState(new MethodUnderTest(methodName, methodSignature,
                1235, classname));
        Assertions.assertEquals(GutterState.DATA_AVAILABLE, state);

        //test update gutter status flow
        atomicRecordService.setCandidateStateForCandidate(candidate.getCandidateId(), classname,
                methodName + "#" + methodSignature, StoredCandidateMetadata.CandidateStatus.PASSING);

        StoredCandidateMetadata.CandidateStatus savedStatus = atomicRecordService.getStoredRecords().get(classname)
                .getStoredCandidateMap().get(methodName + "#" + methodSignature).get(0).getMetadata()
                .getCandidateStatus();

        Assertions.assertEquals(StoredCandidateMetadata.CandidateStatus.PASSING, savedStatus);

        //gutter state - same case
        state = atomicRecordService.computeGutterState(new MethodUnderTest(methodName, methodSignature,
                1235, classname));
        Assertions.assertEquals(GutterState.NO_DIFF, state);

        //diff state
        atomicRecordService.setCandidateStateForCandidate(candidate.getCandidateId(), classname,
                methodName + "#" + methodSignature, StoredCandidateMetadata.CandidateStatus.FAILING);
        state = atomicRecordService.computeGutterState(new MethodUnderTest(methodName, methodSignature,
                1235, classname));
        Assertions.assertEquals(GutterState.DIFF, state);

        //code changed state
        state = atomicRecordService.computeGutterState(new MethodUnderTest(methodName, methodSignature,
                12345, classname));
        Assertions.assertEquals(GutterState.EXECUTE, state);

        //test writeall sync
        //should be updated in file
        atomicRecordService.writeAll();
        Assertions.assertEquals(StoredCandidateMetadata.CandidateStatus.FAILING,
                atomicRecordService.updateMap().get(classname)
                        .getStoredCandidateMap().get(methodName + "#" + methodSignature).get(0).getMetadata()
                        .getCandidateStatus());

        //test delete flow
        atomicRecordService.deleteStoredCandidate(classname, methodName + "#" + methodSignature,
                candidate.getCandidateId());
        Assertions.assertEquals(0,
                atomicRecordService.getStoredRecords().get(classname)
                        .getStoredCandidateMap().get(methodName + "#" + methodSignature).size());

        //add new candidate to existing method
        methodName = "methodA";
        methodSignature = "SignA";

        arguments = new ArrayList<>();
        arguments.add("2");

        candidate = new StoredCandidate();
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate1_New");
        candidate.setDescription("Description 1 new");
        candidate.setMethodArguments(arguments);
        candidate.setMethod(new MethodUnderTest(methodName, null, 123, classname));
        metadata = new StoredCandidateMetadata();
        metadata.setRecordedBy("unlogged");
        metadata.setHostMachineName("unlogged");
        metadata.setTimestamp(System.currentTimeMillis());
        candidate.setMetadata(metadata);

        atomicRecordService.saveCandidate(new MethodUnderTest(methodName, methodSignature, 0, classname), candidate);

        //length should be 2
        Assertions.assertEquals(2,
                atomicRecordService.getStoredRecords().get(classname)
                        .getStoredCandidateMap().get(methodName + "#" + methodSignature).size());
    }

    @Test
    public void testExceptionMessage() {
        //add flow
        Assertions.assertEquals("Added Record.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.ADD, true));
        Assertions.assertEquals("Failed to add record." +
                        "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.ADD, false));

        //update flow
        Assertions.assertEquals("Updated Record.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.UPDATE, true));
        Assertions.assertEquals("Failed to update record." +
                        "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.UPDATE, false));

        //delete flow
        Assertions.assertEquals("Deleted Record.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.DELETE, true));
        Assertions.assertEquals("Failed to delete record." +
                        "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                atomicRecordService.getMessageForOperationType
                        (AtomicRecordService.FileUpdateType.DELETE, false));
    }

    @Test
    public void testBasePathLocation() {
        String saveLocation = atomicRecordService.getSaveLocation();
        String currentDir = System.getProperty("user.dir");
        currentDir = currentDir + File.separator + getResourcePath()
                + "unlogged" + File.separator;
        Assertions.assertEquals(currentDir, saveLocation);
    }

    @Test
    public void testFilterCandidates() {
        Map<String, List<StoredCandidate>> candidates = new TreeMap<>();
        String key1 = "A#a";
        List<StoredCandidate> storedCandidateList = new ArrayList<>();
        StoredCandidate candidate = new StoredCandidate();
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate1");
        candidate.setDescription("Description 1");
        candidate.setMethodArguments(new ArrayList<>());
        candidate.setMethod(new MethodUnderTest("a", null, 123, null));
        StoredCandidateMetadata metadata = new StoredCandidateMetadata();
        metadata.setRecordedBy("unlogged");
        metadata.setHostMachineName("unlogged");
        metadata.setTimestamp(System.currentTimeMillis());
        candidate.setMetadata(metadata);

        storedCandidateList.add(candidate);

        candidate = new StoredCandidate();
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate11");
        candidate.setDescription("Description 11");
        candidate.setMethodArguments(new ArrayList<>());
        candidate.setMethod(new MethodUnderTest("a", null, 123, null));
        metadata = new StoredCandidateMetadata();
        metadata.setRecordedBy("unlogged");
        metadata.setHostMachineName("unlogged");
        metadata.setTimestamp(System.currentTimeMillis());
        candidate.setMetadata(metadata);

        storedCandidateList.add(candidate);
        candidates.put(key1, storedCandidateList);

        String key2 = "B#b";

        candidate = new StoredCandidate();
        candidate.setCandidateId(UUID.randomUUID().toString());
        candidate.setName("Candidate11");
        candidate.setDescription("Description 11");
        candidate.setMethodArguments(new ArrayList<>());
        candidate.setMethod(new MethodUnderTest("b", null, 123, null));
        metadata = new StoredCandidateMetadata();
        metadata.setRecordedBy("unlogged");
        metadata.setHostMachineName("unlogged");
        metadata.setTimestamp(System.currentTimeMillis());
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

    @Test
    public void testFailedToFetchFromFile() {
        AtomicRecord record = atomicRecordService.getAtomicRecordFromFile(
                new File(atomicRecordService.getSaveLocation() + "test.json"));
        Assertions.assertEquals(null, record);
    }

    @Test
    public void testCandidateFetchForNonMehtodsnotStored() {
        boolean hasCandidates = atomicRecordService
                .hasStoredCandidateForMethod("someclass", "some#method");
        Assertions.assertEquals(false, hasCandidates);

        List<StoredCandidate> candidates = atomicRecordService
                .getStoredCandidatesForMethod("someclass", "some#method");
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
