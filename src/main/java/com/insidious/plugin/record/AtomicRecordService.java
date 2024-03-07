package com.insidious.plugin.record;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.mocking.DeclaredMock;
import com.insidious.plugin.mocking.ParameterMatcher;
import com.insidious.plugin.pojo.atomic.AtomicRecord;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.util.LoggerUtil;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static java.io.File.separator;

public class AtomicRecordService {
    public static final String TEST_CONTENT_PATH = "src" + separator + "test" + separator;
    public static final String TEST_RESOURCES_PATH = TEST_CONTENT_PATH + "resources" + separator;
    private static final Logger logger = LoggerUtil.getInstance(AtomicRecordService.class);
    private final String UNLOGGED_RESOURCE_FOLDER_NAME = "unlogged";
    private final ObjectMapper objectMapper = ObjectMapperInstance.getInstance();
    private final String projectBasePath;
    InsidiousService insidiousService;
    private Map<String, AtomicRecord> classAtomicRecordMap = null;
    private boolean useNotifications = false;

    public AtomicRecordService(Project project) {
        insidiousService = project.getService(InsidiousService.class);
        projectBasePath = insidiousService.getProject().getBasePath();
        init();
    }

    public void init() {
        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        if (dumbService != null) {
            dumbService.runWhenSmart(this::checkPreRequisites);
        }
    }

    public void saveCandidate(MethodUnderTest methodUnderTest, StoredCandidate candidate) {
        try {
            AtomicRecord existingRecord = null;
            AtomicRecord atomicRecord = classAtomicRecordMap.get(methodUnderTest.getClassName());
            String methodKey = methodUnderTest.getMethodHashKey();
            if (atomicRecord == null) {
                //create record
                logger.info("[ATRS] No Atomic record found for this class, " +
                        "creating a new atomic record");
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
                        if (
                                (candidate.getCandidateId() != null && candidate.getCandidateId()
                                        .equals(storedCandidate.getCandidateId()))
                                        || storedCandidate.getMethodArguments().equals(candidate.getMethodArguments())
                        ) {
                            foundCandidate = true;
                            //replace
                            if (useNotifications) {
                                InsidiousNotification.notifyMessage("Replacing existing record",
                                        NotificationType.INFORMATION);
                            }
                            logger.info("[ATRS] Replacing existing record");
//                            candidate.setMockIds(candidate.getMockIds());
                            storedCandidate.copyFrom(candidate);
                            break;
                        }
                    }
                }
                if (!foundMethod) {
                    logger.info("[ATRS] Adding new map entry");
                    List<StoredCandidate> candidates = new ArrayList<>();
                    candidates.add(candidate);
                    atomicRecord.getStoredCandidateMap().put(methodKey, candidates);
                    writeToFile(new File(getFilenameForClass(methodUnderTest.getClassName())),
                            atomicRecord, FileUpdateType.UPDATE_CANDIDATE, useNotifications);
                } else if (!foundCandidate) {
                    //add to stored candidates
                    logger.info("[ATRS] Adding Candidate");
                    existingRecord.getStoredCandidateMap().get(methodKey).add(candidate);
                    existingRecord.setStoredCandidateMap(filterCandidates(existingRecord.getStoredCandidateMap()));
                    writeToFile(new File(getFilenameForClass(methodUnderTest.getClassName())),
                            atomicRecord, FileUpdateType.UPDATE_CANDIDATE, useNotifications);
                } else {
                    logger.info("[ATRS] Replacing existing record (found)");
                    existingRecord.setStoredCandidateMap(filterCandidates(existingRecord.getStoredCandidateMap()));
                    writeToFile(new File(getFilenameForClass(methodUnderTest.getClassName())),
                            atomicRecord, FileUpdateType.UPDATE_CANDIDATE, useNotifications);
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

        Module[] modulesList = instance.getModules();

        Map<String, Boolean> checkedPaths = new HashMap<>();
        for (Module module : modulesList) {
            VirtualFile moduleRootPath = ProjectUtil.guessModuleDir(module);
            if (moduleRootPath == null) {
                continue;
            }
            String moduleTestResourcesPath = buildModuleBasePath(moduleRootPath)
                    + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME;
            if (checkedPaths.containsKey(moduleTestResourcesPath)) {
                continue;
            }
            checkedPaths.put(moduleTestResourcesPath, true);
            File moduleUnloggedResourcesDir = new File(moduleTestResourcesPath);
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
        return getFilenameForClass(classname, guessModuleForClassName(classname));
    }


    private String getFilenameForClass(String classname, Module module) {

        String destinationFileName = separator + classname + ".json";
        String defaultPath = projectBasePath + separator + TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME + destinationFileName;
        if (module == null) {
            return defaultPath;
        }

        VirtualFile moduleDirectoryFile = ProjectUtil.guessModuleDir(module);
        if (moduleDirectoryFile == null) {
            return defaultPath;
        }
        String testContentPathFromModule = buildModuleBasePath(moduleDirectoryFile);

        // sometimes returned virtual file is a location in build
        // artifact this removes it from file location
        int indexBuild = testContentPathFromModule.indexOf("/build/generated/sources");
        if (indexBuild != -1) {
            testContentPathFromModule = testContentPathFromModule.substring(0, indexBuild);
        }

        String testResourcesPathFromModulePath = testContentPathFromModule + separator +
                TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME;

        return testResourcesPathFromModulePath + destinationFileName;
    }

    private Module getModuleForClass(PsiClass psiClassResult) {
        final ProjectFileIndex index = ProjectRootManager.getInstance(psiClassResult.getProject()).getFileIndex();
        return ApplicationManager.getApplication().runReadAction(
                (Computable<Module>) () -> index.getModuleForFile(psiClassResult.getContainingFile().getVirtualFile()));
    }

    public Module guessModuleForClassName(String className) {
        if (DumbService.getInstance(insidiousService.getProject()).isDumb()) {
            InsidiousNotification.notifyMessage("Please try after ide indexing is complete", NotificationType.WARNING);
            return null;
        }

        Project project = insidiousService.getProject();

        PsiClass psiClass;
        try {
            psiClass = ApplicationManager.getApplication()
                    .executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(
                            (Computable<PsiClass>) () -> {
                                if (DumbService.getInstance(insidiousService.getProject()).isDumb()) {
                                    InsidiousNotification.notifyMessage("Please try after ide indexing is complete",
                                            NotificationType.WARNING);
                                    return null;
                                }

                                return JavaPsiFacade.getInstance(project)
                                        .findClass(className, GlobalSearchScope.allScope(project));
                            })).get();
            if (psiClass == null) {
                logger.warn("Class not found [" + className + "] for saving atomic records");
            } else {
                return ApplicationManager.getApplication().executeOnPooledThread(() ->
                        ApplicationManager.getApplication().runReadAction(
                                (Computable<Module>) () -> guessModuleForPsiClass(psiClass))).get();
            }
        } catch (Exception e) {
            // failed to find modules and a more specific save path
            logger.warn("Failed to specific module for class: " + className, e);
        }
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length > 0) {
            return modules[0];
        } else {
            return null;
        }
    }

    public String guessModuleBasePath(PsiClass psiClass) {
        try {
            Module module = getModuleForClass(psiClass);
            if (module == null) {
                return projectBasePath;
            }
            VirtualFile moduleDirectoryFile = ProjectUtil.guessModuleDir(module);
            if (moduleDirectoryFile == null) {
                return projectBasePath;
            }
            return buildModuleBasePath(moduleDirectoryFile);
        } catch (Exception e) {
            return projectBasePath;
        }
    }

    public Module guessModuleForPsiClass(PsiClass psiClass) {
        try {
            final ProjectFileIndex index = ProjectRootManager.getInstance(psiClass.getProject()).getFileIndex();
            return index.getModuleForFile(psiClass.getContainingFile().getVirtualFile());
        } catch (Exception e) {
            Module[] modules = ModuleManager.getInstance(psiClass.getProject()).getModules();
            if (modules.length > 0) {
                return modules[0];
            }
            return null;
        }
    }

    private String buildModuleBasePath(VirtualFile virtualFile) {
        String moduleSrcDirPath = virtualFile.getCanonicalPath();
        String moduleBasePath = moduleSrcDirPath;
        if (moduleSrcDirPath == null) {
            return projectBasePath + separator;
        } else if (moduleSrcDirPath.endsWith("src/main")) {
            moduleBasePath = moduleSrcDirPath.substring(0, moduleSrcDirPath.indexOf("/src/main"));
            return moduleBasePath + separator;
        } else {
            return moduleBasePath + separator;
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
                        if (storedCandidateMap.get(hash).getMetadata().getTimestamp() <
                                candidate.getMetadata().getTimestamp()) {
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
        AtomicRecord record = new AtomicRecord(className);
        ArrayList<StoredCandidate> value = new ArrayList<>();
        value.add(candidate);
        record.getStoredCandidateMap().put(methodHashKey, value);
        classAtomicRecordMap.put(className, record);
        writeToFile(new File(getFilenameForClass(className)), record, FileUpdateType.ADD_CANDIDATE, useNotifications);
    }

    private void writeToFile(File file, AtomicRecord atomicRecord, FileUpdateType type, boolean notify) {
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        logger.info("[ATRS] writing to file : " + file.getName());
        try (FileOutputStream resourceFile = new FileOutputStream(file)) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(atomicRecord);
            resourceFile.write(json.getBytes(StandardCharsets.UTF_8));
            resourceFile.flush();
            logger.info("[ATRS] file write successful => " + file.getAbsolutePath());
            if (notify) {
                InsidiousNotification.notifyMessage(getMessageForOperationType(type, file.getPath(), true),
                        NotificationType.INFORMATION);
            }
            VirtualFile virtialFile = VirtualFileManager.getInstance()
                    .findFileByNioPath(file.getParentFile().toPath());
            if (virtialFile != null) {
                virtialFile.refresh(false, false);
            }
        } catch (Exception e) {
            logger.info("[ATRS] Failed to write to file : " + e);
            logger.error(e.getMessage(), e);
            InsidiousNotification.notifyMessage(getMessageForOperationType(type, file.getPath(), false),
                    NotificationType.ERROR);
        }
    }

    public String getMessageForOperationType(FileUpdateType type, String path, boolean positive) {
        switch (type) {
            case ADD_CANDIDATE:
                if (positive) {
                    return "Added test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to add test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case UPDATE_CANDIDATE:
                if (positive) {
                    return "Updated test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to update test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case DELETE_CANDIDATE:
                if (positive) {
                    return "Deleted test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to delete test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case ADD_MOCK:
                if (positive) {
                    return "Saved mock" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to add test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case UPDATE_MOCK:
                if (positive) {
                    return "Updated mock" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to update test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case DELETE_MOCK:
                if (positive) {
                    return "Deleted mock" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to delete test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            default:
                throw new RuntimeException("Unknown operation: " + type);
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
        for (Map.Entry<String, AtomicRecord> stringAtomicRecordEntry : recordsMap.entrySet()) {
            AtomicRecord record = stringAtomicRecordEntry.getValue();
            for (Map.Entry<String, List<DeclaredMock>> stringListEntry : record.getDeclaredMockMap().entrySet()) {
                String methodHashKey = stringListEntry.getKey();

                List<DeclaredMock> mockList = stringListEntry.getValue();
                for (DeclaredMock declaredMock : mockList) {
                    if (declaredMock.getMethodHashKey() == null) {
                        declaredMock.setMethodHashKey(methodHashKey);
                    }
                }

            }
            for (Map.Entry<String, List<StoredCandidate>> stringListEntry : record.getStoredCandidateMap().entrySet()) {
                String methodHashKey = stringListEntry.getKey();
                String[] keyParts = methodHashKey.split("#");

                List<StoredCandidate> mockList = stringListEntry.getValue();
                for (StoredCandidate storedCandidate : mockList) {
                    storedCandidate.setMethod(new MethodUnderTest(keyParts[1],
                            keyParts.length == 3 ? keyParts[2] : record.getClassname()
                            , 0, keyParts[0]));
                }

            }

        }

        return recordsMap;
    }

    public Boolean hasStoredCandidateForMethod(MethodUnderTest methodUnderTest) {
        try {
            AtomicRecord record = classAtomicRecordMap.get(methodUnderTest.getClassName());
            if (record == null) {
                return false;
            }
            String methodKey = methodUnderTest.getMethodHashKey();
            Map<String, List<StoredCandidate>> storedCandidateMap = record.getStoredCandidateMap();
            return storedCandidateMap.containsKey(methodKey) &&
                    storedCandidateMap.get(methodKey).size() > 0;
        } catch (Exception e) {
            logger.warn("Exception checking if method has stored candidates", e);
            return false;
        }
    }

    private AtomicRecord getAtomicRecordFromFile(File file) {
        try {
            if (file == null || !file.exists()) {
                return null;
            }
            InputStream inputStream = new FileInputStream(file);
            return objectMapper.readValue(inputStream, AtomicRecord.class);
        } catch (MismatchedInputException e) {
            logger.warn("file is empty: " + file.getAbsolutePath());
            return null;
        } catch (IOException e) {
            logger.warn("Exception getting atomic record from file", e);
            return null;
        }
    }


    public List<StoredCandidate> getStoredCandidatesForMethod(MethodUnderTest methodUnderTest) {
        if (methodUnderTest.getClassName() == null) {
            return new ArrayList<>();
        }
        AtomicRecord record = classAtomicRecordMap.get(methodUnderTest.getClassName());
        if (record == null) {
            return List.of();
        }
        return record.getStoredCandidateMap().getOrDefault(methodUnderTest.getMethodHashKey(), List.of());
    }

    public void deleteStoredCandidate(MethodUnderTest methodUnderTest, String candidateId) {
        if (methodUnderTest.getClassName() == null || methodUnderTest.getMethodHashKey() == null) {
            return;
        }
        AtomicRecord record = classAtomicRecordMap.get(methodUnderTest.getClassName());
        Map<String, List<StoredCandidate>> storedCandidateMap = record.getStoredCandidateMap();
        if (!storedCandidateMap.containsKey(methodUnderTest.getMethodHashKey())) {
            // this shouldnt have happened
            return;
        }
        List<StoredCandidate> storedCandidates = storedCandidateMap.get(methodUnderTest.getMethodHashKey());
        if (storedCandidates.size() == 0) {
            return;
        }
        StoredCandidate candidateToRemove = null;
        List<StoredCandidate> existingStoredCandidates = storedCandidateMap
                .get(methodUnderTest.getMethodHashKey());

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
        writeToFile(new File(getFilenameForClass(methodUnderTest.getClassName())), record,
                FileUpdateType.DELETE_CANDIDATE,
                useNotifications);
        UsageInsightTracker.getInstance().RecordEvent("Candidate_Deleted", null);
        insidiousService.triggerGutterIconReload();
        insidiousService.updateCoverageReport();
    }

    public void setCandidateStateForCandidate(String candidateID, String classname,
                                              String methodKey, StoredCandidateMetadata.CandidateStatus state) {
        AtomicRecord record = classAtomicRecordMap.get(classname);
        if (record == null
                || !record.getStoredCandidateMap().containsKey(methodKey)
                || record.getStoredCandidateMap().get(methodKey).size() == 0) {
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
//    public void writeAll() {
//        try {
//            if (classAtomicRecordMap.size() == 0) {
//                return;
//            }
//            for (String classname : classAtomicRecordMap.keySet()) {
//                AtomicRecord recordForClass = classAtomicRecordMap.get(classname);
//                try {
//                    writeToFile(new File(getFilenameForClass(classname)), recordForClass,
//                            FileUpdateType.UPDATE_CANDIDATE,
//                            false);
//                } catch (Exception e) {
//                    // class not found... class was renamed
//                    // this record is now orphan
//                }
//            }
//        } catch (Exception e) {
//            logger.info("Failed to sync on exit " + e);
//        }
//    }

    public void checkPreRequisites() {
        classAtomicRecordMap = updateMap();
        insidiousService.updateCoverageReport();
    }

    public boolean isUseNotifications() {
        return useNotifications;
    }

    public void setUseNotifications(boolean useNotifications) {
        this.useNotifications = useNotifications;
    }


    /**
     * Returns mocks of the class
     *
     * @param methodUnderTest target method
     * @return mocks which can be used in the class
     */
    public List<DeclaredMock> getDeclaredMocksOf(MethodUnderTest methodUnderTest) {

        if (classAtomicRecordMap == null || methodUnderTest == null ||
                !classAtomicRecordMap.containsKey(methodUnderTest.getClassName())) {
            return List.of();
        }
        Map<String, List<DeclaredMock>> declaredMockMap = classAtomicRecordMap
                .get(methodUnderTest.getClassName())
                .getDeclaredMockMap();
        String methodHashKey = methodUnderTest.getMethodHashKey();
        if (!declaredMockMap.containsKey(methodHashKey)) {
            return List.of();
        }
        return declaredMockMap.get(methodHashKey)
                .stream().map(DeclaredMock::new)
                .collect(Collectors.toList());


    }

    /**
     * returns mocks which can be used in the class
     *
     * @param methodUnderTest source method
     * @return mocks which can be used in the class
     */
    public List<DeclaredMock> getDeclaredMocksFor(MethodUnderTest methodUnderTest) {
        return classAtomicRecordMap.values()
                .stream().map(e -> e.getDeclaredMockMap().values())
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .filter(e -> e.getSourceClassName() != null && e.getSourceClassName()
                        .equals(methodUnderTest.getClassName()))
                .collect(Collectors.toList());

    }

    public List<DeclaredMock> getAllDeclaredMocks() {
        return classAtomicRecordMap.values()
                .stream().map(e -> e.getDeclaredMockMap().values())
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

    }

    public List<StoredCandidate> getAllTestCandidates() {
        return classAtomicRecordMap.values()
                .stream().map(e -> e.getStoredCandidateMap().values())
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

    }

    public String saveMockDefinition(DeclaredMock declaredMock) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("className", declaredMock.getFieldTypeName());
        jsonObject.put("methodName", declaredMock.getMethodName());
        jsonObject.put("signature", declaredMock.getMethodHashKey());

        AtomicRecord record;
        String className = declaredMock.getFieldTypeName();
        if (!classAtomicRecordMap.containsKey(className)) {
            record = new AtomicRecord(className);
            classAtomicRecordMap.put(className, record);
        } else {
            record = classAtomicRecordMap.get(className);
        }

        List<DeclaredMock> existingMocks;
        Map<String, List<DeclaredMock>> declaredMockMap = record.getDeclaredMockMap();
        String methodHashKey = declaredMock.getMethodHashKey();

        if (!declaredMockMap.containsKey(methodHashKey)) {
            existingMocks = new ArrayList<>();
            declaredMockMap.put(methodHashKey, existingMocks);
        } else {
            existingMocks = declaredMockMap.get(methodHashKey);

        }
        boolean updated = false;
        for (DeclaredMock existingMock : existingMocks) {
            if (existingMock.getId().equals(declaredMock.getId())) {
                updated = true;
                existingMocks.remove(existingMock);
                break;
            }
            if (isSameMatcher(existingMock.getWhenParameter(), declaredMock.getWhenParameter())) {
                updated = true;
                existingMocks.remove(existingMock);
                declaredMock.setId(existingMock.getId());
                break;
            }
        }

        existingMocks.add(declaredMock);
        String eventname = "SAVED_NEW_MOCK";
        if (updated) {
            eventname = "UPDATED_EXISTING_MOCK";
        }
        UsageInsightTracker.getInstance().RecordEvent(eventname, jsonObject);
        File file = new File(
                getFilenameForClass(className, guessModuleForClassName(declaredMock.getSourceClassName())));
        writeToFile(file, record,
                updated ? FileUpdateType.UPDATE_MOCK : FileUpdateType.ADD_MOCK, true);
        return declaredMock.getId();
    }

    private boolean isSameMatcher(List<ParameterMatcher> whenParameter, List<ParameterMatcher> whenParameter1) {
        if (whenParameter.size() != whenParameter1.size()) {
            return false;
        }

        for (int i = 0; i < whenParameter.size(); i++) {
            ParameterMatcher left = whenParameter.get(i);
            ParameterMatcher right = whenParameter1.get(i);
            if (!left.equals(right)) {
                return false;
            }

        }
        return true;

    }

    public void deleteMockDefinition(DeclaredMock declaredMock) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("className", declaredMock.getFieldTypeName());
        jsonObject.put("methodName", declaredMock.getMethodName());
        jsonObject.put("signature", declaredMock.getMethodHashKey());

        AtomicRecord record;
        String className = declaredMock.getFieldTypeName();
        if (!classAtomicRecordMap.containsKey(className)) {
            return;
        } else {
            record = classAtomicRecordMap.get(className);
        }

        List<DeclaredMock> existingMocks;
        Map<String, List<DeclaredMock>> declaredMockMap = record.getDeclaredMockMap();
        String methodHashKey = declaredMock.getMethodHashKey();

        if (!declaredMockMap.containsKey(methodHashKey)) {
            return;
        } else {
            existingMocks = declaredMockMap.get(methodHashKey);

        }
        boolean found = false;
        for (DeclaredMock existingMock : existingMocks) {
            if (existingMock.getId().equals(declaredMock.getId())) {
                found = true;
                existingMocks.remove(existingMock);
                break;
            }
        }
        if (!found) {
            UsageInsightTracker.getInstance().RecordEvent("DELETE_MOCK_FAILED", jsonObject);
            return;
        }
        UsageInsightTracker.getInstance().RecordEvent("DELETED_MOCK", jsonObject);
        writeToFile(new File(getFilenameForClass(className)), record, FileUpdateType.DELETE_MOCK, true);
    }

    public Map<String, List<StoredCandidate>> getCandidatesByClass(String fullyClassifiedClassName) {
        if (!classAtomicRecordMap.containsKey(fullyClassifiedClassName)) {
            return null;
        }
        return classAtomicRecordMap.get(fullyClassifiedClassName).getStoredCandidateMap();
    }

    public StoredCandidate getStoredCandidateFor(TestCandidateMetadata testCandidate) {
        return getStoredCandidate(new StoredCandidate(testCandidate));
    }

    public StoredCandidate getStoredCandidate(StoredCandidate potentialCandidate) {
        MethodUnderTest methodUnderTest = potentialCandidate.getMethod();
        AtomicRecord ars = classAtomicRecordMap.get(potentialCandidate.getMethod().getClassName());
        if (ars == null) {
            return null;
        }

        List<StoredCandidate> methodStoredCandidates = ars.getStoredCandidateMap()
                .get(methodUnderTest.getMethodHashKey());
        if (methodStoredCandidates == null || methodStoredCandidates.size() == 0) {
            return null;
        }

        for (StoredCandidate methodStoredCandidate : methodStoredCandidates) {
            List<String> arguments = potentialCandidate.getMethodArguments();
            if (methodStoredCandidate.getMethodArguments().size() == arguments.size()) {
                boolean match = true;
                List<String> methodArguments = methodStoredCandidate.getMethodArguments();
                for (int i = 0; i < methodArguments.size(); i++) {
                    String methodArgument = methodArguments.get(i);
                    String expectedValue = potentialCandidate.getMethodArguments().get(i);
                    if (!methodArgument.equals(expectedValue)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return methodStoredCandidate;
                }

            }
        }


        return null;
    }

    public enum FileUpdateType {
        ADD_MOCK,
        UPDATE_MOCK,
        DELETE_MOCK,
        ADD_CANDIDATE,
        UPDATE_CANDIDATE,
        DELETE_CANDIDATE
    }
}
