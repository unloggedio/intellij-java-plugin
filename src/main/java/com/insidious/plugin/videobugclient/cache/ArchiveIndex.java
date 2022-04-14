package com.insidious.plugin.videobugclient.cache;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.insidious.plugin.videobugclient.pojo.local.ObjectInfoDocument;
import com.insidious.plugin.videobugclient.pojo.local.StringInfoDocument;
import com.insidious.plugin.videobugclient.pojo.local.TypeInfoDocument;

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

    public IndexedCollection<StringInfoDocument> Strings() {
        return stringInfoIndex;
    }
    public IndexedCollection<ObjectInfoDocument> Objects() {
        return objectInfoIndex;
    }
    public IndexedCollection<TypeInfoDocument> Types() {
        return typeInfoIndex;
    }
}
