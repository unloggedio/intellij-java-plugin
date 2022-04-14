package com.insidious.plugin.videobugclient.pojo;

import com.insidious.plugin.index.InsidiousIndexParser;

public class ArchiveFilesIndex {
    public ArchiveFilesIndex(InsidiousIndexParser archiveIndex) {
        byte[] arhiveProbeLookup = archiveIndex.unionProbeIdIndex();
        byte[] arhiveValueLookup  = archiveIndex.unionValueIdIndex();
    }
}
