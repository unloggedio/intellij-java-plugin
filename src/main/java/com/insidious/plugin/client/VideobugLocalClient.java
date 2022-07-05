package com.insidious.plugin.client;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.radixinverted.InvertedRadixTreeIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import com.insidious.common.FilteredDataEventsRequest;
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
import com.insidious.plugin.pojo.SearchQuery;
import com.insidious.plugin.pojo.TestCandidate;
import com.insidious.plugin.pojo.TracePoint;
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
                executionSession.setName(file.getName());
                list.add(executionSession);
            }
        }

        list.sort(Comparator.comparing(ExecutionSession::getName));
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
                    return new NameWithBytes(entryName, IOUtils.toByteArray(indexArchive));
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

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText(
                    "Searching locally by value [" + searchQuery.getQuery() + "]");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                getProjectSessionErrorsCallback.completed();
                return;
            }
        }


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

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loaded " + valueIds.size() + " strings from archive " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    getProjectSessionErrorsCallback.success(tracePointList);
                    return;
                }
            }


            if (valueIds.size() > 0) {
                tracePointList.addAll(getTracePointsByValueIds(sessionArchive, valueIds));
            }
        }
        tracePointList.forEach(e -> e.setExecutionSession(session));
        getProjectSessionErrorsCallback.success(tracePointList);

    }


    @Override
    public void queryTracePointsByType(SearchQuery searchQuery,
                                       String sessionId, int historyDepth,
                                       ClientCallBack<TracePoint> clientCallBack) {
        logger.info("get trace by object type: " + searchQuery.getQuery());
        List<File> archives = refreshSessionArchivesList(sessionId);


        int totalCount = sessionArchives.size();
        int currentCount = 0;
        int totalMatched = 0;

        for (File sessionArchive : archives) {
            currentCount++;


            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading type names: " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }


            if (historyDepth != -1) {
                if (historyDepth < 1) {
                    break;
                }
            }

            NameWithBytes fileBytes = null;
            fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            if (fileBytes == null) {
                continue;
            }

            ArchiveIndex typesIndex;
            try {
                typesIndex = readArchiveIndex(fileBytes.getBytes(), INDEX_TYPE_DAT_FILE);
                logger.info("loaded [" + typesIndex.Types().size() + "] typeInfo from index in [" + sessionArchive.getAbsolutePath() + "]");
            } catch (Exception e) {
                logger.error("failed to read type index file  [" + sessionArchive.getName() + "]", e);
                continue;
            }

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Querying type names from: " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }


            Query<TypeInfoDocument> typeQuery = in(TypeInfoDocument.TYPE_NAME
                    , List.of(((String) searchQuery.getQuery()).split(",")));
            ResultSet<TypeInfoDocument> searchResult = typesIndex.Types().retrieve(typeQuery);
            Set<Integer> typeIds = searchResult.stream().map(TypeInfoDocument::getTypeId).collect(Collectors.toSet());
            searchResult.close();
            logger.info("type query matches [" + typeIds.size() + "] items");


            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Index: " + sessionArchive.getName() + " matched " + typeIds.size() + " of the total " + typesIndex.Types().size());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }


            if (typeIds.size() == 0) {
                continue;
            }


            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading matched objects");
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }


            NameWithBytes objectIndexFileBytes = null;
            objectIndexFileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            if (objectIndexFileBytes == null) {
                logger.warn("object index file bytes are empty, skipping");
                continue;
            }


            ArchiveIndex objectIndex = null;
            try {
                objectIndex = readArchiveIndex(objectIndexFileBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            } catch (IOException e) {
                logger.warn("failed to read object index file: " + e.getMessage());
                continue;

            }
            Query<ObjectInfoDocument> query = in(ObjectInfoDocument.OBJECT_TYPE_ID, typeIds);
            ResultSet<ObjectInfoDocument> typeInfoSearchResult = objectIndex.Objects().retrieve(query);
            Set<Long> valueIds = typeInfoSearchResult.stream().map(ObjectInfoDocument::getObjectId).collect(Collectors.toSet());
            typeInfoSearchResult.close();

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Index: " + sessionArchive.getName() + " matched " + valueIds.size() + " objects of total " + objectIndex.Objects().size());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }


            logger.info("matched [" + valueIds.size() + "] objects by of the total " + objectIndex.Objects().size());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }

            if (valueIds.size() > 0) {
                List<TracePoint> tracePointsByValueIds = getTracePointsByValueIds(sessionArchive, valueIds);
                tracePointsByValueIds.forEach(e -> e.setExecutionSession(session));
                clientCallBack.success(tracePointsByValueIds);
                totalMatched += tracePointsByValueIds.size();
                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                    ProgressIndicatorProvider.getGlobalProgressIndicator().setText("Matched " + totalMatched + " events");
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        throw new ProcessCanceledException();
                    }
                }

            }
            if (historyDepth != -1) {
                historyDepth--;
            }

        }
        clientCallBack.completed();
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
            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading events index " + sessionArchive.getName() + " to match against " + valueIds.size() + " values");
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }


            bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
            assert bytes != null;
            eventsIndex = readEventIndex(bytes.getBytes());


            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading objects from " + sessionArchive.getName() + " to match against " + valueIds.size() + " values");
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }
            NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            assert objectIndexBytes != null;
            objectIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            logger.info("object index has [" + objectIndex.Objects().size() + "] objects");
            objectInfoMap = objectIndex.getObjectsByObjectId(valueIds);

            Set<Integer> types = objectInfoMap.values().stream().map(ObjectInfo::getTypeId).map(Long::intValue).collect(Collectors.toSet());

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading types from " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }

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

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Matching events for item " + currentIndex + " of " + valueIds.size());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }

            boolean archiveHasSeenValue = finalEventsIndex.hasValueId(valueId);
            List<UploadFile> matchedFilesForString = new LinkedList<>();
            logger.info("value [" + valueId + "] found in archive: [" + archiveHasSeenValue + "]");

            if (archiveHasSeenValue) {
                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                    ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Events matched in " + sessionArchive.getName());
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        throw new ProcessCanceledException();
                    }
                }
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

                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                    ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading events data from " + matchedFile.getPath());
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        throw new ProcessCanceledException();
                    }
                }
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
                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                    ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        throw new ProcessCanceledException();
                    }
                }
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

                        TracePoint tracePoint = new TracePoint(classId, dataInfo.getLine(), dataInfo.getDataId(), threadId, e1.getValue(), classInfo.fileName().value(), classInfo.className().value(), typeInfo.getTypeNameFromClass(), timestamp, e1.getNanoTime());
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
            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(
                        "Loading events index " + sessionArchive.getName() + " to match against " + probeIds.size() + " values");
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }


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

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(
                        "Matching events for probe " + currentIndex + " of " + probeIds.size());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }

            boolean archiveHasSeenValue = finalEventsIndex.hasProbeId(probeId);
            List<UploadFile> matchedFilesForString = new LinkedList<>();
            logger.info("probeId [" + probeId + "] found in archive: [" + archiveHasSeenValue + "]");

            if (archiveHasSeenValue) {
                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                    ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Events matched in " + sessionArchive.getName());
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        throw new ProcessCanceledException();
                    }
                }
                matchedFilesForString =
                        finalEventsIndex.querySessionFilesByProbeId(probeId);
                for (UploadFile uploadFile : matchedFilesForString) {
                    String filePath = uploadFile.getPath();
                    int threadId = Integer.parseInt(Path.of(filePath).getFileName().toString().split("\\.")[0].split("-")[2]);
                    UploadFile uploadFileToAdd = new UploadFile(filePath, threadId, null, null);
                    uploadFileToAdd.setProbeIds(new Integer[]{probeId});
                    matchedFiles.put(filePath, uploadFile);
                }
            }
        });
        logger.info("matched [" + matchedFiles.size() + "] files");

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText("Found " + matchedFiles.size() + " archives with matching values");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
        }


        for (UploadFile matchedFile : matchedFiles.values()) {
            try {

                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                    ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading events data from " + matchedFile.getPath());
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        throw new ProcessCanceledException();
                    }
                }
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

                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                    ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
                    if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                        throw new ProcessCanceledException();
                    }
                }
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
                        TypeInfo typeInfo = getTypeInfo((int) objectInfo.getTypeId());

                        TracePoint tracePoint = new TracePoint(classId, dataInfo.getLine(), dataInfo.getDataId(), threadId, e1.getValue(), classInfo.fileName().value(), classInfo.className().value(), typeInfo.getTypeNameFromClass(), timestamp, e1.getNanoTime());
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

    private TypeInfo getTypeInfo(Integer typeId) throws ClassInfoNotFoundException, IOException {
        List<File> archives = this.sessionArchives;

        for (File sessionArchive : archives) {
            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            if (typeIndexBytes == null) {
                continue;
            }
            ArchiveIndex typeIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);

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
                .map(this::toDataInfo).collect(Collectors.toList());

    }


    @NotNull
    private DataInfo toDataInfo(KaitaiInsidiousClassWeaveParser.ProbeInfo e) {
        String descriptorValue = e.valueDescriptor().value();

        Descriptor valueDesc = Descriptor.Object;
        if (!descriptorValue.startsWith("L")) {
            valueDesc = Descriptor.get(descriptorValue);
        }
        return new DataInfo(Math.toIntExact(e.classId()), Math.toIntExact(e.methodId()),
                Math.toIntExact(e.dataId()),
                Math.toIntExact(e.lineNumber()),
                Math.toIntExact(e.instructionIndex()),
                EventType.valueOf(e.eventType().value()),
                valueDesc,
                e.attributes().value());
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
                        && ids.contains(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).probeId()))
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


        Path path = Path.of(this.pathToSessions, session.getName(), cacheKey, indexFilterType.getFileName());
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
                        new ClassInfo((int) classInfo.classId(),
                                classInfo.container().value(),
                                classInfo.fileName().value(),
                                classInfo.className().value(),
                                LogLevel.valueOf(classInfo.logLevel().value()),
                                classInfo.hash().value(),
                                classInfo.classLoaderIdentifier().value())
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
    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest)
            throws IOException {

        File archiveToServe = null;
        for (File sessionArchive : this.sessionArchives) {
            long timestamp = Long.parseLong(sessionArchive.getName().split("-")[2].split("\\.")[0]);
            if (timestamp < filteredDataEventsRequest.getNanotime()) {
                archiveToServe = sessionArchive;
                break;
            }
        }

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading archive: " + archiveToServe.getName());
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                return null;
            }
        }


        NameWithBytes bytesWithName = createFileOnDiskFromSessionArchiveFile(
                archiveToServe, String.valueOf(filteredDataEventsRequest.getNanotime()));


        assert bytesWithName != null;


        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Parsing events: " + bytesWithName.getName());
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                return null;
            }
        }

        KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(
                new ByteBufferKaitaiStream(bytesWithName.getBytes()));


        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(
                    "Mapping " + eventsContainer.event().entries().size() + " events ");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                return null;
            }
        }

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

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading class mappings");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                return null;
            }
        }

        classWeaveInfo.classInfo().forEach(e -> {

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading class: " + e.className());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }

            classInfo.put(String.valueOf(e.classId()), new ClassInfo((int) e.classId(),
                    e.container().value(),
                    e.fileName().value(),
                    e.className().value(),
                    LogLevel.valueOf(e.logLevel().value()),
                    e.hash().value(),
                    e.classLoaderIdentifier().value()));

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(
                        "Loading " + e.probeCount() + " probes in class: " + e.className());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }

            e.probeList().forEach(r -> {
                dataInfo.put(String.valueOf(r.dataId()),
                        new DataInfo((int) r.classId(),
                                (int) r.methodId(),
                                (int) r.dataId(),
                                (int) r.lineNumber(),
                                (int) r.instructionIndex(),
                                EventType.valueOf(r.eventType().value()),
                                Descriptor.get(r.valueDescriptor().value()),
                                r.attributes().value())
                );
            });
        });

        Set<Integer> probeIds = dataEventList.stream().map(DataEventWithSessionId::getDataId).collect(Collectors.toSet());
        Set<Long> valueIds = dataEventList.stream().map(DataEventWithSessionId::getValue).collect(Collectors.toSet());


        Map<String, StringInfo> stringInfo = new HashMap<>();
        Map<String, ObjectInfo> objectInfo = new HashMap<>();
        Map<String, TypeInfo> typeInfo = new HashMap<>();

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading types");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
        }
        for (File sessionArchive : this.sessionArchives) {

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading objects from " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }

            NameWithBytes objectsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            if (objectsIndexBytes == null) {
                continue;
            }

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading objects from " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }
            ArchiveIndex objectIndex = readArchiveIndex(objectsIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            Map<String, ObjectInfo> sessionObjectInfo = objectIndex.getObjectsByObjectId(valueIds);
            objectInfo.putAll(sessionObjectInfo);

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading strings from " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }
            NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            assert stringsIndexBytes != null;


            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading strings from " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }
            ArchiveIndex stringIndex = readArchiveIndex(stringsIndexBytes.getBytes(), INDEX_STRING_DAT_FILE);
            Map<String, StringInfo> sessionStringInfo = stringIndex.getStringsById(valueIds.stream().filter(e -> e > 10).collect(Collectors.toSet()));
            stringInfo.putAll(sessionStringInfo);


            Set<Integer> typeIds = objectInfo.values().stream().map(ObjectInfo::getTypeId).map(Long::intValue).collect(Collectors.toSet());


            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loading types from " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    throw new ProcessCanceledException();
                }
            }
            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            assert typeIndexBytes != null;
            ArchiveIndex typesIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            Map<String, TypeInfo> sessionTypeInfo = typesIndex.getTypesById(typeIds);
            typeInfo.putAll(sessionTypeInfo);

        }


        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Completed loading");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
        }
        return new ReplayData(dataEventList, classInfo, dataInfo, stringInfo, objectInfo, typeInfo, "DESC");
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

                queryTracePointsByType(SearchQuery.ByType(typeNameList), session.getSessionId(), 2,
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
                           ClientCallBack<TestCandidate> tracePointsCallback) {

        classWeaveInfo
                .classInfo()
                .forEach(classInfo -> tracePointsCallback.success(
                        classInfo.methodList()
                                .stream().map(e -> new MethodInfo(
                                        (int) e.classId(),
                                        (int) e.methodId(),
                                        classInfo.className().value(),
                                        e.methodName().value(),
                                        e.methodDescriptor().value(),
                                        (int) e.access(),
                                        e.sourceFileName().value(),
                                        e.methodHash().value()))
                                .map(TestCandidate::new)
                                .collect(Collectors.toSet()))
                );
        tracePointsCallback.completed();
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
                .map(this::toDataInfo).collect(Collectors.toList());


    }

    private void readClassWeaveInfo(@NotNull File sessionFile) {

        if (classWeaveInfo == null) {
            NameWithBytes fileBytes =
                    createFileOnDiskFromSessionArchiveFile(sessionFile, WEAVE_DAT_FILE.getFileName());
            if (fileBytes == null) {
                logger.error("failed to read class weave info from " +
                        "sessionFile [" + sessionFile.getName() + "]");
                return;
            }
            classWeaveInfo = new KaitaiInsidiousClassWeaveParser(
                    new ByteBufferKaitaiStream(fileBytes.getBytes()));
        }
    }

    @Override
    public void queryTracePointsByDataIds(
            SearchQuery searchQuery,
            String sessionId,
            ClientCallBack<TracePoint> tracePointsCallback) {
        logger.info("trace by probe ids: " + searchQuery);
        ExecutionSession executionSession = new ExecutionSession();
        executionSession.setSessionId(sessionId);
        setSession(executionSession);

        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            ProgressIndicatorProvider.getGlobalProgressIndicator().setText(
                    "Searching locally by value [" + searchQuery.getQuery() + "]");
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                tracePointsCallback.completed();
                return;
            }
        }


        List<DataInfo> probeIds = null;
        for (File sessionArchive : sessionArchives) {
            logger.info("check archive [" + sessionArchive.getName() + "] for " +
                    "probes");
            if (probeIds == null) {
                Collection<EventType> eventTypes =
                        (Collection<EventType>) searchQuery.getQuery();
                probeIds = queryProbeFromFileByEventType(sessionArchive,
                        eventTypes);
            }

            if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2("Loaded " + probeIds.size() + " objects from archive " + sessionArchive.getName());
                if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                    tracePointsCallback.success(List.of());
                    return;
                }
            }


            if (probeIds.size() > 0) {
                List<TracePoint> tracePointsByProbeIds = getTracePointsByProbeIds(sessionArchive,
                        probeIds.stream().map(DataInfo::getDataId)
                                .collect(Collectors.toSet()));
                tracePointsByProbeIds.forEach(e -> e.setExecutionSession(session));
                tracePointsCallback.success(tracePointsByProbeIds);
            }
        }
        tracePointsCallback.completed();
    }

}
