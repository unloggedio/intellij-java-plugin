package com.insidious.plugin.datafile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.GutterState;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.AtomicRecord;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.pojo.atomic.StoredCandidateMetadata;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AtomicRecordService {
    InsidiousService insidiousService;
    private String basePath;
    private final String unloggedFolderName = ".unlogged";
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String,List<AtomicRecord>> storedRecords=null;
    public AtomicRecordService(InsidiousService service)
    {
        this.insidiousService = service;
        DumbService dumbService = DumbService.getInstance(insidiousService.getProject());
        dumbService.runWhenSmart(() -> {
            checkPreRequisits();
        });

    }
    private static final Logger logger = LoggerUtil.getInstance(AtomicRecordService.class);

    public GutterState computeGutterState(String classname, String method, int hashcode) {

        List<AtomicRecord> records = this.storedRecords.get(classname);
        if(records==null)
        {
            return null;
        }
        List<StoredCandidate> candidates = new ArrayList<>();
        for(AtomicRecord record : records)
        {
            if(record.getMethod().equals(method))
            {
                if(record.getStoredCandidateList()!=null && record.getStoredCandidateList().size()>0)
                {
                    candidates.addAll(record.getStoredCandidateList());
                }
            }
        }
        if(records.size()==0)
        {
            return null;
        }
        else
        {
            boolean hashChange = false;
            StoredCandidateMetadata.CandidateStatus status = null;
            for(StoredCandidate candidate : candidates)
            {
                if(!candidate.getMethodHash().equals(hashcode+""))
                {
                    hashChange = true;
                }
                if(status==null) {
                    status = candidate.getMetadata().getCandidateStatus();
                }
                else
                {
                    if(candidate.getMetadata().getCandidateStatus()
                            .equals(StoredCandidateMetadata.CandidateStatus.FAILING))
                    {
                        status = StoredCandidateMetadata.CandidateStatus.FAILING;
                    }
                }
            }
            if(hashChange)
            {
                return GutterState.EXECUTE;
            }
            if(status == null)
            {
                return GutterState.DATA_AVAILABLE;
            }
            if(status.equals(StoredCandidateMetadata.CandidateStatus.FAILING))
            {
                return GutterState.DIFF;
            }
            else if(status.equals(StoredCandidateMetadata.CandidateStatus.PASSING))
            {
                return GutterState.NO_DIFF;
            }
            else
            {
                return GutterState.DATA_AVAILABLE;
            }
        }
    }

    private enum FileUpdateType {ADD,UPDATE,DELETE}

    public void addStoredCandidate(String classname,String methodName, String signature, StoredCandidate candidate)
    {
        try {
            AtomicRecord existingRecord = null;
            if(this.storedRecords == null)
            {
                updateMap();
            }
            List<AtomicRecord> obj = this.storedRecords.get(classname);
            if(obj == null)
            {
                //create record
                logger.info("[ATRS] creating a new record");
                addNewRecord(methodName+"#"+signature,classname,candidate);
            }
            else
            {
                //read as array of AtomicRecords
                logger.info("[ATRS] creating a new record");
                boolean foundMethod=false;
                boolean foundCandidate=false;
                for(AtomicRecord record : obj)
                {
                    if(record.getMethod().equals(methodName+"#"+signature))
                    {
                        foundMethod = true;
                        existingRecord = record;
                        List<StoredCandidate> candidates = record.getStoredCandidateList();
                        for(StoredCandidate storedCandidate : candidates)
                        {
                            if (storedCandidate.getMethodArguments().equals(candidate.getMethodArguments()))
                            {
                                foundCandidate = true;
                                //replace
                                InsidiousNotification.notifyMessage("Replacing existing record", NotificationType.INFORMATION);
                                logger.info("[ATRS] Replacing existing record");
                                storedCandidate.copyFrom(candidate);
                            }
                        }
                    }
                }
                if(!foundMethod)
                {
                    logger.info("[ATRS] Adding new record");
                    if(existingRecord!=null) {
                        existingRecord.setStoredCandidateList(filterCandidates(existingRecord.getStoredCandidateList()));
                    }
                    addNewRecord(methodName+"#"+signature,classname,candidate);
                }
                else if(foundMethod && !foundCandidate)
                {
                    //add to stored candidates
                    logger.info("[ATRS] Adding Candidate");
                    if(existingRecord!=null) {
                        existingRecord.getStoredCandidateList().add(candidate);
                        existingRecord.setStoredCandidateList(filterCandidates(existingRecord.getStoredCandidateList()));
                    }
                    writeToFile(new File(getFilenameForClass(classname))
                            ,obj,FileUpdateType.UPDATE,true);
                }
                else
                {
                    logger.info("[ATRS] Replacing existing record (found)");
                    existingRecord.setStoredCandidateList(filterCandidates(existingRecord.getStoredCandidateList()));
                    writeToFile(new File(getFilenameForClass(classname))
                            ,obj,FileUpdateType.UPDATE,true);
                }
            }
            JSONObject properties = new JSONObject();
            properties.put("status", candidate.getMetadata().getCandidateStatus());
            properties.put("recordedBy", candidate.getMetadata().getRecordedBy());
            properties.put("hostMachineName", candidate.getMetadata().getHostMachineName());
            properties.put("timestamp", candidate.getMetadata().getTimestamp());
            UsageInsightTracker.getInstance().RecordEvent("Candidate_Added",properties);
            insidiousService.triggerAtomicTestsWindowRefresh();
        }
        catch (Exception e)
        {
            logger.info("Exception adding candidate : "+e);
        }
    }

    private String getFilenameForClass(String classname)
    {
        return basePath+File.separator+unloggedFolderName+File.separator+classname+".json";
    }

    private List<StoredCandidate> filterCandidates(List<StoredCandidate> candidates)
    {
        if(candidates==null || candidates.size()==0)
        {
            return null;
        }
        Map<Integer,StoredCandidate> storedCandidateMap = new TreeMap<>();
        for(StoredCandidate candidate : candidates)
        {
            if(candidate.getName()!=null)
            {
                int hash = candidate.getMethodArguments().hashCode()+(candidate.getMethodName().hashCode());
                if(storedCandidateMap.containsKey(hash))
                {
                    if(storedCandidateMap.get(hash).getMetadata().getTimestamp()<candidate.getMetadata().getTimestamp())
                    {
                        storedCandidateMap.put(hash,candidate);
                    }
                }
                else {
                    storedCandidateMap.put(hash, candidate);
                }
            }
        }
        return new ArrayList<>(storedCandidateMap.values());
    }

    private void addNewRecord(String method, String classname, StoredCandidate candidate)
    {
        AtomicRecord record = new AtomicRecord();
        record.setClassname(classname);
        record.setMethod(method);
        List<StoredCandidate> candidates = new ArrayList<>();
        candidates.add(candidate);
        record.setStoredCandidateList(candidates);

        List<AtomicRecord> records = this.storedRecords.get(classname);
        if(records==null)
        {
            records = new ArrayList<>();
        }
        records.add(record);
        this.storedRecords.put(classname,records);
        writeToFile(new File(getFilenameForClass(classname))
                ,records,FileUpdateType.ADD,true);
    }

    private void writeToFile(File file, List<AtomicRecord> atomicRecords, FileUpdateType type,
                             boolean notify)
    {
        logger.info("[ATRS] writing to file : "+file.getName());
        try (FileOutputStream resourceFile = new FileOutputStream(file)) {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(atomicRecords);
            resourceFile.write(json.getBytes(StandardCharsets.UTF_8));
            logger.info("[ATRS] file write successful");
            if(notify) {
                InsidiousNotification.notifyMessage("Completed Operation :  " + type.toString(), NotificationType.INFORMATION);
            }
            resourceFile.close();
        }
        catch (Exception e)
        {
            logger.info("[ATRS] Failed to write to file : "+e);
            InsidiousNotification.notifyMessage("Exception in : "+type.toString(), NotificationType.ERROR);
        }
    }

    //called once in start, when map is null, updated after that.
    public void updateMap()
    {
        System.out.println("In Update Map");
        this.storedRecords = new TreeMap<>();
        File rootDir = new File(basePath+File.separator+unloggedFolderName);
        File[] files = rootDir.listFiles();
        if (files==null || files.length==0)
        {

            return;
        }
        for(int i=0;i< files.length;i++)
        {
            List<AtomicRecord> records = getJsonArrayFromFile(files[i]);
            if(records!=null && records.size()>0)
            {
                String classname = records.get(0).getClassname();
                this.storedRecords.put(classname,records);
            }
        }
    }

    public Boolean hasStoredCandidateForMethod(String classname, String method)
    {
        List<AtomicRecord> records = this.storedRecords.get(classname);
        if(records==null)
        {
            return false;
        }
        for(AtomicRecord record : records)
        {
            if(record.getMethod().equals(method))
            {
                if(record.getStoredCandidateList()!=null && record.getStoredCandidateList().size()>0)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void ensureUnloggedFolder()
    {
        File unloggedFolder = new File(basePath+File.separator+unloggedFolderName);
        if(!(unloggedFolder.exists() && unloggedFolder.isDirectory()))
        {
            logger.info(".unlogged directory created");
            System.out.println(".unlogged directory created");
            unloggedFolder.mkdirs();
        }
    }

    private List<AtomicRecord> getJsonArrayFromFile(File file)
    {
        try {
            InputStream inputStream = new FileInputStream(file);
            String stringSource = toString(inputStream);
            return objectMapper.readValue(stringSource,
                    new TypeReference<List<AtomicRecord>>() {
            });
        }
        catch (IOException e)
        {
            logger.info("Exception getting atomic records from file: "+e);
            ensureUnloggedFolder();
            return null;
        }
    }

    private String toString(InputStream stream) throws IOException {
        char[] buffer = new char[8192];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();
    }

    public List<StoredCandidate> getStoredCandidatesForMethod(String classname, String method) {
        List<AtomicRecord> records = this.storedRecords.get(classname);
        if(records == null)
        {
            return null;
        }
        for (AtomicRecord record : records)
        {
            if(record.getMethod().equals(method))
            {
                return record.getStoredCandidateList();
            }
        }
        return null;
    }

    public void deleteStoredCandidate(String classname, String method, String candidateId) {
        if(classname == null || method == null)
        {
            return;
        }
        List<AtomicRecord> records = this.storedRecords.get(classname);
        if(records == null || records.size()==0)
        {
            return;
        }
        StoredCandidate candidateToRemove=null;
        List<StoredCandidate> list=null;
        for(AtomicRecord record : records)
        {
            if(record.getMethod().equals(method))
            {
                for(StoredCandidate candidate : record.getStoredCandidateList())
                {
                    if(candidate.getCandidateId()!=null && candidate.getCandidateId().equals(candidateId))
                    {
                        candidateToRemove = candidate;
                        list = record.getStoredCandidateList();
                    }
                }
            }
        }
        if(list!=null && candidateToRemove!=null) {
            list.remove(candidateToRemove);
        }
        writeToFile(new File(getFilenameForClass(classname))
                ,records,FileUpdateType.DELETE,true);
        UsageInsightTracker.getInstance().RecordEvent("Candidate_Deleted",null);
        insidiousService.triggerGutterIconReload();
        insidiousService.triggerAtomicTestsWindowRefresh();
    }

    public String getSaveLocation() {
        return basePath+File.separator+unloggedFolderName+File.separator;
    }

    public void setCandidateStateForCandidate(String candidateID, String classname,
                                              String method, StoredCandidateMetadata.CandidateStatus state)
    {
        List<AtomicRecord> records = this.storedRecords.get(classname);
        if(records==null)
        {
            return;
        }
        List<StoredCandidate> candidates = new ArrayList<>();
        for(AtomicRecord record : records)
        {
            if(record.getMethod().equals(method))
            {
                candidates.addAll(record.getStoredCandidateList());
            }
        }
        for(StoredCandidate candidate : candidates)
        {
            if(candidate.getCandidateId().equals(candidateID))
            {
                candidate.getMetadata().setCandidateStatus(state);
            }
        }
    }

    //call to sync at session close
    public void writeAll()
    {
        if(storedRecords.size()==0)
        {
            return;
        }
        for(String classname : storedRecords.keySet())
        {
            List<AtomicRecord> recordsForClass = storedRecords.get(classname);
            writeToFile(new File(getFilenameForClass(classname))
                    ,recordsForClass,FileUpdateType.UPDATE,false);
        }
    }

    private void checkPreRequisits()
    {
        System.out.println("In SETUP pre req");
        ensureUnloggedFolder();
        basePath = insidiousService.getProject().getBasePath();
        if(this.storedRecords==null)
        {
            updateMap();
        }
    }
}
