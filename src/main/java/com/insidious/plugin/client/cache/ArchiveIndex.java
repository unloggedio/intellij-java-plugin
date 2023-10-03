package com.insidious.plugin.client.cache;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import com.insidious.common.cqengine.ObjectInfoDocument;
import com.insidious.common.cqengine.StringInfoDocument;
import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.ObjectInfo;
import com.insidious.common.weaver.StringInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.ProbeInfoDocument;
import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.googlecode.cqengine.query.QueryFactory.*;

public class ArchiveIndex {
    private final ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex;
    private final ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex;
    private ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex;
    private ConcurrentIndexedCollection<ProbeInfoDocument> probeInfoIndex;
    private static final Logger logger = LoggerUtil.getInstance(ArchiveIndex.class);

    public ArchiveIndex(ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex, ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex, ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex, Map<Long, ClassInfo> classInfoMap) {
        this.typeInfoIndex = typeInfoIndex;
        this.stringInfoIndex = stringInfoIndex;
        this.objectInfoIndex = objectInfoIndex;
    }

    public ConcurrentIndexedCollection<TypeInfoDocument> getTypeInfoIndex() {
        return typeInfoIndex;
    }

    
    public List<Long> getStringIdsFromStringValues(String value) {
        Query<StringInfoDocument> query = equal(StringInfoDocument.STRING_VALUE, value);
        ResultSet<StringInfoDocument> searchResult = stringInfoIndex.retrieve(query);
        List<Long> collect = searchResult.stream()
                .map(StringInfoDocument::getStringId)
                .collect(Collectors.toList());
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

    public Map<Long, ObjectInfo> getObjectsByObjectId(Collection<Long> objectIds) {

        Query<ObjectInfoDocument> query = in(ObjectInfoDocument.OBJECT_ID, objectIds);
        ResultSet<ObjectInfoDocument> retrieve = objectInfoIndex.retrieve(query);
        Stream<ObjectInfoDocument> stream = retrieve.stream();

        Map<Long, ObjectInfo> collect = new HashMap<>();
        stream.map(e -> new ObjectInfo(e.getObjectId(), e.getTypeId(), 0))
                .forEach(e -> {
                    collect.put(e.getObjectId(), e);
                });

        retrieve.close();
        return collect;
    }

    public ObjectInfo getObjectByObjectId(Long objectIds) {

        Query<ObjectInfoDocument> query = equal(ObjectInfoDocument.OBJECT_ID, objectIds);
        ResultSet<ObjectInfoDocument> retrieve = objectInfoIndex.retrieve(query);

        List<ObjectInfo> collect = new LinkedList<>();
        retrieve.stream()
                .map(e -> new ObjectInfo(e.getObjectId(), e.getTypeId(), 0))
                .forEach(collect::add);

        retrieve.close();
        if (collect.size() == 0) {
            return null;
        }
        return collect.get(0);
    }


    public Map<Long, ObjectInfo> getObjectsByObjectIdWithLongKeys(Collection<Long> objectIds) {

        Query<ObjectInfoDocument> query = in(ObjectInfoDocument.OBJECT_ID, objectIds);
        ResultSet<ObjectInfoDocument> retrieve = objectInfoIndex.retrieve(query);
        Stream<ObjectInfoDocument> stream = retrieve.stream();

        Map<Long, ObjectInfo> collect = new HashMap<>();
        stream.map(e -> new ObjectInfo(e.getObjectId(), e.getTypeId(), 0))
                .forEach(e -> {
                    collect.put(e.getObjectId(), e);
                });

        retrieve.close();
        return collect;
    }

    public Map<String, ObjectInfo> getObjectsByTypeIds(Set<Integer> typeIds) {
        return objectInfoIndex.stream()
                .filter(e -> typeIds.contains(e.getTypeId()))
                .map(e -> new ObjectInfo(e.getObjectId(), e.getTypeId(), 0))
                .collect(Collectors.toMap(e -> String.valueOf(e.getObjectId()), r -> r));
    }

    public Map<String, StringInfo> getStringsById(Set<Long> valueIds) {

        Query<StringInfoDocument> query = in(StringInfoDocument.STRING_ID, valueIds);
        ResultSet<StringInfoDocument> retrieve = stringInfoIndex.retrieve(query);
        Stream<StringInfoDocument> stream = retrieve.stream();
        Map<String, StringInfo> collect = stream.map(e -> new StringInfo(e.getStringId(), e.getString()))
                .collect(Collectors.toMap(e -> String.valueOf(e.getStringId()), r -> r));
        retrieve.close();
        return collect;
    }

    public Map<Long, StringInfo> getStringsByIdWithLongKeys(Set<Long> valueIds) {

        Query<StringInfoDocument> query = in(StringInfoDocument.STRING_ID, valueIds);
        ResultSet<StringInfoDocument> retrieve = stringInfoIndex.retrieve(query);
        Stream<StringInfoDocument> stream = retrieve.stream();
        Map<Long, StringInfo> collect = stream.map(e -> new StringInfo(e.getStringId(), e.getString()))
                .collect(Collectors.toMap(StringInfo::getStringId, r -> r));
        retrieve.close();
        return collect;
    }

    public TypeInfo getTypesByName(String typeName) {
        Set<String> typesToSearch = new HashSet<>();
        if (typeName.startsWith("L")) {
            typeName = typeName.substring(1);
        }
        if (typeName.contains("/")) {
            typeName = typeName.replace('/', '.');
        }
        if (typeName.endsWith(";")) {
            typeName = typeName.substring(0, typeName.length() - 1);
        }
        typesToSearch.add(typeName);


        Query<TypeInfoDocument> query = in(TypeInfoDocument.TYPE_NAME, typesToSearch);
        ResultSet<TypeInfoDocument> retrieve = typeInfoIndex.retrieve(query);
        final Map<String, TypeInfo> collect = new HashMap<>();
        HashSet<Integer> superClasses = new HashSet<>();
        Optional<TypeInfo> returnValue = retrieve.stream()
                .map(e -> {
                    byte[] typeBytes = e.getTypeBytes();
                    TypeInfo typeInfo = TypeInfo.fromBytes(typeBytes);
                    if (typeInfo.getSuperClass() != -1) {
                        superClasses.add(typeInfo.getSuperClass());
                    }
                    if (typeInfo.getComponentType() != -1) {
                        superClasses.add(typeInfo.getComponentType());
                    }
                    if (typeInfo.getInterfaces() != null && typeInfo.getInterfaces().length > 0) {
                        for (int anInterface : typeInfo.getInterfaces()) {
                            superClasses.add(anInterface);
                        }

                    }

                    return typeInfo;
                })
                .findFirst();
        retrieve.close();
        if (returnValue.isEmpty()) {
            return null;
        }
        return returnValue.get();
    }

    public TypeInfo getObjectType(long objectId) {
        ObjectInfo object = getObjectByObjectId(objectId);
        if (object == null) {
            return null;
        }
        long aLong = object.getTypeId();
        return getTypeById((int) aLong);
    }

    public TypeInfo getTypeById(Integer valueIds) {

        Query<TypeInfoDocument> query = equal(TypeInfoDocument.TYPE_ID, valueIds);
        ResultSet<TypeInfoDocument> retrieve = typeInfoIndex.retrieve(query);

        Optional<TypeInfo> result = retrieve.stream()
                .map(e -> {
                    byte[] typeBytes = e.getTypeBytes();
                    return TypeInfo.fromBytes(typeBytes);
                })
                .findFirst();
        retrieve.close();

        return result.orElse(null);

    }


    public Map<String, TypeInfo> getTypesById(Set<Integer> valueIds) {

        Query<TypeInfoDocument> query = in(TypeInfoDocument.TYPE_ID, valueIds);
        ResultSet<TypeInfoDocument> retrieve = typeInfoIndex.retrieve(query);
        final Map<String, TypeInfo> collect = new HashMap<>();
        HashSet<Integer> superClasses = new HashSet<>();
        retrieve.stream()
                .map(e -> {
                    byte[] typeBytes = e.getTypeBytes();
                    TypeInfo typeInfo = TypeInfo.fromBytes(typeBytes);
                    if (typeInfo.getSuperClass() != -1) {
                        superClasses.add(typeInfo.getSuperClass());
                    }
                    if (typeInfo.getComponentType() != -1) {
                        superClasses.add(typeInfo.getComponentType());
                    }
                    if (typeInfo.getInterfaces() != null && typeInfo.getInterfaces().length > 0) {
                        for (int anInterface : typeInfo.getInterfaces()) {
                            superClasses.add(anInterface);
                        }

                    }

                    return typeInfo;
                })
                .forEach(e -> {
                    collect.put(String.valueOf(e.getTypeId()), e);
                });
        retrieve.close();

        if (superClasses.size() > 0) {
            superClasses.removeAll(valueIds);
            Map<String, TypeInfo> superClassesTypes = getTypesById(superClasses);
            collect.putAll(superClassesTypes);
        }

        return collect;

    }


    public Map<Integer, TypeInfo> getTypesByIdWithLongKeys(Set<Integer> valueIds) {

        Query<TypeInfoDocument> query = in(TypeInfoDocument.TYPE_ID, valueIds);
        ResultSet<TypeInfoDocument> retrieve = typeInfoIndex.retrieve(query);
        final Map<Integer, TypeInfo> collect = new HashMap<>();
        HashSet<Integer> superClasses = new HashSet<>();
        retrieve.stream()
                .map(e -> {
                    byte[] typeBytes = e.getTypeBytes();
                    TypeInfo typeInfo = TypeInfo.fromBytes(typeBytes);
                    if (typeInfo.getSuperClass() != -1) {
                        superClasses.add(typeInfo.getSuperClass());
                    }
                    if (typeInfo.getComponentType() != -1) {
                        superClasses.add(typeInfo.getComponentType());
                    }
                    if (typeInfo.getInterfaces() != null && typeInfo.getInterfaces().length > 0) {
                        for (int anInterface : typeInfo.getInterfaces()) {
                            superClasses.add(anInterface);
                        }

                    }

                    return typeInfo;
                })
                .forEach(e -> {
                    collect.put(e.getTypeId(), e);
                });
        retrieve.close();

        if (superClasses.size() > 0) {
            superClasses.removeAll(valueIds);
            Map<Integer, TypeInfo> superClassesTypes = getTypesByIdWithLongKeys(superClasses);
            collect.putAll(superClassesTypes);
        }

        return collect;

    }

    public ConcurrentIndexedCollection<ObjectInfoDocument> getObjectIndex() {
        return objectInfoIndex;
    }

    public void setObjectInfoIndex(ConcurrentIndexedCollection<ObjectInfoDocument> objectIndexFile) {
        this.objectInfoIndex = objectIndexFile;
    }

    public void setProbeInfoIndex(ConcurrentIndexedCollection<ProbeInfoDocument> probeInfoIndex) {
        this.probeInfoIndex = probeInfoIndex;
    }

    public Set<Integer> queryTypeIdsByName(SearchQuery searchQuery) {
        String query = (String) searchQuery.getQuery();

        Query<TypeInfoDocument> typeQuery = startsWith(TypeInfoDocument.TYPE_NAME, query);
        if (query.endsWith("*")) {
            typeQuery = startsWith(TypeInfoDocument.TYPE_NAME, query.substring(0, query.length() - 1));
        }

        ResultSet<TypeInfoDocument> searchResult = Types().retrieve(typeQuery);
        Set<Integer> typeIds = searchResult.stream().map(TypeInfoDocument::getTypeId).collect(Collectors.toSet());
        searchResult.close();
        logger.info("type query [" + searchQuery + "] matched [" + typeIds.size() + "] items");

        if (typeIds.size() == 0) {
            return new HashSet<>();
        }
        return typeIds;
    }

    public void close() {
        DiskPersistence typeInfoIndexPersistence = (DiskPersistence) typeInfoIndex.getPersistence();
        typeInfoIndexPersistence.close();

    }
}
