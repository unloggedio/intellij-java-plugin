package com.insidious.plugin.datafile;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String,List<AtomicRecord>> storedRecords=null;
    public AtomicRecordService(InsidiousService service)
    {
        this.insidiousService = service;
    }
    private static final Logger logger = LoggerUtil.getInstance(AtomicRecordService.class);

    public GutterState computeGutterState(String classname, String method, int hashcode) {

        if(this.storedRecords==null)
        {
            updateMap();
        }
        ensureUnloggedFolder();
//        System.out.println("#STATE FETCH : "+storedRecords.toString());
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
                    //System.out.println("HASH CHANGE :\ncurrent - "+hashcode+" \nnew - "+candidate.getMethodHash());
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
        System.out.println("#ADD RECORD");
        if(basePath==null) {
            setProjectBasePath();
        }
        ensureUnloggedFolder();
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
                System.out.println("[ATRS] creating a new record");
                addNewRecord(methodName+"#"+signature,classname,candidate);
            }
            else
            {
                //read as array of AtomicRecords
                System.out.println("[ATRS] creating a new record");
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
                            if(storedCandidate.getCandidateId()==null)
                            {
//                                System.out.println("[NO ID CASE] CANDIDATE : "+storedCandidate.toString());
                            }
                            if (storedCandidate.getMethodArguments().equals(candidate.getMethodArguments()))
                            {
                                foundCandidate = true;
                                System.out.println("[ATRS] Replace existing "+storedCandidate.getMethodArguments().toString());
                                System.out.println("[ATRS] Replace new "+candidate.getMethodArguments().toString());
                                //replace
                                InsidiousNotification.notifyMessage("Replacing existing record", NotificationType.INFORMATION);
                                logger.info("[ATRS] Replacing existing record");
//                                System.out.println("[ATRS] Replacing existing record");
                                if(storedCandidate.getCandidateId()==null)
                                {
                                    System.out.println("No ID Case");
                                }
                                storedCandidate.setCandidateId(candidate.getCandidateId());
                                storedCandidate.setName(candidate.getName());
                                storedCandidate.setDescription(candidate.getDescription());
                                storedCandidate.setAssertionType(candidate.getAssertionType());
                                storedCandidate.setReturnValue(candidate.getReturnValue());
                                storedCandidate.setReturnDataEventSerializedValue(new String(candidate.getReturnDataEventSerializedValue()));
                                storedCandidate.setReturnDataEventValue(candidate.getReturnDataEventValue());
                                storedCandidate.setEntryProbeIndex(candidate.getEntryProbeIndex());
                                storedCandidate.setBooleanType(candidate.isBooleanType());
                                storedCandidate.setProbSerializedValue(candidate.getProbSerializedValue());
                                storedCandidate.setException(candidate.isException());
                                storedCandidate.setReturnValueClassname(candidate.getReturnValueClassname());
                            }
                        }
                    }
                }
                if(!foundMethod)
                {
                    System.out.println("[ATRS] Adding new record");
                    logger.info("[ATRS] Adding new record");
                    if(existingRecord!=null) {
                        existingRecord.setStoredCandidateList(filterCandidates(existingRecord.getStoredCandidateList()));
                    }
                    addNewRecord(methodName+"#"+signature,classname,candidate);
                }
                else if(foundMethod && !foundCandidate)
                {
                    //add to stored candidates
                    System.out.println("[ATRS] Adding Candidate");
                    logger.info("[ATRS] Adding Candidate");
                    if(existingRecord!=null) {
                        existingRecord.getStoredCandidateList().add(candidate);
                        existingRecord.setStoredCandidateList(filterCandidates(existingRecord.getStoredCandidateList()));
                    }
                    writeToFile(new File(basePath+"/"+unloggedFolderName+"/"+classname+".json")
                            ,obj,FileUpdateType.UPDATE,true);
                }
                else
                {
                    System.out.println("[ATRS] Replacing existing record (found)");
                    logger.info("[ATRS] Replacing existing record (found)");
                    existingRecord.setStoredCandidateList(filterCandidates(existingRecord.getStoredCandidateList()));
                    writeToFile(new File(basePath+"/"+unloggedFolderName+"/"+classname+".json")
                            ,obj,FileUpdateType.UPDATE,true);
                }
            }
            UsageInsightTracker.getInstance().RecordEvent("Candidate_Added",null);
            insidiousService.triggerAtomicTestsWindowRefresh();
        }
        catch (Exception e)
        {
            System.out.println("Exception  : "+e);
            e.printStackTrace();
        }
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
        writeToFile(new File(basePath+"/"+unloggedFolderName+"/"+classname+".json")
                ,records,FileUpdateType.ADD,true);
    }

    private void writeToFile(File file, List<AtomicRecord> atomicRecords, FileUpdateType type,
                             boolean notify)
    {
        logger.info("[ATRS] writing to file : "+file.getName());
        String json = gson.toJson(atomicRecords);
        try (FileOutputStream resourceFile = new FileOutputStream(file)) {
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
            e.printStackTrace();
            InsidiousNotification.notifyMessage("Exception in : "+type.toString(), NotificationType.ERROR);
        }
    }

    //called once in start, when map is null, updated after that.
    public void updateMap()
    {
        System.out.println("#UPDATE MAP");
        if(basePath==null)
        {
            basePath = insidiousService.getProject().getBasePath();
        }
        ensureUnloggedFolder();
        this.storedRecords = new TreeMap<>();
        File rootDir = new File(basePath+"/"+unloggedFolderName);
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
        System.out.println("#POST UPDATE : "+storedRecords.toString());
    }

    public Boolean hasStoredCandidateForMethod(String classname, String method)
    {
        if(this.storedRecords==null)
        {
            updateMap();
        }
        ensureUnloggedFolder();
//        System.out.println("#ICON FETCH : "+storedRecords.toString());
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

    private void setProjectBasePath()
    {
        basePath = insidiousService.getProject().getBasePath();
    }

    public void ensureUnloggedFolder()
    {
        File unloggedFolder = new File(basePath+"/"+unloggedFolderName);
        if(!(unloggedFolder.exists() && unloggedFolder.isDirectory()))
        {
            logger.info(".unlogged directory created");
            unloggedFolder.mkdirs();
        }
    }

    public List<AtomicRecord> getJsonArrayForClass(String classname) {
        try {
            InputStream inputStream = new FileInputStream(basePath + "/" + unloggedFolderName + "/" + classname + ".json");
            String stringSource = toString(inputStream);
            return gson.fromJson(stringSource, new TypeToken<List<AtomicRecord>>() {
            }.getType());
        }
        catch (IOException e)
        {
            logger.info("Exception getting atomic records : "+e);
            e.printStackTrace();
            ensureUnloggedFolder();
            return null;
        }
    }

    private List<AtomicRecord> getJsonArrayFromFile(File file)
    {
        try {
            InputStream inputStream = new FileInputStream(file);
            String stringSource = toString(inputStream);
            return gson.fromJson(stringSource, new TypeToken<List<AtomicRecord>>() {
            }.getType());
        }
        catch (IOException e)
        {
            logger.info("Exception getting atomic records from file: "+e);
            e.printStackTrace();
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

        if(this.storedRecords==null)
        {
            updateMap();
        }
//        System.out.println("#FETCH STORED CANDIDATES : "+storedRecords.toString());
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
//        System.out.println("In delete for : "+candidateId);
        if(classname == null || method == null)
        {
            return;
        }
        //remove from records.
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
//        System.out.println("[DEL] list : "+list!=null ? list.toString() : null);
//        System.out.println("[DEL] CANDIDATE : "+candidateToRemove!=null ? candidateToRemove.toString() : null);
        if(list!=null && candidateToRemove!=null) {
            list.remove(candidateToRemove);
        }
        writeToFile(new File(basePath+"/"+unloggedFolderName+"/"+classname+".json")
                ,records,FileUpdateType.DELETE,true);
        UsageInsightTracker.getInstance().RecordEvent("Candidate_Deleted",null);
        insidiousService.triggerGutterIconReload();
        insidiousService.triggerAtomicTestsWindowRefresh();
    }

    public String getSaveLocation() {
        if(basePath==null) {
            basePath = insidiousService.getProject().getBasePath();
        }
        return basePath+"/"+unloggedFolderName+"/";
    }

    public void setCandidateStateForCandidate(String candidateID, String classname,
                                              String method, StoredCandidateMetadata.CandidateStatus state)
    {
        if(this.storedRecords==null)
        {
            updateMap();
        }
        ensureUnloggedFolder();
//        System.out.println("#SAVE STATE : "+storedRecords.toString());
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
        if(this.storedRecords==null)
        {
            updateMap();
        }
        ensureUnloggedFolder();
        if(storedRecords.size()==0)
        {
            return;
        }
        for(String classname : storedRecords.keySet())
        {
            List<AtomicRecord> recordsForClass = storedRecords.get(classname);
            writeToFile(new File(basePath+"/"+unloggedFolderName+"/"+classname+".json")
                    ,recordsForClass,FileUpdateType.UPDATE,false);
        }
    }
}
