package com.insidious.plugin.datafile;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.atomic.AtomicRecord;
import com.insidious.plugin.pojo.atomic.StoredCandidate;
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
    private Gson gson = new Gson();
    private Map<String,List<AtomicRecord>> storedRecords;
    public AtomicRecordService(InsidiousService service)
    {
        this.insidiousService = service;
    }
    private static final Logger logger = LoggerUtil.getInstance(AtomicRecordService.class);

    public void addStoredCandidate(String classname,String methodName, String signature, StoredCandidate candidate)
    {
        if(basePath==null) {
            setProjectBasePath();
        }
        ensureUnloggedFolder();
        try {
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
                boolean found=false;
                for(AtomicRecord record : obj)
                {
                    if(record.getMethod().equals(methodName+"#"+signature))
                    {
                        found = true;
                        List<StoredCandidate> candidates = record.getStoredCandidateList();
                        for(StoredCandidate storedCandidate : candidates)
                        {
                            if (storedCandidate.getMethodArguments().equals(candidate.getMethodArguments()))
                            {
                                //replace
                                InsidiousNotification.notifyMessage("Replacing existing record", NotificationType.INFORMATION);
                                logger.info("[ATRS] Replacing existing record");
                                if(storedCandidate.getCandidateId()==null)
                                {
                                    storedCandidate.setCandidateId(candidate.getCandidateId());
                                }
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
                if(!found)
                {
                    addNewRecord(methodName+"#"+signature,classname,candidate);
                }
                else
                {
                    writeToFile(new File(basePath+"/"+unloggedFolderName+"/"+classname+".json")
                            ,obj);
                }
            }
            UsageInsightTracker.getInstance().RecordEvent("Candidate_Added",null);
        }
        catch (Exception e)
        {
            System.out.println("Exception  : "+e);
            e.printStackTrace();
        }
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
        writeToFile(new File(basePath+"/"+unloggedFolderName+"/"+classname+".json")
                ,records);
    }

    private void writeToFile(File file, List<AtomicRecord> atomicRecords)
    {
        logger.info("[ATRS] writing to file : "+file.getName());
        String json = gson.toJson(atomicRecords);
        try (FileOutputStream resourceFile = new FileOutputStream(file)) {
            resourceFile.write(json.getBytes(StandardCharsets.UTF_8));
            logger.info("[ATRS] file write successful");
//            InsidiousNotification.notifyMessage("Added record", NotificationType.INFORMATION);
            resourceFile.close();
        }
        catch (Exception e)
        {
            logger.info("[ATRS] Failed to write to file : "+e);
            e.printStackTrace();
        }
    }

    //called once in start, when map is null, updated after that.
    public void updateMap()
    {
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
    }

    public Boolean hasStoredCandidateForMethod(String classname, String method)
    {
        if(this.storedRecords==null)
        {
            updateMap();
        }
        ensureUnloggedFolder();

        List<AtomicRecord> records = this.storedRecords.get(classname);
        if(records==null)
        {
            return false;
        }
        for(AtomicRecord record : records)
        {
            if(record.getMethod().equals(method))
            {
                if(record.getStoredCandidateList().size()>0)
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
        writeToFile(new File(basePath+"/"+unloggedFolderName+"/"+classname+".json"),records);
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
}
