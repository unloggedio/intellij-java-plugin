package com.insidious.plugin.datafile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.CandidateSearchQuery;
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
import com.intellij.openapi.project.DumbService;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AtomicRecordService {
    private static final Logger logger = LoggerUtil.getInstance(AtomicRecordService.class);
    private final String unloggedFolderName = "unlogged";
    private final ObjectMapper objectMapper = new ObjectMapper();
    InsidiousService insidiousService;
    private String basePath;
    private Map<String, AtomicRecord> storedRecords = null;
    private boolean useNotifications = true;

    public AtomicRecordService(InsidiousService service) {
        this.insidiousService = service;
    }

    public void init() {
        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        if (dumbService != null) {
            dumbService.runWhenSmart(this::checkPreRequisits);
        }
    }

    public GutterState computeGutterState(MethodUnderTest method) {

        try {
            String methodKey = method.getMethodKey();
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
            AtomicRecord obj = this.storedRecords.get(methodUnderTest.getClassName());
            String methodKey = methodUnderTest.getMethodKey();
            if (obj == null) {
                //create record
                logger.info("[ATRS] creating a new record");
                addNewRecord(methodKey, methodUnderTest.getClassName(), candidate);
            } else {
                //read as array of AtomicRecords
                logger.info("[ATRS] creating a new record");
                boolean foundMethod = false;
                boolean foundCandidate = false;

                if (obj.getStoredCandidateMap().get(methodKey) != null) {
                    foundMethod = true;
                    existingRecord = obj;
                    List<StoredCandidate> candidates = obj.getStoredCandidateMap().get(methodKey);
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
                    obj.getStoredCandidateMap().put(methodKey, candidates);

                } else if (!foundCandidate) {
                    //add to stored candidates
                    logger.info("[ATRS] Adding Candidate");
                    existingRecord.getStoredCandidateMap().get(methodKey).add(candidate);
                    existingRecord.setStoredCandidateMap(filterCandidates(existingRecord.getStoredCandidateMap()));
                    writeToFile(new File(getFilenameForClass(methodUnderTest.getClassName()))
                            , obj, FileUpdateType.UPDATE, useNotifications);
                } else {
                    logger.info("[ATRS] Replacing existing record (found)");
                    existingRecord.setStoredCandidateMap(filterCandidates(existingRecord.getStoredCandidateMap()));
                    writeToFile(new File(getFilenameForClass(methodUnderTest.getClassName()))
                            , obj, FileUpdateType.UPDATE, useNotifications);
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

    private String getFilenameForClass(String classname) {
        return basePath + File.separator + getResourcePath() +
                unloggedFolderName + File.separator + classname + ".json";
    }

    public String getResourcePath() {
        return "src" + File.separator + "test" + File.separator + "resources" + File.separator;
    }

    public Map<String, List<StoredCandidate>> filterCandidates(Map<String, List<StoredCandidate>> candidates) {
        if (candidates == null || candidates.size() == 0) {
            return null;
        }

        for (String key : candidates.keySet()) {
            Map<Integer, StoredCandidate> storedCandidateMap = new TreeMap<>();
            List<StoredCandidate> storedCandidateList = candidates.get(key);

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
            candidates.put(key, new ArrayList<>(storedCandidateMap.values()));
        }
        return candidates;
    }

    private void addNewRecord(String method, String classname, StoredCandidate candidate) {
        AtomicRecord record = new AtomicRecord();
        record.setClassname(classname);
        List<StoredCandidate> candidates = new ArrayList<>();
        candidates.add(candidate);
        Map<String, List<StoredCandidate>> listMap = new TreeMap<>();
        listMap.put(method, candidates);
        record.setStoredCandidateMap(listMap);

        this.storedRecords.put(classname, record);
        writeToFile(new File(getFilenameForClass(classname)), record, FileUpdateType.ADD, useNotifications);
    }

    private void writeToFile(File file, AtomicRecord atomicRecord, FileUpdateType type,
                             boolean notify) {
        logger.info("[ATRS] writing to file : " + file.getName());
        try (FileOutputStream resourceFile = new FileOutputStream(file)) {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(atomicRecord);
            resourceFile.write(json.getBytes(StandardCharsets.UTF_8));
            logger.info("[ATRS] file write successful");
            if (notify) {
                InsidiousNotification.notifyMessage(getMessageForOperationType(type, true),
                        NotificationType.INFORMATION);
            }
            resourceFile.close();
        } catch (Exception e) {
            logger.info("[ATRS] Failed to write to file : " + e);
            InsidiousNotification.notifyMessage(getMessageForOperationType(type, false),
                    NotificationType.ERROR);
        }
    }

    public String getMessageForOperationType(FileUpdateType type, boolean positive) {
        switch (type) {
            case ADD:
                if (positive) {
                    return "Added test candidate";
                } else {
                    return "Failed to add test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case UPDATE:
                if (positive) {
                    return "Updated test candidate";
                } else {
                    return "Failed to update test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            default:
                if (positive) {
                    return "Deleted test candidate";
                } else {
                    return "Failed to delete test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
        }
    }

    //called once in start, when map is null, updated after that.
    public Map<String, AtomicRecord> updateMap() {
        Map<String, AtomicRecord> recordsMap = new TreeMap<>();
        File[] files = getFilesInUnloggedFolder();
        if (files == null || files.length == 0) {
            return new TreeMap<>();
        }
        for (int i = 0; i < files.length; i++) {
            AtomicRecord record = getAtomicRecordFromFile(files[i]);
            if (record != null) {
                String classname = record.getClassname();
                recordsMap.put(classname, record);
            }
        }
        return recordsMap;
    }

    private File[] getFilesInUnloggedFolder() {
        File rootDir = new File(basePath + File.separator
                + getResourcePath() + unloggedFolderName);
        return rootDir.listFiles();
    }

    public Boolean hasStoredCandidateForMethod(CandidateSearchQuery candidateSearchQuery) {
        try {
            AtomicRecord record = this.storedRecords.get(candidateSearchQuery.getClassName());
            if (record == null) {
                return false;
            }
            String methodKey = candidateSearchQuery.getMethodName() + "#" + candidateSearchQuery.getMethodSignature();
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

    public void ensureUnloggedFolder() {
        File unloggedFolder = new File(basePath + File.separator
                + getResourcePath() + unloggedFolderName);
        if (!(unloggedFolder.exists() && unloggedFolder.isDirectory())) {
            logger.info("unlogged directory created");
            System.out.println("unlogged directory created");
            unloggedFolder.mkdirs();
        }
    }

    public AtomicRecord getAtomicRecordFromFile(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            return objectMapper.readValue(inputStream, AtomicRecord.class);
        } catch (IOException e) {
            logger.info("Exception getting atomic record from file: " + e);
            ensureUnloggedFolder();
            return null;
        }
    }

    @NotNull
    public List<StoredCandidate> getStoredCandidatesForMethod(CandidateSearchQuery candidateSearchQuery) {
        AtomicRecord record = this.storedRecords.get(candidateSearchQuery.getClassName());
        if (record == null) {
            return List.of();
        }
        return record.getStoredCandidateMap().getOrDefault(
                candidateSearchQuery.getMethodName() + "#" + candidateSearchQuery.getMethodSignature(), List.of());
    }

    public void deleteStoredCandidate(String classname, String methodKey, String candidateId) {
        if (classname == null || methodKey == null) {
            return;
        }
        AtomicRecord record = this.storedRecords.get(classname);
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
        writeToFile(new File(getFilenameForClass(classname)), record, FileUpdateType.DELETE, useNotifications);
        UsageInsightTracker.getInstance().RecordEvent("Candidate_Deleted", null);
        insidiousService.triggerGutterIconReload();
        insidiousService.updateCoverageReport();
    }

    public String getSaveLocation() {
        return basePath + File.separator +
                getResourcePath() + unloggedFolderName + File.separator;
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
                writeToFile(new File(getFilenameForClass(classname))
                        , recordForClass, FileUpdateType.UPDATE, false);
            }
        } catch (Exception e) {
            logger.info("Failed to sync on exit " + e);
        }
    }

    public void checkPreRequisits() {
        basePath = insidiousService.getProject().getBasePath();
        ensureUnloggedFolder();
        if (this.storedRecords == null) {
            this.storedRecords = updateMap();
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
