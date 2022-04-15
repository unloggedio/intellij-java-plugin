package com.insidious.common;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.suffix.SuffixTreeIndex;
import com.googlecode.cqengine.metadata.MetadataEngine;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.googlecode.cqengine.query.QueryFactory.and;
import static com.googlecode.cqengine.query.QueryFactory.equal;



public class DebugIndex {
    private final IndexedCollection<UploadFile> indexedFileCollection;
    private final MetadataEngine<UploadFile> metadataEngine;
    private final String indexStoragePath;
    private final List<UploadFile> pendingFiles = new LinkedList<>();
    private boolean isReady = false;

    public DebugIndex(
             String storagePath
    ) {
        this.indexStoragePath = storagePath;

        String indexPath = Path.of(storagePath, "events.dat").toAbsolutePath().toString();
        indexedFileCollection = new ConcurrentIndexedCollection<>(
                DiskPersistence.onPrimaryKeyInFile(UploadFile.UPLOAD_ID, new File(indexPath))
//                CompositePersistence.of(
//                        OnHeapPersistence.onPrimaryKey(UploadFile.UPLOAD_ID),
//                        DiskPersistence.onPrimaryKeyInFile(UploadFile.UPLOAD_ID, new File(indexPath)),
//                        List.of(OffHeapPersistence.onPrimaryKey(UploadFile.UPLOAD_ID))
//                )

        );

        indexedFileCollection.addIndex(HashIndex.onAttribute(UploadFile.UPLOAD_ID));
        indexedFileCollection.addIndex(HashIndex.onAttribute(UploadFile.PROBE_ID));
        indexedFileCollection.addIndex(NavigableIndex.onAttribute(UploadFile.VALUE_ID));

        indexedFileCollection.addIndex(NavigableIndex.onAttribute(UploadFile.NANOTIME_EVENT));
        indexedFileCollection.addIndex(SuffixTreeIndex.onAttribute(UploadFile.SESSION_ID));

        Supplier<QueryOptions> openResourceHandler = () -> new QueryOptions(new HashMap<>());
        Consumer<QueryOptions> closeResourceHandler = (e) -> {};
        metadataEngine = new MetadataEngine<>(indexedFileCollection, openResourceHandler, closeResourceHandler);
        isReady = true;

        if (pendingFiles.size() > 0) {
            long start = System.currentTimeMillis();
            for (UploadFile pendingFile : pendingFiles) {
                addToIndex(pendingFile);
            }
            long end = System.currentTimeMillis();
            pendingFiles.clear();
        }

    }

    public void rebuildIndex(List<UploadFile> files) {
        long start = System.currentTimeMillis();

        int i = 0;
        List<UploadFile> filesToAdd = new LinkedList<>();
        for (UploadFile file : files) {
            if (file.getProbeIds() != null && file.getProbeIds() != null && file.getProbeIds().length > 0) {
                i++;
                filesToAdd.add(file);
            }
        }
        addAllToIndex(filesToAdd);
        long end = System.currentTimeMillis();

        // UploadFileIndexedCollection

    }

    public void addToIndex(UploadFile UploadFile) {
        if (UploadFile.getProbeIds() == null) {
            return;
        }
        if (UploadFile.getValueIds() == null) {
            return;
        }
        if (UploadFile.getNanotimes() == null) {
            return;
        }
        if (!isReady) {
            this.pendingFiles.add(UploadFile);
            return;
        }
        indexedFileCollection.add(UploadFile);
    }

    public void addAllToIndex(Collection<UploadFile> UploadFiles) {
        if (!isReady) {
            this.pendingFiles.addAll(UploadFiles);
            return;
        }
        indexedFileCollection.addAll(UploadFiles);

    }


    public Collection<UploadFile> findFilesWithValueIds(
            Collection<String> sessionId, Collection<Long> valueIdList) {
        Set<UploadFile> allMatches = new HashSet<>();

        for (String s : sessionId) {
            for (Long aLong : valueIdList) {
                Query<UploadFile> query = and(
                        equal(UploadFile.SESSION_ID, s),
                        equal(UploadFile.VALUE_ID, aLong)
                );

                ResultSet<UploadFile> files = indexedFileCollection.retrieve(query);
                Set<UploadFile> matches = files.stream().collect(Collectors.toSet());
                allMatches.addAll(matches);

            }
        }


        return allMatches;
    }

    public Set<UploadFile> findFilesWithValueIds(FilteredDataEventsRequest filteredDataEventsRequest) {

        Query<UploadFile> query = and(
                equal(UploadFile.SESSION_ID, filteredDataEventsRequest.getSessionId()),
                equal(UploadFile.NANOTIME_EVENT, filteredDataEventsRequest.getNanotime()),
                equal(UploadFile.VALUE_ID, filteredDataEventsRequest.getValueId().get(0)),
                equal(UploadFile.PROBE_ID, filteredDataEventsRequest.getProbeId())
        );

        ResultSet<UploadFile> files = indexedFileCollection.retrieve(query);
        List<UploadFile> matches = files.stream().collect(Collectors.toList());

        return new HashSet<>(matches);

    }
}
