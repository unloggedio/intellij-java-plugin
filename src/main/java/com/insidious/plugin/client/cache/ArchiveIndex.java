package com.insidious.plugin.client.cache;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import com.insidious.common.weaver.StringInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.pojo.ObjectInfo;
import com.insidious.plugin.client.pojo.local.ObjectInfoDocument;
import com.insidious.plugin.client.pojo.local.StringInfoDocument;
import com.insidious.plugin.client.pojo.local.TypeInfoDocument;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.googlecode.cqengine.query.QueryFactory.equal;

public class ArchiveIndex {
    private final ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex;
    private final ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex;
    private final ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex;

    public ArchiveIndex(
            ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex,
            ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex,
            ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex) {
        this.typeInfoIndex = typeInfoIndex;
        this.stringInfoIndex = stringInfoIndex;
        this.objectInfoIndex = objectInfoIndex;
    }


    @NotNull
    public List<Long> getStringIdsFromStringValues(String value) {
        Query<StringInfoDocument> query = equal(StringInfoDocument.STRING_VALUE, value);
        ResultSet<StringInfoDocument> searchResult = stringInfoIndex.retrieve(query);
        return searchResult.stream().map(StringInfoDocument::getStringId).collect(Collectors.toList());
    }

    public IndexedCollection<StringInfoDocument> Strings() {
        return stringInfoIndex;
    }

    public IndexedCollection<ObjectInfoDocument> Objects() {
        return objectInfoIndex;
    }

    public IndexedCollection<TypeInfoDocument> Types() {
        return typeInfoIndex;
    }

    public Map<String, ObjectInfo> getObjectsById(Set<Long> valueIds) {
        return objectInfoIndex.stream().filter(e -> valueIds.contains(e.getObjectId()))
                .map(e -> new ObjectInfo(e.getObjectId(), e.getTypeId(), null))
                .collect(Collectors.toMap(e -> String.valueOf(e.getObjectId()), r -> r));
    }

    public Map<String, StringInfo> getStringsById(Set<Long> valueIds) {
        return stringInfoIndex.stream().filter(e -> valueIds.contains(e.getStringId()))
                .map(e -> new StringInfo(e.getStringId(), e.getString()))
                .collect(Collectors.toMap(e -> String.valueOf(e.getStringId()), r -> r));
    }

    public Map<String, TypeInfo> getTypesById(Set<Long> valueIds) {
        return typeInfoIndex.stream().filter(e -> valueIds.contains((long) e.getTypeId()))
                .map(e -> new TypeInfo("", e.getTypeId(), e.getTypeName(),
                        "", "", "", ""))
                .collect(Collectors.toMap(e -> String.valueOf(e.getTypeId()), r -> r));
    }
}
