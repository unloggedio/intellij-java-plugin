package com.insidious.plugin.datafile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.AtomicRecord;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.io.File.separator;

public class AtomicRecordService {
    public static final String TEST_RESOURCES_PATH = "src" + separator + "test" + separator + "resources" + separator;
    private static final Logger logger = LoggerUtil.getInstance(AtomicRecordService.class);
    private final String UNLOGGED_RESOURCE_FOLDER_NAME = "unlogged";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String projectBasePath;
    InsidiousService insidiousService;
    private Map<String, AtomicRecord> storedRecords = null;
    private boolean useNotifications = true;

    public AtomicRecordService(InsidiousService service) {
        this.insidiousService = service;
        projectBasePath = insidiousService.getProject().getBasePath();
    }

    public void init() {
        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        if (dumbService != null) {
            dumbService.runWhenSmart(this::checkPreRequisites);
        }
    }

    public GutterState computeGutterState(MethodUnderTest method) {

        try {
            String methodKey = method.getMethodHashKey();
            AtomicRecord record = this.storedRecords.get(method.getClassName());
            if (record == null) {
                return null;
            }
            List<StoredCandidate> candidates;
            if (record.getStoredCandidateMap().get(methodKey) != null) {
                candidates = new ArrayList<>(record.getStoredCandidateMap().get(methodKey));
            } else {
                return null;
            }
            boolean hashChange = false;
            StoredCandidateMetadata.CandidateStatus status = null;
            for (StoredCandidate candidate : candidates) {
                MethodUnderTest candidateMethodUnderTest = candidate.getMethod();
                if (candidateMethodUnderTest.getMethodHash() != method.getMethodHash()) {
                    hashChange = true;
                }
                if (status == null) {
                    status = candidate.getMetadata().getCandidateStatus();
                } else {
                    if (candidate.getMetadata().getCandidateStatus()
                            .equals(StoredCandidateMetadata.CandidateStatus.FAILING)) {
                        status = StoredCandidateMetadata.CandidateStatus.FAILING;
                    }
                }
            }
            if (hashChange) {
                return GutterState.EXECUTE;
            }
            if (status == null) {
                return GutterState.DATA_AVAILABLE;
            }
            if (status.equals(StoredCandidateMetadata.CandidateStatus.FAILING)) {
                return GutterState.DIFF;
            } else if (status.equals(StoredCandidateMetadata.CandidateStatus.PASSING)) {
                return GutterState.NO_DIFF;
            } else {
                return GutterState.DATA_AVAILABLE;
            }
        } catch (Exception e) {
            logger.info("Exception computing gutter state." + e);
            return null;
        }
    }


    public void saveCandidate(MethodUnderTest methodUnderTest, StoredCandidate candidate) {
        try {
            AtomicRecord existingRecord = null;
            AtomicRecord atomicRecord = this.storedRecords.get(methodUnderTest.getClassName());
            String methodKey = methodUnderTest.getMethodHashKey();
            if (atomicRecord == null) {
                //create record
                logger.info("[ATRS] creating a new record");
                addNewRecord(methodKey, methodUnderTest.getClassName(), candidate);
            } else {
                //read as array of AtomicRecords
                logger.info("[ATRS] creating a new record");
                boolean foundMethod = false;
                boolean foundCandidate = false;

                if (atomicRecord.getStoredCandidateMap().get(methodKey) != null) {
                    foundMethod = true;
                    existingRecord = atomicRecord;
                    List<StoredCandidate> candidates = atomicRecord.getStoredCandidateMap().get(methodKey);
                    for (StoredCandidate storedCandidate : candidates) {
                        if (storedCandidate.getMethodArguments().equals(candidate.getMethodArguments())) {
                            foundCandidate = true;
                            //replace
                            if (useNotifications) {
                                InsidiousNotification.notifyMessage("Replacing existing record",
                                        NotificationType.INFORMATION);
                            }
                            logger.info("[ATRS] Replacing existing record");
                            storedCandidate.copyFrom(candidate);
                        }
                    }
                }
                if (!foundMethod) {
                    logger.info("[ATRS] Adding new map entry");
                    List<StoredCandidate> candidates = new ArrayList<>();
                    candidates.add(candidate);
                    atomicRecord.getStoredCandidateMap().put(methodKey, candidates);

                } else if (!foundCandidate) {
                    //add to stored candidates
                    logger.info("[ATRS] Adding Candidate");
                    existingRecord.getStoredCandidateMap().get(methodKey).add(candidate);
                    existingRecord.setStoredCandidateMap(filterCandidates(existingRecord.getStoredCandidateMap()));
                    writeToFile(new File(getFilenameForClass(methodUnderTest.getClassName())),
                            atomicRecord, FileUpdateType.UPDATE, useNotifications);
                } else {
                    logger.info("[ATRS] Replacing existing record (found)");
                    existingRecord.setStoredCandidateMap(filterCandidates(existingRecord.getStoredCandidateMap()));
                    writeToFile(new File(getFilenameForClass(methodUnderTest.getClassName())),
                            atomicRecord, FileUpdateType.UPDATE, useNotifications);
                }
            }
            JSONObject properties = new JSONObject();
            properties.put("status", candidate.getMetadata().getCandidateStatus());
            properties.put("recordedBy", candidate.getMetadata().getRecordedBy());
            properties.put("hostMachineName", candidate.getMetadata().getHostMachineName());
            properties.put("timestamp", candidate.getMetadata().getTimestamp());
            UsageInsightTracker.getInstance().RecordEvent("Candidate_Added", properties);
            insidiousService.updateCoverageReport();
        } catch (Exception e) {
            logger.info("Exception adding candidate : " + e);
        }
    }

    private List<File> getFilesInUnloggedFolder() {
        ArrayList<File> returnFileList = new ArrayList<>();

        File rootDir = new File(projectBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME);
        File[] files = rootDir.listFiles();
        if (files != null) {
            Collections.addAll(returnFileList, files);
        }

        ModuleManager instance = ModuleManager.getInstance(insidiousService.getProject());
        if (instance == null) {
            return returnFileList;
        }

        Module[] modulesList = instance.getModules();
        if (modulesList == null) {
            return returnFileList;
        }

        Map<String, Boolean> checkedPaths = new HashMap<>();
        for (Module module : modulesList) {
            VirtualFile moduleRootPath = ProjectUtil.guessModuleDir(module);
            if (moduleRootPath == null) {
                continue;
            }
            String modulePath = buildTestResourcesPathFromModulePath(moduleRootPath);
            if (checkedPaths.containsKey(modulePath)) {
                continue;
            }
            checkedPaths.put(modulePath, true);
            File moduleUnloggedResourcesDir = new File(modulePath);
            if (moduleUnloggedResourcesDir.exists()) {
                File[] moduleResourceFiles = moduleUnloggedResourcesDir.listFiles();
                if (moduleResourceFiles != null) {
                    Collections.addAll(returnFileList, moduleResourceFiles);
                }
            }
        }


        return returnFileList;
    }


    private String getFilenameForClass(String classname) {
        Project project = insidiousService.getProject();
        String destinationFileName = separator + classname + ".json";
        PsiClass psiClassResult;
        try {
            psiClassResult = JavaPsiFacade.getInstance(project)
                    .findClass(classname, GlobalSearchScope.projectScope(project));
        } catch (Exception e) {
            // failed to find modules and a more specific save path
            logger.warn("Failed to specific module for class: " + classname, e);
            return projectBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME + destinationFileName;
        }

        if (psiClassResult == null) {
            // failed to find class file :o ooo o
            return projectBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME + destinationFileName;
        }

        final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
        Module module = index.getModuleForFile(psiClassResult.getContainingFile().getVirtualFile());
        assert module != null;
        VirtualFile moduleDirectoryFile = ProjectUtil.guessModuleDir(module);

        if (moduleDirectoryFile == null) {
            return projectBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME + destinationFileName;
        }

        return buildTestResourcesPathFromModulePath(moduleDirectoryFile) + destinationFileName;

    }

    private String buildTestResourcesPathFromModulePath(VirtualFile virtualFile) {
        String moduleSrcDirPath = virtualFile.getCanonicalPath();
        String moduleBasePath = moduleSrcDirPath;
        if (moduleSrcDirPath == null) {
            return projectBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME;
        } else if (moduleSrcDirPath.endsWith("src/main")) {
            moduleBasePath = moduleSrcDirPath.substring(0, moduleSrcDirPath.indexOf("/src/main"));
            return moduleBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME;
        } else if (moduleSrcDirPath.endsWith("/main")) {
            moduleBasePath = moduleSrcDirPath.substring(0, moduleSrcDirPath.indexOf("/main"));
            return moduleBasePath + separator + "test" + separator + "resources" + separator + UNLOGGED_RESOURCE_FOLDER_NAME;
        } else {
            return moduleBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME;
        }
    }

    public Map<String, List<StoredCandidate>> filterCandidates(Map<String, List<StoredCandidate>> candidates) {
        if (candidates == null || candidates.size() == 0) {
            return null;
        }

        for (String methodHashKey : candidates.keySet()) {
            Map<Integer, StoredCandidate> storedCandidateMap = new TreeMap<>();
            List<StoredCandidate> storedCandidateList = candidates.get(methodHashKey);

            for (StoredCandidate candidate : storedCandidateList) {
                if (candidate.getName() != null) {
                    int hash = candidate.getMethodArguments().hashCode() + (candidate.getMethod().getName().hashCode());
                    if (storedCandidateMap.containsKey(hash)) {
                        if (storedCandidateMap.get(hash).getMetadata().getTimestamp() < candidate.getMetadata()
                                .getTimestamp()) {
                            storedCandidateMap.put(hash, candidate);
                        }
                    } else {
                        storedCandidateMap.put(hash, candidate);
                    }
                }
            }
            candidates.put(methodHashKey, new ArrayList<>(storedCandidateMap.values()));
        }
        return candidates;
    }

    private void addNewRecord(String methodHashKey, String className, StoredCandidate candidate) {
        AtomicRecord record = new AtomicRecord();
        record.setClassname(className);
        List<StoredCandidate> candidates = new ArrayList<>();
        candidates.add(candidate);
        Map<String, List<StoredCandidate>> listMap = new TreeMap<>();
        listMap.put(methodHashKey, candidates);
        record.setStoredCandidateMap(listMap);

        this.storedRecords.put(className, record);
        writeToFile(new File(getFilenameForClass(className)), record, FileUpdateType.ADD, useNotifications);
    }

    private void writeToFile(File file, AtomicRecord atomicRecord, FileUpdateType type, boolean notify) {
        logger.info("[ATRS] writing to file : " + file.getName());
        try (FileOutputStream resourceFile = new FileOutputStream(file)) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(atomicRecord);
            resourceFile.write(json.getBytes(StandardCharsets.UTF_8));
            logger.info("[ATRS] file write successful => " + file.getAbsolutePath());
            if (notify) {
                InsidiousNotification.notifyMessage(getMessageForOperationType(type, file.getPath(), true),
                        NotificationType.INFORMATION);
            }
            resourceFile.close();
        } catch (Exception e) {
            logger.info("[ATRS] Failed to write to file : " + e);
            InsidiousNotification.notifyMessage(getMessageForOperationType(type, file.getPath(), false),
                    NotificationType.ERROR);
        }
    }

    public String getMessageForOperationType(FileUpdateType type, String path, boolean positive) {
        switch (type) {
            case ADD:
                if (positive) {
                    return "Added test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to add test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case UPDATE:
                if (positive) {
                    return "Updated test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to update test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            default:
                if (positive) {
                    return "Deleted test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to delete test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
        }
    }

    //called once in start, when map is null, updated after that.
    public Map<String, AtomicRecord> updateMap() {
        Map<String, AtomicRecord> recordsMap = new TreeMap<>();
        List<File> files = getFilesInUnloggedFolder();
        if (files.size() == 0) {
            return new TreeMap<>();
        }
        for (File file : files) {
            AtomicRecord record = getAtomicRecordFromFile(file);
            if (record != null) {
                String classname = record.getClassname();
                recordsMap.put(classname, record);
            }
        }
        return recordsMap;
    }

    public Boolean hasStoredCandidateForMethod(MethodUnderTest methodUnderTest) {
        try {
            AtomicRecord record = this.storedRecords.get(methodUnderTest.getClassName());
            if (record == null) {
                return false;
            }
            String methodKey = methodUnderTest.getMethodHashKey();
            if (record.getStoredCandidateMap().get(methodKey) != null &&
                    record.getStoredCandidateMap().get(methodKey).size() > 0) {
                return true;

            }
            return false;
        } catch (Exception e) {
            logger.info("Exception checking if method has stored candidates." + e);
            return false;
        }
    }

    public AtomicRecord getAtomicRecordFromFile(File file) {
        try {
            if (file == null || !file.exists()) {
                return null;
            }
            InputStream inputStream = new FileInputStream(file);
            return objectMapper.readValue(inputStream, AtomicRecord.class);
        } catch (IOException e) {
            logger.info("Exception getting atomic record from file: " + e);
            return null;
        }
    }

    @NotNull
    public List<StoredCandidate> getStoredCandidatesForMethod(MethodUnderTest methodUnderTest) {
        if (methodUnderTest.getClassName() == null) {
            return new ArrayList<>();
        }
        AtomicRecord record = this.storedRecords.get(methodUnderTest.getClassName());
        if (record == null) {
            return List.of();
        }
        return record.getStoredCandidateMap().getOrDefault(methodUnderTest.getMethodHashKey(), List.of());
    }

    public void deleteStoredCandidate(String className, String methodKey, String candidateId) {
        if (className == null || methodKey == null) {
            return;
        }
        AtomicRecord record = this.storedRecords.get(className);
        if (record == null || record.getStoredCandidateMap().get(methodKey).size() == 0) {
            return;
        }
        StoredCandidate candidateToRemove = null;
        List<StoredCandidate> existingStoredCandidates = record.getStoredCandidateMap().get(methodKey);

        for (StoredCandidate candidate : existingStoredCandidates) {
            if (candidate.getCandidateId() != null &&
                    candidate.getCandidateId().equals(candidateId)) {
                candidateToRemove = candidate;
                break;
            }
        }
        if (candidateToRemove != null) {
            existingStoredCandidates.remove(candidateToRemove);
        }
        writeToFile(new File(getFilenameForClass(className)), record, FileUpdateType.DELETE, useNotifications);
        UsageInsightTracker.getInstance().RecordEvent("Candidate_Deleted", null);
        insidiousService.triggerGutterIconReload();
        insidiousService.updateCoverageReport();
    }

    public String getSaveLocation() {
        return projectBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME + separator;
    }

    public void setCandidateStateForCandidate(@NotNull String candidateID, String classname,
                                              String methodKey, StoredCandidateMetadata.CandidateStatus state) {
        AtomicRecord record = this.storedRecords.get(classname);
        if (record == null || record.getStoredCandidateMap().get(methodKey).size() == 0) {
            return;
        }
        List<StoredCandidate> list = record.getStoredCandidateMap().get(methodKey);
        for (StoredCandidate candidate : list) {
            if (candidateID.equals(candidate.getCandidateId())) {
                candidate.getMetadata().setCandidateStatus(state);
            }
        }
    }

    //call to sync at session close
    public void writeAll() {
        try {
            if (storedRecords.size() == 0) {
                return;
            }
            for (String classname : storedRecords.keySet()) {
                AtomicRecord recordForClass = storedRecords.get(classname);
                writeToFile(new File(getFilenameForClass(classname)), recordForClass, FileUpdateType.UPDATE, false);
            }
        } catch (Exception e) {
            logger.info("Failed to sync on exit " + e);
        }
    }

    public void checkPreRequisites() {
        if (storedRecords == null) {
            storedRecords = updateMap();
            insidiousService.updateCoverageReport();
        }
    }

    public Map<String, AtomicRecord> getStoredRecords() {
        return this.storedRecords;
    }

    public boolean isUseNotifications() {
        return useNotifications;
    }

    public void setUseNotifications(boolean useNotifications) {
        this.useNotifications = useNotifications;
    }

    public enum FileUpdateType {ADD, UPDATE, DELETE}
}
