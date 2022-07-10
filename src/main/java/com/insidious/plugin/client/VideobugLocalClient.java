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
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.cache.ArchiveIndex;
import com.insidious.plugin.client.exception.ClassInfoNotFoundException;
import com.insidious.plugin.client.pojo.*;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import io.kaitai.struct.ByteBufferKaitaiStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.googlecode.cqengine.query.QueryFactory.in;
import static com.insidious.plugin.client.DatFileType.*;

public class VideobugLocalClient implements VideobugClientInterface {

    private static final Logger logger = Logger.getInstance(VideobugLocalClient.class.getName());
    private final String pathToSessions;
    private final Map<String, ClassInfo> classInfoMap = new HashMap<>();
    private final VideobugNetworkClient networkClient;
    private final Map<String, ArchiveIndex> indexCache = new HashMap<>();
    private final ScheduledExecutorService threadPoolExecutor5Seconds = Executors.newScheduledThreadPool(1);
    private ExecutionSession session;
    private ProjectItem currentProject;
    private KaitaiInsidiousClassWeaveParser classWeaveInfo;
    private List<File> sessionArchives;
    private final Map<String, NameWithBytes> entryCache = new HashMap<>();

    public VideobugLocalClient(String pathToSessions) {
        if (!pathToSessions.endsWith("/")) {
            pathToSessions = pathToSessions + "/";
        }
        this.pathToSessions = pathToSessions;
        this.networkClient = new VideobugNetworkClient("https://cloud.bug.video");
    }

    @Override
    public ExecutionSession getCurrentSession() {
        return this.session;
    }

    @Override
    public void setSession(ExecutionSession session) {
        this.session = session;
        this.sessionArchives = refreshSessionArchivesList(session.getSessionId());
    }

    @Override
    public void signup(String serverUrl, String username, String password, SignUpCallback callback) {
        callback.success();
    }

    @Override
    public void signin(SigninRequest signinRequest, SignInCallback signInCallback) {
        signInCallback.success("localhost-token");
    }

    @Override
    public void getProjectByName(String projectName, GetProjectCallback getProjectCallback) {
        getProjectCallback.success(projectName);
    }

    @Override
    public ProjectItem fetchProjectByName(String projectName) {
        ProjectItem projectItem = new ProjectItem();
        projectItem.setName(projectName);
        projectItem.setId("1");
        projectItem.setCreatedAt(new Date().toString());
        return projectItem;
    }

    @Override
    public void createProject(String projectName, NewProjectCallback newProjectCallback) {
        newProjectCallback.success("1");
    }

    @Override
    public void getProjectToken(ProjectTokenCallback projectTokenCallback) {
        projectTokenCallback.success("localhost-token");
    }

    @Override
    public void getProjectSessions(GetProjectSessionsCallback getProjectSessionsCallback) throws IOException {
        getProjectSessionsCallback.success(getLocalSessions());
    }

    private List<ExecutionSession> getLocalSessions() {
        List<ExecutionSession> list = new LinkedList<>();
        logger.info(String.format("looking for sessions in [%s]", pathToSessions));
        File currentDir = new File(pathToSessions);
        if (!currentDir.exists()) {
            currentDir.mkdirs();
            return List.of();
        }
        File[] sessionDirectories = currentDir.listFiles();
        if (sessionDirectories == null) {
            return List.of();
        }
        for (File file : sessionDirectories) {
            if (file.isDirectory() && file.getName().contains("selogger")) {
                ExecutionSession executionSession = new ExecutionSession();
                executionSession.setSessionId(file.getName());
                executionSession.setCreatedAt(new Date(file.lastModified()));
                executionSession.setHostname("localhost");
                executionSession.setLastUpdateAt(file.lastModified());
                list.add(executionSession);
            }
        }

        list.sort(Comparator.comparing(ExecutionSession::getSessionId));
        Collections.reverse(list);
        int i = -1;
        if (list.size() > 0) {

            for (ExecutionSession executionSession : list) {
                i++;
                if (i == 0) {
                    continue;
                }
                deleteDirectory(Path.of(this.pathToSessions, executionSession.getSessionId()).toFile());
            }
        }

        return list;

    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    @Override
    public DataResponse<ExecutionSession> fetchProjectSessions() {
        List<ExecutionSession> localSessions = getLocalSessions();
        return new DataResponse<>(localSessions, localSessions.size(), 1);
    }

    private NameWithBytes createFileOnDiskFromSessionArchiveFile(File sessionFile, String pathName) {
        logger.debug(String.format("get file[%s] from archive[%s]", pathName, sessionFile.getName()));
        ZipInputStream indexArchive = null;
        try {
            indexArchive = new ZipInputStream(new FileInputStream(sessionFile));


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
                    String cacheKey = sessionFile.getName() + entryName;
                    if (entryCache.containsKey(cacheKey)) {
                        return entryCache.get(cacheKey);
                    }
                    NameWithBytes nameWithBytes = new NameWithBytes(entryName, IOUtils.toByteArray(indexArchive));
                    entryCache.put(cacheKey, nameWithBytes);
                    return nameWithBytes;
                }
                String[] nameParts = entryName.split("@");
                if (nameParts.length == 2 && filterValueLong > 0) {
                    int fileThread = Integer.parseInt(nameParts[1].split("\\.")[0].split("-")[2]);
                    long fileTimeStamp = Long.parseLong(nameParts[0]);
                }
            }
        } catch (Exception e) {
            logger.warn("failed to create file [" + pathName + "] on disk from" +
                    " archive[" + sessionFile.getName() + "]", e);
            return null;
        }
        return null;

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

    private List<File> refreshSessionArchivesList(String sessionId) {
        logger.info("refresh session archives list");
        File sessionDirectory = new File(this.pathToSessions + sessionId);
        assert sessionDirectory.exists();
        classWeaveInfo = null;
        List<File> sessionFiles = Arrays.stream(
                        Objects.requireNonNull(sessionDirectory.listFiles()))
                .sorted((a, b) -> -1 * a.getName().compareTo(b.getName()))
                .filter(e -> e.getName().endsWith(".zip")
                        && e.getName().startsWith("index-"))
                .collect(Collectors.toList());
        logger.info("found [" + sessionFiles.size() + "] session archives");
        return sessionFiles;
    }

    @Override
    public void queryTracePointsByValue(
            SearchQuery searchQuery,
            String sessionId,
            ClientCallBack<TracePoint> getProjectSessionErrorsCallback) {
        logger.info("trace by string value: " + searchQuery);
        List<File> archives = refreshSessionArchivesList(sessionId);

        checkProgressIndicator("Searching locally by value [" + searchQuery.getQuery() + "]", null);


        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionArchive : archives) {

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
        tracePointList.forEach(e -> e.setExecutionSession(session));
        getProjectSessionErrorsCallback.success(tracePointList);

    }

    @Override
    public ReplayData fetchObjectHistoryByObjectId(
            FilteredDataEventsRequest filteredDataEventsRequest
    ) {

        List<DataEventWithSessionId> dataEventList = new LinkedList<>();
        Map<String, ClassInfo> classInfoMap = new HashMap<>();
        Map<String, DataInfo> probeInfoMap = new HashMap<>();
        Map<String, StringInfo> stringInfoMap = new HashMap<>();
        Map<String, ObjectInfo> objectInfoMap = new HashMap<>();
        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
        Map<String, MethodInfo> methodInfoMap = new HashMap<>();

        final long objectId = filteredDataEventsRequest.getObjectId();
        Collection<ObjectInfo> sessionObjectInfo = List.of();
        if (objectId != -1 ) {
            sessionObjectInfo = getObjectInfoById(
                    List.of(objectId));
        }


        for (ObjectInfo info : sessionObjectInfo) {
            objectInfoMap.put(String.valueOf(info.getObjectId()), info);
        }

        Collections.sort(this.sessionArchives);

        PageInfo pageInfo = filteredDataEventsRequest.getPageInfo();
        if (pageInfo.isDesc()) {
            Collections.reverse(this.sessionArchives);
        }


        final AtomicInteger skip = new AtomicInteger(pageInfo.getNumber() * pageInfo.getSize());
        Integer remaining = pageInfo.getSize();

        final AtomicLong previousEventAt = new AtomicLong(-1);
        for (File sessionArchive : this.sessionArchives) {

            KaitaiInsidiousClassWeaveParser classWeaveInfoLocal = readClassWeaveInfo(sessionArchive);
            if (classWeaveInfoLocal == null) {
                continue;
            }

            checkProgressIndicator(null, "Loading class mappings");

            classWeaveInfoLocal.classInfo().forEach(e -> {

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


            try {
                List<String> archiveFiles = listArchiveFiles(sessionArchive);
                if (archiveFiles.size() == 0) {
                    continue;
                }


                Collections.sort(archiveFiles);
                if (pageInfo.isDesc()) {
                    Collections.reverse(archiveFiles);
                }

                for (String archiveFile : archiveFiles) {

                    if (remaining == 0) {
                        break;
                    }

                    if (!archiveFile.endsWith(".selog")) {
                        continue;
                    }
                    long startTime = Long.parseLong(archiveFile.split("@")[0]);
                    final int fileThreadId = Integer.parseInt(
                            archiveFile.split("\\.")[0].split("-")[2]
                    );
                    if (filteredDataEventsRequest.getThreadId() != -1
                            && fileThreadId != filteredDataEventsRequest.getThreadId()) {
                        continue;
                    }

                    NameWithBytes bytesWithName = createFileOnDiskFromSessionArchiveFile(
                            sessionArchive, archiveFile);

                    assert bytesWithName != null;


                    KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(
                            new ByteBufferKaitaiStream(bytesWithName.getBytes()));


                    ArrayList<KaitaiInsidiousEventParser.Block> events =
                            eventsContainer.event().entries();

                    List<KaitaiInsidiousEventParser.Block> eventsSublist = events;

                    if (pageInfo.isDesc()) {
                        Collections.reverse(eventsSublist);
                    }




                    List<DataEventWithSessionId> dataEventGroupedList = eventsSublist
                            .stream()
                            .filter(e -> {
                                boolean isDataEvent = e.magic() == 4;
                                if (!isDataEvent) {
                                    return false;
                                }
                                long currentFirstEventAt = previousEventAt.get();

                                KaitaiInsidiousEventParser.DataEventBlock dataEventBlock =
                                        (KaitaiInsidiousEventParser.DataEventBlock) e.block();
                                long currentEventId = dataEventBlock.eventId();

                                if (filteredDataEventsRequest.getNanotime() != -1) {
                                    if (pageInfo.isAsc()) {
                                        if (dataEventBlock.eventId() < filteredDataEventsRequest.getNanotime()) {
                                            return false;
                                        }
                                    } else {
                                        if (dataEventBlock.eventId() > filteredDataEventsRequest.getNanotime()) {
                                            return false;
                                        }

                                    }
                                }

                                if (currentFirstEventAt != -1 &&
                                        Math.abs(currentFirstEventAt - currentEventId)
                                                < pageInfo.getBufferSize()) {
                                    return true;
                                }

                                boolean isRequestedObject =
                                        dataEventBlock.valueId() == objectId
                                                || objectId == -1;

                                if (isRequestedObject) {
                                    previousEventAt.set(dataEventBlock.eventId());
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
                            .map(e -> (KaitaiInsidiousEventParser.DataEventBlock) e.block())
                            .map(e -> {
                                DataEventWithSessionId d = new DataEventWithSessionId();
                                d.setDataId((int) e.probeId());
                                d.setNanoTime(e.eventId());
                                d.setRecordedAt(new Date(e.timestamp()));
                                d.setThreadId(fileThreadId);
                                d.setValue(e.valueId());
                                return d;
                            }).collect(Collectors.toList());

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


            checkProgressIndicator(null, "Loading strings from " + sessionArchive.getName());
            NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(
                    sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            assert stringsIndexBytes != null;


            checkProgressIndicator(null, "Loading strings from " + sessionArchive.getName());
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

            checkProgressIndicator(null, "Loading obbjects from " + sessionArchive.getName());
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
            objectInfoMap.putAll(sessionObjectsInfo);


            Set<Integer> typeIds = objectInfoMap.values()
                    .stream().map(ObjectInfo::getTypeId)
                    .map(Long::intValue).collect(Collectors.toSet());


            checkProgressIndicator(null, "Loading types from " + sessionArchive.getName());
            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            assert typeIndexBytes != null;
            ArchiveIndex typesIndex = null;
            try {
                typesIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            } catch (IOException e) {
                logger.error("failed to read type index from session archive", e);
                continue;
            }
            Map<String, TypeInfo> sessionTypeInfo = typesIndex.getTypesById(typeIds);
            typeInfoMap.putAll(sessionTypeInfo);


            checkProgressIndicator(null, "Completed loading");

        }

        return new ReplayData(
                this, filteredDataEventsRequest, dataEventList, classInfoMap, probeInfoMap,
                stringInfoMap, objectInfoMap, typeInfoMap, methodInfoMap);

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

    @Override
    public void queryTracePointsByTypes(SearchQuery searchQuery,
                                        String sessionId, int historyDepth,
                                        ClientCallBack<TracePoint> clientCallBack) {
        logger.info("get trace by object type: " + searchQuery.getQuery());
        List<File> archives = refreshSessionArchivesList(sessionId);


        int totalCount = sessionArchives.size();
        int currentCount = 0;
        int totalMatched = 0;

        for (File sessionArchive : archives) {
            currentCount++;


            checkProgressIndicator(null, "Loading type names: " + sessionArchive.getName());


            if (historyDepth != -1) {
                if (historyDepth < 1) {
                    break;
                }
            }

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
                tracePointsByValueIds.forEach(e -> e.setExecutionSession(session));
                clientCallBack.success(tracePointsByValueIds);
                totalMatched += tracePointsByValueIds.size();

                checkProgressIndicator("Matched " + totalMatched + " events", null);
            }
            if (historyDepth != -1) {
                historyDepth--;
            }

        }
        clientCallBack.completed();
    }

    private Set<ObjectInfoDocument> queryObjectsByTypeFromSessionArchive(SearchQuery searchQuery,
                                                                         File sessionArchive) {
        NameWithBytes fileBytes = null;
        fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
        if (fileBytes == null) {
            return Set.of();
        }

        ArchiveIndex typesIndex;
        try {
            typesIndex = readArchiveIndex(fileBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            logger.info("loaded [" + typesIndex.Types().size() +
                    "] typeInfo from index in [" + sessionArchive.getAbsolutePath() + "]");
        } catch (Exception e) {
            logger.error("failed to read type index file  [" + sessionArchive.getName() + "]", e);
            return Set.of();
        }

        checkProgressIndicator(null, "Querying type names from: " + sessionArchive.getName());


        Query<TypeInfoDocument> typeQuery = in(TypeInfoDocument.TYPE_NAME
                , List.of(((String) searchQuery.getQuery()).split(",")));
        ResultSet<TypeInfoDocument> searchResult = typesIndex.Types().retrieve(typeQuery);
        Set<Integer> typeIds = searchResult.stream().map(TypeInfoDocument::getTypeId).collect(Collectors.toSet());
        searchResult.close();
        logger.info("type query matches [" + typeIds.size() + "] items");


        checkProgressIndicator(null, "Index: " + sessionArchive.getName() +
                " matched " + typeIds.size() + " of the total " + typesIndex.Types().size());


        if (typeIds.size() == 0) {
            return Set.of();
        }


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

        checkProgressIndicator(null, "Index: " + sessionArchive.getName() +
                " matched " + objects.size() + " objects of total " + objectIndex.Objects().size());


        logger.info("matched [" + objects.size() + "] objects by of the total " + objectIndex.Objects().size());
        return objects;
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

            NameWithBytes typesInfoBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            assert typesInfoBytes != null;
            ArchiveIndex typeIndex = readArchiveIndex(typesInfoBytes.getBytes(), INDEX_TYPE_DAT_FILE);
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
                        tracePoint.setExecutionSession(session);
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
            }

        }

        return tracePointList;
    }


    private List<TracePoint> getTracePointsByProbeIds(File sessionArchive,
                                                      Set<Integer> probeIds) {
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
                return List.of();
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

                        tracePoint.setExecutionSession(session);
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
                logger.error("exception while creating trace points in file[" + matchedFile.path + "]",
                        ex);
            }

        }

        return tracePointList;
    }


    private KaitaiInsidiousClassWeaveParser.ClassInfo getClassInfo(int classId) throws ClassInfoNotFoundException {

        for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeaveInfo.classInfo()) {
            if (classInfo.classId() == classId) {
                return classInfo;
            }
        }
        throw new ClassInfoNotFoundException(classId);
    }

    private TypeInfo getTypeInfo(Integer typeId) {
        List<File> archives = this.sessionArchives;

        for (File sessionArchive : archives) {
            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            if (typeIndexBytes == null) {
                continue;
            }

            ArchiveIndex typeIndex = null;
            try {
                typeIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            } catch (IOException e) {
                logger.warn("failed to read archive for types index: " + e.getMessage());
                continue;
            }

            Map<String, TypeInfo> result = typeIndex.getTypesById(Set.of(typeId));
            if (result.size() > 0) {
                return result.get(String.valueOf(typeId));
            }

        }

        return new TypeInfo("local", typeId, "unidentified type", "", "", "", "");
    }


    private List<DataInfo> getProbeInfo(File sessionFile, Set<Integer> dataId) throws IOException {


        readClassWeaveInfo(sessionFile);
        if (classWeaveInfo == null) {
            return List.of();
        }

        return classWeaveInfo.classInfo().stream()
                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
                .flatMap(Collection::stream)
                .filter(e -> dataId.contains(Math.toIntExact(e.dataId())))
                .map(KaitaiUtils::toDataInfo).collect(Collectors.toList());

    }

    private List<DataEventWithSessionId> getDataEventsFromPathByValueIds(byte[] bytes, Long[] valueIds) throws IOException {

        Set<Long> ids = Set.of(valueIds);
        KaitaiInsidiousEventParser dataEvents = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(bytes));

        return dataEvents.event().entries().stream().filter(e -> e.magic() == 4
                        && ids.contains(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).valueId()))
                .map(e -> {
                    long valueId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).valueId();

                    int probeId = Math.toIntExact(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).probeId());

                    long eventId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).eventId();
                    long timestamp = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).timestamp();

                    DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                    dataEvent.setDataId(probeId);
                    dataEvent.setValue(valueId);
                    dataEvent.setNanoTime(eventId);
                    dataEvent.setRecordedAt(new Date(timestamp));
                    return dataEvent;
                }).collect(Collectors.toList());
    }

    private List<DataEventWithSessionId> getDataEventsFromPathByProbeIds(byte[] bytes, Integer[] probeIds) {

        Set<Integer> ids = Set.of(probeIds);
        KaitaiInsidiousEventParser dataEvents = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(bytes));

        return dataEvents.event().entries().stream().filter(e -> e.magic() == 4
                        && ids.contains((int) ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).probeId()))
                .map(e -> {
                    long valueId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).valueId();

                    int probeId = Math.toIntExact(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).probeId());

                    long eventId = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).eventId();
                    long timestamp = ((KaitaiInsidiousEventParser.DataEventBlock) e.block()).timestamp();

                    DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                    dataEvent.setDataId(probeId);
                    dataEvent.setValue(valueId);
                    dataEvent.setNanoTime(eventId);
                    dataEvent.setRecordedAt(new Date(timestamp));
                    return dataEvent;
                }).collect(Collectors.toList());
    }

    private ArchiveFilesIndex readEventIndex(byte[] bytes) throws IOException {
        KaitaiInsidiousIndexParser archiveIndex = new KaitaiInsidiousIndexParser(new ByteBufferKaitaiStream(bytes));

        return new ArchiveFilesIndex(archiveIndex);
    }

    private ArchiveIndex readArchiveIndex(byte[] bytes, DatFileType indexFilterType) throws IOException {

        ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex = null;
        String cacheKey = bytesHex(bytes, indexFilterType.getFileName());


        Path path = Path.of(this.pathToSessions, session.getSessionId(), cacheKey,
                indexFilterType.getFileName());
        Path parentPath = path.getParent();
        parentPath.toFile().mkdirs();

        Files.write(path, bytes);

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

        ArchiveIndex archiveIndex = new ArchiveIndex(typeInfoIndex, stringInfoIndex, objectInfoIndex, classInfoMap);
        indexCache.put(cacheKey, archiveIndex);
        return archiveIndex;
    }


    @NotNull
    private String bytesHex(byte[] bytes, String indexFilterType) {
        String md5Hex = DigestUtils.md5Hex(bytes);
        String cacheKey = md5Hex + "-" + indexFilterType;
        return cacheKey;
    }

    @Override
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
                .entries().stream().filter(e -> e.magic() == 4)
                .map(e -> (KaitaiInsidiousEventParser.DataEventBlock) e.block())
                .map(e -> {
                    DataEventWithSessionId d = new DataEventWithSessionId();

                    d.setDataId((int) e.probeId());
                    d.setNanoTime(e.eventId());
                    d.setRecordedAt(new Date(e.timestamp()));
                    d.setThreadId(filteredDataEventsRequest.getThreadId());
                    d.setValue(e.valueId());
                    return d;
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
            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            assert typeIndexBytes != null;
            ArchiveIndex typesIndex = null;
            try {
                typesIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            } catch (IOException e) {
                logger.error("failed to read type index from session archive", e);
                continue;
            }
            Map<String, TypeInfo> sessionTypeInfo = typesIndex.getTypesById(typeIds);
            typeInfo.putAll(sessionTypeInfo);

        }


        checkProgressIndicator(null, "Completed loading");
        return new ReplayData(this, filteredDataEventsRequest, dataEventList, classInfo,
                dataInfo, stringInfo, objectInfo, typeInfo, methodInfoMap);
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

    @Override
    public String getToken() {
        return "localhost-token";
    }

    @Override
    public ProjectItem getProject() {
        return this.currentProject;
    }

    @Override
    public void setProject(String projectName) {
        ProjectItem currentProject = new ProjectItem();
        currentProject.setName(projectName);
        currentProject.setId("1");
        currentProject.setCreatedAt(new Date().toString());
        this.currentProject = currentProject;
    }

    @Override
    public String getEndpoint() {
        return pathToSessions;
    }

    @Override
    public void getAgentDownloadUrl(AgentDownloadUrlCallback agentDownloadUrlCallback) {
        networkClient.getAgentDownloadUrl(agentDownloadUrlCallback);
    }

    @Override
    public void downloadAgentFromUrl(String url, String insidiousLocalPath, boolean overwrite) {
        networkClient.downloadAgentFromUrl(url, insidiousLocalPath, overwrite);
    }

    @Override
    public void close() {
        threadPoolExecutor5Seconds.shutdown();
    }

    @Override
    public void onNewException(Collection<String> typeNameList, VideobugExceptionCallback videobugExceptionCallback) {


        threadPoolExecutor5Seconds.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (1 < 2) {
                    return;
                }

                List<ExecutionSession> sessions = getLocalSessions();
                ExecutionSession executionSession = sessions.get(0);
                setSession(executionSession);

                queryTracePointsByTypes(SearchQuery.ByType(typeNameList), session.getSessionId(), 2,
                        new ClientCallBack<TracePoint>() {
                            @Override
                            public void error(ExceptionResponse errorResponse) {
                                logger.info("failed to query traces by type in scheduler: " + errorResponse.getMessage());
                            }

                            @Override
                            public void success(Collection<TracePoint> tracePoints) {
                                if (tracePoints.size() > 0) {
                                    videobugExceptionCallback.onNewTracePoints(tracePoints);
                                }
                            }

                            @Override
                            public void completed() {

                            }
                        });
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void getMethods(String sessionId,
                           Integer typeId, ClientCallBack<TestCandidate> tracePointsCallback) {

        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setSessionId(sessionId);
        List<File> archives = refreshSessionArchivesList(sessionId);
        KaitaiInsidiousClassWeaveParser classWeaveInfo1 = readClassWeaveInfo(archives.get(0));

        if (classWeaveInfo1 == null) {
            ExceptionResponse errorResponse = new ExceptionResponse();
            errorResponse.setMessage("session not found [" + sessionId + "]");
            tracePointsCallback.error(errorResponse);
            return;
        }
        classWeaveInfo1
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

    /**
     * we need to find all unique objects of these given class types
     *
     * @param searchQuery    class type query
     * @param clientCallBack results go here
     */
    @Override
    public void getObjectsByType(
            SearchQuery searchQuery,
            String sessionId,
            ClientCallBack<ObjectsWithTypeInfo> clientCallBack
    ) {

        ExecutionSession session = new ExecutionSession();
        session.setSessionId(sessionId);
        setSession(session);
        List<File> archives = refreshSessionArchivesList(sessionId);

        for (File sessionArchive : archives) {

            Set<ObjectInfoDocument> objects = queryObjectsByTypeFromSessionArchive(searchQuery,
                    sessionArchive);

            Map<Long, TypeInfo> typesMap = objects.stream()
                    .map(ObjectInfoDocument::getTypeId)
                    .collect(Collectors.toSet())
                    .stream()
                    .map(this::getTypeInfo)
                    .collect(Collectors.toMap(TypeInfo::getTypeId, typeInfo -> typeInfo));

            Set<ObjectsWithTypeInfo> collect = objects.stream()
                    .map(e ->
                            new ObjectsWithTypeInfo(
                                    new ObjectInfo(e.getObjectId(), e.getTypeId(), 0),
                                    typesMap.get((long) e.getTypeId())))
                    .collect(Collectors.toSet());
            if (collect.size() > 0) {
                clientCallBack.success(collect);
            }


        }

        clientCallBack.completed();


    }

    @Override
    public ClassWeaveInfo getSessionClassWeave(String sessionId) {

//        ExecutionSession executionSession = new ExecutionSession();
//        executionSession.setSessionId(sessionId);
        List<File> archives = refreshSessionArchivesList(sessionId);
        KaitaiInsidiousClassWeaveParser classWeaveInfo1 = null;

        int i = 0;
        while (classWeaveInfo1 == null) {
            classWeaveInfo1 = readClassWeaveInfo(archives.get(i));
            i++;
        }

        List<ClassInfo> classInfoList = new LinkedList<>();
        List<MethodInfo> methodInfoList = new LinkedList<>();
        List<DataInfo> dataInfoList = new LinkedList<>();


        classWeaveInfo1
                .classInfo()
                .forEach(classInfo -> {

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

                });

        ClassWeaveInfo classWeave = new ClassWeaveInfo(classInfoList,
                methodInfoList, dataInfoList);
        return classWeave;
    }


    private List<DataInfo> queryProbeFromFileByEventType(File sessionFile,
                                                         Collection<EventType> eventTypes) {

        readClassWeaveInfo(sessionFile);

        if (classWeaveInfo == null) {
            return List.of();
        }

        return classWeaveInfo.classInfo().stream()
                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
                .flatMap(Collection::stream)
                .filter(e -> eventTypes.size() == 0 ||
                        // dont check contains if the list is empty
                        eventTypes.contains(EventType.valueOf(e.eventType().value())))
                .map(KaitaiUtils::toDataInfo).collect(Collectors.toList());


    }

    private KaitaiInsidiousClassWeaveParser readClassWeaveInfo(@NotNull File sessionFile) {

        if (classWeaveInfo == null || true) {
            NameWithBytes fileBytes =
                    createFileOnDiskFromSessionArchiveFile(sessionFile, WEAVE_DAT_FILE.getFileName());
            if (fileBytes == null) {
                logger.debug("failed to read class weave info from " +
                        "sessionFile [" + sessionFile.getName() + "]");
                return null;
            }
            classWeaveInfo = new KaitaiInsidiousClassWeaveParser(
                    new ByteBufferKaitaiStream(fileBytes.getBytes()));
        }
        return classWeaveInfo;
    }

    @Override
    public void queryTracePointsByProbe(
            SearchQuery searchQuery,
            String sessionId,
            ClientCallBack<TracePoint> tracePointsCallback) {
        logger.info("trace by probe ids: " + searchQuery);
        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setSessionId(sessionId);
        setSession(executionSession);

        checkProgressIndicator("Searching locally by value [" + searchQuery.getQuery() + "]", null);


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
                List<TracePoint> tracePointsByProbeIds = getTracePointsByProbeIds(sessionArchive,
                        probeIds.stream().map(DataInfo::getDataId)
                                .collect(Collectors.toSet()));
                if (tracePointsByProbeIds.size() != 0) {
                    tracePointsByProbeIds.forEach(e -> e.setExecutionSession(session));
                    tracePointsCallback.success(tracePointsByProbeIds);
                }
            }
        }
        tracePointsCallback.completed();
    }

}
