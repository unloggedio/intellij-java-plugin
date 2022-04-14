package com.insidious.plugin.pojo;

import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import com.insidious.plugin.videobugclient.cache.ArchiveIndex;
import com.insidious.plugin.videobugclient.pojo.local.StringInfoDocument;
import com.insidious.plugin.videobugclient.pojo.local.TypeInfoDocument;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SessionCache {
    private final Map<String, ArchiveIndex> indexMap = new HashMap<>();
    String sessionId;

    public SessionCache(String sessionId) {
        this.sessionId = sessionId;
    }

    public void put(String name, ArchiveIndex index) {
        this.indexMap.put(name, index);
    }

    public boolean containsFile(String name) {
        return indexMap.containsKey(name);
    }

    public List<StringInfoDocument> retrieveByString(Query<StringInfoDocument> query) {

        List<StringInfoDocument> result = new LinkedList<>();
        for (Map.Entry<String, ArchiveIndex> entry : indexMap.entrySet()) {
            ResultSet<StringInfoDocument> results = entry.getValue().Strings().retrieve(query);
            result.addAll(results.stream().collect(Collectors.toList()));
        }

        return result;
    }

    public List<TypeInfoDocument> retriveByType(Query<TypeInfoDocument> query) {
        List<TypeInfoDocument> result = new LinkedList<>();
        for (Map.Entry<String, ArchiveIndex> entry : indexMap.entrySet()) {
            ResultSet<TypeInfoDocument> results = entry.getValue().Types().retrieve(query);
            result.addAll(results.stream().collect(Collectors.toList()));
        }
        return result;
    }

    public String getSessionId() {
        return sessionId;
    }
}
