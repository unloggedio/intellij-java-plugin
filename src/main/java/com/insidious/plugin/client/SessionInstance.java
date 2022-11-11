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
import com.insidious.plugin.client.pojo.ArchiveFilesIndex;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.NameWithBytes;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.DatabaseVariableContainer;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.*;
import com.insidious.plugin.pojo.dao.ArchiveFile;
import com.insidious.plugin.pojo.dao.LogFile;
import com.insidious.plugin.pojo.dao.ProbeInfo;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import io.kaitai.struct.ByteBufferKaitaiStream;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.sqlite.SQLiteException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.googlecode.cqengine.query.QueryFactory.in;
import static com.googlecode.cqengine.query.QueryFactory.startsWith;
import static com.insidious.common.weaver.EventType.GET_INSTANCE_FIELD_RESULT;
import static com.insidious.common.weaver.EventType.PUT_INSTANCE_FIELD_VALUE;
import static com.insidious.plugin.client.DatFileType.*;

public class SessionInstance {
    public static final String COMPLETED = "completed";
    public static final String PENDING = "pending";
    private static final Logger logger = LoggerUtil.getInstance(SessionInstance.class);
    private final File sessionDirectory;
    private final ExecutionSession executionSession;
    private final List<File> sessionArchives;
    private final Map<String, String> cacheEntries = new HashMap<>();
    private final DatabasePipe databasePipe;
    private final DaoService daoService;
    private final Map<String, List<String>> zipFileListMap = new HashMap<>();
    private final ExecutorService executorPool;
    //    private KaitaiInsidiousClassWeaveParser classWeaveInfo;
    private ArchiveIndex archiveIndex;

    private ChronicleMap<Long, ObjectInfoDocument> objectInfoIndex;
    private ChronicleMap<Integer, DataInfo> probeInfoIndex;
    private ChronicleMap<Integer, TypeInfoDocument> typeInfoIndex;
    private ChronicleMap<Integer, MethodInfo> methodInfoIndex;
    private ChronicleMap<Integer, ClassInfo> classInfoIndex;

    public SessionInstance(ExecutionSession executionSession) throws SQLException, IOException {
        this.sessionDirectory = Path.of(executionSession.getPath()).toFile();
        this.executionSession = executionSession;

        boolean dbFileExists = Path.of(executionSession.getDatabasePath()).toFile().exists();
        ConnectionSource connectionSource = new JdbcConnectionSource(executionSession.getDatabaseConnectionString());
        daoService = new DaoService(connectionSource);

        if (!dbFileExists) {
            try {
                TableUtils.createTable(connectionSource, com.insidious.plugin.pojo.dao.TestCandidateMetadata.class);
                TableUtils.createTable(connectionSource, com.insidious.plugin.pojo.dao.MethodCallExpression.class);
                TableUtils.createTable(connectionSource, com.insidious.plugin.pojo.dao.Parameter.class);
                TableUtils.createTable(connectionSource, ProbeInfo.class);
                TableUtils.createTable(connectionSource, DataEventWithSessionId.class);
                TableUtils.createTable(connectionSource, LogFile.class);
                TableUtils.createTable(connectionSource, ArchiveFile.class);
                TableUtils.createTable(connectionSource, com.insidious.plugin.pojo.dao.TypeInfo.class);
            } catch (SQLException sqlException) {
                logger.warn("probably table already exists: " + sqlException);
            }
        }

        databasePipe = new DatabasePipe(new LinkedTransferQueue<>());
        executorPool = Executors.newFixedThreadPool(4);

//        createObjectInfoIndex();
        this.sessionArchives = refreshSessionArchivesList();
        executorPool.submit(databasePipe);
//        archiveIndex.setObjectInfoIndex(objectInfoIndex);
//        archiveIndex.setProbeInfoIndex(probeInfoIndex);

    }

    private static int getThreadIdFromFileName(String archiveFile) {
        return Integer.parseInt(archiveFile.substring(archiveFile.lastIndexOf("-") + 1, archiveFile.lastIndexOf(".")));
    }

    @NotNull
    private static Map<String, LogFile> getLogFileMap(DaoService daoService) {
        Map<String, LogFile> logFileMap = new HashMap<>();
        List<LogFile> logFiles = daoService.getLogFiles();
        for (LogFile logFile : logFiles) {
            logFileMap.put(logFile.getName(), logFile);
        }
        return logFileMap;
    }

    @NotNull
    private static Map<String, ArchiveFile> getArchiveFileMap(DaoService daoService) {
        List<ArchiveFile> archiveFileList = daoService.getArchiveList();
        Map<String, ArchiveFile> archiveFileMap = new HashMap<>();
        for (ArchiveFile archiveFile : archiveFileList) {
            archiveFileMap.put(archiveFile.getName(), archiveFile);
        }
        return archiveFileMap;
    }

    @NotNull
    private static DataEventWithSessionId createDataEventFromBlock(int fileThreadId, KaitaiInsidiousEventParser.DetailedEventBlock eventBlock) {
        DataEventWithSessionId dataEvent = new DataEventWithSessionId(fileThreadId);
        dataEvent.setDataId((int) eventBlock.probeId());
        dataEvent.setNanoTime(eventBlock.eventId());
        dataEvent.setRecordedAt(eventBlock.timestamp());
        dataEvent.setValue(eventBlock.valueId());
        dataEvent.setSerializedValue(eventBlock.serializedData());
        return dataEvent;
    }

    private static void addMethodToCandidate(List<TestCandidateMetadata> testCandidateMetadataStack, MethodCallExpression methodCall) {
        if (methodCall.isStaticCall()) {
            testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1).addMethodCall(methodCall);
        } else if (!methodCall.getMethodName().startsWith("<")) {
            Parameter callSubject = methodCall.getSubject();
            DataInfo callSubjectProbe = callSubject.getProbeInfo();
            if (!(callSubjectProbe.getEventType().equals(EventType.METHOD_PARAM) || callSubjectProbe.getEventType()
                    .equals(EventType.METHOD_ENTRY) || callSubjectProbe.getEventType()
                    .equals(EventType.METHOD_NORMAL_EXIT) || callSubjectProbe.getEventType()
                    .equals(EventType.CALL) || callSubjectProbe.getEventType().equals(EventType.LOCAL_LOAD))) {
                testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1).addMethodCall(methodCall);
            }
        }
    }

    public ExecutionSession getExecutionSession() {
        return executionSession;
    }

    private List<File> refreshSessionArchivesList() throws IOException, SQLException {
        if (sessionDirectory.listFiles() == null) {
            return List.of();
        }
        logger.info("refresh session archives list");
        List<File> sessionFiles = Arrays.stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .sorted((a, b) -> -1 * a.getName().compareTo(b.getName()))
                .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                .collect(Collectors.toList());
        logger.info("found [" + sessionFiles.size() + "] session archives");

        List<File> filesToRemove = new LinkedList<>();
        int i;
        KaitaiInsidiousClassWeaveParser classWeaveInfo = null;
        typeInfoIndex = createTypeInfoIndex();
        objectInfoIndex = createObjectInfoIndex();
        for (i = 0; i < sessionFiles.size() && classWeaveInfo == null; i++) {
            classWeaveInfo = readClassWeaveInfo(sessionFiles.get(i));

            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionFiles.get(i),
                    INDEX_TYPE_DAT_FILE.getFileName());
            if (typeIndexBytes == null) {
                filesToRemove.add(sessionFiles.get(i));
                continue;
            }

            try {
                archiveIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
                ConcurrentIndexedCollection<TypeInfoDocument> typeIndex = archiveIndex.getTypeInfoIndex();
                typeIndex.parallelStream().forEach(e -> typeInfoIndex.put(e.getTypeId(), e));
            } catch (IOException e) {
                e.printStackTrace();
                logger.warn("failed to read archive for types index: " + e.getMessage());
            }
        }

        probeInfoIndex = createProbeInfoIndex();
        methodInfoIndex = createMethodInfoIndex();
        classInfoIndex = createClassInfoIndex();


        if (classWeaveInfo == null) {
            throw new RuntimeException("Class weave information not found in the session");
        }


        AtomicInteger counter = new AtomicInteger(0);
        final long totalClassCount = classWeaveInfo.classCount();
        classWeaveInfo.classInfo().forEach(classInfo -> {
//            logger.warn("Reading class info: " + classInfo.classId());
//            checkProgressIndicator(null, "Loading " + classInfo.probeCount() + " probes in class: " + classInfo.className());
            int current = counter.addAndGet(1);
            checkProgressIndicator(null, "Loading " + current + " / " + totalClassCount + " class information");

            ClassInfo existingClassInfo = classInfoIndex.get((int) classInfo.classId());
            if (existingClassInfo != null) {
                return;
            }

            ClassInfo classInfo1 = KaitaiUtils.toClassInfo(classInfo);
            classInfoIndex.put(classInfo1.getClassId(), classInfo1);


            final String className = classInfo.className().value();

            Map<Integer, MethodInfo> methodInfoMap = classInfo.methodList().stream()
                    .map(methodInfo -> KaitaiUtils.toMethodInfo(methodInfo, className))
                    .collect(Collectors.toMap(MethodInfo::getMethodId, e -> e));
            methodInfoIndex.putAll(methodInfoMap);

            Map<Integer, DataInfo> probesMap = classInfo.probeList().stream().map(KaitaiUtils::toDataInfo)
                    .collect(Collectors.toMap(DataInfo::getDataId, e -> e));
            probeInfoIndex.putAll(probesMap);
        });
        classWeaveInfo._io().close();

        sessionFiles.removeAll(filesToRemove);
        Collections.sort(sessionFiles);
        return sessionFiles;
    }

    private ChronicleMap<Integer, DataInfo> createProbeInfoIndex() {


        ChronicleMapBuilder<Integer, DataInfo> probeInfoMapBuilder = ChronicleMapBuilder.of(Integer.class,
                DataInfo.class).name("probe-info-map").averageValue(new DataInfo()).entries(1_000_000);
        return probeInfoMapBuilder.create();

//
//        File objectIndexFile = Path.of(executionSession.getPath(), "probe_index.dat").toFile();
//
//        CompositePersistence<DataInfo, Integer> persistence = CompositePersistence.of(
//                DiskPersistence.onPrimaryKeyInFile(DataInfo.PROBE_ID, objectIndexFile),
//                OffHeapPersistence.onPrimaryKey(DataInfo.PROBE_ID)
//        );
//
//        OffHeapPersistence<DataInfo, Integer> objectInfoDocumentIntegerDiskPersistence =
//                OffHeapPersistence.onPrimaryKey(DataInfo.PROBE_ID);
//
//        ConcurrentIndexedCollection<DataInfo> probeInfoIndex1 =
//                new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
////        probeInfoIndex1.addIndex(OffHeapIndex.onAttribute(DataInfo.PROBE_ID));
//        return probeInfoIndex1;
    }


    private ChronicleMap<Integer, TypeInfoDocument> createTypeInfoIndex() {


        ChronicleMapBuilder<Integer, TypeInfoDocument> probeInfoMapBuilder = ChronicleMapBuilder.of(Integer.class,
                        TypeInfoDocument.class).name("type-info-map")
                .averageValue(new TypeInfoDocument(1, "Type-name-class", new byte[100])).entries(20_000);
        return probeInfoMapBuilder.create();

//
//        File objectIndexFile = Path.of(executionSession.getPath(), "probe_index.dat").toFile();
//
//        CompositePersistence<DataInfo, Integer> persistence = CompositePersistence.of(
//                DiskPersistence.onPrimaryKeyInFile(DataInfo.PROBE_ID, objectIndexFile),
//                OffHeapPersistence.onPrimaryKey(DataInfo.PROBE_ID)
//        );
//
//        OffHeapPersistence<DataInfo, Integer> objectInfoDocumentIntegerDiskPersistence =
//                OffHeapPersistence.onPrimaryKey(DataInfo.PROBE_ID);
//
//        ConcurrentIndexedCollection<DataInfo> probeInfoIndex1 =
//                new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
////        probeInfoIndex1.addIndex(OffHeapIndex.onAttribute(DataInfo.PROBE_ID));
//        return probeInfoIndex1;
    }


    private ChronicleMap<Long, ObjectInfoDocument> createObjectInfoIndex() {


        ChronicleMapBuilder<Long, ObjectInfoDocument> probeInfoMapBuilder = ChronicleMapBuilder.of(Long.class,
                        ObjectInfoDocument.class).name("object-info-map").averageValue(new ObjectInfoDocument(1, 1))
                .entries(10_000_000);
        return probeInfoMapBuilder.create();

//
//        File objectIndexFile = Path.of(executionSession.getPath(), "probe_index.dat").toFile();
//
//        CompositePersistence<DataInfo, Integer> persistence = CompositePersistence.of(
//                DiskPersistence.onPrimaryKeyInFile(DataInfo.PROBE_ID, objectIndexFile),
//                OffHeapPersistence.onPrimaryKey(DataInfo.PROBE_ID)
//        );
//
//        OffHeapPersistence<DataInfo, Integer> objectInfoDocumentIntegerDiskPersistence =
//                OffHeapPersistence.onPrimaryKey(DataInfo.PROBE_ID);
//
//        ConcurrentIndexedCollection<DataInfo> probeInfoIndex1 =
//                new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
////        probeInfoIndex1.addIndex(OffHeapIndex.onAttribute(DataInfo.PROBE_ID));
//        return probeInfoIndex1;
    }


    private ChronicleMap<Integer, MethodInfo> createMethodInfoIndex() {

        ChronicleMapBuilder<Integer, MethodInfo> probeInfoMapBuilder = ChronicleMapBuilder.of(Integer.class,
                        MethodInfo.class).name("method-info-map").averageValue(
                        new MethodInfo(1, 2, "class-name", "method-name", "methoddesc", 5, "source-file-name", "method-hash"))
                .entries(100_000);
        return probeInfoMapBuilder.create();


//        File objectIndexFile = Path.of(executionSession.getPath(), "method_index.dat").toFile();
//
//        CompositePersistence<MethodInfo, Integer> persistence = CompositePersistence.of(
//                DiskPersistence.onPrimaryKeyInFile(MethodInfo.METHOD_ID, objectIndexFile),
//                OffHeapPersistence.onPrimaryKey(MethodInfo.METHOD_ID)
//        );

//
//        OffHeapPersistence<MethodInfo, Integer> objectInfoDocumentIntegerDiskPersistence =
//                OffHeapPersistence.onPrimaryKey(MethodInfo.METHOD_ID);
//
//        ConcurrentIndexedCollection<MethodInfo> probeInfoIndex1 =
//                new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
////        probeInfoIndex1.addIndex(OffHeapIndex.onAttribute(MethodInfo.METHOD_ID));
//        return probeInfoIndex1;
    }


    private ChronicleMap<Integer, ClassInfo> createClassInfoIndex() {

        ChronicleMapBuilder<Integer, ClassInfo> probeInfoMapBuilder = ChronicleMapBuilder.of(Integer.class,
                        ClassInfo.class).name("class-info-map").averageValue(
                        new ClassInfo(1, "container-name", "file-name", "class-name", LogLevel.Normal, "hashvalue",
                                "class-loader-identifier", new String[]{"classinterface-1"}, "super-class-name", "signaure"))
                .entries(10_000);
        return probeInfoMapBuilder.create();

//        File objectIndexFile = Path.of(executionSession.getPath(), "class_index.dat").toFile();
//
//        CompositePersistence<ClassInfo, Integer> persistence = CompositePersistence.of(
//                DiskPersistence.onPrimaryKeyInFile(ClassInfo.CLASS_ID, objectIndexFile),
//                OffHeapPersistence.onPrimaryKey(ClassInfo.CLASS_ID)
//        );
//
//        OffHeapPersistence<ClassInfo, Integer> objectInfoDocumentIntegerDiskPersistence =
//                OffHeapPersistence.onPrimaryKey(ClassInfo.CLASS_ID);
//
//        ConcurrentIndexedCollection<ClassInfo> probeInfoIndex1 =
//                new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
////        probeInfoIndex1.addIndex(NavigableIndex.onAttribute(DataInfo.PROBE_ID));
//        return probeInfoIndex1;
    }


    public Collection<TracePoint> queryTracePointsByValue(SearchQuery searchQuery) {
        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionArchive : this.sessionArchives) {

            NameWithBytes bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                    INDEX_STRING_DAT_FILE.getFileName());
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


            checkProgressIndicator(null,
                    "Loaded " + valueIds.size() + " strings from archive " + sessionArchive.getName());

            if (valueIds.size() > 0) {
                tracePointList.addAll(getTracePointsByValueIds(sessionArchive, valueIds));
            }
        }
        tracePointList.forEach(e -> e.setExecutionSession(executionSession));
        return tracePointList;
    }

    private List<TracePoint> getTracePointsByValueIds(File sessionArchive, Set<Long> valueIds) {
        logger.info("Query for valueIds [" + valueIds.toString() + "]");
        List<TracePoint> tracePointList = new LinkedList<>();
        NameWithBytes bytes;
        ArchiveFilesIndex eventsIndex = null;
        ArchiveIndex objectIndex = null;
        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
        Map<Long, ObjectInfo> objectInfoMap = new HashMap<>();
        try {
            checkProgressIndicator(null,
                    "Loading events index " + sessionArchive.getName() + " " + "to match against " + valueIds.size() + " values");


            bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
            assert bytes != null;
            eventsIndex = readEventIndex(bytes.getBytes());


            checkProgressIndicator(null,
                    "Loading objects from " + sessionArchive.getName() + " to match against " + valueIds.size() + " values");
            NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                    INDEX_OBJECT_DAT_FILE.getFileName());
            assert objectIndexBytes != null;
            objectIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            logger.info("object index has [" + objectIndex.Objects().size() + "] objects");
            objectInfoMap = objectIndex.getObjectsByObjectId(valueIds);

            Set<Integer> types = objectInfoMap.values().stream().map(ObjectInfo::getTypeId).collect(Collectors.toSet());

            checkProgressIndicator(null, "Loading types from " + sessionArchive.getName());
            typeInfoMap = archiveIndex.getTypesById(types);
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
        Map<Long, ObjectInfo> finalObjectInfoMap = objectInfoMap;
        Map<String, TypeInfo> finalTypeInfoMap = typeInfoMap;
        logger.info("matched [" + matchedFiles.size() + "] files");

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .setText("Found " + matchedFiles.size() + " archives with matching values");
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
                    logger.error(
                            String.format("matched file not found " + "inside the session archive [%s] -> [%s] -> [%s]",
                                    fileName, sessionArchive, fileList));
                    throw new RuntimeException("matched file not found inside the session archive -> " + fileList);
                }
                long timestamp = Long.parseLong(fileBytes.getName().split("@")[0]);
                int threadId = Integer.parseInt(fileBytes.getName().split("-")[2].split("\\.")[0]);


                List<DataEventWithSessionId> dataEvents = getDataEventsFromPathByValueIds(fileBytes.getBytes(),
                        matchedFile.getValueIds());
                checkProgressIndicator(null,
                        "Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
                List<TracePoint> matchedTracePoints = dataEvents.stream().map(e1 -> {

                    try {
                        List<DataInfo> dataInfoList = getProbeInfo(Set.of(e1.getDataId()));
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
                        TypeInfo typeInfo = getTypeInfo(objectInfo.getTypeId());

                        TracePoint tracePoint = new TracePoint(classId, dataInfo.getLine(), dataInfo.getDataId(),
                                threadId, e1.getValue(), classInfo.fileName().value(), classInfo.className().value(),
                                typeInfo.getTypeNameFromClass(), timestamp, e1.getNanoTime());
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
                        ProgressIndicatorProvider.getGlobalProgressIndicator()
                                .setText(tracePointList.size() + " matched...");
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

    private List<DataInfo> getProbeInfo(Set<Long> dataId) throws IOException {

        throw new RuntimeException("who is using this");

//        return classWeaveInfo.classInfo().stream()
//                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
//                .flatMap(Collection::stream)
//                .filter(e -> dataId.contains(e.dataId()))
//                .map(KaitaiUtils::toDataInfo).collect(Collectors.toList());

    }

    private KaitaiInsidiousClassWeaveParser readClassWeaveInfo(@NotNull File sessionFile) throws IOException {

        KaitaiInsidiousClassWeaveParser classWeaveInfo1 = null;
        logger.warn("creating class weave info from scratch from file: " + sessionFile.getName());
        NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionFile, WEAVE_DAT_FILE.getFileName());
        if (fileBytes == null) {
            logger.debug("failed to read class weave info from " + "sessionFile [" + sessionFile.getName() + "]");
            return null;
        }
//        logger.warn("Class weave information from " + sessionFile.getName() + " is " + fileBytes.getBytes().length + " bytes");
        ByteBufferKaitaiStream io = new ByteBufferKaitaiStream(fileBytes.getBytes());
        classWeaveInfo1 = new KaitaiInsidiousClassWeaveParser(io);
        io.close();
        return classWeaveInfo1;
    }

    private KaitaiInsidiousClassWeaveParser.ClassInfo getClassInfo(int classId) throws ClassInfoNotFoundException {

//        for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeaveInfo.classInfo()) {
//            if (classInfo.classId() == classId) {
//                return classInfo;
//            }
//        }
        throw new ClassInfoNotFoundException(classId);
    }

    private List<DataEventWithSessionId> getDataEventsFromPathByValueIds(byte[] bytes, Long[] valueIds) throws IOException {

        Set<Long> ids = Set.of(valueIds);
        KaitaiInsidiousEventParser dataEvents = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(bytes));

        return dataEvents.event().entries().stream().filter(e -> ids.contains(e.block().valueId())).map(e -> {
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

    public TypeInfo getTypeInfo(Integer typeId) {

        Map<String, TypeInfo> result = archiveIndex.getTypesById(Set.of(typeId));
        if (result.size() > 0) {
            return result.get(String.valueOf(typeId));
        }

        return new TypeInfo(typeId, "unidentified type", "", 0, 0, "", new int[0]);
    }

    public TypeInfo getTypeInfo(String name) {

        TypeInfo result = archiveIndex.getTypesByName(name);
        if (result != null) {
            return result;
        }

        return new TypeInfo(-1, name, "", 0, 0, "", new int[0]);
    }

    public List<TypeInfoDocument> getAllTypes() {
        return new ArrayList<>(archiveIndex.Types());
    }

    private List<String> listArchiveFiles(File sessionFile) throws IOException {
        if (zipFileListMap.containsKey(sessionFile.getName())) {
            return zipFileListMap.get(sessionFile.getName());
        }
        logger.warn("open archive [" + sessionFile + "]");
        List<String> files = new LinkedList<>();

        ZipInputStream indexArchive = new ZipInputStream(new FileInputStream(sessionFile));

        ZipEntry entry = null;
        while ((entry = indexArchive.getNextEntry()) != null) {
            String entryName = entry.getName();
            indexArchive.closeEntry();
            files.add(entryName);
        }
        indexArchive.close();
        files = files.stream().filter(e -> e.contains("@")).map(e -> e.split("@")[1]).collect(Collectors.toList());
        Collections.sort(files);
        zipFileListMap.put(sessionFile.getName(), files);
        return files;

    }

    private ArchiveFilesIndex readEventIndex(byte[] bytes) throws IOException {
        KaitaiInsidiousIndexParser archiveIndex = new KaitaiInsidiousIndexParser(new ByteBufferKaitaiStream(bytes));

        return new ArchiveFilesIndex(archiveIndex);
    }

    private ArchiveIndex readArchiveIndex(byte[] bytes, DatFileType indexFilterType) throws IOException {

        ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex = null;
        String cacheKey = bytesHex(bytes, indexFilterType.getFileName());


        Path path = Path.of(this.sessionDirectory.getAbsolutePath(), cacheKey, indexFilterType.getFileName());
        Path parentPath = path.getParent();
        parentPath.toFile().mkdirs();

        if (!path.toFile().exists()) {
            Files.write(path, bytes);
        }

        if (indexFilterType.equals(INDEX_TYPE_DAT_FILE)) {
            DiskPersistence<TypeInfoDocument, Integer> typeInfoDocumentStringDiskPersistence = DiskPersistence.onPrimaryKeyInFile(
                    TypeInfoDocument.TYPE_ID, path.toFile());
            typeInfoIndex = new ConcurrentIndexedCollection<>(typeInfoDocumentStringDiskPersistence);
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_NAME));
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_ID));
        }


        ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex = null;
        if (indexFilterType.equals(INDEX_STRING_DAT_FILE)) {
            DiskPersistence<StringInfoDocument, Long> stringInfoDocumentStringDiskPersistence = DiskPersistence.onPrimaryKeyInFile(
                    StringInfoDocument.STRING_ID, path.toFile());
            stringInfoIndex = new ConcurrentIndexedCollection<>(stringInfoDocumentStringDiskPersistence);

            stringInfoIndex.addIndex(InvertedRadixTreeIndex.onAttribute(StringInfoDocument.STRING_VALUE));
        }

        ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex = null;
        if (indexFilterType.equals(INDEX_OBJECT_DAT_FILE)) {
            DiskPersistence<ObjectInfoDocument, Long> objectInfoDocumentIntegerDiskPersistence = DiskPersistence.onPrimaryKeyInFile(
                    ObjectInfoDocument.OBJECT_ID, path.toFile());

            objectInfoIndex = new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_TYPE_ID));
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_ID));
        }

        if (indexFilterType.equals(WEAVE_DAT_FILE)) {
            KaitaiInsidiousClassWeaveParser classWeave = new KaitaiInsidiousClassWeaveParser(
                    new ByteBufferKaitaiStream(bytes));
            throw new RuntimeException("this is not to be used");
//            for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeave.classInfo()) {
//                classInfoMap.put(classInfo.classId(), KaitaiUtils.toClassInfo(classInfo));
//            }
        }

        return new ArchiveIndex(typeInfoIndex, stringInfoIndex, objectInfoIndex, null);
    }

    @NotNull
    private String bytesHex(byte[] bytes, String indexFilterType) {
        String md5Hex = DigestUtils.md5Hex(bytes);
        return md5Hex + "-" + indexFilterType;
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
                FileInputStream inputStream = new FileInputStream(cacheFile);
                byte[] bytes = IOUtils.toByteArray(inputStream);
                inputStream.close();
                return new NameWithBytes(name, bytes);
            }

            FileInputStream sessionFileInputStream = new FileInputStream(sessionFile);
            indexArchive = new ZipInputStream(sessionFileInputStream);


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
                    logger.info(
                            pathName + " file from " + sessionFile.getName() + " is " + nameWithBytes.getBytes().length + " bytes");
                    indexArchive.closeEntry();
                    indexArchive.close();
                    sessionFileInputStream.close();
                    return nameWithBytes;
                }
            }
        } catch (Exception e) {
            if (indexArchive != null) {
                try {
                    indexArchive.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            e.printStackTrace();
            logger.warn(
                    "failed to create file [" + pathName + "] on disk from" + " archive[" + sessionFile.getName() + "]");
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
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText(text1);
            }
        }
    }

    private List<DataInfo> queryProbeFromFileByEventType(File sessionFile, Collection<EventType> eventTypes) {
//        return classWeaveInfo.classInfo().stream()
//                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
//                .flatMap(Collection::stream)
//                .filter(e -> eventTypes.size() == 0 ||
//                        // dont check contains if the list is empty
//                        eventTypes.contains(EventType.valueOf(e.eventType().name())))
//                .map(KaitaiUtils::toDataInfo).collect(Collectors.toList());

        return List.of();

    }

    public void queryTracePointsByEventType(SearchQuery searchQuery, ClientCallBack<TracePoint> tracePointsCallback) {


        List<DataInfo> probeIds = null;
        for (File sessionArchive : sessionArchives) {
            logger.info("check archive [" + sessionArchive.getName() + "] for " + "probes");
            if (probeIds == null || probeIds.size() == 0) {
                Collection<EventType> eventTypes = (Collection<EventType>) searchQuery.getQuery();
                probeIds = queryProbeFromFileByEventType(sessionArchive, eventTypes);
            }

            checkProgressIndicator(null,
                    "Loaded " + probeIds.size() + " objects from archive " + sessionArchive.getName());


            if (probeIds.size() > 0) {
                getTracePointsByProbeIds(sessionArchive,
                        probeIds.stream().map(DataInfo::getDataId).collect(Collectors.toSet()), tracePointsCallback);
            }
        }
        tracePointsCallback.completed();

    }

    private void getTracePointsByProbeIds(File sessionArchive, Set<Integer> probeIds, ClientCallBack<TracePoint> tracePointsCallback) {
        logger.info("Query for probeIds [" + probeIds.toString() + "]");
        List<TracePoint> tracePointList = new LinkedList<>();
        NameWithBytes bytes;
        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
        Map<String, ObjectInfo> objectInfoMap = new HashMap<>();


        ArchiveFilesIndex eventsIndex = null;
        ArchiveIndex objectIndex = null;
        try {
            checkProgressIndicator(null,
                    "Loading events index " + sessionArchive.getName() + " to match against " + probeIds.size() + " values");


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
                matchedFilesForString = finalEventsIndex.querySessionFilesByProbeId(probeId);
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

        checkProgressIndicator("Found " + matchedFiles.size() + " archives with matching values", null);


        for (UploadFile matchedFile : matchedFiles.values()) {
            try {

                checkProgressIndicator(null, "Loading events data from " + matchedFile.getPath());
                String fileName = Path.of(matchedFile.getPath()).getFileName().toString();

                NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, fileName);
                if (fileBytes == null) {
                    List<String> fileList = listArchiveFiles(sessionArchive);
                    logger.error(
                            String.format("matched file not found " + "inside the session archive [%s] -> [%s] -> [%s]",
                                    fileName, sessionArchive, fileList));
                    throw new RuntimeException("matched file not found inside the session archive -> " + fileList);
                }
                long timestamp = Long.parseLong(fileBytes.getName().split("@")[0]);
                int threadId = Integer.parseInt(fileBytes.getName().split("-")[2].split("\\.")[0]);


                List<DataEventWithSessionId> dataEvents = getDataEventsFromPathByProbeIds(fileBytes.getBytes(),
                        matchedFile.getProbeIds());

                checkProgressIndicator(null,
                        "Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
                List<TracePoint> matchedTracePoints = dataEvents.stream().map(e1 -> {

                    try {
                        List<DataInfo> dataInfoList = getProbeInfo(Set.of(e1.getDataId()));
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
                            TypeInfo typeInfo = getTypeInfo(objectInfo.getTypeId());
                            typeName = typeInfo.getTypeNameFromClass();
                        }

                        TracePoint tracePoint = new TracePoint(classId, dataInfo.getLine(), dataInfo.getDataId(),
                                threadId, e1.getValue(), classInfo.fileName().value(), classInfo.className().value(),
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
                logger.error("exception while creating trace points in file[" + matchedFile.path + "]", ex);
            }

        }
        if (tracePointList.size() != 0) {
            tracePointList.forEach(e -> e.setExecutionSession(executionSession));
            tracePointsCallback.success(tracePointList);
        }
    }

    private void getTracePointsByProbeIdsWithoutIndex(File sessionArchive, Set<Integer> probeIds, ClientCallBack<TracePoint> tracePointsCallback) {
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
            checkProgressIndicator(null,
                    "Loading events index " + sessionArchive.getName() + " to match against " + probeIds.size() + " values");
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
                matchedFilesForString = listArchiveFiles(sessionArchive).stream().filter(e -> e.endsWith("selog"))
                        .map(e -> {
                            UploadFile uploadFile = new UploadFile(e, 0, null, null);
                            uploadFile.setProbeIds(probeIdArray);
                            return uploadFile;
                        }).collect(Collectors.toList());
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
                    logger.error(
                            String.format("matched file not found " + "inside the session archive [%s] -> [%s] -> [%s]",
                                    fileName, sessionArchive, fileList));
                    throw new RuntimeException("matched file not found inside the session archive -> " + fileList);
                }
                long timestamp = Long.parseLong(fileBytes.getName().split("@")[0]);
                int threadId = Integer.parseInt(fileBytes.getName().split("-")[2].split("\\.")[0]);


                List<DataEventWithSessionId> dataEvents = getDataEventsFromPathByProbeIds(fileBytes.getBytes(),
                        matchedFile.getProbeIds());

                checkProgressIndicator(null,
                        "Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
                List<TracePoint> matchedTracePoints = dataEvents.stream().map(e1 -> {

                    try {
                        List<DataInfo> dataInfoList = getProbeInfo(Set.of(e1.getDataId()));
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
                            TypeInfo typeInfo = getTypeInfo(objectInfo.getTypeId());
                            typeName = typeInfo.getTypeNameFromClass();
                        }

                        TracePoint tracePoint = new TracePoint(classId, dataInfo.getLine(), dataInfo.getDataId(),
                                threadId, e1.getValue(), classInfo.fileName().value(), classInfo.className().value(),
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
                logger.error("exception while creating trace points in file[" + matchedFile.path + "]", ex);
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

        return dataEvents.event().entries().stream().filter(e -> ids.contains((int) e.block().probeId())).map(e -> {
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


        NameWithBytes bytesWithName = createFileOnDiskFromSessionArchiveFile(archiveToServe,
                String.valueOf(filteredDataEventsRequest.getNanotime()));


        assert bytesWithName != null;


        checkProgressIndicator(null, "Parsing events: " + bytesWithName.getName());

        KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(
                new ByteBufferKaitaiStream(bytesWithName.getBytes()));


        checkProgressIndicator(null, "Mapping " + eventsContainer.event().entries().size() + " events ");

        List<DataEventWithSessionId> dataEventList = eventsContainer.event().entries().stream().map(e -> {
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

        checkProgressIndicator(null, "Loading class mappings for data events");

//        classWeaveInfo.classInfo().forEach(e -> {
//
//            checkProgressIndicator(null, "Loading class: " + e.className());
//
//            classInfo.put(e.classId(), KaitaiUtils.toClassInfo(e));
//
//            checkProgressIndicator(null, "Loading " + e.probeCount() + " probes in class: " + e.className());
//
//            e.probeList().forEach(r -> {
//                dataInfo.put(r.dataId(), KaitaiUtils.toDataInfo(r));
//            });
//        });

        Set<Long> probeIds = dataEventList.stream().map(DataEventWithSessionId::getDataId).collect(Collectors.toSet());
        Set<Long> valueIds = dataEventList.stream().map(DataEventWithSessionId::getValue).collect(Collectors.toSet());


        Map<Long, StringInfo> stringInfo = new HashMap<>();
        Map<Long, ObjectInfo> objectInfo = new HashMap<>();
        Map<Integer, TypeInfo> typeInfo = new HashMap<>();
        Map<Long, MethodInfo> methodInfoMap = new HashMap<>();

        checkProgressIndicator(null, "Loading types");
        for (File sessionArchive : this.sessionArchives) {

            checkProgressIndicator(null, "Loading objects from " + sessionArchive.getName());

            NameWithBytes objectsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                    INDEX_OBJECT_DAT_FILE.getFileName());
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
            NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                    INDEX_STRING_DAT_FILE.getFileName());
            assert stringsIndexBytes != null;


            checkProgressIndicator(null, "Loading strings from " + sessionArchive.getName());
            ArchiveIndex stringIndex = null;
            try {
                stringIndex = readArchiveIndex(stringsIndexBytes.getBytes(), INDEX_STRING_DAT_FILE);
            } catch (IOException e) {
                logger.error("failed to read string index from session bytes: " + e.getMessage(), e);
                continue;
            }
            Map<Long, StringInfo> sessionStringInfo = stringIndex.getStringsByIdWithLongKeys(
                    valueIds.stream().filter(e -> e > 10).collect(Collectors.toSet()));
            stringInfo.putAll(sessionStringInfo);


            Set<Integer> typeIds = objectInfo.values().stream().map(ObjectInfo::getTypeId).collect(Collectors.toSet());


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
            Map<Integer, TypeInfo> sessionTypeInfo = archiveIndex.getTypesByIdWithLongKeys(typeIds);
            typeInfo.putAll(sessionTypeInfo);

        }


        checkProgressIndicator(null, "Completed loading");
        return new ReplayData(null, filteredDataEventsRequest, dataEventList, classInfoIndex, probeInfoIndex,
                stringInfo, objectInfo, typeInfo, methodInfoIndex);
    }

    public void getMethods(Integer typeId, ClientCallBack<TestCandidate> tracePointsCallback) {

//        if (classWeaveInfo == null) {
//            ExceptionResponse errorResponse = new ExceptionResponse();
//            errorResponse.setMessage("session not found [" + executionSession.getSessionId() + "]");
//            tracePointsCallback.error(errorResponse);
//            return;
//        }
//        classWeaveInfo
//                .classInfo()
//                .forEach(classInfo -> {
//
//                    if (classInfo.classId() != typeId) {
//                        return;
//                    }
//                    ClassInfo classInfoContainer = KaitaiUtils.toClassInfo(classInfo);
//                    tracePointsCallback.success(
//                            classInfo.methodList()
//                                    .stream().map(e -> KaitaiUtils.toMethodInfo(e,
//                                            classInfo.className().value()))
//                                    .map(methodInfo ->
//                                            new TestCandidate(methodInfo,
//                                                    classInfoContainer,
//                                                    0, null))
//                                    .collect(Collectors.toSet()));
//                });
//        tracePointsCallback.completed();
    }

    public void getObjectsByType(SearchQuery searchQuery, ClientCallBack<ObjectWithTypeInfo> clientCallBack) {

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

            Set<ObjectInfoDocument> objects = queryObjectsByTypeFromSessionArchive(searchQuery, sessionArchive);
            if (objects.size() == 0) {
                continue;
            }

            Map<Integer, TypeInfo> typesMap = objects.stream().map(ObjectInfoDocument::getTypeId)
                    .collect(Collectors.toSet()).stream().map(this::getTypeInfo)
                    .collect(Collectors.toMap(TypeInfo::getTypeId, typeInfo -> typeInfo));

            Set<ObjectWithTypeInfo> collect = objects.stream()
                    .map(e -> new ObjectWithTypeInfo(new ObjectInfo(e.getObjectId(), e.getTypeId(), 0),
                            typesMap.get(e.getTypeId()))).collect(Collectors.toSet());
            if (collect.size() > 0) {
                checkProgressIndicator(null, archiveName + " matched " + collect.size() + " objects");
                clientCallBack.success(collect);
            }


        }

        clientCallBack.completed();

    }

    private Set<ObjectInfoDocument> queryObjectsByTypeFromSessionArchive(SearchQuery searchQuery, File sessionArchive) {

        checkProgressIndicator(null, "querying type names from: " + sessionArchive.getName());


        Set<Integer> typeIds = queryTypeIdsByName(searchQuery);


        checkProgressIndicator(null, "Loading matched objects");


        NameWithBytes objectIndexFileBytes = null;
        objectIndexFileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                INDEX_OBJECT_DAT_FILE.getFileName());
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
        Set<ObjectInfoDocument> objects = typeInfoSearchResult.stream().collect(Collectors.toSet());
        typeInfoSearchResult.close();

        checkProgressIndicator(null,
                sessionArchive.getName() + " matched " + objects.size() + " objects of total " + objectIndex.Objects()
                        .size());


        return objects;
    }

    private Set<Integer> queryTypeIdsByName(SearchQuery searchQuery) {
        String query = (String) searchQuery.getQuery();

        Query<TypeInfoDocument> typeQuery = startsWith(TypeInfoDocument.TYPE_NAME, query);
        if (query.endsWith("*")) {
            typeQuery = startsWith(TypeInfoDocument.TYPE_NAME, query.substring(0, query.length() - 1));
        }

        ResultSet<TypeInfoDocument> searchResult = archiveIndex.Types().retrieve(typeQuery);
        Set<Integer> typeIds = searchResult.stream().map(TypeInfoDocument::getTypeId).collect(Collectors.toSet());
        searchResult.close();
        logger.info("type query [" + searchQuery + "] matched [" + typeIds.size() + "] items");

        if (typeIds.size() == 0) {
            return Set.of();
        }
        return typeIds;
    }

    public List<String> getArchiveNamesList() throws IOException, SQLException {
        return refreshSessionArchivesList().stream().map(File::getName).collect(Collectors.toList());
    }

    public ClassWeaveInfo getClassWeaveInfo() {


        int i = 0;
        List<ClassInfo> classInfoList = new LinkedList<>();
        List<MethodInfo> methodInfoList = new LinkedList<>();
        List<DataInfo> dataInfoList = new LinkedList<>();


//        for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeaveInfo.classInfo()) {
//            i += 1;
//            checkProgressIndicator(null,
//                    "Parsing class [ " + i + " of " + classInfoList.size() + " ]");
//            ClassInfo classInfoContainer = KaitaiUtils.toClassInfo(classInfo);
//            classInfoList.add(classInfoContainer);
//
//            classInfo.methodList()
//                    .stream().map(e1 -> KaitaiUtils.toMethodInfo(e1,
//                            classInfo.className().value()))
//                    .forEach(methodInfoList::add);
//
//            classInfo.probeList()
//                    .stream()
//                    .filter(e -> !Objects.equals(e.eventType().name(), EventType.RESERVED.toString()))
//                    .map(KaitaiUtils::toDataInfo)
//                    .forEach(dataInfoList::add);
//
//        }

        return new ClassWeaveInfo(classInfoList, methodInfoList, dataInfoList);
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

    private List<KaitaiInsidiousEventParser.Block> getEventsFromFile(File sessionArchive, String archiveFile) throws IOException {
        logger.warn("Read events from file: " + archiveFile);
        NameWithBytes bytesWithName = createFileOnDiskFromSessionArchiveFile(sessionArchive, archiveFile);

        assert bytesWithName != null;


        ByteBufferKaitaiStream kaitaiStream = new ByteBufferKaitaiStream(bytesWithName.getBytes());
        KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(kaitaiStream);


        ArrayList<KaitaiInsidiousEventParser.Block> events = eventsContainer.event().entries();
        kaitaiStream.close();
        return events;
    }

    public ReplayData fetchObjectHistoryByObjectId(FilteredDataEventsRequest filteredDataEventsRequest) {

        List<DataEventWithSessionId> dataEventList = new LinkedList<>();
        Map<Long, StringInfo> stringInfoMap = new HashMap<>();
        Map<Long, ObjectInfo> objectInfoMap = new HashMap<>();
        Map<Integer, TypeInfo> typeInfoMap = new HashMap<>();


        final long objectId = filteredDataEventsRequest.getObjectId();

        LinkedList<File> sessionArchivesLocal = new LinkedList<>(this.sessionArchives);

        Collections.sort(sessionArchivesLocal);

        PageInfo pageInfo = filteredDataEventsRequest.getPageInfo();
        if (pageInfo.isDesc()) {
            Collections.reverse(sessionArchivesLocal);
        }


        final AtomicInteger skip = new AtomicInteger(pageInfo.getNumber() * pageInfo.getSize());
        Integer remaining = pageInfo.getSize();


        checkProgressIndicator(null, "Loading class mappings for object history");


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
                    logger.info(
                            "no files were matched, listing files from session archive [" + sessionArchive.getName() + "] -> " + archiveFiles);
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

                        metadata = new SELogFileMetadata(eventId(firstEvent), eventId(lastEvent), fileThreadId);
                        fileEventIdPairs.put(archiveFile, metadata);

                    }


                    if (pageInfo.isDesc()) {
                        Collections.reverse(eventsSublist);
                    }


                    SELogFileMetadata finalMetadata = metadata;
                    List<DataEventWithSessionId> dataEventGroupedList = eventsSublist.stream().filter(e -> {
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
                            }).filter(e -> {
                                if (skip.get() > 0) {
                                    int remainingNow = skip.decrementAndGet();
                                    return remainingNow <= 0;
                                }
                                return true;
                            }).map(e -> createDataEventFromBlock(finalMetadata.getThreadId(), e.block()))
                            .collect(Collectors.toList());

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


            Set<Long> probeIds = dataEventList.stream().map(DataEventWithSessionId::getDataId)
                    .collect(Collectors.toSet());
            Set<Long> valueIds = dataEventList.stream().map(DataEventWithSessionId::getValue)
                    .collect(Collectors.toSet());


            NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                    INDEX_STRING_DAT_FILE.getFileName());
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

                sessionStringInfo.values().stream().map(StringInfo::getStringId).collect(Collectors.toList())
                        .forEach(potentialStringIds::remove);


                remainingStringIds.addAll(potentialStringIds);
            }
            stringInfoMap.putAll(sessionStringInfo);

            Set<Long> objectIds = dataEventList.stream().map(DataEventWithSessionId::getValue).filter(e -> e > 1000)
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

                sessionObjectsInfo.values().stream().map(ObjectInfo::getObjectId).collect(Collectors.toList())
                        .forEach(objectIds::remove);
                remainingObjectIds.addAll(objectIds);
            }
            objectInfoMap.putAll(sessionObjectsInfo);


            Set<Integer> typeIds = objectInfoMap.values().stream().map(ObjectInfo::getTypeId)
                    .collect(Collectors.toSet());

            Map<Integer, TypeInfo> sessionTypeInfo = archiveIndex.getTypesByIdWithLongKeys(typeIds);
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

                    sessionObjectsInfo.values().stream().map(ObjectInfo::getObjectId).collect(Collectors.toList())
                            .forEach(objectIds::remove);
                }
                objectInfoMap.putAll(sessionObjectsInfo);


                NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                        INDEX_STRING_DAT_FILE.getFileName());
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

                    sessionStringInfo.values().stream().map(StringInfo::getStringId).collect(Collectors.toList())
                            .forEach(remainingStringIds::remove);
                }

                stringInfoMap.putAll(sessionStringInfo);


                Set<Integer> typeIds = objectInfoMap.values().stream().map(ObjectInfo::getTypeId)
                        .collect(Collectors.toSet());

                Map<Integer, TypeInfo> sessionTypeInfo = archiveIndex.getTypesByIdWithLongKeys(typeIds);
                if (sessionTypeInfo.size() < typeIds.size()) {
                    logger.warn("expected [" + typeIds.size() + "] type info but got only: " + sessionTypeInfo.size());
                }
                typeInfoMap.putAll(sessionTypeInfo);


            }


        }


        // todo: rework this to use the indexes
        return new ReplayData(null, filteredDataEventsRequest, dataEventList, classInfoIndex, probeInfoIndex,
                stringInfoMap, objectInfoMap, typeInfoMap, methodInfoIndex);

    }

    public void scanDataAndBuildReplay() throws Exception {

        long scanStart = System.currentTimeMillis();

        LinkedList<File> sessionArchivesLocal = new LinkedList<>(this.sessionArchives);

        checkProgressIndicator(null, "Loading class mappings to scan events");

        AtomicInteger index = new AtomicInteger(0);
        List<Long> valueStack = new LinkedList<>();

        Map<String, VariableContainer> classStaticFieldMap = new HashMap<>();

        long currentCallId = daoService.getMaxCallId();


        Map<String, ArchiveFile> archiveFileMap = getArchiveFileMap(daoService);
        Map<String, LogFile> logFileMap = getLogFileMap(daoService);
        Map<String, List<LogFile>> filesByArchive = logFileMap.values().stream()
                .collect(Collectors.groupingBy(LogFile::getArchiveName));

        Set<Integer> existingProbes = new HashSet<>();
        try {
            existingProbes = new HashSet<>(daoService.getProbes());
        } catch (SQLiteException se) {
            logger.warn("no existing probes found - " + se.getMessage());
        }

        List<Integer> threadsProcessed = new LinkedList<>();
        List<Integer> threadsPending = new LinkedList<>();

        threadsPending.add(0);

        Map<String, Boolean> objectIndexRead = new HashMap<>();

        int currentFile = 0;
        int totalFileCount = 0;
        while (threadsPending.size() > 0) {

            List<MethodCallExpression> callStack = new LinkedList<>();

            AtomicReference<MethodCallExpression> theCallWhichJustReturned = new AtomicReference<>();
            List<String> upcomingObjectTypeStack = new LinkedList<>();
            List<TestCandidateMetadata> testCandidateMetadataStack = new LinkedList<>();

            Integer threadId = threadsPending.remove(0);
            for (File sessionArchive : sessionArchivesLocal) {
                ArchiveFile archiveFile = archiveFileMap.get(sessionArchive.getName());
                if (archiveFile != null && archiveFile.getStatus().equals(COMPLETED)) {
                    continue;
                }
                checkProgressIndicator("Processing archive: " + sessionArchive.getName(), null);

                if (archiveFile == null) {
                    archiveFile = new ArchiveFile();
                    archiveFile.setStatus(PENDING);
                    archiveFile.setName(sessionArchive.getName());
                    daoService.updateArchiveFile(archiveFile);
                }

                if (!objectIndexRead.containsKey(sessionArchive.getName())) {
                    objectIndexRead.put(sessionArchive.getName(), true);
                    NameWithBytes objectIndex = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                            INDEX_OBJECT_DAT_FILE.getFileName());
                    assert objectIndex != null;
                    ArchiveIndex archiveObjectIndex = readArchiveIndex(objectIndex.getBytes(), INDEX_OBJECT_DAT_FILE);
                    archiveObjectIndex.getObjectIndex().parallelStream()
                            .forEach(e -> objectInfoIndex.put(e.getObjectId(), e));
                }

                List<String> archiveFiles = filesByArchive.getOrDefault(sessionArchive.getName(), List.of()).stream()
                        .map(LogFile::getName).collect(Collectors.toList());

                if (archiveFiles.size() == 0) {
                    archiveFiles = listArchiveFiles(sessionArchive);
                }
                totalFileCount += archiveFiles.size();

                if (archiveFiles.size() == 0) {
                    archiveFile.setStatus(COMPLETED);
                    daoService.updateArchiveFile(archiveFile);
                    continue;
                }

                for (String file : archiveFiles) {
                    LogFile logFile = logFileMap.get(file);
                    if (logFile == null) {
                        logFile = new LogFile();
                        logFile.setName(file);
                        logFile.setArchiveName(archiveFile.getName());
                        logFile.setStatus(PENDING);
                        daoService.updateLogFile(logFile);
                        logFileMap.put(file, logFile);
                    }
                }

                boolean processedAllFiles = true;
                DatabaseVariableContainer parameterContainer = new DatabaseVariableContainer(daoService, archiveIndex);
                for (String logFile : archiveFiles) {


                    if (!logFile.endsWith(".selog")) {
                        continue;
                    }

                    currentFile++;
                    checkProgressIndicator(null, "Processing file " + currentFile + " / " + totalFileCount);


                    LogFile logFileEntry = logFileMap.get(logFile);
                    if (logFileEntry != null && logFileEntry.getStatus().equals(COMPLETED)) {
                        continue;
                    }


                    final int fileThreadId = getThreadIdFromFileName(logFile);
                    if (fileThreadId != threadId) {
                        processedAllFiles = false;
                        if (!threadsPending.contains(fileThreadId) && !threadsProcessed.contains(fileThreadId)) {
                            threadsPending.add(fileThreadId);
                        }
                        continue;
                    }


                    List<KaitaiInsidiousEventParser.Block> eventsSublist = getEventsFromFile(sessionArchive, logFile);
                    if (eventsSublist.size() == 0) {
                        continue;
                    }

                    List<DataEventWithSessionId> eventsToSave = new LinkedList<>();
                    List<DataInfo> probesToSave = new LinkedList<>();
                    Set<MethodCallExpression> callsToSave = new HashSet<>();
                    Set<MethodCallExpression> callsToUpdate = new HashSet<>();
                    List<TestCandidateMetadata> candidatesToSave = new LinkedList<>();
                    Date start = new Date();
                    for (KaitaiInsidiousEventParser.Block e : eventsSublist) {

                        KaitaiInsidiousEventParser.DetailedEventBlock eventBlock = e.block();
                        DataEventWithSessionId dataEvent = null;


                        final long eventValue = eventBlock.valueId();

//                        DataInfo probeInfo = probeInfoMap.get(eventBlock.probeId());
                        DataInfo probeInfo = probeInfoIndex.get((int) eventBlock.probeId());
                        Parameter existingParameter = null;
                        boolean saveProbe = false;
                        MethodCallExpression exceptionCallExpression;
                        TestCandidateMetadata completedExceptional;
                        MethodCallExpression methodCall;
                        boolean isModified;
                        String nameFromProbe;
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
                                    nameFromProbe = probeInfo.getAttribute("Name",
                                            probeInfo.getAttribute("FieldName", null));
                                    if (!existingParameter.hasName(nameFromProbe)) {
                                        existingParameter.addName(nameFromProbe);
                                    } else {
                                        existingParameter = null;
                                    }
                                }

                                break;

                            case LOCAL_LOAD:
                                if (eventValue == 0) {
                                    continue;
                                }
                                existingParameter = parameterContainer.getParameterByValue(eventValue);

                                String nameForParameter = probeInfo.getAttribute("Name",
                                        probeInfo.getAttribute("FieldName", null));
                                if (!existingParameter.hasName(nameForParameter)) {
                                    existingParameter.addName(nameForParameter);
                                    existingParameter.setType(
                                            ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V")));
                                } else {
                                    // set it to null because we don't need to save this again.
                                    existingParameter = null;
                                }


                                break;

                            case GET_STATIC_FIELD:
                                callStack.get(callStack.size() - 1).setUsesFields(true);
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                if (existingParameter != null) {
                                    nameFromProbe = probeInfo.getAttribute("Name",
                                            probeInfo.getAttribute("FieldName", null));
                                    isModified = false;
                                    if (!existingParameter.hasName(nameFromProbe)) {
                                        existingParameter.addName(nameFromProbe);
                                        isModified = true;
                                    }
                                    if (existingParameter.getProbeInfo() == null) {
                                        existingParameter.setType(
                                                ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V")));

                                        dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                        existingParameter.setProbeInfo(probeInfo);
                                        existingParameter.setProb(dataEvent);

                                        saveProbe = true;
                                        isModified = true;
                                    }
                                    if (!isModified) {
                                        existingParameter = null;
                                    }
                                }


                                break;

                            case GET_INSTANCE_FIELD_RESULT:
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                nameFromProbe = probeInfo.getAttribute("Name",
                                        probeInfo.getAttribute("FieldName", null));
                                isModified = false;
                                if (!existingParameter.hasName(nameFromProbe)) {
                                    isModified = true;
                                    existingParameter.addName(nameFromProbe);
                                }
                                String typeFromProbe = ClassTypeUtils.getDottedClassName(
                                        probeInfo.getAttribute("Type", "V"));
                                if (existingParameter.getType() == null || !existingParameter.getType()
                                        .equals(typeFromProbe)) {
                                    existingParameter.setType(ClassTypeUtils.getDottedClassName(typeFromProbe));
                                }
                                saveProbe = true;
                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                existingParameter.setProbeInfo(probeInfo);
                                existingParameter.setProb(dataEvent);
                                testCandidateMetadataStack.get(testCandidateMetadataStack.size() - 1).getFields()
                                        .add(existingParameter);
                                callStack.get(callStack.size() - 1).setUsesFields(true);
                                if (!isModified) {
                                    existingParameter = null;
                                }

                                break;

                            case PUT_INSTANCE_FIELD:


                                // we are going to set this field in the next event
                                valueStack.add(eventValue);
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                if (existingParameter != null && existingParameter.getProb() != null) {
                                    if (existingParameter.getType() == null || existingParameter.getType()
                                            .contains(".Object")) {
                                        existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                                ClassTypeUtils.getDottedClassName(
                                                        probeInfo.getAttribute("Owner", "V"))));

                                    } else {
                                        existingParameter = null;
                                    }
                                } else {
                                    // new variable identified ?
                                    dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                    existingParameter = parameterContainer.getParameterByValue(eventValue);
                                    existingParameter.setProbeInfo(probeInfo);
                                    existingParameter.setProb(dataEvent);
                                    existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                            ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V"))));

                                    existingParameter.addName(
                                            probeInfo.getAttribute("Name", probeInfo.getAttribute("FieldName", null)));
                                }

                                break;

                            case PUT_INSTANCE_FIELD_VALUE:


                                Long parentValue = valueStack.remove(valueStack.size() - 1);
                                Parameter valueParameter = parameterContainer.getParameterByValue(parentValue);
                                VariableContainer parentFields = valueParameter.getFields();


                                existingParameter = parentFields.getParametersById(eventValue);
                                if (existingParameter != null) {
                                    nameFromProbe = probeInfo.getAttribute("Name",
                                            probeInfo.getAttribute("FieldName", null));
                                    if (!existingParameter.hasName(nameFromProbe)) {
                                        existingParameter.addName(nameFromProbe);
                                    } else {
                                        existingParameter = null;
                                    }
                                } else {
                                    // new field
                                    dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                    existingParameter = parameterContainer.getParameterByValue(eventValue);
                                    existingParameter.setType(
                                            ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V")));

                                    existingParameter.addName(
                                            probeInfo.getAttribute("Name", probeInfo.getAttribute("FieldName", null)));
                                    existingParameter.setProbeInfo(probeInfo);
                                    existingParameter.setProb(dataEvent);

                                    saveProbe = true;

                                    parentFields.add(existingParameter);
                                }
                                break;


                            case PUT_STATIC_FIELD:


                                VariableContainer classStaticFieldContainer = classStaticFieldMap.computeIfAbsent(
                                        ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Owner", null)),
                                        (e1) -> new VariableContainer());
                                isModified = false;
                                existingParameter = classStaticFieldContainer.getParametersById(eventValue);
                                if (existingParameter != null) {
                                    // field is already present and we are overwriting it here
                                    // setting this to null so it is not inserted into the database again
                                    existingParameter = null;
                                } else {
                                    existingParameter = parameterContainer.getParameterByValue(eventValue);
                                    if (existingParameter.getProb() == null) {
                                        // we are coming across this field for the first time
                                        dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                        existingParameter = parameterContainer.getParameterByValue(eventValue);
                                        existingParameter.addName(probeInfo.getAttribute("Name",
                                                probeInfo.getAttribute("FieldName", null)));
                                        existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                                ClassTypeUtils.getDottedClassName(
                                                        probeInfo.getAttribute("Type", "V"))));

                                        existingParameter.setProbeInfo(probeInfo);
                                        existingParameter.setProb(dataEvent);

                                        saveProbe = true;

                                        classStaticFieldContainer.add(existingParameter);

                                    } else {
                                        nameFromProbe = probeInfo.getAttribute("Name",
                                                probeInfo.getAttribute("FieldName", null));
                                        if (!existingParameter.hasName(nameFromProbe)) {
                                            existingParameter.addName(nameFromProbe);
                                            isModified = true;
                                        }
                                        typeFromProbe = ClassTypeUtils.getDottedClassName(
                                                probeInfo.getAttribute("Type", "V"));
                                        if (existingParameter.getType() == null || !existingParameter.getType()
                                                .equals(typeFromProbe)) {
                                            existingParameter.setType(ClassTypeUtils.getDottedClassName(typeFromProbe));
                                            isModified = true;
                                        }
                                        classStaticFieldContainer.add(existingParameter);
                                        if (!isModified) {
                                            existingParameter = null;
                                        }
                                    }
                                }

                                break;

                            case CALL:
                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                saveProbe = true;
                                isModified = false;

                                if (existingParameter.getProbeInfo() == null) {
                                    existingParameter.setProbeInfo(probeInfo);
                                    existingParameter.setProb(dataEvent);
                                    isModified = eventValue != 0;
                                }
                                if ((existingParameter.getType() == null || existingParameter.getType()
                                        .equals("java.lang.Object"))) {
                                    existingParameter.setType(
                                            ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Owner", "V")));
                                    isModified = eventValue != 0;
                                }

                                currentCallId++;
                                String methodName = probeInfo.getAttribute("Name", null);

                                methodCall = new MethodCallExpression(methodName, existingParameter, new LinkedList<>(),
                                        null, callStack.size());
                                methodCall.setEntryProbeInfo(probeInfo);
                                methodCall.setEntryProbe(dataEvent);
                                methodCall.setId(currentCallId);

                                if ("Static".equals(probeInfo.getAttribute("CallType", null))) {
                                    methodCall.setStaticCall(true);
                                    methodCall.setSubject(existingParameter);
                                }


                                callStack.add(methodCall);
                                addMethodToCandidate(testCandidateMetadataStack, methodCall);
                                if (!isModified) {
                                    existingParameter = null;
                                }


                                break;


                            case CALL_PARAM:
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                MethodCallExpression currentMethodCallExpression = callStack.get(callStack.size() - 1);
                                isModified = false;
                                if ((existingParameter.getType() == null || existingParameter.getType()
                                        .endsWith(".Object"))) {
                                    existingParameter.setType(
                                            ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V")));
                                    // TODO: This is getting ugly, but
                                    // we need some way to prefer some kind of events/probes combination
                                    // over other kind of events/probes
                                    // for instance events which are of type CALL_RETURN/CALL_PARAM are going to have
                                    // the serialized value which we use to recreate the object, but for fields we need
                                    // the events/probes of type GET_INSTANCE_FIELD_RESULT/PUT since we use those
                                    // to identify if a parameter is a field of a class
                                    // (and so mock the calls on those parameters)
                                    // maybe we need to restructure the parameter class to store this information
                                    // instead of storing the event/probe
                                    if (existingParameter.getProbeInfo() == null || (!existingParameter.getProbeInfo()
                                            .getEventType()
                                            .equals(PUT_INSTANCE_FIELD_VALUE) && !existingParameter.getProbeInfo()
                                            .getEventType().equals(GET_INSTANCE_FIELD_RESULT))) {
                                        existingParameter.setProbeInfo(probeInfo);
                                        existingParameter.setProb(dataEvent);
                                        isModified = true;
                                    }
                                }
                                saveProbe = true;
                                currentMethodCallExpression.addArgument(existingParameter);
                                currentMethodCallExpression.addArgumentProbe(dataEvent);
                                if (!isModified) {
                                    existingParameter = null;
                                }
                                break;

                            case METHOD_ENTRY:
                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);

                                MethodInfo methodInfo = methodInfoIndex.get(probeInfo.getMethodId());
//                                MethodInfo methodInfo = methodInfoResult.iterator().next();
//                                methodInfoResult.close();

//                                MethodInfo methodInfo = methodInfoMap.get((long) probeInfo.getMethodId());
//                                if (methodInfo.getMethodName().equals("getAllDoctors")) {
//                                LoggerUtil.logEvent("SCAN", callStack.size(), instructionIndex, dataEvent, probeInfo, classInfoMap.get((long) probeInfo.getClassId()), methodInfo);
//                                }


                                methodCall = null;
                                // a method_entry event can come in without a corresponding event for call,
                                // in which case this is actually a separate method call
                                if (callStack.size() > 0) {
                                    methodCall = callStack.get(callStack.size() - 1);
                                    @NotNull String expectedClassName = ClassTypeUtils.getDottedClassName(
                                            methodInfo.getClassName());
                                    String owner = ClassTypeUtils.getDottedClassName(
                                            methodCall.getEntryProbeInfo().getAttribute("Owner", null));
                                    if (owner == null) {
                                        methodCall = null;
                                    } else {
                                        // sometimes we can enter a method_entry without a call
                                        if (!owner.startsWith(expectedClassName) || !methodInfo.getMethodName()
                                                .equals(methodCall.getMethodName())) {
                                            methodCall = null;
                                        }
                                    }
                                }

                                TestCandidateMetadata newCandidate = new TestCandidateMetadata();

                                newCandidate.setEntryProbeIndex(eventBlock.eventId());


                                isModified = false;
                                if (methodCall == null) {
                                    existingParameter = parameterContainer.getParameterByValue(eventValue);
                                    if (existingParameter.getProb() == null) {

                                        existingParameter.setProbeInfo(probeInfo);
                                        existingParameter.setProb(dataEvent);

                                        existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                                ClassTypeUtils.getDottedClassName(methodInfo.getClassName())));
                                        isModified = true;
                                    }
                                    if (existingParameter.getType() == null || existingParameter.getType()
                                            .equals("java.lang.Object")) {
                                        existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                                ClassTypeUtils.getDottedClassName(methodInfo.getClassName())));


                                        isModified = true;
                                    }


                                    currentCallId++;
                                    methodCall = new MethodCallExpression(methodInfo.getMethodName(), existingParameter,
                                            new LinkedList<>(), null, callStack.size());

                                    saveProbe = true;

                                    methodCall.setId(currentCallId);
                                    methodCall.setEntryProbeInfo(probeInfo);
                                    methodCall.setEntryProbe(dataEvent);
                                    if (testCandidateMetadataStack.size() > 0) {
                                        addMethodToCandidate(testCandidateMetadataStack, methodCall);
                                    }
                                    callStack.add(methodCall);
                                } else {
                                    saveProbe = true;
//                                    methodCall.setEntryProbeInfo(probeInfo);
//                                    methodCall.setEntryProbe(dataEvent);
                                }
                                newCandidate.setMainMethod(methodCall);
                                testCandidateMetadataStack.add(newCandidate);

                                int methodAccess = methodInfo.getAccess();
                                methodCall.setMethodAccess(methodAccess);
                                if (!isModified) {
                                    existingParameter = null;
                                }

                                break;


                            case METHOD_PARAM:

                                // if the caller was probed then we already have the method arguments
                                // in that case we can verify here
                                // else if the caller was a third party, then we need to extract parameters from here

                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                existingParameter = parameterContainer.getParameterByValue(eventValue);


                                isModified = false;
                                if (existingParameter.getProb() == null) {
                                    existingParameter.setProbeInfo(probeInfo);
                                    existingParameter.setProb(dataEvent);
                                    isModified = true;
                                }
                                saveProbe = true;

                                MethodCallExpression methodExpression = callStack.get(callStack.size() - 1);

                                EventType entryProbeEventType = methodExpression.getEntryProbeInfo().getEventType();
                                if (entryProbeEventType == EventType.CALL) {
                                    // not adding these since we will record method_params only for cases in which we dont have a method_entry probe
                                } else if (entryProbeEventType == EventType.METHOD_ENTRY) {
                                    methodExpression.addArgument(existingParameter);
                                    methodExpression.addArgumentProbe(dataEvent);
                                } else {
                                    throw new RuntimeException("unexpected entry probe event type");
                                }
                                if (!isModified) {
                                    existingParameter = null;
                                }
                                break;


                            case METHOD_EXCEPTIONAL_EXIT:
                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
//                                LoggerUtil.logEvent("SCAN", callStack.size(), instructionIndex, dataEvent, probeInfo, classInfo, methodInfo);

                                exceptionCallExpression = callStack.get(callStack.size() - 1);
                                entryProbeEventType = exceptionCallExpression.getEntryProbeInfo().getEventType();
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                if (existingParameter.getType() == null) {
                                    String typeName = ClassTypeUtils.getDottedClassName(typeInfoIndex.get(
                                                    objectInfoIndex.get(existingParameter.getValue()).getTypeId())
                                            .getTypeName());
                                    existingParameter.setType(typeName);
                                }

                                existingParameter.setProbeInfo(probeInfo);
                                existingParameter.setProb(dataEvent);
                                isModified = true;
                                saveProbe = true;


                                if (entryProbeEventType == EventType.CALL) {
                                    // we need to pop two calls here, since the CALL will not have a matching call_return
//
                                    MethodCallExpression topCall = callStack.remove(callStack.size() - 1);
                                    topCall.setReturnValue(existingParameter);
                                    topCall.setReturnDataEvent(dataEvent);
                                    callsToSave.add(topCall);
                                    theCallWhichJustReturned.set(topCall);
                                    if (callStack.get(callStack.size() - 1) == testCandidateMetadataStack.get(
                                            testCandidateMetadataStack.size() - 1).getMainMethod()) {
                                        topCall = callStack.remove(callStack.size() - 1);
                                        topCall.setReturnValue(existingParameter);
                                        topCall.setReturnDataEvent(dataEvent);
                                        callsToSave.add(topCall);
                                        theCallWhichJustReturned.set(topCall);
                                    } else {
                                        logger.warn("not popping second call");
                                    }

                                } else if (entryProbeEventType == EventType.METHOD_ENTRY) {
                                    // we need to pop only 1 call here from the stack
                                    MethodCallExpression topCall = callStack.remove(callStack.size() - 1);
                                    topCall.setReturnValue(existingParameter);
                                    topCall.setReturnDataEvent(dataEvent);
                                    callsToSave.add(topCall);
                                    theCallWhichJustReturned.set(topCall);

                                } else {
                                    throw new RuntimeException(
                                            "unexpected entry probe event type [" + entryProbeEventType + "]");
                                }


                                completedExceptional = testCandidateMetadataStack.remove(
                                        testCandidateMetadataStack.size() - 1);

                                completedExceptional.setExitProbeIndex(dataEvent.getNanoTime());
                                if (completedExceptional.getMainMethod() != null) {
                                    DataEventWithSessionId entryProbe = ((MethodCallExpression) (completedExceptional.getMainMethod())).getEntryProbe();
                                    if (entryProbe != null) {
                                        completedExceptional.setCallTimeNanoSecond(
                                                eventBlock.timestamp() - entryProbe.getRecordedAt());
                                    }
                                }
                                if (completedExceptional.getMainMethod() != null) {
                                    completedExceptional.setTestSubject(
                                            ((MethodCallExpression) completedExceptional.getMainMethod()).getSubject());
                                }


                                if (testCandidateMetadataStack.size() > 0) {
                                    TestCandidateMetadata newCurrent = testCandidateMetadataStack.get(
                                            testCandidateMetadataStack.size() - 1);
                                    newCurrent.addAllMethodCall(completedExceptional.getCallsList());

                                    if (((MethodCallExpression) newCurrent.getMainMethod()).getSubject().getType()
                                            .equals(((MethodCallExpression) completedExceptional.getMainMethod()).getSubject()
                                                    .getType())) {
                                        for (Parameter parameter : completedExceptional.getFields().all()) {
                                            newCurrent.getFields().add(parameter);
                                        }
                                    }

                                } else {
                                    if (callStack.size() > 0) {
                                        logger.warn("inconsistent call stack state, flushing calls list");
                                    }
                                }

                                if (completedExceptional.getTestSubject() != null) {
                                    candidatesToSave.add(completedExceptional);
                                }
                                if (!isModified) {
                                    existingParameter = null;
                                }
                                break;


                            case METHOD_NORMAL_EXIT:

                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
//                                LoggerUtil.logEvent("SCAN", callStack.size(), instructionIndex, dataEvent, probeInfo, classInfo, methodInfo);

                                MethodCallExpression currentCallExpression = callStack.get(callStack.size() - 1);
                                entryProbeEventType = currentCallExpression.getEntryProbeInfo().getEventType();
                                existingParameter = parameterContainer.getParameterByValue(eventValue);
                                isModified = false;
                                saveProbe = true;
                                if (existingParameter.getProb() == null || existingParameter.getProbeInfo() == null) {
                                    existingParameter.setProbeInfo(probeInfo);
                                    existingParameter.setProb(dataEvent);
                                    isModified = eventValue != 0;
                                }

                                if (entryProbeEventType == EventType.CALL) {
                                    // we dont pop it here, wait for the CALL_RETURN to pop the call

                                } else if (entryProbeEventType == EventType.METHOD_ENTRY || probeInfo.getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT) {
                                    // we can pop the current call here since we never had the CALL event in the first place
                                    // this might be going out of our hands
                                    MethodCallExpression topCall = callStack.remove(callStack.size() - 1);


                                    if (topCall.getMethodName().startsWith("<")) {
                                        topCall.setReturnValue(topCall.getSubject());

                                    } else {
                                        topCall.setReturnValue(existingParameter);
                                    }
                                    topCall.setReturnDataEvent(dataEvent);
                                    callsToSave.add(topCall);
                                    theCallWhichJustReturned.set(topCall);

                                } else {
                                    throw new RuntimeException(
                                            "unexpected entry probe event type [" + entryProbeEventType + "]");
                                }


                                TestCandidateMetadata completed = testCandidateMetadataStack.remove(
                                        testCandidateMetadataStack.size() - 1);

                                completed.setExitProbeIndex(eventBlock.eventId());
                                if (completed.getMainMethod() != null) {
                                    DataEventWithSessionId entryProbe = ((MethodCallExpression) (completed.getMainMethod())).getEntryProbe();
                                    if (entryProbe != null) {
                                        completed.setCallTimeNanoSecond(
                                                eventBlock.timestamp() - entryProbe.getRecordedAt());
                                    }
                                }
                                if (completed.getMainMethod() != null) {
                                    completed.setTestSubject(
                                            ((MethodCallExpression) completed.getMainMethod()).getSubject());
                                }

                                if (testCandidateMetadataStack.size() > 0) {
                                    TestCandidateMetadata newCurrent = testCandidateMetadataStack.get(
                                            testCandidateMetadataStack.size() - 1);
                                    newCurrent.addAllMethodCall(completed.getCallsList());
                                    Parameter completedCallSubject = ((MethodCallExpression) completed.getMainMethod()).getSubject();
                                    Parameter newCurrentCallSubject = ((MethodCallExpression) newCurrent.getMainMethod()).getSubject();
                                    if (newCurrentCallSubject.getType().equals(completedCallSubject.getType())) {
                                        for (Parameter parameter : completed.getFields().all()) {
                                            newCurrent.getFields().add(parameter);
                                        }
                                    }


//                                newCurrent.getFields().all().addAll(completed.gertFields().all());
                                } else {
                                    if (callStack.size() > 0) {
                                        logger.warn("inconsistent call stack state, flushing calls list");
                                        callStack.clear();

                                    }
                                }

                                if (completed.getTestSubject() != null) {
                                    candidatesToSave.add(completed);
                                }
                                if (!isModified) {
                                    existingParameter = null;
                                }
                                break;

                            case CALL_RETURN:

                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
//                                LoggerUtil.logEvent("SCAN", callStack.size(), instructionIndex, dataEvent, probeInfo, classInfo, methodInfo);
                                existingParameter = parameterContainer.getParameterByValue(eventValue);

                                isModified = false;
                                saveProbe = true;
                                if ((existingParameter.getType() == null || existingParameter.getType()
                                        .endsWith(".Object"))) {
                                    existingParameter.setProbeInfo(probeInfo);
                                    existingParameter.setProb(dataEvent);
                                    saveProbe = true;
                                    existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                            ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V"))));

                                    isModified = true;
                                }

                                MethodCallExpression callExpression = callStack.get(callStack.size() - 1);
                                EventType entryEventType = callExpression.getEntryProbeInfo().getEventType();
                                if (entryEventType == EventType.CALL) {
                                    // we pop it now

                                    MethodCallExpression topCall = callStack.remove(callStack.size() - 1);
                                    topCall.setReturnValue(existingParameter);
                                    topCall.setReturnDataEvent(dataEvent);
                                    callsToSave.add(topCall);
                                    theCallWhichJustReturned.set(topCall);

                                } else if (entryEventType == EventType.METHOD_ENTRY) {
                                    // this is probably not a matching event
                                } else {
                                    throw new RuntimeException("this should not happen");
                                }
                                if (!isModified) {
                                    existingParameter = null;
                                }

                                break;
                            case NEW_OBJECT:
                                upcomingObjectTypeStack.add(
                                        ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V")));
                                break;
                            case NEW_OBJECT_CREATED:
                                MethodCallExpression theCallThatJustEnded = theCallWhichJustReturned.get();
                                String upcomingObjectType = upcomingObjectTypeStack.remove(
                                        upcomingObjectTypeStack.size() - 1);
                                existingParameter = theCallThatJustEnded.getSubject();
                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                existingParameter.setProbeInfo(probeInfo);
                                existingParameter.setProb(dataEvent);
                                existingParameter.setType(ClassTypeUtils.getDottedClassName(upcomingObjectType));
                                theCallThatJustEnded.setReturnValue(existingParameter);
                                if (!callsToSave.contains(theCallThatJustEnded)) {
                                    callsToUpdate.add(theCallThatJustEnded);
                                }
                                saveProbe = true;
                                break;

                            case METHOD_OBJECT_INITIALIZED:
                                MethodCallExpression topCall = callStack.get(callStack.size() - 1);
                                existingParameter = topCall.getSubject();
                                dataEvent = createDataEventFromBlock(fileThreadId, eventBlock);
                                existingParameter.setProbeInfo(probeInfo);
                                existingParameter.setProb(dataEvent);
                                saveProbe = true;
                                topCall.setSubject(existingParameter);
                                topCall.setReturnValue(existingParameter);

                                break;
                        }
                        if (saveProbe) {
//                            logger.warn("save probe: " + dataEvent);
                            eventsToSave.add(dataEvent);
                            if (!existingProbes.contains(probeInfo.getDataId())) {
                                probesToSave.add(probeInfo);
                                existingProbes.add(probeInfo.getDataId());
                            }
                        }
                        if (existingParameter != null && existingParameter.getProb() != null && existingParameter.getValue() != 0) {
                            parameterContainer.add(existingParameter);
                        }
                    }
                    long timeInMs = (new Date().getTime() - start.getTime()) + 1;
                    logger.warn(
                            "[" + logFile + "] Took [" + timeInMs + " ms] to process [" + eventsSublist.size() + " events] => " + (1000 * eventsSublist.size()) / timeInMs + " events per second");
                    logger.debug("parameterContainer = " + parameterContainer.all()
                            .size() + ",  eventsToSave = " + eventsToSave.size() + ",  probesToSave = " + probesToSave.size());
                    eventsSublist = null;

//                    logger.warn("Saving [" + eventsToSave.size() + " events] [" + probesToSave.size() + " probes]");

                    // using the database pipe for saving events results in some gap between the database actually
                    // being updated and the time when we query the database for this same item immediately obviously
                    // which breaks the scan implementation because it was expecting updated information and got stale
                    // information from db, because we did not wait for the update to happen.
//                    databasePipe.addDataEvents(eventsToSave);
//                    databasePipe.addProbeInfo(probesToSave);
//                    databasePipe.addMethodCallsToSave(callsToSave);
//                    databasePipe.addMethodCallsToUpdate(callsToUpdate);
//                    databasePipe.addTestCandidates(candidatesToSave);

                    daoService.createOrUpdateDataEvent(eventsToSave);
                    daoService.createOrUpdateProbeInfo(probesToSave);

                    // this is saving/updating all parameters every single time.
                    // need to save/update only modified parameters

                    daoService.createOrUpdateCall(callsToSave);
                    daoService.updateCalls(callsToUpdate);
                    daoService.createOrUpdateTestCandidate(candidatesToSave);

                    logFileEntry.setStatus(COMPLETED);
                    daoService.updateLogFile(logFileEntry);

                }
                // there would a need to verify that the parameter types we have identified from probes are consistent
                // with the information we have in the object -> type index.
                // the information in objectInfo ->  typeInfo would be more specific than the information we get from
                // probe attributes
//                Map<Long, ObjectInfo> objectInfoMap = archiveIndex
//                        .getObjectsByObjectId(parameterContainer.all().stream().map(e -> e.getValue()).collect(Collectors.toList()));
//                List<Parameter> parametersToSave = parameterContainer.all();
//                for (Parameter parameter : parametersToSave) {
//                    ObjectInfo objectInfo = objectInfoMap.get(parameter.getValue());
//                    if (objectInfo == null) {
//                        continue;
//                    }
//                    TypeInfo objectType = archiveIndex.getTypeById((int) objectInfo.getTypeId());
//                    if (objectType != null) {
//                    }
//                }


                databasePipe.addParameters(parameterContainer.all());
                if (processedAllFiles) {
                    archiveFile.setStatus(COMPLETED);
                }
                daoService.updateArchiveFile(archiveFile);

            }
            if (callStack.size() > 0) {
                logger.warn("call stack is not 0, should it be ? - " + callStack.size());
            }
            threadsProcessed.add(threadId);
        }
        databasePipe.close();
        daoService.close();
        try {
            long scanEndtime = System.currentTimeMillis();
            float scanTime = (scanEndtime - scanStart) / 1000;
            File sessionDir = new File(this.sessionDirectory.getParent());
            long size_folder = getFolderSize(sessionDir);
            long archives_size = 0;
            for (File f : this.sessionArchives) {
                long sizeInBytes = f.length();
                archives_size += sizeInBytes;
            }
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("session_scan_time", scanTime);
            eventProperties.put("session_folder_size", (size_folder / 1000000));
            eventProperties.put("session_scanned_archives_size", (archives_size / 1000000));
            UsageInsightTracker.getInstance().RecordEvent("ScanMetrics", eventProperties);
        } catch (Exception e) {
            logger.error("Exception recording folder size and scan time : " + e);
            e.printStackTrace();
        }


    }

    private long getFolderSize(File folder) {
        long length = 0;
        File[] files = folder.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                length += files[i].length();
            } else {
                length += getFolderSize(files[i]);
            }
        }
        return length;
    }

    public DaoService getDaoService() {
        return daoService;
    }

    private long eventId(KaitaiInsidiousEventParser.Block lastEvent) {
        KaitaiInsidiousEventParser.DetailedEventBlock eventBlock = lastEvent.block();
        return eventBlock.eventId();
    }

    public void queryTracePointsByTypes(SearchQuery searchQuery, ClientCallBack<TracePoint> clientCallBack) {

        int totalCount = sessionArchives.size();
        int currentCount = 0;
        int totalMatched = 0;

        for (File sessionArchive : sessionArchives) {
            currentCount++;

            checkProgressIndicator("Checking archive " + sessionArchive.getName(), null);

            Collection<Long> objectIds = queryObjectsByTypeFromSessionArchive(searchQuery, sessionArchive).stream()
                    .map(ObjectInfoDocument::getObjectId).collect(Collectors.toSet());

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

        tracePointList = queryTracePointsByValue(searchQuery);

        getProjectSessionErrorsCallback.success(tracePointList);

    }

    public List<TestCandidateMetadata> getTestCandidatesForClass(String className) {
        return daoService.getTestCandidatesForClass(className);
    }

    public List<VideobugTreeClassAggregateNode> getTestCandidateAggregates() {
        return daoService.getTestCandidateAggregates();
    }

    public List<TestCandidateMethodAggregate> getTestCandidateAggregatesByClassName(String className) {
        return daoService.getTestCandidateAggregatesForType(className);
    }

    public List<TestCandidateMetadata> getTestCandidatesForMethod(String className, String methodName, boolean loadCalls) {
        return daoService.getTestCandidatesForMethod(className, methodName, loadCalls);
    }

    public List<TestCandidateMetadata> getTestCandidatesUntil(long subjectId, long entryProbeIndex, long mainMethodId, boolean loadCalls) {
        return daoService.getTestCandidates(subjectId, entryProbeIndex, mainMethodId, loadCalls);
    }

    public TestCandidateMetadata getTestCandidateById(Long testCandidateId) {
        return daoService.getTestCandidateById(testCandidateId);
    }

    public void close() throws Exception {
        executorPool.shutdownNow();
        daoService.close();

        classInfoIndex.close();
        probeInfoIndex.close();
        methodInfoIndex.close();

        archiveIndex = null;
    }

    class DatabasePipe implements Runnable {

        private final LinkedTransferQueue<Parameter> parameterQueue;
        private final ArrayBlockingQueue<Boolean> isSaving = new ArrayBlockingQueue<>(1);
        private final List<DataEventWithSessionId> eventsToSave = new LinkedList<>();
        private final List<DataInfo> dataInfoList = new LinkedList<>();
        private final List<MethodCallExpression> methodCallToSave = new LinkedList<>();
        private final List<MethodCallExpression> methodCallToUpdate = new LinkedList<>();
        private final List<TestCandidateMetadata> testCandidateMetadataList = new LinkedList<>();
        private boolean stop = false;

        public DatabasePipe(LinkedTransferQueue<Parameter> parameterQueue) {
            this.parameterQueue = parameterQueue;
        }

        @Override
        public void run() {
            while (!stop) {
                Parameter param = null;
                try {
                    param = parameterQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.warn("database pipe interrupted - " + e.getMessage());
                    throw new RuntimeException(e);
                }

                if (dataInfoList.size() > 0) {
                    Collection<DataInfo> saving = new LinkedList<>();
                    dataInfoList.removeAll(saving);
                    daoService.createOrUpdateProbeInfo(saving);
                }

                if (eventsToSave.size() > 0) {
                    Collection<DataEventWithSessionId> saving = new LinkedList<>();
                    eventsToSave.removeAll(saving);
                    daoService.createOrUpdateDataEvent(saving);
                }
                if (methodCallToSave.size() > 0) {
                    Collection<MethodCallExpression> saving = new LinkedList<>();
                    methodCallToSave.removeAll(saving);
                    daoService.createOrUpdateCall(saving);
                }
                if (methodCallToUpdate.size() > 0) {
                    Collection<MethodCallExpression> saving = new LinkedList<>();
                    methodCallToUpdate.removeAll(saving);
                    daoService.updateCalls(saving);
                }
                if (testCandidateMetadataList.size() > 0) {
                    Collection<TestCandidateMetadata> saving = new LinkedList<>();
                    testCandidateMetadataList.removeAll(saving);
                    daoService.createOrUpdateTestCandidate(saving);
                }

                if (param == null) {
                    continue;
                }
                List<Parameter> batch = new LinkedList<>();
                parameterQueue.drainTo(batch);
                batch.add(param);
                logger.warn("Saving " + batch.size() + " parameters");
                daoService.createOrUpdateParameter(batch);
            }
            isSaving.offer(true);
        }

        public void close() throws InterruptedException {
            stop = true;
            isSaving.take();
            logger.warn("saving after close");
            List<Parameter> batch = new LinkedList<>();
            parameterQueue.drainTo(batch);
            daoService.createOrUpdateParameter(batch);


            if (dataInfoList.size() > 0) {
                Collection<DataInfo> saving = new LinkedList<>();
                dataInfoList.removeAll(saving);
                daoService.createOrUpdateProbeInfo(saving);
            }

            if (eventsToSave.size() > 0) {
                Collection<DataEventWithSessionId> saving = new LinkedList<>();
                eventsToSave.removeAll(saving);
                daoService.createOrUpdateDataEvent(saving);
            }
            if (methodCallToSave.size() > 0) {
                Collection<MethodCallExpression> saving = new LinkedList<>();
                methodCallToSave.removeAll(saving);
                daoService.createOrUpdateCall(saving);
            }
            if (methodCallToUpdate.size() > 0) {
                Collection<MethodCallExpression> saving = new LinkedList<>();
                methodCallToUpdate.removeAll(saving);
                daoService.updateCalls(saving);
            }
            if (testCandidateMetadataList.size() > 0) {
                Collection<TestCandidateMetadata> saving = new LinkedList<>();
                testCandidateMetadataList.removeAll(saving);
                daoService.createOrUpdateTestCandidate(saving);
            }

        }

        public void addParameters(List<Parameter> all) {
            parameterQueue.addAll(all);
        }

        public void addDataEvents(List<DataEventWithSessionId> events) {
            eventsToSave.addAll(events);
        }

        public void addProbeInfo(List<DataInfo> probesToSave) {
            dataInfoList.addAll(probesToSave);
        }

        public void addMethodCallsToSave(Set<MethodCallExpression> callsToSave) {
            methodCallToSave.addAll(callsToSave);
        }

        public void addMethodCallsToUpdate(Set<MethodCallExpression> callsToUpdate) {
            methodCallToUpdate.addAll(callsToUpdate);
        }

        public void addTestCandidates(List<TestCandidateMetadata> candidatesToSave) {
            testCandidateMetadataList.addAll(candidatesToSave);
        }
    }
}
