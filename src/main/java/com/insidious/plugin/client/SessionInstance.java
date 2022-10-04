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
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.DatabaseVariableContainer;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.routine.ObjectRoutineContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.pojo.dao.ProbeInfo;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.util.text.Strings;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.squareup.javapoet.ClassName;
import io.kaitai.struct.ByteBufferKaitaiStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
    private final Map<String, String> cacheEntries = new HashMap<>();
    private KaitaiInsidiousClassWeaveParser classWeaveInfo;
    private ArchiveIndex typeIndex;
    private Map<Long, DataInfo> probeInfoMap;
    private Map<Long, ClassInfo> classInfoMap;
    private Map<Long, MethodInfo> methodInfoMap;

    public SessionInstance(File sessionDirectory, ExecutionSession executionSession) {
        this.sessionDirectory = sessionDirectory;
        this.executionSession = executionSession;
        this.sessionArchives = refreshSessionArchivesList();

    }

    private static int getThreadIdFromFileName(String archiveFile) {
        if (archiveFile.contains("\\")) {
            archiveFile = archiveFile.substring(archiveFile.lastIndexOf("\\") + 1);
        }

        if (archiveFile.contains("/")) {
            archiveFile = archiveFile.substring(archiveFile.lastIndexOf("/") + 1);
        }

        return Integer.parseInt(archiveFile.split("\\.")[0].split("-")[2]);
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


        probeInfoMap = new HashMap<>();
        classInfoMap = new HashMap<>();
        methodInfoMap = new HashMap<>();

        classWeaveInfo.classInfo().forEach(e -> {

            checkProgressIndicator(null, "Loading class: " + e.className());

            ClassInfo classInfo = KaitaiUtils.toClassInfo(e);
            classInfoMap.put(e.classId(), classInfo);

            checkProgressIndicator(null, "Loading " + e.probeCount() + " probes in class: " + e.className());

            e.methodList().forEach(m -> {
                MethodInfo methodInfo = KaitaiUtils.toMethodInfo(m, classInfo.getClassName());
                methodInfoMap.put(m.methodId(), methodInfo);
            });

            e.probeList().forEach(r -> {
                probeInfoMap.put(r.dataId(), KaitaiUtils.toDataInfo(r));
            });

        });


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
                    int threadId = getThreadIdFromFileName(Path.of(filePath).getFileName().toString());
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

    private List<DataInfo> getProbeInfo(File sessionFile, Set<Long> dataId) throws IOException {


        return classWeaveInfo.classInfo().stream()
                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
                .flatMap(Collection::stream)
                .filter(e -> dataId.contains(e.dataId()))
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
                .filter(e -> ids.contains(e.block().valueId()))
                .map(e -> {
                    long valueId = e.block().valueId();
                    int probeId = Math.toIntExact(e.block().probeId());
                    long eventId = e.block().eventId();
                    long timestamp = e.block().timestamp();

                    DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                    dataEvent.setDataId(probeId);
                    dataEvent.setValue(valueId);
                    dataEvent.setNanoTime(eventId);
                    dataEvent.setRecordedAt(timestamp);
                    return dataEvent;
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
        if (result != null) {
            return result;
        }

        return new TypeInfo("local", -1, name, "",
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

        if (!path.toFile().exists()) {
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
                classInfoMap.put(classInfo.classId(), KaitaiUtils.toClassInfo(classInfo));
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
                    logger.info(pathName + " file from "
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
                    int threadId = getThreadIdFromFileName(Path.of(filePath).getFileName().toString());
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
                int threadId = getThreadIdFromFileName(Path.of(filePath).getFileName().toString());
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
                .filter(e -> ids.contains((int) e.block().probeId()))
                .map(e -> {
                    long valueId = e.block().valueId();
                    int probeId = Math.toIntExact(e.block().probeId());
                    long eventId = e.block().eventId();
                    long timestamp = e.block().timestamp();

                    DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                    dataEvent.setDataId(probeId);
                    dataEvent.setValue(valueId);
                    dataEvent.setNanoTime(eventId);
                    dataEvent.setRecordedAt(timestamp);
                    return dataEvent;


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
                .entries().stream()
                .map(e -> {
                    long valueId = e.block().valueId();
                    int probeId = Math.toIntExact(e.block().probeId());
                    long eventId = e.block().eventId();
                    long timestamp = e.block().timestamp();

                    DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                    dataEvent.setDataId(probeId);
                    dataEvent.setValue(valueId);
                    dataEvent.setNanoTime(eventId);
                    dataEvent.setRecordedAt(timestamp);
                    return dataEvent;
                }).collect(Collectors.toList());

        Collections.reverse(dataEventList);


        Map<Long, ClassInfo> classInfo = new HashMap<>();
        Map<Long, DataInfo> dataInfo = new HashMap<>();

        checkProgressIndicator(null, "Loading class mappings");

        classWeaveInfo.classInfo().forEach(e -> {

            checkProgressIndicator(null, "Loading class: " + e.className());

            classInfo.put(e.classId(), KaitaiUtils.toClassInfo(e));

            checkProgressIndicator(null, "Loading " + e.probeCount() + " probes in class: " + e.className());

            e.probeList().forEach(r -> {
                dataInfo.put(r.dataId(), KaitaiUtils.toDataInfo(r));
            });
        });

        Set<Long> probeIds = dataEventList.stream().map(DataEventWithSessionId::getDataId).collect(Collectors.toSet());
        Set<Long> valueIds = dataEventList.stream().map(DataEventWithSessionId::getValue).collect(Collectors.toSet());


        Map<Long, StringInfo> stringInfo = new HashMap<>();
        Map<Long, ObjectInfo> objectInfo = new HashMap<>();
        Map<Long, TypeInfo> typeInfo = new HashMap<>();
        Map<Long, MethodInfo> methodInfoMap = new HashMap<>();

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
            Map<Long, ObjectInfo> sessionObjectInfo = objectIndex.getObjectsByObjectIdWithLongKeys(valueIds);
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
            Map<Long, StringInfo> sessionStringInfo = stringIndex.getStringsByIdWithLongKeys(valueIds.stream().filter(e -> e > 10).collect(Collectors.toSet()));
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
            Map<Long, TypeInfo> sessionTypeInfo = typeIndex.getTypesByIdWithLongKeys(typeIds);
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
        String query = (String) searchQuery.getQuery();

        Query<TypeInfoDocument> typeQuery = startsWith(TypeInfoDocument.TYPE_NAME, query);
        if (query.endsWith("*")) {
            typeQuery = startsWith(TypeInfoDocument.TYPE_NAME, query.substring(0,
                    query.length() - 1));
        }

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


        return eventsContainer.event().entries();
    }

    public ReplayData fetchObjectHistoryByObjectId(FilteredDataEventsRequest filteredDataEventsRequest) {

        List<DataEventWithSessionId> dataEventList = new LinkedList<>();
        Map<Long, StringInfo> stringInfoMap = new HashMap<>();
        Map<Long, ObjectInfo> objectInfoMap = new HashMap<>();
        Map<Long, TypeInfo> typeInfoMap = new HashMap<>();


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


//        logger.warn("classInfoMap size: " + classInfoMap.size());
//        logger.warn("methodInfoMap size: " + methodInfoMap.size());
//        logger.warn("probeInfoMap size: " + probeInfoMap.size());


        final AtomicLong previousEventAt = new AtomicLong(-1);

        Set<Long> remainingObjectIds = new HashSet<>();
        Set<Long> remainingStringIds = new HashSet<>();

        Map<String, SELogFileMetadata> fileEventIdPairs = new HashMap();

        for (File sessionArchive : sessionArchivesLocal) {
            logger.warn("open archive [" + sessionArchive.getName() + "]");


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
                    logger.info("File matched for object id [" + objectId + "] -> " + uploadFile + " -> " + filePath);
                    int threadId = getThreadIdFromFileName(Path.of(filePath).getFileName().toString());
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
                    checkProgressIndicator(null, "Reading events from  " + archiveFile);

                    if (remaining == 0) {
                        break;
                    }

                    if (!archiveFile.endsWith(".selog")) {
                        continue;
                    }

                    logger.warn("loading next file: " + archiveFile + " need [" + remaining + "] more events");

                    SELogFileMetadata metadata = fileEventIdPairs.get(archiveFile);
                    List<KaitaiInsidiousEventParser.Block> eventsSublist = null;

                    logger.info("Checking file " + archiveFile + " for data");
                    final int fileThreadId = getThreadIdFromFileName(archiveFile);

                    if (metadata == null && filteredDataEventsRequest.getNanotime() != -1) {

                        if (filteredDataEventsRequest.getThreadId() != -1 && fileThreadId != filteredDataEventsRequest.getThreadId()) {
                            continue;
                        }

                        eventsSublist = getEventsFromFile(sessionArchive, archiveFile);

                        KaitaiInsidiousEventParser.Block firstEvent = eventsSublist.get(0);
                        KaitaiInsidiousEventParser.Block lastEvent = eventsSublist.get(eventsSublist.size() - 1);

                        metadata = new SELogFileMetadata(eventId(firstEvent), eventId(lastEvent), fileThreadId);
                        fileEventIdPairs.put(archiveFile, metadata);


                    }

                    if (filteredDataEventsRequest.getNanotime() != -1) {
                        if (pageInfo.isAsc()) {
                            if (metadata.getLastEventId() < filteredDataEventsRequest.getNanotime()) {
                                continue;
                            }
                        } else {
                            if (metadata.getFirstEventId() > filteredDataEventsRequest.getNanotime()) {
                                continue;
                            }
                        }
                    }

                    if (eventsSublist == null) {
                        eventsSublist = getEventsFromFile(sessionArchive, archiveFile);
                        if (eventsSublist.size() == 0) {
                            continue;
                        }

                        KaitaiInsidiousEventParser.Block firstEvent = eventsSublist.get(0);
                        KaitaiInsidiousEventParser.Block lastEvent = eventsSublist.get(eventsSublist.size() - 1);

                        metadata = new SELogFileMetadata(eventId(firstEvent), eventId(lastEvent),
                                fileThreadId);
                        fileEventIdPairs.put(archiveFile, metadata);

                    }


                    if (pageInfo.isDesc()) {
                        Collections.reverse(eventsSublist);
                    }


                    SELogFileMetadata finalMetadata = metadata;
                    List<DataEventWithSessionId> dataEventGroupedList = eventsSublist
                            .stream()
                            .filter(e -> {
                                long currentFirstEventAt = previousEventAt.get();

                                long currentEventId = -1;
                                long valueId = -1;

                                KaitaiInsidiousEventParser.DetailedEventBlock detailedEventBlock = e.block();
                                currentEventId = detailedEventBlock.eventId();
                                valueId = detailedEventBlock.valueId();


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

                                boolean isRequestedObject = valueId == objectId || objectId == -1;

                                if (isRequestedObject) {
                                    previousEventAt.set(currentEventId);
                                }

                                if (currentFirstEventAt != -1 && currentEventId - currentFirstEventAt <= pageInfo.getBufferSize()) {
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
                                KaitaiInsidiousEventParser.DetailedEventBlock eventBlock
                                        = (KaitaiInsidiousEventParser.DetailedEventBlock) e.block();
                                DataEventWithSessionId d = new DataEventWithSessionId();
                                d.setDataId((int) eventBlock.probeId());
                                d.setNanoTime(eventBlock.eventId());
                                d.setRecordedAt(eventBlock.timestamp());
                                d.setThreadId(finalMetadata.getThreadId());
                                d.setValue(eventBlock.valueId());
                                d.setSerializedValue(eventBlock.serializedData());
                                return d;

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
            if (dataEventList.size() == 0) {
                continue;
            }


            Set<Long> probeIds = dataEventList.stream().map(DataEventWithSessionId::getDataId).collect(Collectors.toSet());
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
            Set<Long> potentialStringIds = valueIds.stream().filter(e -> e > 10).collect(Collectors.toSet());
            Map<Long, StringInfo> sessionStringInfo = stringIndex.getStringsByIdWithLongKeys(potentialStringIds);
            if (potentialStringIds.size() != sessionStringInfo.size()) {

                sessionStringInfo.values().stream().map(StringInfo::getStringId).collect(Collectors.toList()).forEach(potentialStringIds::remove);


                remainingStringIds.addAll(potentialStringIds);
            }
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

            Map<Long, ObjectInfo> sessionObjectsInfo = objectsIndex.getObjectsByObjectIdWithLongKeys(objectIds);
            if (sessionObjectsInfo.size() != objectIds.size()) {
//                logger.warn("expected [" + objectIds.size() + "] object infos results but got " +
//                        "only " + sessionObjectsInfo.size());

                sessionObjectsInfo.values().stream()
                        .map(ObjectInfo::getObjectId)
                        .collect(Collectors.toList())
                        .forEach(objectIds::remove);
                remainingObjectIds.addAll(objectIds);
            }
            objectInfoMap.putAll(sessionObjectsInfo);


            Set<Integer> typeIds = objectInfoMap.values()
                    .stream().map(ObjectInfo::getTypeId)
                    .map(Long::intValue).collect(Collectors.toSet());

            Map<Long, TypeInfo> sessionTypeInfo = typeIndex.getTypesByIdWithLongKeys(typeIds);
            if (sessionTypeInfo.size() < typeIds.size()) {
                logger.warn("expected [" + typeIds.size() + "] type info but got only: " + sessionTypeInfo.size());
            }

            typeInfoMap.putAll(sessionTypeInfo);

            if (remaining == 0) {
                break;
            }

        }

        // we need to go thru the archives again to load the set of object information which we
        // did not find earlier since the object was probably created earlier
        if (remainingObjectIds.size() > 0 || remainingStringIds.size() > 0) {

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


                Map<Long, ObjectInfo> sessionObjectsInfo = objectsIndex.getObjectsByObjectIdWithLongKeys(objectIds);
                if (sessionObjectsInfo.size() > 0) {
//                    logger.warn("expected [" + objectIds.size() + "] results but got only " + sessionObjectsInfo.size());

                    sessionObjectsInfo.values().stream().map(ObjectInfo::getObjectId).collect(Collectors.toList()).forEach(objectIds::remove);
                }
                objectInfoMap.putAll(sessionObjectsInfo);


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
                Map<Long, StringInfo> sessionStringInfo = stringIndex.getStringsByIdWithLongKeys(remainingStringIds);
                if (remainingStringIds.size() != sessionStringInfo.size()) {

                    sessionStringInfo.values().stream()
                            .map(StringInfo::getStringId)
                            .collect(Collectors.toList())
                            .forEach(remainingStringIds::remove);
                }

                stringInfoMap.putAll(sessionStringInfo);


                Set<Integer> typeIds = objectInfoMap.values()
                        .stream().map(ObjectInfo::getTypeId)
                        .map(Long::intValue).collect(Collectors.toSet());

                Map<Long, TypeInfo> sessionTypeInfo = typeIndex.getTypesByIdWithLongKeys(typeIds);
                if (sessionTypeInfo.size() < typeIds.size()) {
                    logger.warn("expected [" + typeIds.size() + "] type info but got only: " + sessionTypeInfo.size());
                }
                typeInfoMap.putAll(sessionTypeInfo);


            }


        }


        return new ReplayData(null, filteredDataEventsRequest, dataEventList, classInfoMap, probeInfoMap, stringInfoMap, objectInfoMap, typeInfoMap, methodInfoMap);

    }

    public Collection<ObjectRoutineContainer> scanDataAndBuildReplay(FilteredDataEventsRequest request) throws Exception {

        File dbFile = new File("execution.db");
        boolean dbFileExists = dbFile.exists();
        String databaseUrl = "jdbc:sqlite:execution.db";
        ConnectionSource connectionSource = new JdbcConnectionSource(databaseUrl);

        DaoService daoService = new DaoService(connectionSource);

        // if you need to create the 'accounts' table make this call
        if (!dbFileExists) {
            try {
                TableUtils.createTable(connectionSource, com.insidious.plugin.pojo.dao.TestCandidateMetadata.class);
                TableUtils.createTable(connectionSource, com.insidious.plugin.pojo.dao.MethodCallExpression.class);
                TableUtils.createTable(connectionSource, com.insidious.plugin.pojo.dao.Parameter.class);
                TableUtils.createTable(connectionSource, ProbeInfo.class);
                TableUtils.createTable(connectionSource, DataEventWithSessionId.class);
            } catch (SQLException sqlException) {
                logger.warn("probably table already exists: " + sqlException.toString());
            }
        }


        LinkedList<File> sessionArchivesLocal = new LinkedList<>(this.sessionArchives);

        assert classInfoMap.size() > 0;
        assert probeInfoMap.size() > 0;
        assert methodInfoMap.size() > 0;

        List<TestCandidateMetadata> testCandidateMetadataStack = new LinkedList<>();
        List<TestCandidateMetadata> testCandidateMetadataList = new LinkedList<>();
        Map<Long, ObjectRoutineContainer> objectRoutineContainerMap = new HashMap<>();

        Collections.sort(sessionArchivesLocal);


        checkProgressIndicator(null, "Loading class mappings");
//        VariableContainer variableContainer = new VariableContainer();

        List<MethodCallExpression> callStack = new LinkedList<>();
        AtomicReference<MethodCallExpression> mostRecentCall = new AtomicReference<>();
        AtomicInteger index = new AtomicInteger(0);
        List<Long> valueStack = new LinkedList<>();

        Map<String, VariableContainer> classStaticFieldMap = new HashMap<>();

        File output = new File(String.format("output%d.txt", request.getThreadId()));
        FileOutputStream outputWriter = new FileOutputStream(output);
        BufferedOutputStream outputStream = new BufferedOutputStream(outputWriter);


        List<ArchiveIndex> objectsIndexList = new LinkedList<>();
        for (File sessionArchive : sessionArchivesLocal) {
            logger.warn("open archive [" + sessionArchive.getName() + "]");

            List<String> archiveFiles = new LinkedList<>();

            archiveFiles = listArchiveFiles(sessionArchive);

            if (archiveFiles.size() == 0) {
                continue;
            }


            NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            assert stringsIndexBytes != null;
            ArchiveIndex stringIndex = readArchiveIndex(stringsIndexBytes.getBytes(), INDEX_STRING_DAT_FILE);

            NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            assert objectIndexBytes != null;
            ArchiveIndex objectsIndex1 = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            objectsIndexList.add(objectsIndex1);

            Collections.sort(archiveFiles);

            DatabaseVariableContainer parameterContainer = new DatabaseVariableContainer(daoService);

            List<Integer> existingProbes = daoService.getProbes();

            for (String archiveFile : archiveFiles) {
                checkProgressIndicator(null, "Reading events from  " + archiveFile);

                if (!archiveFile.endsWith(".selog")) {
                    continue;
                }
                final int fileThreadId = getThreadIdFromFileName(archiveFile);
                if (fileThreadId != request.getThreadId()) {
                    continue;
                }

                logger.warn("loading next file: [" + archiveFile + "]");


                logger.info("Checking file " + archiveFile + " for data");


                List<KaitaiInsidiousEventParser.Block> eventsSublist = getEventsFromFile(sessionArchive, archiveFile);
                if (eventsSublist.size() == 0) {
                    continue;
                }

                List<DataEventWithSessionId> eventsToSave = new LinkedList<>();
                List<DataInfo> probesToSave = new LinkedList<>();

                for (KaitaiInsidiousEventParser.Block e : eventsSublist) {

                    KaitaiInsidiousEventParser.DetailedEventBlock eventBlock = e.block();
                    DataEventWithSessionId dataEvent = new DataEventWithSessionId(fileThreadId);
                    dataEvent.setDataId((int) eventBlock.probeId());
                    dataEvent.setNanoTime(eventBlock.eventId());
                    dataEvent.setRecordedAt(eventBlock.timestamp());
                    dataEvent.setValue(eventBlock.valueId());
                    dataEvent.setSerializedValue(eventBlock.serializedData());


                    final long probeId = dataEvent.getDataId();
                    final long eventValue = dataEvent.getValue();

                    DataInfo probeInfo = probeInfoMap.get(probeId);
                    ClassInfo classInfo = classInfoMap.get((long) probeInfo.getClassId());
                    MethodInfo methodInfo = methodInfoMap.get((long) probeInfo.getMethodId());
                    int instructionIndex = index.getAndIncrement();
                    LoggerUtil.logEvent("SCAN", callStack.size(), instructionIndex, dataEvent, probeInfo, classInfo, methodInfo);

                    Parameter existingParameter = null;
                    String fieldType = ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V"));
                    String nameFromProbe = probeInfo.getAttribute("Name", probeInfo.getAttribute("FieldName", null));
                    String callType = probeInfo.getAttribute("CallType", null);
                    String owner = ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Owner", null));
                    boolean saveProbe = false;

                    switch (probeInfo.getEventType()) {

                        case LABEL:
                            // nothing to do
                            break;
                        case LINE_NUMBER:
                            // we always have this information in the probeInfo
                            // nothing to do
                            break;

                        case LOCAL_STORE:

                            existingParameter = parameterContainer.getParameterByValue(eventValue);
                            if (existingParameter != null) {
                                existingParameter.addName(nameFromProbe);
                            }

                            break;

                        case LOCAL_LOAD:
                            existingParameter = parameterContainer.getParameterByValue(eventValue);

                            existingParameter.addName(nameFromProbe);
                            existingParameter.setType(
                                    ClassTypeUtils.getDottedClassName(
                                            fieldType
                                    )
                            );
                            saveProbe = true;


                            existingParameter.setProb(dataEvent);
                            existingParameter.setProbeInfo(probeInfo);
                            TestCandidateMetadata currentTestCandidate = testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1);
                            VariableContainer candidateVariables = currentTestCandidate.getVariables();
                            candidateVariables.add(existingParameter);

                            break;

                        case GET_STATIC_FIELD:
                            existingParameter = parameterContainer.getParameterByValue(eventValue);
                            if (existingParameter != null) {
                                existingParameter.addName(nameFromProbe);

                            } else {
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                existingParameter.addName(nameFromProbe);
                                existingParameter.setType(
                                        ClassTypeUtils.getDottedClassName(
                                                fieldType
                                        )
                                );
                                existingParameter.setProb(dataEvent);
                                saveProbe = true;
                                existingParameter.setProbeInfo(probeInfo);
                                currentTestCandidate = testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1);
                                candidateVariables = currentTestCandidate.getVariables();
                                candidateVariables.add(existingParameter);

                            }

                            break;

                        case GET_INSTANCE_FIELD_RESULT:
                            existingParameter = parameterContainer.getParameterByValue(eventValue);
                            if (existingParameter.getProb() == null) {
                                continue;
                            }
                            existingParameter.addName(nameFromProbe);
                            testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1).getFields().add(existingParameter);
                            break;

                        case PUT_INSTANCE_FIELD:


                            // we are going to set this field in the next event
                            valueStack.add(eventValue);
                            existingParameter = parameterContainer.getParameterByValue(eventValue);
                            if (existingParameter != null && existingParameter.getProb() != null) {
                                if (existingParameter.getType() == null || existingParameter.getType().contains(".Object")) {
                                    existingParameter.setType(ClassTypeUtils.getDottedClassName(owner));
                                }
                            } else {
                                // new variable identified ?
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                existingParameter.setProb(dataEvent);
                                existingParameter.setProbeInfo(probeInfo);
                                existingParameter.setType(ClassTypeUtils.getDottedClassName(fieldType));
                                existingParameter.addName(nameFromProbe);
                            }

                            break;

                        case PUT_INSTANCE_FIELD_VALUE:


                            Long parentValue = valueStack.remove(valueStack.size() - 1);
                            Parameter valueParameter = parameterContainer.getParameterByValue(parentValue);
                            VariableContainer parentFields = valueParameter.getFields();


                            existingParameter = parentFields.getParametersById(eventValue);
                            if (existingParameter != null) {
                                existingParameter.addName(nameFromProbe);
                            } else {
                                // new field
                                Parameter newField = parameterContainer.getParameterByValue(eventValue);
                                newField.setType(ClassTypeUtils.getDottedClassName(fieldType));
                                newField.addName(nameFromProbe);
                                newField.setProb(dataEvent);
                                saveProbe = true;

                                newField.setProbeInfo(probeInfo);
                                parentFields.add(newField);
                            }
                            break;


                        case PUT_STATIC_FIELD:


                            VariableContainer classStaticFieldContainer = classStaticFieldMap.getOrDefault(owner, new VariableContainer());

                            existingParameter = classStaticFieldContainer.getParametersById(eventValue);
                            if (existingParameter != null) {
                                // field is already present and we are overwriting it here
                                // how to keep track of this ?

                            } else {
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                if (existingParameter.getProb() == null) {
                                    // we are coming across this field for the first time
                                    existingParameter = parameterContainer.getParameterByValue(eventValue);
                                    existingParameter.addName(nameFromProbe);
                                    existingParameter.setType(fieldType);
                                    existingParameter.setProb(dataEvent);
                                    saveProbe = true;

                                    existingParameter.setProbeInfo(probeInfo);

                                    classStaticFieldContainer.add(existingParameter);

                                } else {
                                    existingParameter.addName(nameFromProbe);
                                    existingParameter.setType(fieldType);
                                    classStaticFieldContainer.add(existingParameter);
                                }
                            }

                            break;

                        case PUT_INSTANCE_FIELD_BEFORE_INITIALIZATION:

                            break;

                        case GET_INSTANCE_FIELD:

                            break;

                        case CALL:

                            existingParameter = parameterContainer.getParameterByValue(eventValue);
                            saveProbe = true;

                            if (existingParameter.getProbeInfo() == null) {
                                existingParameter.setProbeInfo(probeInfo);
                                existingParameter.setProb(dataEvent);
                            }
                            existingParameter.setType(owner);


                            MethodCallExpression methodCallExpression = new MethodCallExpression(
                                    nameFromProbe, existingParameter, new VariableContainer(), null, callStack.size()
                            );
                            methodCallExpression.setEntryProbeInfo(probeInfo);
                            methodCallExpression.setEntryProbe(dataEvent);

                            if (callType.equals("Static")) {
                                methodCallExpression.setStaticCall(true);
                            }


                            callStack.add(methodCallExpression);
                            mostRecentCall.set(methodCallExpression);


//                                        variableContainer.add(subjectParameter);


                            break;


                        case CALL_PARAM:
                            existingParameter = parameterContainer.getParameterByValue(eventValue);


                            existingParameter.setProbeInfo(probeInfo);
                            existingParameter.setType(ClassTypeUtils.getDottedClassName(fieldType));
                            existingParameter.setProb(dataEvent);

                            saveProbe = true;


                            MethodCallExpression currentMethodCallExpression = callStack.get(callStack.size() - 1);
                            currentMethodCallExpression.getArguments().add(existingParameter);
                            break;

                        case METHOD_ENTRY:


                            MethodCallExpression methodCall = null;
                            if (callStack.size() > 0) {
                                methodCall = callStack.get(callStack.size() - 1);
                                @NotNull String expectedClassName = ClassTypeUtils.getDottedClassName(methodInfo.getClassName());
                                if (owner == null) {
                                    owner = methodCall.getSubject().getType();
                                }
                                if (owner == null) {
                                    methodCall = null;
                                } else {
                                    // sometimes we can enter a method_entry without a call
                                    @NotNull String actualClassName = ClassTypeUtils.getDottedClassName(owner);
                                    if (!actualClassName.startsWith(expectedClassName) ||
                                            !methodInfo.getMethodName().equals(methodCall.getMethodName())) {
                                        methodCall = null;
                                    }
                                }
                            }

                            TestCandidateMetadata newCandidate = new TestCandidateMetadata();

                            testCandidateMetadataStack.add(newCandidate);
                            newCandidate.setEntryProbeIndex(instructionIndex);


                            if (methodCall != null) {
                                newCandidate.setMainMethod(methodCall);
                            } else {
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                if (eventValue != 0) {
                                    existingParameter.setProb(dataEvent);

                                    existingParameter.setProbeInfo(probeInfo);
                                    if (owner != null) {
                                        existingParameter.setType(ClassTypeUtils.getDottedClassName(owner));
                                    } else {
                                        existingParameter.setType(
                                                ClassTypeUtils.getDottedClassName(
                                                        methodInfo.getClassName()
                                                )
                                        );
                                    }
                                }


                                methodCall = new MethodCallExpression(
                                        methodInfo.getMethodName(), existingParameter, new VariableContainer(), null,
                                        callStack.size());

                                saveProbe = true;

                                methodCall.setEntryProbeInfo(probeInfo);
                                methodCall.setEntryProbe(dataEvent);
                                newCandidate.setMainMethod(methodCall);
                                mostRecentCall.set(methodCall);
                                callStack.add(methodCall);

//                                            currentMethodCall.set(methodCall);
                            }


                            break;


                        case METHOD_PARAM:

                            // if the caller was probed then we already have the method arguments
                            // in that case we can verify here
                            // else if the caller was a third party, then we need to extract parameters from here

                            existingParameter = parameterContainer.getParameterByValue(eventValue);
                            existingParameter.setProb(dataEvent);

                            saveProbe = true;

                            existingParameter.setProbeInfo(probeInfo);

                            MethodCallExpression methodExpression = callStack.get(callStack.size() - 1);

                            EventType entryProbeEventType = methodExpression.getEntryProbeInfo().getEventType();
                            if (entryProbeEventType == EventType.CALL) {

                                methodExpression.getArguments().getParametersById(existingParameter.getProb().getValue());

                            } else if (entryProbeEventType == EventType.METHOD_ENTRY) {
                                // oo la la
                                methodExpression.getArguments().add(existingParameter);
                            } else {
                                throw new RuntimeException("unexpected entry probe event type");
                            }
                            break;


                        case METHOD_EXCEPTIONAL_EXIT:

                            MethodCallExpression exceptionCallExpression = callStack.get(callStack.size() - 1);

                            entryProbeEventType = exceptionCallExpression.getEntryProbeInfo().getEventType();

                            existingParameter = parameterContainer.getParameterByValue(eventValue);

                            saveProbe = true;

                            existingParameter.setProbeInfo(probeInfo);
                            existingParameter.setProb(dataEvent);


                            if (entryProbeEventType == EventType.CALL) {
                                // we need to pop two calls here, since the CALL will not have a matching call_return

                                MethodCallExpression topCall = callStack.remove(callStack.size() - 1);
                                topCall.setReturnValue(existingParameter);
                                daoService.createOrUpdateCall(topCall);


                                topCall = callStack.remove(callStack.size() - 1);
                                topCall.setReturnValue(existingParameter);
                                daoService.createOrUpdateCall(topCall);


                            } else if (entryProbeEventType == EventType.METHOD_ENTRY) {
                                // we need to pop only 1 call here from the stack
                                MethodCallExpression topCall = callStack.remove(callStack.size() - 1);
                                topCall.setReturnValue(existingParameter);
                                daoService.createOrUpdateCall(topCall);

                                // also the test candidate metadata need to be finished
//                                                    TestCandidateMetadata currentCandidate = testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1);
//                                                    currentCandidate.setMainMethod(topCall);

                            } else {
                                throw new RuntimeException("unexpected entry probe event type [" + entryProbeEventType + "]");
                            }


                            TestCandidateMetadata completedExceptional = testCandidateMetadataStack.remove(testCandidateMetadataStack.size() - 1);
                            if (testCandidateMetadataStack.size() > 0) {
                                TestCandidateMetadata newCurrent = testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1);
                                newCurrent.getCallsList().addAll(completedExceptional.getCallsList());
                            } else {
                                if (callStack.size() > 0) {
                                    logger.warn("inconsistent call stack state, flushing calls list");
                                    callStack.clear();
                                }
                            }
                            completedExceptional.setExitProbeIndex(index.get());
                            if (completedExceptional.getMainMethod() != null) {
                                DataEventWithSessionId entryProbe = ((MethodCallExpression) (completedExceptional.getMainMethod())).getEntryProbe();
                                if (entryProbe != null) {
                                    completedExceptional.setCallTimeNanoSecond(
                                            dataEvent.getRecordedAt() - entryProbe.getRecordedAt()
                                    );
                                }
                            }
                            if (completedExceptional.getMainMethod() != null) {
                                completedExceptional.setTestSubject(((MethodCallExpression) completedExceptional.getMainMethod()).getSubject());
                            }
//                                        testCandidateMetadataList.add(completed);
                            try {
                                if (completedExceptional.getTestSubject() != null) {
                                    writeCandidate(completedExceptional, outputStream);
                                    daoService.createOrUpdateTestCandidate(completedExceptional);
                                }
                                outputStream.flush();
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            break;

                        case METHOD_NORMAL_EXIT:


                            MethodCallExpression currentCallExpression = callStack.get(callStack.size() - 1);

                            entryProbeEventType = currentCallExpression.getEntryProbeInfo().getEventType();

                            existingParameter = parameterContainer.getParameterByValue(eventValue);

                            saveProbe = true;

                            existingParameter.setProbeInfo(probeInfo);
                            existingParameter.setProb(dataEvent);


                            if (entryProbeEventType == EventType.CALL) {
                                // we dont pop it here, wait for the CALL_RETURN to pop the call


                            } else if (entryProbeEventType == EventType.METHOD_ENTRY ||
                                    probeInfo.getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT) {
                                // we can pop the current call here since we never had the CALL event in the first place
                                // this might be going out of our hands
                                MethodCallExpression topCall = callStack.remove(callStack.size() - 1);


                                if (topCall.getMethodName().startsWith("<")) {
                                    topCall.setReturnValue(topCall.getSubject());

                                } else {
                                    topCall.setReturnValue(existingParameter);
                                }
                                daoService.createOrUpdateCall(topCall);

                                // also the test candidate metadata need to be finished
//                                                    TestCandidateMetadata currentCandidate = testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1);
//                                                    currentCandidate.setMainMethod(topCall);

                            } else {
                                throw new RuntimeException("unexpected entry probe event type [" + entryProbeEventType + "]");
                            }


                            TestCandidateMetadata completed = testCandidateMetadataStack.remove(testCandidateMetadataStack.size() - 1);
                            if (testCandidateMetadataStack.size() > 0) {
                                TestCandidateMetadata newCurrent = testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1);
                                newCurrent.getCallsList().addAll(completed.getCallsList());
                            } else {
                                if (callStack.size() > 0) {
                                    logger.warn("inconsistent call stack state, flushing calls list");
                                    callStack.clear();
                                }
                            }
                            completed.setExitProbeIndex(index.get());
                            if (completed.getMainMethod() != null) {
                                DataEventWithSessionId entryProbe = ((MethodCallExpression) (completed.getMainMethod())).getEntryProbe();
                                if (entryProbe != null) {
                                    completed.setCallTimeNanoSecond(
                                            dataEvent.getRecordedAt() - entryProbe.getRecordedAt()
                                    );
                                }
                            }
                            if (completed.getMainMethod() != null) {
                                completed.setTestSubject(((MethodCallExpression) completed.getMainMethod()).getSubject());
                            }

                            try {
                                if (completed.getTestSubject() != null) {
                                    writeCandidate(completed, outputStream);
                                    daoService.createOrUpdateTestCandidate(completed);
                                }
                                outputStream.flush();
                            } catch (IOException e1) {
                                //
                            } catch (SQLException e2) {
                                e2.printStackTrace();
//                                                    throw new RuntimeException(e);
                            }

                            break;

                        case CALL_RETURN:

                            existingParameter = parameterContainer.getParameterByValue(eventValue);

                            existingParameter.setProb(dataEvent);

                            saveProbe = true;

                            existingParameter.setProbeInfo(probeInfo);
                            existingParameter.setType(ClassTypeUtils.getDottedClassName(fieldType));

                            MethodCallExpression callExpression = callStack.get(callStack.size() - 1);
                            EventType entryEventType = callExpression.getEntryProbeInfo().getEventType();
                            if (entryEventType == EventType.CALL) {
                                // we pop it now

                                MethodCallExpression topCall = callStack.remove(callStack.size() - 1);
                                topCall.setReturnValue(existingParameter);

                                daoService.createOrUpdateCall(topCall);

                                testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1).getCallsList().add(topCall);


                            } else if (entryEventType == EventType.METHOD_ENTRY) {
                                // this is probably not a matching event

                            } else {
                                throw new RuntimeException("this should not happen");
                            }

                            break;

                        case NEW_OBJECT_CREATED:
                            MethodCallExpression theCallThatJustEnded = mostRecentCall.get();
                            existingParameter = theCallThatJustEnded.getSubject();
                            existingParameter.setProb(dataEvent);
                            theCallThatJustEnded.setReturnValue(existingParameter);
                            daoService.createOrUpdateCall(theCallThatJustEnded);
                            saveProbe = true;

                            break;
                        case METHOD_OBJECT_INITIALIZED:
                            MethodCallExpression currentCall = callStack.get(callStack.size() - 1);
                            Parameter callSubject = currentCall.getSubject();
                            callSubject.setProb(dataEvent);
                            callSubject.setProbeInfo(probeInfo);

                            saveProbe = true;


                            currentCall.setSubject(callSubject);
                            currentCall.setReturnValue(callSubject);
                            daoService.createOrUpdateCall(currentCall);

                    }
                    if (saveProbe) {
                        eventsToSave.add(dataEvent);
                        if (!existingProbes.contains(probeInfo.getDataId())) {
                            probesToSave.add(probeInfo);
                            existingProbes.add(probeInfo.getDataId());
                        }
                    }
                    if (existingParameter != null && existingParameter.getProb() != null) {
                        daoService.createOrUpdateParameter(existingParameter);
                        parameterContainer.add(existingParameter);
                    }
                }

                daoService.createOrUpdateDataEvent(eventsToSave);
                daoService.createOrUpdateProbeInfo(probesToSave);


            }


        }
        connectionSource.close();

        return objectRoutineContainerMap.values();
    }

    private void writeCandidate(TestCandidateMetadata candidate, OutputStream outputStream) throws IOException {
        DataOutputStream writer = new DataOutputStream(outputStream);
        if ((long) candidate.getTestSubject().getValue() == 0) {
            return;
        }
        writer.writeBytes("Candidate [" + candidate.getTestSubject().getValue() + "] => " + candidate.getTestSubject().getType() + "\n");
        MethodCallExpression mainMethod = (MethodCallExpression) candidate.getMainMethod();
        if (mainMethod.getSubject().getName() == null) {
            return;
        }
        writer.writeBytes("\t" + mainMethod.getReturnValue() + " = " + mainMethod.getSubject().getName() + "." + mainMethod.getMethodName() + "(" +
                Strings.join(mainMethod.getArguments().all().stream().map(Parameter::getName).collect(Collectors.toList()), ", ") +
                ")\n");
//        writer.writeBytes("Arguments: " + mainMethod.getArguments().all().size() + "\n");
//        for (Parameter parameter : mainMethod.getArguments().all()) {
//            writer.writeBytes("\t" + parameter.getName() + " => " + parameter.getValue() + "\n");
//        }
        List<MethodCallExpression> callsToMock = candidate.getCallsList().stream()
                .filter(e -> e.getSubject().getProbeInfo().getEventType() != EventType.LOCAL_LOAD && !e.getMethodName().startsWith("<"))
                .collect(Collectors.toList());
        writer.writeBytes("Calls: " + candidate.getCallsList().size() + "\n");


        for (MethodCallExpression methodCallExpression : callsToMock) {
            String methodName = methodCallExpression.getMethodName();
            @NotNull String paramString = Strings.join(methodCallExpression.getArguments().all().stream().map(Parameter::getName).collect(Collectors.toList()), ", ");
            if (methodCallExpression.getReturnValue() != null) {
                writer.writeBytes("\t" + methodCallExpression.getReturnValue().getName() + " = ");
            } else {
                writer.writeBytes("\t");
            }
            if (methodCallExpression.isStaticCall()) {
                String typeToCreate = methodCallExpression.getSubject().getType();
                writer.writeBytes(ClassName.bestGuess(typeToCreate).simpleName() + "." + methodName + "(" +
                        paramString + ")" + "\n");
            } else {
                if (methodName.equals("<init>")) {
                    String typeToCreate = methodCallExpression.getSubject().getType();
                    writer.writeBytes("new " + ClassName.bestGuess(typeToCreate).simpleName() + "(" +
                            paramString + ")" + "\n");
                } else {
                    writer.writeBytes(methodCallExpression.getSubject().getName() + "." + methodName + "(" +
                            paramString + ")" + "\n");
                }
            }
        }
        writer.writeBytes("\n");


    }

    private long eventId(KaitaiInsidiousEventParser.Block lastEvent) {
        KaitaiInsidiousEventParser.DetailedEventBlock eventBlock = lastEvent.block();
        return eventBlock.eventId();
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
