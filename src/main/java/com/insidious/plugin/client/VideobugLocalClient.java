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
import com.insidious.plugin.pojo.TracePoint;
import io.kaitai.struct.ByteBufferKaitaiStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.googlecode.cqengine.query.QueryFactory.in;
import static com.insidious.plugin.client.DatFileType.*;

public class VideobugLocalClient implements VideobugClientInterface {

    private static final Logger logger = java.util.logging.Logger.getLogger(VideobugLocalClient.class.getName());
    private final String pathToSessions;
    private final Map<String, KaitaiInsidiousClassWeaveParser.ClassInfo> classInfoMap = new HashMap<>();
    private ExecutionSession session;
    private ProjectItem currentProject;
    private KaitaiInsidiousClassWeaveParser classWeaveInfo;
    private List<File> sessionArchives;
    private File sessionDirectory;
    private Map<String, ArchiveIndex> indexCache = new HashMap<>();

    public VideobugLocalClient(String pathToSessions) {
        if (!pathToSessions.endsWith("/")) {
            pathToSessions = pathToSessions + "/";
        }
        this.pathToSessions = pathToSessions;
    }

    private static byte[] extractFile(ZipInputStream is)
            throws IOException {
        ByteArrayOutputStream fos = null;
        try {
            fos = new ByteArrayOutputStream();
            final byte[] buf = new byte[1024];
            int read = 0;
            int length;
            while ((length = is.read(buf, 0, buf.length)) >= 0) {
                fos.write(buf, 0, length);
            }
        } catch (IOException ioex) {
            fos.close();
        }
        return fos.toByteArray();
    }

    @Override
    public ExecutionSession getCurrentSession() {
        return this.session;
    }

    @Override
    public void setSession(ExecutionSession session) {
        this.session = session;
        refreshSessionArchivesList();
    }

    @Override
    public void signup(String serverUrl, String username, String password, SignUpCallback callback) {
        callback.success();
    }

    @Override
    public void signin(SigninRequest signinRequest, SignInCallback signInCallback) {
        signInCallback.success("localhost");
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
        File currentDir = new File(pathToSessions);
        logger.info(String.format("looking for sessions for project in [%s]", currentDir.getAbsolutePath()));
        for (File file : Objects.requireNonNull(currentDir.listFiles())) {
            if (file.isDirectory() && file.getName().contains("selogger")) {
                ExecutionSession executionSession = new ExecutionSession();
                executionSession.setId(file.getName());
                executionSession.setCreatedAt(new Date(file.lastModified()));
                executionSession.setHostname("localhost");
                executionSession.setLastUpdateAt(file.lastModified());
                executionSession.setName(file.getName());
                list.add(executionSession);
            }
        }
        Collections.reverse(list);
        return list;

    }

    @Override
    public DataResponse<ExecutionSession> fetchProjectSessions() {
        List<ExecutionSession> localSessions = getLocalSessions();
        return new DataResponse<>(localSessions, localSessions.size(), 1);
    }

    @Override
    public void getTracesByObjectType(
            List<String> classList,
            GetProjectSessionErrorsCallback getProjectSessionErrorsCallback) throws IOException {
        logger.info("trace by class: " + classList);
        refreshSessionArchivesList();


        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionArchive : sessionArchives) {

            logger.info("initialize index for archive: " + sessionArchive.getAbsolutePath());
            NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            if (fileBytes == null) {
                continue;
            }

            ArchiveIndex typesIndex = readArchiveIndex(fileBytes.getBytes(), INDEX_TYPE_DAT_FILE);

            Query<TypeInfoDocument> typeQuery = in(TypeInfoDocument.TYPE_NAME, classList);
            ResultSet<TypeInfoDocument> searchResult = typesIndex.Types().retrieve(typeQuery);
            Set<Integer> typeIds = searchResult.stream()
                    .map(TypeInfoDocument::getTypeId)
                    .collect(Collectors.toSet());

            NameWithBytes objectIndexFileBytes = createFileOnDiskFromSessionArchiveFile(
                    sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            if (objectIndexFileBytes == null) {
                continue;
            }


            ArchiveIndex objectIndex = readArchiveIndex(objectIndexFileBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            Query<ObjectInfoDocument> query = in(ObjectInfoDocument.OBJECT_TYPE_ID, typeIds);
            ResultSet<ObjectInfoDocument> typeInfoSearchResult = objectIndex.Objects().retrieve(query);
            Set<Long> objectIds = typeInfoSearchResult.stream()
                    .map(ObjectInfoDocument::getObjectId)
                    .collect(Collectors.toSet());


            if (objectIds.size() > 0) {
                queryForValueId(tracePointList, sessionArchive, objectIds);
            }

        }
        getProjectSessionErrorsCallback.success(tracePointList);
    }

    private NameWithBytes createFileOnDiskFromSessionArchiveFile(File sessionFile, String pathName) throws IOException {
        ZipInputStream indexArchive = new ZipInputStream(new FileInputStream(sessionFile));
//        String outDirName = getOutDirName(sessionFile);
//        File outDirFile = new File(this.pathToSessions + session.getName() + "/" + outDirName);
//        outDirFile.mkdirs();

        ZipEntry entry = null;
        while ((entry = indexArchive.getNextEntry()) != null) {
            if (entry.getName().contains(pathName)) {
                return new NameWithBytes(entry.getName(), IOUtils.toByteArray(indexArchive));
            }
        }
        return null;

    }

    private void refreshSessionArchivesList() {
        sessionDirectory = new File(this.pathToSessions + session.getName());
        assert sessionDirectory.exists();
        sessionArchives = Arrays
                .stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .sorted((a, b) -> -1 * a.getName().compareTo(b.getName()))
                .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                .collect(Collectors.toList());
    }

    @Override
    public void getTracesByObjectValue(
            String value, GetProjectSessionErrorsCallback getProjectSessionErrorsCallback
    ) throws IOException {
        logger.info("trace by string value: " + value);
        refreshSessionArchivesList();

        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionArchive : sessionArchives) {

            NameWithBytes bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            if (bytes == null) {
                logger.warning("archive [" + sessionArchive.getAbsolutePath() + "] is not complete or is corrupt.");
                continue;
            }
            logger.info("initialize index for archive: " + sessionArchive.getAbsolutePath());


            ArchiveIndex index = readArchiveIndex(bytes.getBytes(), INDEX_STRING_DAT_FILE);
            Set<Long> stringIds = new HashSet<>(index.getStringIdsFromStringValues(value));
            if (stringIds.size() > 0) {
                queryForValueId(tracePointList, sessionArchive, stringIds);
            }
        }
        getProjectSessionErrorsCallback.success(tracePointList);

    }

    private void queryForValueId(List<TracePoint> tracePointList, File sessionArchive, Set<Long> valueIds) {
        NameWithBytes bytes;
        ArchiveFilesIndex archiveFileIndex = null;
        ArchiveIndex objectIndex = null;
        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
        Map<String, ObjectInfo> objectInfoMap = new HashMap<>();
        try {
            bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
            assert bytes != null;
            archiveFileIndex = readEventIndex(bytes.getBytes());


            NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(
                    sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            assert objectIndexBytes != null;
            objectIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            objectInfoMap = objectIndex.getObjectsByObjectId(valueIds);

            Set<Integer> types = objectInfoMap.values().stream()
                    .map(ObjectInfo::getTypeId)
                    .map(Long::intValue)
                    .collect(Collectors.toSet());

            NameWithBytes typesInfoBytes = createFileOnDiskFromSessionArchiveFile(
                    sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            assert typesInfoBytes != null;
            ArchiveIndex typeIndex = readArchiveIndex(typesInfoBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            typeInfoMap = typeIndex.getTypesById(types);

        } catch (IOException ex) {
            ex.printStackTrace();
        }


        ArchiveFilesIndex finalArchiveFileIndex = archiveFileIndex;
        Set<UploadFile> matchedFiles = valueIds.stream().map(
                valueId -> {
                    assert finalArchiveFileIndex != null;
                    boolean archiveHasSeenValue = finalArchiveFileIndex.hasValueId(valueId);
                    List<UploadFile> matchedFilesForString = new LinkedList<>();
                    if (archiveHasSeenValue) {
                        matchedFilesForString = finalArchiveFileIndex.queryEventsByStringId(valueId);
                    }
                    return matchedFilesForString;
                }).flatMap(Collection::parallelStream).collect(Collectors.toSet());
        Map<String, ObjectInfo> finalObjectInfoMap = objectInfoMap;
        Map<String, TypeInfo> finalTypeInfoMap = typeInfoMap;
        matchedFiles.forEach(e -> {
            try {

                String fileName = Path.of(e.getPath()).getFileName().toString();

                NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, fileName);
                long timestamp = Long.parseLong(fileBytes.getName().split("@")[0]);
                int threadId = Integer.parseInt(fileBytes.getName().split("-")[2].split("\\.")[0]);


                List<DataEventWithSessionId> dataEvents = getDataEventsFromPath(fileBytes.getBytes(), e.getValueIds());

                tracePointList.addAll(
                        dataEvents.stream().map(e1 -> {

                            try {
                                List<DataInfo> dataInfoList = getProbeInfo(sessionArchive,
                                        Set.of(e1.getDataId()));

                                DataInfo dataInfo = dataInfoList.get(0);
                                int classId = dataInfo.getClassId();
                                KaitaiInsidiousClassWeaveParser.ClassInfo classInfo
                                        = getClassInfo(classId);

                                ObjectInfo objectInfo = finalObjectInfoMap.get(String.valueOf(e1.getValue()));
                                TypeInfo typeInfo = getTypeInfo((int) objectInfo.getTypeId());

                                return new TracePoint(classId,
                                        dataInfo.getLine(), dataInfo.getDataId(),
                                        threadId, e1.getValue(),
                                        session.getId(),
                                        classInfo.fileName().value(),
                                        typeInfo.getTypeNameFromClass(),
                                        typeInfo.getSuperClass(),
                                        e1.getRecordedAt().getTime(), timestamp);
                            } catch (ClassInfoNotFoundException | Exception ex) {
                                logger.info("failed to get data probe information: "
                                        + ex.getMessage() + " -- " + ex.getCause());
                            }
                            return null;


                        }).filter(Objects::nonNull).collect(Collectors.toList()));

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }


    private KaitaiInsidiousClassWeaveParser.ClassInfo getClassInfo(int classId)
            throws ClassInfoNotFoundException {

        for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeaveInfo.classInfo()) {
            if (classInfo.classId() == classId) {
                return classInfo;
            }
        }
        throw new ClassInfoNotFoundException(classId);
    }

    private TypeInfo getTypeInfo(Integer typeId)
            throws ClassInfoNotFoundException, IOException {
        List<File> archives = this.sessionArchives;

        for (int i = archives.size(); i > 0; i--) {
            File sessionArchive = archives.get(i - 1);
            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            assert typeIndexBytes != null;
            ArchiveIndex typeIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);

            Map<String, TypeInfo> result = typeIndex.getTypesById(Set.of(typeId));
            if (result.size() > 0) {
                return result.get(String.valueOf(typeId));
            }

        }

        return new TypeInfo("s", typeId, "unidentified type", "", "", "", "");
    }


    private List<DataInfo> getProbeInfo(File sessionFile, Set<Integer> dataId) throws IOException {


        if (classWeaveInfo == null) {
            NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionFile, WEAVE_DAT_FILE.getFileName());
            assert fileBytes != null;
            classWeaveInfo = new KaitaiInsidiousClassWeaveParser(new ByteBufferKaitaiStream(fileBytes.getBytes()));
        }

        return classWeaveInfo.classInfo().parallelStream()
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
            valueDesc = Descriptor.valueOf(descriptorValue);
        }
        return new DataInfo(
                Math.toIntExact(e.classId()),
                Math.toIntExact(e.methodId()),
                Math.toIntExact(e.dataId()),
                Math.toIntExact(e.lineNumber()),
                Math.toIntExact(e.instructionIndex()),
                EventType.valueOf(e.eventType().value()),
                valueDesc,
                e.attributes().value()
        );
    }

    private List<DataEventWithSessionId> getDataEventsFromPath(byte[] bytes, Long[] valueIds) throws IOException {

        Set<Long> ids = Set.of(valueIds);
        KaitaiInsidiousEventParser dataEvents
                = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(bytes));

        return dataEvents.event().entries().stream().filter(
                e -> e.magic() == 4 && ids.contains(((KaitaiInsidiousEventParser.DataEventBlock) e.block()).valueId())
        ).map(e -> {
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
        KaitaiInsidiousIndexParser archiveIndex = new KaitaiInsidiousIndexParser(
                new ByteBufferKaitaiStream(bytes));

        return new ArchiveFilesIndex(archiveIndex);
    }

    private ArchiveIndex readArchiveIndex(byte[] bytes, DatFileType indexFilterType) throws IOException {

        ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex = null;
        String md5Hex = DigestUtils.md5Hex(bytes);
        String cacheKey = md5Hex + "-" + indexFilterType.getFileName();
//        if (indexCache.containsKey(cacheKey)) {
//            return indexCache.get(cacheKey);
//        }

        Path path = Path.of(this.pathToSessions, session.getName(), indexFilterType.getFileName());
        Files.write(path, bytes);

        if (indexFilterType.equals(INDEX_TYPE_DAT_FILE)) {
            DiskPersistence<TypeInfoDocument, Integer> typeInfoDocumentStringDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(TypeInfoDocument.TYPE_ID, path.toFile());
            typeInfoIndex = new ConcurrentIndexedCollection<>(typeInfoDocumentStringDiskPersistence);
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_NAME));
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_ID));
        }


        ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex = null;
        if (indexFilterType.equals(INDEX_STRING_DAT_FILE)) {
            DiskPersistence<StringInfoDocument, Long> stringInfoDocumentStringDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(StringInfoDocument.STRING_ID, path.toFile());
            stringInfoIndex = new ConcurrentIndexedCollection<>(stringInfoDocumentStringDiskPersistence);

            stringInfoIndex.addIndex(InvertedRadixTreeIndex.onAttribute(StringInfoDocument.STRING_VALUE));
        }

        ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex = null;
        if (indexFilterType.equals(INDEX_OBJECT_DAT_FILE)) {
            DiskPersistence<ObjectInfoDocument, Long> objectInfoDocumentIntegerDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(ObjectInfoDocument.OBJECT_ID, path.toFile());

            objectInfoIndex = new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_TYPE_ID));
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_ID));
        }

        if (indexFilterType.equals(WEAVE_DAT_FILE)) {
            KaitaiInsidiousClassWeaveParser classWeave = new KaitaiInsidiousClassWeaveParser(
                    new ByteBufferKaitaiStream(bytes));

            for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeave.classInfo()) {
                classInfoMap.put(classInfo.className().value(), classInfo);
            }
            return new ArchiveIndex(null, null, null);
        }

        ArchiveIndex archiveIndex = new ArchiveIndex(typeInfoIndex, stringInfoIndex, objectInfoIndex);
        indexCache.put(cacheKey, archiveIndex);
        return archiveIndex;
    }

    @Override
    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) throws IOException {

        File archiveToServe = null;
        for (File sessionArchive : this.sessionArchives) {
            long timestamp = Long.parseLong(sessionArchive.getName().split("-")[2].split("\\.")[0]);
            if (timestamp < filteredDataEventsRequest.getNanotime()) {
                archiveToServe = sessionArchive;
                break;
            }
        }


        NameWithBytes bytesWithName = createFileOnDiskFromSessionArchiveFile(
                archiveToServe, String.valueOf(filteredDataEventsRequest.getNanotime()));

        KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(
                new ByteBufferKaitaiStream(bytesWithName.getBytes()));

        List<DataEventWithSessionId> dataEventList = eventsContainer.event().entries().stream()
                .filter(e -> e.magic() == 4)
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


        Map<String, KaitaiInsidiousClassWeaveParser.ClassInfo> classInfo = new HashMap<>();
        Map<String, KaitaiInsidiousClassWeaveParser.ProbeInfo> dataInfo = new HashMap<>();

        classWeaveInfo.classInfo().forEach(e -> {
            classInfo.put(String.valueOf(e.classId()), e);
            e.probeList().forEach(r -> {
                dataInfo.put(String.valueOf(r.dataId()), r);
            });
        });

        Set<Integer> probeIds = dataEventList.stream()
                .map(DataEventWithSessionId::getDataId).collect(Collectors.toSet());
        Set<Long> valueIds = dataEventList.stream()
                .map(DataEventWithSessionId::getValue).collect(Collectors.toSet());


        Map<String, StringInfo> stringInfo = new HashMap<>();
        Map<String, ObjectInfo> objectInfo = new HashMap<>();
        Map<String, TypeInfo> typeInfo = new HashMap<>();

        for (File sessionArchive : this.sessionArchives) {

            NameWithBytes objectsIndexBytes =
                    createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_OBJECT_DAT_FILE.getFileName());
            if (objectsIndexBytes == null) {
                continue;
            }
            ArchiveIndex objectIndex = readArchiveIndex(objectsIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            Map<String, ObjectInfo> sessionObjectInfo = objectIndex.getObjectsByObjectId(valueIds);
            objectInfo.putAll(sessionObjectInfo);


            NameWithBytes stringsIndexBytes =
                    createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            assert stringsIndexBytes != null;
            ArchiveIndex stringIndex = readArchiveIndex(stringsIndexBytes.getBytes(), INDEX_STRING_DAT_FILE);
            Map<String, StringInfo> sessionStringInfo = stringIndex.getStringsById(
                    valueIds.stream().filter(e -> e > 10).collect(Collectors.toSet()));
            stringInfo.putAll(sessionStringInfo);


            Set<Integer> typeIds = objectInfo.values().stream()
                    .map(ObjectInfo::getTypeId)
                    .map(Long::intValue)
                    .collect(Collectors.toSet());

            NameWithBytes typeIndexBytes =
                    createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            assert typeIndexBytes != null;
            ArchiveIndex typesIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            Map<String, TypeInfo> sessionTypeInfo = typesIndex.getTypesById(typeIds);
            typeInfo.putAll(sessionTypeInfo);

        }

        return new ReplayData(
                dataEventList, classInfo, dataInfo, stringInfo, objectInfo, typeInfo, "DESC"
        );
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
        agentDownloadUrlCallback.success("");
    }

    @Override
    public void downloadAgentFromUrl(String url, String insidiousLocalPath, boolean overwrite) {

    }
}
