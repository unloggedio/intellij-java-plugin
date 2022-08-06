package com.insidious.plugin.client;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.radixinverted.InvertedRadixTreeIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.UploadFile;
import com.insidious.common.cqengine.ObjectInfoDocument;
import com.insidious.common.cqengine.StringInfoDocument;
import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.parser.KaitaiInsidiousEventParser;
import com.insidious.common.parser.KaitaiInsidiousIndexParser;
import com.insidious.common.weaver.*;
import com.insidious.plugin.callbacks.ClientCallBack;
import com.insidious.plugin.client.cache.ArchiveIndex;
import com.insidious.plugin.client.exception.ClassInfoNotFoundException;
import com.insidious.plugin.client.pojo.*;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import io.kaitai.struct.ByteBufferKaitaiStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.googlecode.cqengine.query.QueryFactory.in;
import static com.googlecode.cqengine.query.QueryFactory.startsWith;
import static com.insidious.plugin.client.DatFileType.*;

public class SessionInstance {
    private static final Logger logger = LoggerUtil.getInstance(SessionInstance.class);
    private final File sessionDirectory;
    private final ExecutionSession executionSession;
    private final List<File> sessionArchives;
    private final Map<String, ClassInfo> classInfoMap = new HashMap<>();
    private final Map<String, String> cacheEntries = new HashMap<>();
    private KaitaiInsidiousClassWeaveParser classWeaveInfo;
    private ArchiveIndex typeIndex;

    public SessionInstance(File sessionDirectory, ExecutionSession executionSession) {
        this.sessionDirectory = sessionDirectory;
        this.executionSession = executionSession;
        this.sessionArchives = refreshSessionArchivesList();

    }

    public ExecutionSession getExecutionSession() {
        return executionSession;
    }

    private List<File> refreshSessionArchivesList() {
        if (sessionDirectory.listFiles() == null) {
            return List.of();
        }
        logger.info("refresh session archives list");
        List<File> sessionFiles = Arrays.stream(
                        Objects.requireNonNull(sessionDirectory.listFiles()))
                .sorted((a, b) -> -1 * a.getName().compareTo(b.getName()))
                .filter(e -> e.getName().endsWith(".zip")
                        && e.getName().startsWith("index-"))
                .collect(Collectors.toList());
        logger.info("found [" + sessionFiles.size() + "] session archives");

        List<File> filesToRemove = new LinkedList<>();

        for (int i = 0; i < sessionFiles.size() && classWeaveInfo == null; i++) {
            classWeaveInfo = readClassWeaveInfo(sessionFiles.get(i));

            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionFiles.get(i), INDEX_TYPE_DAT_FILE.getFileName());
            if (typeIndexBytes == null) {
                filesToRemove.add(sessionFiles.get(i));
                continue;
            }

            try {
                typeIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            } catch (IOException e) {
                logger.warn("failed to read archive for types index: " + e.getMessage());
            }
        }
        sessionFiles.removeAll(filesToRemove);
        return sessionFiles;
    }

    public Collection<TracePoint> queryTracePointsByValue(SearchQuery searchQuery) {
        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionArchive : this.sessionArchives) {

            NameWithBytes bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            if (bytes == null) {
                logger.warn("archive [" + sessionArchive.getAbsolutePath() + "] is not complete or is corrupt.");
                continue;
            }
            logger.info("initialize index for archive: " + sessionArchive.getAbsolutePath());


            ArchiveIndex index = null;
            try {
                index = readArchiveIndex(bytes.getBytes(), INDEX_STRING_DAT_FILE);
            } catch (Exception e) {
                logger.error("failed to read type index file  [" + sessionArchive.getName() + "]", e);
                continue;
            }
            Set<Long> valueIds = new HashSet<>(index.getStringIdsFromStringValues((String) searchQuery.getQuery()));


            checkProgressIndicator(null, "Loaded " + valueIds.size() + " strings from archive " + sessionArchive.getName());

            if (valueIds.size() > 0) {
                tracePointList.addAll(getTracePointsByValueIds(sessionArchive, valueIds));
            }
        }
        tracePointList.forEach(e -> e.setExecutionSession(executionSession));
        return tracePointList;
    }


    private List<TracePoint> getTracePointsByValueIds(File sessionArchive,
                                                      Set<Long> valueIds) {
        logger.info("Query for valueIds [" + valueIds.toString() + "]");
        List<TracePoint> tracePointList = new LinkedList<>();
        NameWithBytes bytes;
        ArchiveFilesIndex eventsIndex = null;
        ArchiveIndex objectIndex = null;
        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
        Map<String, ObjectInfo> objectInfoMap = new HashMap<>();
        try {
            checkProgressIndicator(null, "Loading events index " + sessionArchive.getName() + " " +
                    "to match against " + valueIds.size() + " values");


            bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
            assert bytes != null;
            eventsIndex = readEventIndex(bytes.getBytes());


            checkProgressIndicator(null, "Loading objects from " + sessionArchive.getName() + " to match against " + valueIds.size() + " values");
            NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            assert objectIndexBytes != null;
            objectIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            logger.info("object index has [" + objectIndex.Objects().size() + "] objects");
            objectInfoMap = objectIndex.getObjectsByObjectId(valueIds);

            Set<Integer> types = objectInfoMap.values().stream().map(ObjectInfo::getTypeId).map(Long::intValue).collect(Collectors.toSet());

            checkProgressIndicator(null, "Loading types from " + sessionArchive.getName());

//            NameWithBytes typesInfoBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
//            assert typesInfoBytes != null;
//            ArchiveIndex typeIndex = readArchiveIndex(typesInfoBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            typeInfoMap = typeIndex.getTypesById(types);
            logger.info("[" + typeInfoMap.size() + "] typeInfo found");

        } catch (IOException ex) {
            ex.printStackTrace();
        }


        ArchiveFilesIndex finalEventsIndex = eventsIndex;
        HashMap<String, UploadFile> matchedFiles = new HashMap<>();
        AtomicInteger counter = new AtomicInteger();
        valueIds.forEach(valueId -> {
            int currentIndex = counter.addAndGet(1);
            assert finalEventsIndex != null;

            checkProgressIndicator(null, "Matching events for item " + currentIndex + " of " + valueIds.size());

            boolean archiveHasSeenValue = finalEventsIndex.hasValueId(valueId);
            List<UploadFile> matchedFilesForString = new LinkedList<>();
            logger.info("value [" + valueId + "] found in archive: [" + archiveHasSeenValue + "]");

            if (archiveHasSeenValue) {
                checkProgressIndicator(null, "Events matched in " + sessionArchive.getName());
                matchedFilesForString = finalEventsIndex.querySessionFilesByValueId(valueId);
                for (UploadFile uploadFile : matchedFilesForString) {
                    String filePath = uploadFile.getPath();
                    int threadId = Integer.parseInt(Path.of(filePath).getFileName().toString().split("\\.")[0].split("-")[2]);
                    UploadFile uploadFileToAdd = new UploadFile(filePath, threadId, null, null);
                    uploadFileToAdd.setValueIds(new Long[]{valueId});
                    matchedFiles.put(filePath, uploadFile);
                }
            }
        });
        Map<String, ObjectInfo> finalObjectInfoMap = objectInfoMap;
        Map<String, TypeInfo> finalTypeInfoMap = typeInfoMap;
        logger.info("matched [" + matchedFiles.size() + "] files");

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText("Found " + matchedFiles.size() + " archives with matching values");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
        }


        for (UploadFile matchedFile : matchedFiles.values()) {
            try {

                checkProgressIndicator(null, "Loading events data from " + matchedFile.getPath());
                String fileName = Path.of(matchedFile.getPath()).getFileName().toString();

                NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, fileName);
                if (fileBytes == null) {
                    List<String> fileList = listArchiveFiles(sessionArchive);
                    logger.error(String.format("matched file not found " + "inside the session archive [%s] -> [%s] -> [%s]", fileName, sessionArchive, fileList));
                    throw new RuntimeException("matched file not found inside the session archive -> " + fileList);
                }
                long timestamp = Long.parseLong(fileBytes.getName().split("@")[0]);
                int threadId = Integer.parseInt(fileBytes.getName().split("-")[2].split("\\.")[0]);


                List<DataEventWithSessionId> dataEvents = getDataEventsFromPathByValueIds(fileBytes.getBytes(), matchedFile.getValueIds());
                checkProgressIndicator(null, "Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
                List<TracePoint> matchedTracePoints = dataEvents.stream().map(e1 -> {

                    try {
                        List<DataInfo> dataInfoList = getProbeInfo(sessionArchive, Set.of(e1.getDataId()));
                        logger.debug("data info list by data id [" + e1.getDataId() + "] => [" + dataInfoList + "]");

                        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                                return null;
                            }
                        }


                        DataInfo dataInfo = dataInfoList.get(0);
                        int classId = dataInfo.getClassId();
                        KaitaiInsidiousClassWeaveParser.ClassInfo classInfo = getClassInfo(classId);

                        ObjectInfo objectInfo = finalObjectInfoMap.get(String.valueOf(e1.getValue()));
                        TypeInfo typeInfo = getTypeInfo((int) objectInfo.getTypeId());

                        TracePoint tracePoint = new TracePoint(
                                classId, dataInfo.getLine(), dataInfo.getDataId(),
                                threadId, e1.getValue(), classInfo.fileName().value(),
                                classInfo.className().value(), typeInfo.getTypeNameFromClass(),
                                timestamp, e1.getNanoTime());
                        tracePoint.setExecutionSession(executionSession);
                        return tracePoint;
                    } catch (ClassInfoNotFoundException | Exception ex) {
                        logger.error("failed to get data probe information", ex);
                    }
                    return null;


                }).filter(Objects::nonNull).collect(Collectors.toList());
                tracePointList.addAll(matchedTracePoints);

                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                    if (tracePointList.size() > 0) {
                        ProgressIndicatorProvider.getGlobalProgressIndicator().setText(tracePointList.size() + " matched...");
                    }
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        return tracePointList;
                    }
                }


            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }

        }

        return tracePointList;
    }


    private List<DataInfo> getProbeInfo(File sessionFile, Set<Integer> dataId) throws IOException {


        return classWeaveInfo.classInfo().stream()
                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
                .flatMap(Collection::stream)
                .filter(e -> dataId.contains(Math.toIntExact(e.dataId())))
                .map(KaitaiUtils::toDataInfo).collect(Collectors.toList());

    }

    private KaitaiInsidiousClassWeaveParser readClassWeaveInfo(@NotNull File sessionFile) {

        KaitaiInsidiousClassWeaveParser classWeaveInfo1;
        logger.warn("creating class weave info from scratch from file: " + sessionFile.getName());
        NameWithBytes fileBytes =
                createFileOnDiskFromSessionArchiveFile(sessionFile, WEAVE_DAT_FILE.getFileName());
        if (fileBytes == null) {
            logger.debug("failed to read class weave info from " +
                    "sessionFile [" + sessionFile.getName() + "]");
            return null;
        }
//        logger.warn("Class weave information from " + sessionFile.getName() + " is " + fileBytes.getBytes().length + " bytes");
        classWeaveInfo1 = new KaitaiInsidiousClassWeaveParser(
                new ByteBufferKaitaiStream(fileBytes.getBytes()));
        return classWeaveInfo1;
    }


    private KaitaiInsidiousClassWeaveParser.ClassInfo getClassInfo(int classId) throws ClassInfoNotFoundException {

        for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeaveInfo.classInfo()) {
            if (classInfo.classId() == classId) {
                return classInfo;
            }
        }
        throw new ClassInfoNotFoundException(classId);
    }


    private List<DataEventWithSessionId> getDataEventsFromPathByValueIds(byte[] bytes, Long[] valueIds) throws IOException {

        Set<Long> ids = Set.of(valueIds);
        KaitaiInsidiousEventParser dataEvents = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(bytes));

        return dataEvents.event().entries().stream()
                .filter(e -> e.magic() == 4 || e.magic() == 7)
                .filter(e -> {
                    if (e.block() instanceof KaitaiInsidiousEventParser.DataEventBlock) {
                        return ids.contains(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).valueId());
                    }
                    if (e.block() instanceof KaitaiInsidiousEventParser.DetailedEventBlock) {
                        return ids.contains(((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).valueId());
                    }
                    return false;
                })
                .map(e -> {

                    if (e.magic() == 4) {
                        long valueId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).valueId();
                        int probeId = Math.toIntExact(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).probeId());
                        long eventId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).eventId();
                        long timestamp = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).timestamp();

                        DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                        dataEvent.setDataId(probeId);
                        dataEvent.setValue(valueId);
                        dataEvent.setNanoTime(eventId);
                        dataEvent.setRecordedAt(timestamp);
                        return dataEvent;

                    } else {
                        long valueId = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).valueId();
                        int probeId = Math.toIntExact(((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).probeId());
                        long eventId = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).eventId();
                        long timestamp = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).timestamp();

                        DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                        dataEvent.setDataId(probeId);
                        dataEvent.setValue(valueId);
                        dataEvent.setNanoTime(eventId);
                        dataEvent.setRecordedAt(timestamp);
                        return dataEvent;
                    }
                })
                .collect(Collectors.toList());
    }


    public TypeInfo getTypeInfo(Integer typeId) {

        Map<String, TypeInfo> result = typeIndex.getTypesById(Set.of(typeId));
        if (result.size() > 0) {
            return result.get(String.valueOf(typeId));
        }

        return new TypeInfo("local", typeId, "unidentified type", "",
                0, 0, "", new int[0]);
    }

    public TypeInfo getTypeInfo(String name) {

        TypeInfo result = typeIndex.getTypesByName(name);
        if (result != null ) {
            return result;
        }

        return new TypeInfo("local", -1, "unidentified type - " + name, "",
                0, 0, "", new int[0]);
    }

    public List<TypeInfoDocument> getAllTypes() {
        return new ArrayList<>(typeIndex.Types());
    }


    private List<String> listArchiveFiles(File sessionFile) throws IOException {
        List<String> files = new LinkedList<>();

        ZipInputStream indexArchive = new ZipInputStream(new FileInputStream(sessionFile));

        long filterValueLong = 0;

        ZipEntry entry = null;
        while ((entry = indexArchive.getNextEntry()) != null) {
            String entryName = entry.getName();
            files.add(entryName);
        }
        return files;

    }


    private ArchiveFilesIndex readEventIndex(byte[] bytes) throws IOException {
        KaitaiInsidiousIndexParser archiveIndex = new KaitaiInsidiousIndexParser(new ByteBufferKaitaiStream(bytes));

        return new ArchiveFilesIndex(archiveIndex);
    }


    private ArchiveIndex readArchiveIndex(byte[] bytes, DatFileType indexFilterType) throws IOException {

        ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex = null;
        String cacheKey = bytesHex(bytes, indexFilterType.getFileName());


        Path path = Path.of(this.sessionDirectory.getAbsolutePath(),
                cacheKey, indexFilterType.getFileName());
        Path parentPath = path.getParent();
        parentPath.toFile().mkdirs();

        if(!path.toFile().exists()) {
            Files.write(path, bytes);
        }

        if (indexFilterType.equals(INDEX_TYPE_DAT_FILE)) {
            DiskPersistence<TypeInfoDocument, Integer> typeInfoDocumentStringDiskPersistence =
                    DiskPersistence.onPrimaryKeyInFile(TypeInfoDocument.TYPE_ID, path.toFile());
            typeInfoIndex = new ConcurrentIndexedCollection<>(typeInfoDocumentStringDiskPersistence);
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_NAME));
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_ID));
        }


        ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex = null;
        if (indexFilterType.equals(INDEX_STRING_DAT_FILE)) {
            DiskPersistence<StringInfoDocument, Long> stringInfoDocumentStringDiskPersistence =
                    DiskPersistence.onPrimaryKeyInFile(StringInfoDocument.STRING_ID, path.toFile());
            stringInfoIndex = new ConcurrentIndexedCollection<>(stringInfoDocumentStringDiskPersistence);

            stringInfoIndex.addIndex(InvertedRadixTreeIndex.onAttribute(StringInfoDocument.STRING_VALUE));
        }

        ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex = null;
        if (indexFilterType.equals(INDEX_OBJECT_DAT_FILE)) {
            DiskPersistence<ObjectInfoDocument, Long> objectInfoDocumentIntegerDiskPersistence =
                    DiskPersistence.onPrimaryKeyInFile(ObjectInfoDocument.OBJECT_ID, path.toFile());

            objectInfoIndex = new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_TYPE_ID));
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_ID));
        }

        if (indexFilterType.equals(WEAVE_DAT_FILE)) {
            KaitaiInsidiousClassWeaveParser classWeave =
                    new KaitaiInsidiousClassWeaveParser(new ByteBufferKaitaiStream(bytes));

            for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeave.classInfo()) {
                classInfoMap.put(classInfo.className().value(),
                        KaitaiUtils.toClassInfo(classInfo)
                );
            }
        }

        return new ArchiveIndex(typeInfoIndex, stringInfoIndex, objectInfoIndex, classInfoMap);
    }


    @NotNull
    private String bytesHex(byte[] bytes, String indexFilterType) {
        String md5Hex = DigestUtils.md5Hex(bytes);
        String cacheKey = md5Hex + "-" + indexFilterType;
        return cacheKey;
    }


    private NameWithBytes createFileOnDiskFromSessionArchiveFile(File sessionFile, String pathName) {
        logger.debug(String.format("get file[%s] from archive[%s]", pathName, sessionFile.getName()));
        String cacheKey = sessionFile.getName() + pathName;
        String cacheFileLocation = this.sessionDirectory + "/cache/" + cacheKey + ".dat";
        ZipInputStream indexArchive = null;
        try {

            if (cacheEntries.containsKey(cacheKey)) {
                String name = cacheEntries.get(cacheKey);
                File cacheFile = new File(cacheFileLocation);
                byte[] bytes = IOUtils.toByteArray(new FileInputStream(cacheFile));
                return new NameWithBytes(name, bytes);
            }

            FileInputStream sessionFileInputStream = new FileInputStream(sessionFile);
            indexArchive = new ZipInputStream(sessionFileInputStream);


            long filterValueLong = 0;
            try {
                filterValueLong = Long.parseLong(pathName);
            } catch (Exception ignored) {

            }

            ZipEntry entry = null;
            while ((entry = indexArchive.getNextEntry()) != null) {
                String entryName = entry.getName();
                logger.debug(String.format("file entry in archive [%s] -> [%s]", sessionFile.getName(), entryName));
                if (entryName.contains(pathName)) {
                    byte[] fileBytes = IOUtils.toByteArray(indexArchive);

                    File cacheFile = new File(cacheFileLocation);
                    FileUtils.writeByteArrayToFile(cacheFile, fileBytes);
                    cacheEntries.put(cacheKey, entryName);

                    NameWithBytes nameWithBytes = new NameWithBytes(entryName, fileBytes);
                    logger.warn(pathName + " file from "
                            + sessionFile.getName() + " is " + nameWithBytes.getBytes().length + " bytes");
                    indexArchive.closeEntry();
                    indexArchive.close();
                    sessionFileInputStream.close();
                    return nameWithBytes;
                }
            }
        } catch (Exception e) {
            logger.warn("failed to create file [" + pathName + "] on disk from" +
                    " archive[" + sessionFile.getName() + "]");
            return null;
        }
        return null;

    }


    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(text1);
            }
        }
    }


    private List<DataInfo> queryProbeFromFileByEventType(File sessionFile,
                                                         Collection<EventType> eventTypes) {
        return classWeaveInfo.classInfo().stream()
                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
                .flatMap(Collection::stream)
                .filter(e -> eventTypes.size() == 0 ||
                        // dont check contains if the list is empty
                        eventTypes.contains(EventType.valueOf(e.eventType().value())))
                .map(KaitaiUtils::toDataInfo).collect(Collectors.toList());


    }


    public void queryTracePointsByEventType(SearchQuery searchQuery, ClientCallBack<TracePoint> tracePointsCallback) {


        List<DataInfo> probeIds = null;
        for (File sessionArchive : sessionArchives) {
            logger.info("check archive [" + sessionArchive.getName() + "] for " +
                    "probes");
            if (probeIds == null || probeIds.size() == 0) {
                Collection<EventType> eventTypes =
                        (Collection<EventType>) searchQuery.getQuery();
                probeIds = queryProbeFromFileByEventType(sessionArchive, eventTypes);
            }

            checkProgressIndicator(null, "Loaded " + probeIds.size() + " objects from archive " + sessionArchive.getName());


            if (probeIds.size() > 0) {
                getTracePointsByProbeIds(sessionArchive,
                        probeIds.stream().map(DataInfo::getDataId)
                                .collect(Collectors.toSet()),
                        tracePointsCallback
                );
            }
        }
        tracePointsCallback.completed();

    }


    public void queryTracePointsByProbeIds(SearchQuery searchQuery,
                                           ClientCallBack<TracePoint> tracePointsCallback) {


        Collection<Integer> probeIds =
                (Collection<Integer>) searchQuery.getQuery();

        for (File sessionArchive : sessionArchives) {
            getTracePointsByProbeIds(sessionArchive,
                    new HashSet<>(probeIds),
                    tracePointsCallback
            );
        }
        tracePointsCallback.completed();

    }

    public void
    queryTracePointsByProbeIdsWithoutIndex(
            SearchQuery searchQuery,
            ClientCallBack<TracePoint> tracePointsCallback) {


        Collection<Integer> probeIds =
                (Collection<Integer>) searchQuery.getQuery();

        for (File sessionArchive : sessionArchives) {
            getTracePointsByProbeIdsWithoutIndex(sessionArchive,
                    new HashSet<>(probeIds),
                    tracePointsCallback
            );
        }
        tracePointsCallback.completed();

    }


    private void getTracePointsByProbeIds(File sessionArchive,
                                          Set<Integer> probeIds,
                                          ClientCallBack<TracePoint> tracePointsCallback) {
        logger.info("Query for probeIds [" + probeIds.toString() + "]");
        List<TracePoint> tracePointList = new LinkedList<>();
        NameWithBytes bytes;
        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
        Map<String, ObjectInfo> objectInfoMap = new HashMap<>();


        ArchiveFilesIndex eventsIndex = null;
        ArchiveIndex objectIndex = null;
        try {
            checkProgressIndicator(null, "Loading events index " + sessionArchive.getName() + " to match against " + probeIds.size() + " values");


            bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
            if (bytes == null) {
                return;
            }
            eventsIndex = readEventIndex(bytes.getBytes());

        } catch (IOException ex) {
            ex.printStackTrace();
        }


        ArchiveFilesIndex finalEventsIndex = eventsIndex;
        HashMap<String, UploadFile> matchedFiles = new HashMap<>();
        AtomicInteger counter = new AtomicInteger();
        probeIds.forEach(probeId -> {
            int currentIndex = counter.addAndGet(1);
            assert finalEventsIndex != null;

            checkProgressIndicator(null, "Matching events for probe " + currentIndex + " of " + probeIds.size());

            boolean archiveHasSeenValue = finalEventsIndex.hasProbeId(probeId);
            List<UploadFile> matchedFilesForString = new LinkedList<>();
            logger.info("probeId [" + probeId + "] found in archive: [" + archiveHasSeenValue + "]");

            if (archiveHasSeenValue) {
                checkProgressIndicator(null, "Events matched in " + sessionArchive.getName());
                matchedFilesForString =
                        finalEventsIndex.querySessionFilesByProbeId(probeId);
                for (UploadFile uploadFile : matchedFilesForString) {
                    String filePath = uploadFile.getPath();
                    int threadId = Integer.parseInt(Path.of(filePath).getFileName().toString().split("\\.")[0].split("-")[2]);
                    UploadFile uploadFileToAdd = new UploadFile(filePath, threadId, null, null);
                    uploadFileToAdd.setProbeIds(new Integer[]{probeId});

                    if (matchedFiles.containsKey(filePath)) {
                        Integer[] existingProbes = matchedFiles.get(filePath).getProbeIds();
                        ArrayList<Integer> arrayList = new ArrayList<>(Arrays.asList(existingProbes));
                        if (!arrayList.contains(probeId)) {
                            arrayList.add(probeId);
                            matchedFiles.get(filePath).setProbeIds(arrayList.toArray(Integer[]::new));
                        }

                    } else {
                        matchedFiles.put(filePath, uploadFile);
                    }

                }
            }
        });
        logger.info("matched [" + matchedFiles.size() + "] files");

        checkProgressIndicator("Found " + matchedFiles.size() + " archives with matching values",
                null);


        for (UploadFile matchedFile : matchedFiles.values()) {
            try {

                checkProgressIndicator(null, "Loading events data from " + matchedFile.getPath());
                String fileName = Path.of(matchedFile.getPath()).getFileName().toString();

                NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, fileName);
                if (fileBytes == null) {
                    List<String> fileList = listArchiveFiles(sessionArchive);
                    logger.error(String.format("matched file not found " + "inside the session archive [%s] -> [%s] -> [%s]", fileName, sessionArchive, fileList));
                    throw new RuntimeException("matched file not found inside the session archive -> " + fileList);
                }
                long timestamp = Long.parseLong(fileBytes.getName().split("@")[0]);
                int threadId = Integer.parseInt(fileBytes.getName().split("-")[2].split("\\.")[0]);


                List<DataEventWithSessionId> dataEvents =
                        getDataEventsFromPathByProbeIds(fileBytes.getBytes(),
                                matchedFile.getProbeIds());

                checkProgressIndicator(null, "Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
                List<TracePoint> matchedTracePoints = dataEvents.stream().map(e1 -> {

                    try {
                        List<DataInfo> dataInfoList = getProbeInfo(sessionArchive, Set.of(e1.getDataId()));
                        logger.debug("data info list by data id [" + e1.getDataId() + "] => [" + dataInfoList + "]");

                        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                                return null;
                            }
                        }


                        DataInfo dataInfo = dataInfoList.get(0);
                        int classId = dataInfo.getClassId();
                        KaitaiInsidiousClassWeaveParser.ClassInfo classInfo = getClassInfo(classId);

                        ObjectInfo objectInfo = objectInfoMap.get(String.valueOf(e1.getValue()));
                        String typeName = "<na>";
                        if (objectInfo != null) {
                            TypeInfo typeInfo = getTypeInfo((int) objectInfo.getTypeId());
                            typeName = typeInfo.getTypeNameFromClass();
                        }

                        TracePoint tracePoint = new TracePoint(
                                classId, dataInfo.getLine(),
                                dataInfo.getDataId(), threadId, e1.getValue(),
                                classInfo.fileName().value(), classInfo.className().value(),
                                typeName, timestamp, e1.getNanoTime());

                        tracePoint.setExecutionSession(executionSession);
                        return tracePoint;
                    } catch (ClassInfoNotFoundException | Exception ex) {
                        logger.error("failed to get data probe information", ex);
                    }
                    return null;


                }).filter(Objects::nonNull).collect(Collectors.toList());
                tracePointList.addAll(matchedTracePoints);

                checkProgressIndicator(null, tracePointList.size() + " matched...");


            } catch (IOException ex) {
                logger.error("exception while creating trace points in file[" + matchedFile.path + "]",
                        ex);
            }

        }
        if (tracePointList.size() != 0) {
            tracePointList.forEach(e -> e.setExecutionSession(executionSession));
            tracePointsCallback.success(tracePointList);
        }
    }


    private void
    getTracePointsByProbeIdsWithoutIndex(
            File sessionArchive,
            Set<Integer> probeIds,
            ClientCallBack<TracePoint> tracePointsCallback) {
        logger.info("Query for probeIds [" + probeIds.toString() + "]");
        List<TracePoint> tracePointList = new LinkedList<>();
        NameWithBytes bytes;
        Integer[] probeIdArray = new Integer[probeIds.size()];
        int k = 0;
        for (Integer probeId : probeIds) {
            probeIdArray[k] = probeId;
            k++;
        }

        ArchiveFilesIndex eventsIndex = null;
        ArchiveIndex objectIndex = null;
        try {
            checkProgressIndicator(null, "Loading events index " + sessionArchive.getName() + " to match against " + probeIds.size() + " values");


            bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
            if (bytes == null) {
                return;
            }
            eventsIndex = readEventIndex(bytes.getBytes());

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
        Map<String, ObjectInfo> objectInfoMap = new HashMap<>();


        HashMap<String, UploadFile> matchedFiles = new HashMap<>();
        AtomicInteger counter = new AtomicInteger();
        probeIds.forEach(probeId -> {
            int currentIndex = counter.addAndGet(1);

            List<UploadFile> matchedFilesForString = new LinkedList<>();

            checkProgressIndicator(null, "Events matched in " + sessionArchive.getName());
            try {
                matchedFilesForString =
                        listArchiveFiles(sessionArchive).stream()
                                .filter(e -> e.endsWith("selog"))
                                .map(e ->
                                {
                                    UploadFile uploadFile = new UploadFile(e, 0,
                                            null, null);
                                    uploadFile.setProbeIds(probeIdArray);
                                    return uploadFile;
                                })
                                .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            for (UploadFile uploadFile : matchedFilesForString) {
                String filePath = uploadFile.getPath();
                int threadId = Integer.parseInt(Path.of(filePath).getFileName().toString().split("\\.")[0].split("-")[2]);
                UploadFile uploadFileToAdd = new UploadFile(filePath, threadId, null, null);
                uploadFileToAdd.setProbeIds(new Integer[]{probeId});

                if (matchedFiles.containsKey(filePath)) {
                    Integer[] existingProbes = matchedFiles.get(filePath).getProbeIds();
                    ArrayList<Integer> arrayList = new ArrayList<>(Arrays.asList(existingProbes));
                    if (!arrayList.contains(probeId)) {
                        arrayList.add(probeId);
                        matchedFiles.get(filePath).setProbeIds(arrayList.toArray(Integer[]::new));
                    }

                } else {
                    matchedFiles.put(filePath, uploadFile);
                }

            }
        });
        logger.info("matched [" + matchedFiles.size() + "] files");


        for (UploadFile matchedFile : matchedFiles.values()) {
            try {

                checkProgressIndicator(null, "Loading events data from " + matchedFile.getPath());
                String fileName = Path.of(matchedFile.getPath()).getFileName().toString();

                NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, fileName);
                if (fileBytes == null) {
                    List<String> fileList = listArchiveFiles(sessionArchive);
                    logger.error(String.format("matched file not found " + "inside the session archive [%s] -> [%s] -> [%s]", fileName, sessionArchive, fileList));
                    throw new RuntimeException("matched file not found inside the session archive -> " + fileList);
                }
                long timestamp = Long.parseLong(fileBytes.getName().split("@")[0]);
                int threadId = Integer.parseInt(fileBytes.getName().split("-")[2].split("\\.")[0]);


                List<DataEventWithSessionId> dataEvents =
                        getDataEventsFromPathByProbeIds(fileBytes.getBytes(),
                                matchedFile.getProbeIds());

                checkProgressIndicator(null, "Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
                List<TracePoint> matchedTracePoints = dataEvents.stream().map(e1 -> {

                    try {
                        List<DataInfo> dataInfoList = getProbeInfo(sessionArchive, Set.of(e1.getDataId()));
                        logger.debug("data info list by data id [" + e1.getDataId() + "] => [" + dataInfoList + "]");

                        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                                return null;
                            }
                        }


                        DataInfo dataInfo = dataInfoList.get(0);
                        int classId = dataInfo.getClassId();
                        KaitaiInsidiousClassWeaveParser.ClassInfo classInfo = getClassInfo(classId);

                        ObjectInfo objectInfo = objectInfoMap.get(String.valueOf(e1.getValue()));
                        String typeName = "<na>";
                        if (objectInfo != null) {
                            TypeInfo typeInfo = getTypeInfo((int) objectInfo.getTypeId());
                            typeName = typeInfo.getTypeNameFromClass();
                        }

                        TracePoint tracePoint = new TracePoint(
                                classId, dataInfo.getLine(),
                                dataInfo.getDataId(), threadId, e1.getValue(),
                                classInfo.fileName().value(), classInfo.className().value(),
                                typeName, timestamp, e1.getNanoTime());

                        tracePoint.setExecutionSession(executionSession);
                        return tracePoint;
                    } catch (ClassInfoNotFoundException | Exception ex) {
                        logger.error("failed to get data probe information", ex);
                    }
                    return null;


                }).filter(Objects::nonNull).collect(Collectors.toList());
                tracePointList.addAll(matchedTracePoints);

                checkProgressIndicator(null, tracePointList.size() + " matched...");


            } catch (IOException ex) {
                logger.error("exception while creating trace points in file[" + matchedFile.path + "]",
                        ex);
            }

        }
        if (tracePointList.size() != 0) {
            tracePointList.forEach(e -> e.setExecutionSession(executionSession));
            tracePointsCallback.success(tracePointList);
        }
    }


    private List<DataEventWithSessionId> getDataEventsFromPathByProbeIds(byte[] bytes, Integer[] probeIds) {

        Set<Integer> ids = Set.of(probeIds);
        KaitaiInsidiousEventParser dataEvents = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(bytes));

        return dataEvents.event().entries().stream()
                .filter(e -> e.magic() == 4 || e.magic() == 7)
                .filter(e -> {
                    if (e.block() instanceof KaitaiInsidiousEventParser.DataEventBlock) {
                        return ids.contains((int) ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).probeId());
                    }
                    if (e.block() instanceof KaitaiInsidiousEventParser.DetailedEventBlock) {
                        return ids.contains((int) ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).probeId());
                    }
                    return false;
                })
                .map(e -> {
                    if (e.magic() == 4) {
                        long valueId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).valueId();
                        int probeId = Math.toIntExact(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).probeId());
                        long eventId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).eventId();
                        long timestamp = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).timestamp();

                        DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                        dataEvent.setDataId(probeId);
                        dataEvent.setValue(valueId);
                        dataEvent.setNanoTime(eventId);
                        dataEvent.setRecordedAt(timestamp);
                        return dataEvent;

                    } else {
                        long valueId = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).valueId();
                        int probeId = Math.toIntExact(((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).probeId());
                        long eventId = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).eventId();
                        long timestamp = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).timestamp();

                        DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                        dataEvent.setDataId(probeId);
                        dataEvent.setValue(valueId);
                        dataEvent.setNanoTime(eventId);
                        dataEvent.setRecordedAt(timestamp);
                        return dataEvent;
                    }


                }).collect(Collectors.toList());
    }

    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) {
        File archiveToServe = null;
        for (File sessionArchive : this.sessionArchives) {
            long timestamp = Long.parseLong(sessionArchive.getName().split("-")[2].split("\\.")[0]);
            if (timestamp < filteredDataEventsRequest.getNanotime()) {
                archiveToServe = sessionArchive;
                break;
            }
        }


        checkProgressIndicator(null, "Loading archive: " + archiveToServe.getName());


        NameWithBytes bytesWithName = createFileOnDiskFromSessionArchiveFile(
                archiveToServe, String.valueOf(filteredDataEventsRequest.getNanotime()));


        assert bytesWithName != null;


        checkProgressIndicator(null, "Parsing events: " + bytesWithName.getName());

        KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(
                new ByteBufferKaitaiStream(bytesWithName.getBytes()));


        checkProgressIndicator(null, "Mapping " + eventsContainer.event().entries().size() + " events ");

        List<DataEventWithSessionId> dataEventList = eventsContainer.event()
                .entries().stream().filter(e -> e.magic() == 4 || e.magic() == 7)
                .map(e -> {
                    if (e.magic() == 4) {
                        long valueId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).valueId();
                        int probeId = Math.toIntExact(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).probeId());
                        long eventId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).eventId();
                        long timestamp = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).timestamp();

                        DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                        dataEvent.setDataId(probeId);
                        dataEvent.setValue(valueId);
                        dataEvent.setNanoTime(eventId);
                        dataEvent.setRecordedAt(timestamp);
                        return dataEvent;

                    } else {
                        long valueId = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).valueId();
                        int probeId = Math.toIntExact(((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).probeId());
                        long eventId = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).eventId();
                        long timestamp = ((KaitaiInsidiousEventParser.DetailedEventBlock) e.block()).timestamp();

                        DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                        dataEvent.setDataId(probeId);
                        dataEvent.setValue(valueId);
                        dataEvent.setNanoTime(eventId);
                        dataEvent.setRecordedAt(timestamp);
                        return dataEvent;
                    }

                }).collect(Collectors.toList());

        Collections.reverse(dataEventList);


        Map<String, ClassInfo> classInfo = new HashMap<>();
        Map<String, DataInfo> dataInfo = new HashMap<>();

        checkProgressIndicator(null, "Loading class mappings");

        classWeaveInfo.classInfo().forEach(e -> {

            checkProgressIndicator(null, "Loading class: " + e.className());

            classInfo.put(String.valueOf(e.classId()), KaitaiUtils.toClassInfo(e));

            checkProgressIndicator(null, "Loading " + e.probeCount() + " probes in class: " + e.className());

            e.probeList().forEach(r -> {
                dataInfo.put(String.valueOf(r.dataId()),
                        KaitaiUtils.toDataInfo(r));
            });
        });

        Set<Integer> probeIds = dataEventList.stream().map(DataEventWithSessionId::getDataId).collect(Collectors.toSet());
        Set<Long> valueIds = dataEventList.stream().map(DataEventWithSessionId::getValue).collect(Collectors.toSet());


        Map<String, StringInfo> stringInfo = new HashMap<>();
        Map<String, ObjectInfo> objectInfo = new HashMap<>();
        Map<String, TypeInfo> typeInfo = new HashMap<>();
        Map<String, MethodInfo> methodInfoMap = new HashMap<>();

        checkProgressIndicator(null, "Loading types");
        for (File sessionArchive : this.sessionArchives) {

            checkProgressIndicator(null, "Loading objects from " + sessionArchive.getName());

            NameWithBytes objectsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            if (objectsIndexBytes == null) {
                continue;
            }

            checkProgressIndicator(null, "Loading objects from " + sessionArchive.getName());
            ArchiveIndex objectIndex = null;
            try {
                objectIndex = readArchiveIndex(objectsIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            } catch (IOException e) {
                logger.error("failed to read object index from session bytes: " + e.getMessage(), e);
                continue;
            }
            Map<String, ObjectInfo> sessionObjectInfo = objectIndex.getObjectsByObjectId(valueIds);
            objectInfo.putAll(sessionObjectInfo);

            checkProgressIndicator(null, "Loading strings from " + sessionArchive.getName());
            NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            assert stringsIndexBytes != null;


            checkProgressIndicator(null, "Loading strings from " + sessionArchive.getName());
            ArchiveIndex stringIndex = null;
            try {
                stringIndex = readArchiveIndex(stringsIndexBytes.getBytes(), INDEX_STRING_DAT_FILE);
            } catch (IOException e) {
                logger.error("failed to read string index from session bytes: " + e.getMessage(), e);
                continue;
            }
            Map<String, StringInfo> sessionStringInfo = stringIndex.getStringsById(valueIds.stream().filter(e -> e > 10).collect(Collectors.toSet()));
            stringInfo.putAll(sessionStringInfo);


            Set<Integer> typeIds = objectInfo.values().stream().map(ObjectInfo::getTypeId).map(Long::intValue).collect(Collectors.toSet());


            checkProgressIndicator(null, "Loading types from " + sessionArchive.getName());
//            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
//            assert typeIndexBytes != null;
//            ArchiveIndex typesIndex = null;
//            try {
//                typesIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
//            } catch (IOException e) {
//                logger.error("failed to read type index from session archive", e);
//                continue;
//            }
            Map<String, TypeInfo> sessionTypeInfo = typeIndex.getTypesById(typeIds);
            typeInfo.putAll(sessionTypeInfo);

        }


        checkProgressIndicator(null, "Completed loading");
        return new ReplayData(null, filteredDataEventsRequest, dataEventList, classInfo,
                dataInfo, stringInfo, objectInfo, typeInfo, methodInfoMap);
    }

    public void getMethods(Integer typeId, ClientCallBack<TestCandidate> tracePointsCallback) {

        if (classWeaveInfo == null) {
            ExceptionResponse errorResponse = new ExceptionResponse();
            errorResponse.setMessage("session not found [" + executionSession.getSessionId() + "]");
            tracePointsCallback.error(errorResponse);
            return;
        }
        classWeaveInfo
                .classInfo()
                .forEach(classInfo -> {

                    if (classInfo.classId() != typeId) {
                        return;
                    }
                    ClassInfo classInfoContainer = KaitaiUtils.toClassInfo(classInfo);
                    tracePointsCallback.success(
                            classInfo.methodList()
                                    .stream().map(e -> KaitaiUtils.toMethodInfo(e,
                                            classInfo.className().value()))
                                    .map(methodInfo ->
                                            new TestCandidate(methodInfo,
                                                    classInfoContainer,
                                                    0, null))
                                    .collect(Collectors.toSet()));
                });
        tracePointsCallback.completed();
    }

    public void
    getObjectsByType(
            SearchQuery searchQuery,
            ClientCallBack<ObjectWithTypeInfo> clientCallBack
    ) {

        checkProgressIndicator("Looking for objects by class: " + searchQuery.getQuery(), null);
        int rangeLow = -1;
        int rangeHigh = 9999999;

        if (searchQuery.getRange() != null && searchQuery.getRange().length() > 0) {
            String[] searchRange = searchQuery.getRange().split("-");
            rangeLow = Integer.parseInt(searchRange[0]);
            if (searchRange.length > 1) {
                rangeHigh = Integer.parseInt(searchRange[1]);
            }
        }

        for (File sessionArchive : this.sessionArchives) {

            checkProgressIndicator("Checking archive " + sessionArchive.getName(), null);
            String archiveName = sessionArchive.getName();
            int archiveIndex = Integer.parseInt(archiveName.split("-")[1]);
            if (archiveIndex < rangeLow || archiveIndex > rangeHigh) {
                continue;
            }

            Set<ObjectInfoDocument> objects = queryObjectsByTypeFromSessionArchive(searchQuery,
                    sessionArchive);
            if (objects.size() == 0) {
                continue;
            }

            Map<Long, TypeInfo> typesMap = objects.stream()
                    .map(ObjectInfoDocument::getTypeId)
                    .collect(Collectors.toSet())
                    .stream()
                    .map(this::getTypeInfo)
                    .collect(Collectors.toMap(TypeInfo::getTypeId, typeInfo -> typeInfo));

            Set<ObjectWithTypeInfo> collect = objects.stream()
                    .map(e ->
                            new ObjectWithTypeInfo(
                                    new ObjectInfo(e.getObjectId(), e.getTypeId(), 0),
                                    typesMap.get((long) e.getTypeId())))
                    .collect(Collectors.toSet());
            if (collect.size() > 0) {
                checkProgressIndicator(null, archiveName + " matched " + collect.size() + " objects");
                clientCallBack.success(collect);
            }


        }

        clientCallBack.completed();

    }

    private Set<ObjectInfoDocument>
    queryObjectsByTypeFromSessionArchive(
            SearchQuery searchQuery,
            File sessionArchive
    ) {

        checkProgressIndicator(null, "querying type names from: " + sessionArchive.getName());


        Set<Integer> typeIds = queryTypeIdsByName(searchQuery);


        checkProgressIndicator(null, "Loading matched objects");


        NameWithBytes objectIndexFileBytes = null;
        objectIndexFileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
        if (objectIndexFileBytes == null) {
            logger.warn("object index file bytes are empty, skipping");
            return Set.of();
        }


        ArchiveIndex objectIndex = null;
        try {
            objectIndex = readArchiveIndex(objectIndexFileBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
        } catch (IOException e) {
            logger.warn("failed to read object index file: " + e.getMessage());
            return Set.of();

        }
        Query<ObjectInfoDocument> query = in(ObjectInfoDocument.OBJECT_TYPE_ID, typeIds);
        ResultSet<ObjectInfoDocument> typeInfoSearchResult = objectIndex.Objects().retrieve(query);
        Set<ObjectInfoDocument> objects = typeInfoSearchResult.stream()
                .collect(Collectors.toSet());
        typeInfoSearchResult.close();

        checkProgressIndicator(null, sessionArchive.getName() +
                " matched " + objects.size() + " objects of total " + objectIndex.Objects().size());


        return objects;
    }

    private Set<Integer> queryTypeIdsByName(SearchQuery searchQuery) {
        Query<TypeInfoDocument> typeQuery = startsWith(TypeInfoDocument.TYPE_NAME
                , (String) searchQuery.getQuery());
        ResultSet<TypeInfoDocument> searchResult = typeIndex.Types().retrieve(typeQuery);
        Set<Integer> typeIds = searchResult.stream().map(TypeInfoDocument::getTypeId).collect(Collectors.toSet());
        searchResult.close();
        logger.info("type query [" + searchQuery + "] matched [" + typeIds.size() + "] items");

        if (typeIds.size() == 0) {
            return Set.of();
        }
        return typeIds;
    }

    public List<String> getArchiveNamesList() {
        return refreshSessionArchivesList()
                .stream()
                .map(File::getName)
                .collect(Collectors.toList());
    }

    public ClassWeaveInfo getClassWeaveInfo() {


        int i = 0;
        List<ClassInfo> classInfoList = new LinkedList<>();
        List<MethodInfo> methodInfoList = new LinkedList<>();
        List<DataInfo> dataInfoList = new LinkedList<>();


        i = 0;
        for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeaveInfo.classInfo()) {
            i += 1;
            checkProgressIndicator(null,
                    "Parsing class [ " + i + " of " + classInfoList.size() + " ]");
            ClassInfo classInfoContainer = KaitaiUtils.toClassInfo(classInfo);
            classInfoList.add(classInfoContainer);

            classInfo.methodList()
                    .stream().map(e1 -> KaitaiUtils.toMethodInfo(e1,
                            classInfo.className().value()))
                    .forEach(methodInfoList::add);

            classInfo.probeList()
                    .stream()
                    .filter(e -> !Objects.equals(e.eventType().value(), EventType.RESERVED.toString()))
                    .map(KaitaiUtils::toDataInfo)
                    .forEach(dataInfoList::add);

        }

        ClassWeaveInfo classWeave = new ClassWeaveInfo(classInfoList,
                methodInfoList, dataInfoList);
        return classWeave;
    }


    @Nullable
    private ArchiveFilesIndex getEventIndex(File sessionArchive) {
        NameWithBytes eventIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                INDEX_EVENTS_DAT_FILE.getFileName());
        if (eventIndexBytes == null) {
            logger.warn("failed to read events index from : " + sessionArchive.getName());
            return null;
        }
        ArchiveFilesIndex eventsIndex = null;
        try {
            eventsIndex = readEventIndex(eventIndexBytes.getBytes());
        } catch (IOException e) {
            logger.warn("failed to read events index from : " + sessionArchive.getName());
            return null;
        }
        return eventsIndex;
    }

    private List<KaitaiInsidiousEventParser.Block> getEventsFromFile(File sessionArchive, String archiveFile) {
        NameWithBytes bytesWithName = createFileOnDiskFromSessionArchiveFile(
                sessionArchive, archiveFile);

        assert bytesWithName != null;


        KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(
                new ByteBufferKaitaiStream(bytesWithName.getBytes()));


        return eventsContainer.event()
                .entries()
                .stream()
                .filter(e -> e.magic() == 4 || e.magic() == 7)
                .collect(Collectors.toList());
    }


    public ReplayData fetchObjectHistoryByObjectId(FilteredDataEventsRequest filteredDataEventsRequest) {

        List<DataEventWithSessionId> dataEventList = new LinkedList<>();
        Map<String, ClassInfo> classInfoMap = new HashMap<>();
        Map<String, DataInfo> probeInfoMap = new HashMap<>();
        Map<String, StringInfo> stringInfoMap = new HashMap<>();
        Map<String, ObjectInfo> objectInfoMap = new HashMap<>();
        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
        Map<String, MethodInfo> methodInfoMap = new HashMap<>();

        final long objectId = filteredDataEventsRequest.getObjectId();

        LinkedList<File> sessionArchivesLocal = new LinkedList<>(this.sessionArchives);

        Collections.sort(sessionArchivesLocal);

        PageInfo pageInfo = filteredDataEventsRequest.getPageInfo();
        if (pageInfo.isDesc()) {
            Collections.reverse(sessionArchivesLocal);
        }


        final AtomicInteger skip = new AtomicInteger(pageInfo.getNumber() * pageInfo.getSize());
        Integer remaining = pageInfo.getSize();


        checkProgressIndicator(null, "Loading class mappings");

        classWeaveInfo.classInfo().forEach(e -> {

            checkProgressIndicator(null, "Loading class: " + e.className());

            ClassInfo classInfo = KaitaiUtils.toClassInfo(e);
            classInfoMap.put(String.valueOf(e.classId()), classInfo);

            checkProgressIndicator(null, "Loading " + e.probeCount() + " probes in class: " + e.className());

            e.methodList().forEach(m -> {
                MethodInfo methodInfo = KaitaiUtils.toMethodInfo(m, classInfo.getClassName());
                methodInfoMap.put(String.valueOf(m.methodId()), methodInfo);
            });

            e.probeList().forEach(r -> {
                probeInfoMap.put(String.valueOf(r.dataId()),
                        KaitaiUtils.toDataInfo(r));
            });

        });

//        logger.warn("classInfoMap size: " + classInfoMap.size());
//        logger.warn("methodInfoMap size: " + methodInfoMap.size());
//        logger.warn("probeInfoMap size: " + probeInfoMap.size());


        final AtomicLong previousEventAt = new AtomicLong(-1);

        Set<Long> remainingObjectIds = new HashSet<>();

        for (File sessionArchive : sessionArchivesLocal) {


            Map<String, UploadFile> matchedFiles = new HashMap<>();
            if (objectId != -1) {
                ArchiveFilesIndex eventsIndex = getEventIndex(sessionArchive);
                if (eventsIndex == null) continue;
                if (!eventsIndex.hasValueId(objectId)) {
                    continue;
                }

                List<UploadFile> matchedFilesForString = eventsIndex.querySessionFilesByValueId(objectId);
                for (UploadFile uploadFile : matchedFilesForString) {
                    String filePath = uploadFile.getPath();
//                    logger.info("File matched for object id [" + objectId + "] -> " + uploadFile + " -> " + filePath);
                    int threadId = Integer.parseInt(Path.of(filePath).getFileName().toString().split("\\.")[0].split("-")[2]);
                    UploadFile uploadFileToAdd = new UploadFile(filePath, threadId, null, null);
                    uploadFileToAdd.setValueIds(new Long[]{objectId});
                    matchedFiles.put(filePath, uploadFile);
                }
                if (matchedFiles.size() == 0) {
                    continue;
                }
            }


            try {
                List<String> archiveFiles = new LinkedList<>();

                if (objectId != -1) {
//                    logger.info("Files were matched: " + matchedFiles);
                    String splitAt = "\\\\";

                    archiveFiles = new LinkedList<>();
                    for (String s : matchedFiles.keySet()) {
                        String[] parts;
                        if (s.contains("/")) {
                            parts = s.split("/");
                        } else {
                            parts = s.split("\\\\");
                        }
                        archiveFiles.add(parts[parts.length - 1]);
                    }

                } else {
                    archiveFiles = listArchiveFiles(sessionArchive);
                    logger.info("no files were matched, listing files from session archive ["
                            + sessionArchive.getName() + "] -> " + archiveFiles);
                }

                if (archiveFiles.size() == 0) {
                    continue;
                }


                Collections.sort(archiveFiles);
                if (pageInfo.isDesc()) {
                    Collections.reverse(archiveFiles);
                }


                for (String archiveFile : archiveFiles) {
                    checkProgressIndicator(null,
                            "Reading events from  " + archiveFile);

                    if (remaining == 0) {
                        break;
                    }

                    if (!archiveFile.endsWith(".selog")) {
                        continue;
                    }
                    logger.info("Checking file " + archiveFile + " for data");
                    final int fileThreadId = Integer.parseInt(
                            archiveFile.split("\\.")[0].split("-")[2]
                    );
                    if (filteredDataEventsRequest.getThreadId() != -1
                            && fileThreadId != filteredDataEventsRequest.getThreadId()) {
                        continue;
                    }

                    List<KaitaiInsidiousEventParser.Block> eventsSublist = getEventsFromFile(sessionArchive,
                            archiveFile);


                    if (pageInfo.isDesc()) {
                        Collections.reverse(eventsSublist);
                    }

                    KaitaiInsidiousEventParser.Block firstEvent = eventsSublist.get(0);
                    KaitaiInsidiousEventParser.Block lastEvent =
                            eventsSublist.get(eventsSublist.size() -1);

                    if (filteredDataEventsRequest.getNanotime() != -1) {
                        if (pageInfo.isAsc()) {
                            if (eventId(lastEvent) < filteredDataEventsRequest.getNanotime()) {
                                continue;
                            }
                        } else {
                            if (eventId(lastEvent) > filteredDataEventsRequest.getNanotime()) {
                                continue;
                            }
                        }
                    }


                    List<DataEventWithSessionId> dataEventGroupedList = eventsSublist
                            .stream()
                            .filter(e -> {
                                boolean isDataEvent = e.magic() == 7 || e.magic() == 4;


                                if (!isDataEvent) {
                                    return false;
                                }
                                long currentFirstEventAt = previousEventAt.get();

                                long currentEventId = -1;
                                long valueId = -1;

                                if (e.magic() == 4) {
                                    KaitaiInsidiousEventParser.DataEventBlock dataEventBlock =
                                            (KaitaiInsidiousEventParser.DataEventBlock) e.block();
                                    currentEventId = dataEventBlock.eventId();
                                    valueId = dataEventBlock.valueId();

                                } else if (e.magic() == 7) {
                                    KaitaiInsidiousEventParser.DetailedEventBlock detailedEventBlock =
                                            (KaitaiInsidiousEventParser.DetailedEventBlock) e.block();
                                    currentEventId = detailedEventBlock.eventId();
                                    valueId = detailedEventBlock.valueId();

                                }

                                if (filteredDataEventsRequest.getNanotime() != -1) {
                                    if (pageInfo.isAsc()) {
                                        if (currentEventId < filteredDataEventsRequest.getNanotime()) {
                                            return false;
                                        }
                                    } else {
                                        if (currentEventId > filteredDataEventsRequest.getNanotime()) {
                                            return false;
                                        }

                                    }
                                }

                                boolean isRequestedObject =
                                        valueId == objectId || objectId == -1;

                                if (isRequestedObject) {
                                    previousEventAt.set(currentEventId);
                                }

                                if (currentFirstEventAt != -1 &&
                                        currentEventId - currentFirstEventAt
                                                <= pageInfo.getBufferSize()) {
                                    return true;
                                }



                                return isRequestedObject;
                            })
                            .filter(e -> {
                                if (skip.get() > 0) {
                                    int remainingNow = skip.decrementAndGet();
                                    return remainingNow <= 0;
                                }
                                return true;
                            })
//                            .map(e -> (KaitaiInsidiousEventParser.DetailedEventBlock) e.block())
                            .map(e -> {
                                if (e.magic() == 4) {
                                    KaitaiInsidiousEventParser.DataEventBlock eventBlock
                                            = (KaitaiInsidiousEventParser.DataEventBlock) e.block();
                                    DataEventWithSessionId d = new DataEventWithSessionId();
                                    d.setDataId((int) eventBlock.probeId());
                                    d.setNanoTime(eventBlock.eventId());
                                    d.setRecordedAt(eventBlock.timestamp());
                                    d.setThreadId(fileThreadId);
                                    d.setValue(eventBlock.valueId());
                                    return d;
                                } else if (e.magic() == 7) {
                                    KaitaiInsidiousEventParser.DetailedEventBlock eventBlock
                                            = (KaitaiInsidiousEventParser.DetailedEventBlock) e.block();
                                    DataEventWithSessionId d = new DataEventWithSessionId();
                                    d.setDataId((int) eventBlock.probeId());
                                    d.setNanoTime(eventBlock.eventId());
                                    d.setRecordedAt(eventBlock.timestamp());
                                    d.setThreadId(fileThreadId);
                                    d.setValue(eventBlock.valueId());
                                    d.setSerializedValue(eventBlock.serializedData());
                                    return d;

                                }


                                return null;
                            }).collect(Collectors.toList());

                    if (dataEventGroupedList.size() > 0) {
                        logger.info("adding " + dataEventGroupedList.size() + " objects");
                    }
                    if (remaining < dataEventGroupedList.size()) {
                        dataEventGroupedList = dataEventGroupedList.subList(0, remaining);
                        remaining = 0;
                    } else {
                        remaining = remaining - dataEventGroupedList.size();
                    }


                    dataEventList.addAll(dataEventGroupedList);

                    if (remaining == 0) {
                        break;
                    }


                }


            } catch (IOException e) {
                logger.warn("failed to read archive [" + sessionArchive.getName() + "]");
                continue;
            }


            Set<Integer> probeIds = dataEventList.stream().map(DataEventWithSessionId::getDataId).collect(Collectors.toSet());
            Set<Long> valueIds = dataEventList.stream().map(DataEventWithSessionId::getValue).collect(Collectors.toSet());


            NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(
                    sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            assert stringsIndexBytes != null;


            ArchiveIndex stringIndex = null;
            try {
                stringIndex = readArchiveIndex(stringsIndexBytes.getBytes(), INDEX_STRING_DAT_FILE);
            } catch (IOException e) {
                logger.error("failed to read string index from session bytes: " + e.getMessage(), e);
                continue;
            }
            Map<String, StringInfo> sessionStringInfo = stringIndex
                    .getStringsById(valueIds.stream().filter(e -> e > 10).collect(Collectors.toSet()));
            stringInfoMap.putAll(sessionStringInfo);

            Set<Long> objectIds = dataEventList.stream()
                    .map(DataEventWithSessionId::getValue)
                    .filter(e -> e > 1000)
                    .collect(Collectors.toSet());

            NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                    INDEX_OBJECT_DAT_FILE.getFileName());
            assert objectIndexBytes != null;
            ArchiveIndex objectsIndex = null;
            try {
                objectsIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            } catch (IOException e) {
                logger.error("failed to read object index from session archive", e);
                continue;
            }


            if (objectId != -1) {
                objectIds.add(objectId);
            }

            Map<String, ObjectInfo> sessionObjectsInfo = objectsIndex.getObjectsByObjectId(objectIds);
            if (sessionObjectsInfo.size() != objectIds.size()) {
//                logger.warn("expected [" + objectIds.size() + "] object infos results but got " +
//                        "only " + sessionObjectsInfo.size());

                sessionObjectsInfo.values().stream()
                        .map(ObjectInfo::getObjectId)
                        .collect(Collectors.toList())
                        .forEach(objectIds::remove);
                remainingObjectIds.addAll( objectIds);
            }
            objectInfoMap.putAll(sessionObjectsInfo);


            Set<Integer> typeIds = objectInfoMap.values()
                    .stream().map(ObjectInfo::getTypeId)
                    .map(Long::intValue).collect(Collectors.toSet());

            Map<String, TypeInfo> sessionTypeInfo = typeIndex.getTypesById(typeIds);
            if (sessionTypeInfo.size() < typeIds.size()){
                logger.warn("expected [" + typeIds.size() + "] type info but got only: " + sessionTypeInfo.size());
            }

            typeInfoMap.putAll(sessionTypeInfo);

            if (remaining == 0) {
                break;
            }

        }

        // we need to go thru the archives again to load the set of object information which we
        // did not find earlier since the object was probably created earlier
        if (remainingObjectIds.size() > 0) {

            Set<Long> objectIds = remainingObjectIds;
            for (File sessionArchive : sessionArchivesLocal) {

                NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                        INDEX_OBJECT_DAT_FILE.getFileName());
                assert objectIndexBytes != null;
                ArchiveIndex objectsIndex = null;
                try {
                    objectsIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
                } catch (IOException e) {
                    logger.error("failed to read object index from session archive", e);
                    continue;
                }


                Map<String, ObjectInfo> sessionObjectsInfo = objectsIndex.getObjectsByObjectId(objectIds);
                if (sessionObjectsInfo.size() != objectIds.size()) {
//                    logger.warn("expected [" + objectIds.size() + "] results but got only " + sessionObjectsInfo.size());

                    sessionObjectsInfo.values().stream()
                            .map(ObjectInfo::getObjectId)
                            .collect(Collectors.toList())
                            .forEach(objectIds::remove);
                }
                objectInfoMap.putAll(sessionObjectsInfo);


                Set<Integer> typeIds = objectInfoMap.values()
                        .stream().map(ObjectInfo::getTypeId)
                        .map(Long::intValue).collect(Collectors.toSet());

                Map<String, TypeInfo> sessionTypeInfo = typeIndex.getTypesById(typeIds);
                if (sessionTypeInfo.size() < typeIds.size()){
                    logger.warn("expected [" + typeIds.size() + "] type info but got only: " + sessionTypeInfo.size());
                }
                typeInfoMap.putAll(sessionTypeInfo);


            }

            if (objectIds.size() > 0) {
                logger.warn("failed to find object information for: " + objectIds);
            }
        }


            return new ReplayData(
                null, filteredDataEventsRequest, dataEventList, classInfoMap, probeInfoMap,
                stringInfoMap, objectInfoMap, typeInfoMap, methodInfoMap);

    }

    private long eventId(KaitaiInsidiousEventParser.Block lastEvent) {
        if (lastEvent.magic() == 4) {
            KaitaiInsidiousEventParser.DataEventBlock eventBlock
                    = (KaitaiInsidiousEventParser.DataEventBlock) lastEvent.block();
            return eventBlock.eventId();
        }
        if (lastEvent.magic() == 7) {
            KaitaiInsidiousEventParser.DetailedEventBlock eventBlock
                    = (KaitaiInsidiousEventParser.DetailedEventBlock) lastEvent.block();
            return eventBlock.eventId();
        }
        return 0;
    }

    private Collection<ObjectInfo> getObjectInfoById(Collection<Long> valueIds) {

        Set<ObjectInfo> objectInfoCollection = new HashSet<>();

        for (File sessionArchive : this.sessionArchives) {

            NameWithBytes objectsIndexBytes = createFileOnDiskFromSessionArchiveFile(
                    sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            if (objectsIndexBytes == null) {
                continue;
            }


            checkProgressIndicator(null, "Loading objects from " + sessionArchive.getName());
            ArchiveIndex objectIndex = null;
            try {
                objectIndex = readArchiveIndex(objectsIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            } catch (IOException e) {
                logger.error("failed to read object index from session bytes: " + e.getMessage(), e);
                continue;
            }
            Map<String, ObjectInfo> sessionObjectInfo = objectIndex.getObjectsByObjectId(valueIds);
            objectInfoCollection.addAll(sessionObjectInfo.values());

        }


        return objectInfoCollection;
    }

    public void queryTracePointsByTypes(SearchQuery searchQuery, ClientCallBack<TracePoint> clientCallBack) {

        int totalCount = sessionArchives.size();
        int currentCount = 0;
        int totalMatched = 0;

        for (File sessionArchive : sessionArchives) {
            currentCount++;

            checkProgressIndicator("Checking archive " + sessionArchive.getName(), null);

            Collection<Long> objectIds =
                    queryObjectsByTypeFromSessionArchive(searchQuery, sessionArchive)
                            .stream()
                            .map(ObjectInfoDocument::getObjectId)
                            .collect(Collectors.toSet());

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }


            if (objectIds.size() > 0) {
                List<TracePoint> tracePointsByValueIds = getTracePointsByValueIds(sessionArchive,
                        Set.copyOf(objectIds));
                tracePointsByValueIds.forEach(e -> e.setExecutionSession(executionSession));
                clientCallBack.success(tracePointsByValueIds);
                totalMatched += tracePointsByValueIds.size();

                checkProgressIndicator("Matched " + totalMatched + " events", null);
            }

        }
        clientCallBack.completed();
    }

    public void queryTracePointsByValue(SearchQuery searchQuery, ClientCallBack<TracePoint> getProjectSessionErrorsCallback) {
        checkProgressIndicator("Searching locally by value [" + searchQuery.getQuery() + "]", null);
        Collection<TracePoint> tracePointList = new LinkedList<>();

        tracePointList = queryTracePointsByValue(
                searchQuery
        );

        getProjectSessionErrorsCallback.success(tracePointList);

    }
}
