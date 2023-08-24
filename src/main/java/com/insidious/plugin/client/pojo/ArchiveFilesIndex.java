package com.insidious.plugin.client.pojo;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.insidious.common.UploadFile;
import com.insidious.common.parser.KaitaiInsidiousIndexParser;
import orestes.bloomfilter.BloomFilter;
import orestes.bloomfilter.json.BloomFilterConverter;

import java.util.LinkedList;
import java.util.List;

public class ArchiveFilesIndex {
    public static final Gson GSON = new Gson();
    private final BloomFilter<Integer> probeFilter;
    private final BloomFilter<Long> valueFilter;
    private final KaitaiInsidiousIndexParser archiveIndex;

    public ArchiveFilesIndex(KaitaiInsidiousIndexParser archiveIndex) {
        this.archiveIndex = archiveIndex;
        byte[] archiveProbeLookup = archiveIndex.unionProbeIdIndex();
        byte[] archiveValueLookup = archiveIndex.unionValueIdIndex();

        this.probeFilter = BloomFilterConverter.fromJson(GSON.fromJson(new String(archiveProbeLookup), JsonElement.class), Integer.class); //{"size":240,"hashes":4,"HashMethod":"MD5","bits":"AAAAEAAAAACAgAAAAAAAAAAAAAAQ"}
        this.valueFilter = BloomFilterConverter.fromJson(GSON.fromJson(new String(archiveValueLookup), JsonElement.class), Long.class); //{"size":240,"hashes":4,"HashMethod":"MD5","bits":"AAAAEAAAAACAgAAAAAAAAAAAAAAQ"}
    }

    public BloomFilter<Integer> getProbeFilter() {
        return probeFilter;
    }

    public BloomFilter<Long> getValueFilter() {
        return valueFilter;
    }

    public boolean hasProbeId(int probeId) {
        return probeFilter.contains(probeId);
    }

    public boolean hasValueId(long valueId) {
        return valueFilter.contains(valueId);
    }

    public List<UploadFile> querySessionFilesByValueId(long valueId) {
        List<UploadFile> files = new LinkedList<>();
        for (KaitaiInsidiousIndexParser.IndexedFile indexFile : archiveIndex.indexFiles()) {
            BloomFilter<Long> fileValueFilter = BloomFilterConverter.fromJson(
                    GSON.fromJson(new String(indexFile.valueIdIndex()), JsonElement.class), Long.class); //{"size":240,"hashes":4,"HashMethod":"MD5","bits":"AAAAEAAAAACAgAAAAAAAAAAAAAAQ"}
            if (fileValueFilter.contains(valueId)) {
                UploadFile uploadFile = new UploadFile(
                        new String(indexFile.filePath().value()),
                        indexFile.threadId(), null, null);
                uploadFile.setValueIds(new Long[]{valueId});
                files.add(uploadFile);
            }

        }
        return files;
    }

    public List<UploadFile> querySessionFilesByProbeId(int probeId) {
        List<UploadFile> files = new LinkedList<>();
        for (KaitaiInsidiousIndexParser.IndexedFile indexFile : archiveIndex.indexFiles()) {
            BloomFilter<Integer> probeIdFilter = BloomFilterConverter.fromJson(
                    GSON.fromJson(new String(indexFile.probeIdIndex()), JsonElement.class), Integer.class); //{"size":240,"hashes":4,"HashMethod":"MD5","bits":"AAAAEAAAAACAgAAAAAAAAAAAAAAQ"}
            if (probeIdFilter.contains(probeId)) {
                UploadFile uploadFile = new UploadFile(
                        new String(indexFile.filePath().value()),
                        indexFile.threadId(), null, null);
                uploadFile.setProbeIds(new Integer[]{probeId});
                files.add(uploadFile);
            }

        }
        return files;
    }

}
