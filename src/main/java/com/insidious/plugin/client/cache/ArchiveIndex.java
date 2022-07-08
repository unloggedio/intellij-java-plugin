package com.insidious.plugin.client.cache;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import com.insidious.common.cqengine.ObjectInfoDocument;
import com.insidious.common.cqengine.StringInfoDocument;
import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.weaver.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.in;

public class ArchiveIndex {
    private final ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex;
    private final ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex;
    private final ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex;

    public ArchiveIndex(
            ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex,
            ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex,
            ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex,
            Map<String, ClassInfo> classInfoMap) {
        this.typeInfoIndex = typeInfoIndex;
        this.stringInfoIndex = stringInfoIndex;
        this.objectInfoIndex = objectInfoIndex;
    }


    @NotNull
    public List<Long> getStringIdsFromStringValues(String value) {
        Query<StringInfoDocument> query = equal(StringInfoDocument.STRING_VALUE, value);
        ResultSet<StringInfoDocument> searchResult = stringInfoIndex.retrieve(query);
        List<Long> collect = searchResult.stream().map(StringInfoDocument::getStringId).collect(Collectors.toList());
        searchResult.close();
        return collect;
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

    public Map<String, ObjectInfo> getObjectsByObjectId(Collection<Long> objectIds) {

        Query<ObjectInfoDocument> query = in(ObjectInfoDocument.OBJECT_ID, objectIds);
        ResultSet<ObjectInfoDocument> retrieve = objectInfoIndex.retrieve(query);
        Stream<ObjectInfoDocument> stream = retrieve.stream();
        Map<String, ObjectInfo> collect = stream
                .map(e -> new ObjectInfo(e.getObjectId(), e.getTypeId(), 0))
                .collect(Collectors.toMap(e -> String.valueOf(e.getObjectId()), r -> r));
        retrieve.close();
        return collect;
    }

    public Map<String, ObjectInfo> getObjectsByTypeIds(Set<Integer> typeIds) {
        return objectInfoIndex.stream().filter(e -> typeIds.contains(e.getTypeId()))
                .map(e -> new ObjectInfo(e.getObjectId(), e.getTypeId(), 0))
                .collect(Collectors.toMap(e -> String.valueOf(e.getObjectId()), r -> r));
    }

    public Map<String, StringInfo> getStringsById(Set<Long> valueIds) {

        Query<StringInfoDocument> query = in(StringInfoDocument.STRING_ID, valueIds);
        ResultSet<StringInfoDocument> retrieve = stringInfoIndex.retrieve(query);
        Stream<StringInfoDocument> stream = retrieve.stream();
        Map<String, StringInfo> collect = stream
                .map(e -> new StringInfo(e.getStringId(), e.getString()))
                .collect(Collectors.toMap(e -> String.valueOf(e.getStringId()), r -> r));
        retrieve.close();
        return collect;
    }

    public Map<String, TypeInfo> getTypesById(Set<Integer> valueIds) {

        Query<TypeInfoDocument> query = in(TypeInfoDocument.TYPE_ID, valueIds);
        ResultSet<TypeInfoDocument> retrieve = typeInfoIndex.retrieve(query);
        Map<String, TypeInfo> collect = retrieve.stream()
                .map(e -> new TypeInfo("", e.getTypeId(), e.getTypeName(),
                        "", "", "", ""))
                .collect(Collectors.toMap(e -> String.valueOf(e.getTypeId()), r -> r));
        retrieve.close();
        return collect;

    }
}
