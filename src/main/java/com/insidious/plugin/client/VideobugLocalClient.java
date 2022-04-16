package com.insidious.plugin.client;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.radixinverted.InvertedRadixTreeIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.UploadFile;
import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.parser.KaitaiInsidiousEventParser;
import com.insidious.common.parser.KaitaiInsidiousIndexParser;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.client.cache.ArchiveIndex;
import com.insidious.plugin.client.exception.ClassInfoNotFoundException;
import com.insidious.plugin.client.pojo.*;
import com.insidious.plugin.client.pojo.local.ObjectInfoDocument;
import com.insidious.plugin.client.pojo.local.StringInfoDocument;
import com.insidious.plugin.client.pojo.local.TypeInfoDocument;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.TracePoint;
import io.kaitai.struct.ByteBufferKaitaiStream;
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

import static com.googlecode.cqengine.query.QueryFactory.equal;
import static com.googlecode.cqengine.query.QueryFactory.in;
import static com.insidious.plugin.client.DatFileType.*;

public class VideobugLocalClient implements VideobugClientInterface {

    private static final Logger logger = java.util.logging.Logger.getLogger(VideobugLocalClient.class.getName());
    private final String pathToSessions;
    private final Map<String, KaitaiInsidiousClassWeaveParser.ClassInfo> classInfoMap = new HashMap<>();
    private ExecutionSession session;
    private ProjectItem currentProject;
    private KaitaiInsidiousClassWeaveParser classWeaveInfo;

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
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    loadSessionIndexes(0);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
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
        logger.info("trace by typeName");

        logger.info("trace by class: " + classList);
        long start;
        File sessionDirectory = new File(this.pathToSessions + session.getName());
        assert sessionDirectory.isDirectory();
        List<File> sessionArchives = Arrays
                .stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                .collect(Collectors.toList());


        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionArchive : sessionArchives) {

            logger.info("initialize index for archive: " + sessionArchive.getAbsolutePath());
            byte[] fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE.getFileName());
            if (fileBytes == null) {
                continue;
            }
            ArchiveIndex index = readArchiveIndex(fileBytes, INDEX_TYPE_DAT_FILE);

            Query<TypeInfoDocument> query = in(TypeInfoDocument.TYPE_NAME, classList);
            ResultSet<TypeInfoDocument> searchResult = index.Types().retrieve(query);

            if (searchResult.size() > 0) {
                List<TracePoint> tracePoints = searchResult.stream()
                        .map(e ->
                                new TracePoint(1, 1, 1, 1,
                                        e.getTypeId(), "1", "1", "1",
                                        e.getTypeName(), 1, 1))
                        .collect(Collectors.toList());

                tracePointList.addAll(tracePoints);
            }
        }
        getProjectSessionErrorsCallback.success(tracePointList);
    }

    private byte[] createFileOnDiskFromSessionArchiveFile(File sessionFile, String pathName) throws IOException {
        ZipInputStream indexArchive = new ZipInputStream(new FileInputStream(sessionFile));
//        String outDirName = getOutDirName(sessionFile);
//        File outDirFile = new File(this.pathToSessions + session.getName() + "/" + outDirName);
//        outDirFile.mkdirs();

        ZipEntry entry = null;
        while ((entry = indexArchive.getNextEntry()) != null) {
            if (entry.getName().contains(pathName)) {
                return IOUtils.toByteArray(indexArchive);
            }
        }
        return null;

    }

    private String getOutDirName(File sessionFile) {
        return sessionFile.getName().split(".zip")[0];
    }

    @Override
    public void getTracesByObjectValue(
            String value, GetProjectSessionErrorsCallback getProjectSessionErrorsCallback
    ) throws IOException {
        logger.info("trace by string value: " + value);
        long start;
        File sessionDirectory = new File(this.pathToSessions + session.getName());
        assert sessionDirectory.isDirectory();
        List<File> sessionArchives = Arrays
                .stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                .collect(Collectors.toList());


        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionArchive : sessionArchives) {

            byte[] bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_STRING_DAT_FILE.getFileName());
            if (bytes == null) {
                logger.warning("archive [" + sessionArchive.getAbsolutePath() + "] is not complete or is corrupt.");
                continue;
            }
            logger.info("initialize index for archive: " + sessionArchive.getAbsolutePath());
            ArchiveIndex index = readArchiveIndex(bytes, INDEX_STRING_DAT_FILE);


            Query<StringInfoDocument> query = equal(StringInfoDocument.STRING_VALUE, value);
            ResultSet<StringInfoDocument> searchResult = index.Strings().retrieve(query);

            if (searchResult.size() > 0) {
                ArchiveFilesIndex archiveFileIndex = null;
                try {
                    bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
                    archiveFileIndex = readEventIndex(bytes);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                ArchiveFilesIndex finalArchiveFileIndex = archiveFileIndex;
                Set<UploadFile> matchedFiles = searchResult.stream().map(
                        e -> {
                            long stringId = e.getStringId();
                            boolean archiveHasSeenValue = finalArchiveFileIndex.hasValueId(stringId);
                            List<UploadFile> matchedFilesForString = new LinkedList<>();
                            if (archiveHasSeenValue) {
                                matchedFilesForString = finalArchiveFileIndex.queryEventsByStringId(stringId);
                            }
                            return matchedFilesForString;
                        }).flatMap(Collection::parallelStream).collect(Collectors.toSet());
                matchedFiles.forEach(e -> {
                    try {

                        String fileName = Path.of(e.getPath()).getFileName().toString();

                        byte[] fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, fileName);
                        List<DataEventWithSessionId> dataEvents = getDataEventsFromPath(fileBytes, e.getValueIds());

                        tracePointList.addAll(
                                dataEvents.stream().map(e1 -> {

                                    try {
                                        List<DataInfo> dataInfoList = getProbeInfo(sessionArchive,
                                                Set.of(e1.getDataId()));

                                        DataInfo dataInfo = dataInfoList.get(0);
                                        int classId = dataInfo.getClassId();
                                        KaitaiInsidiousClassWeaveParser.ClassInfo classInfo
                                                = getClassInfo(classId);


                                        return new TracePoint(classId,
                                                dataInfo.getLine(), dataInfo.getDataId(),
                                                e1.getThreadId(), e1.getValue(),
                                                session.getId(),
                                                classInfo.fileName().value(),
                                                classInfo.className().value(),
                                                classInfo.className().value(),
                                                e1.getRecordedAt().getTime(), e1.getNanoTime());
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
        }
        getProjectSessionErrorsCallback.success(tracePointList);

    }

    private KaitaiInsidiousClassWeaveParser.ClassInfo
    getClassInfo(int classId) throws ClassInfoNotFoundException {

        for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeaveInfo.classInfo()) {
            if (classInfo.classId() == classId) {
                return classInfo;
            }
        }
        throw new ClassInfoNotFoundException(classId);

    }

    private List<DataInfo> getProbeInfo(File sessionFile, Set<Integer> dataId) throws IOException {


        if (classWeaveInfo == null) {
            byte[] fileBytes = createFileOnDiskFromSessionArchiveFile(sessionFile, WEAVE_DAT_FILE.getFileName());
            assert fileBytes != null;
            classWeaveInfo = new KaitaiInsidiousClassWeaveParser(new ByteBufferKaitaiStream(fileBytes));
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

        Path path = Path.of(this.pathToSessions, session.getName(), indexFilterType.getFileName());
        Files.write(path, bytes);

        if (indexFilterType.equals(INDEX_TYPE_DAT_FILE)) {
            DiskPersistence<TypeInfoDocument, String> typeInfoDocumentStringDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(TypeInfoDocument.TYPE_NAME, path.toFile());
            typeInfoIndex = new ConcurrentIndexedCollection<>(typeInfoDocumentStringDiskPersistence);
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_NAME));
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
            DiskPersistence<ObjectInfoDocument, Integer> objectInfoDocumentIntegerDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(ObjectInfoDocument.OBJECT_TYPE_ID, path.toFile());

            objectInfoIndex = new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_TYPE_ID));
        }

        if (indexFilterType.equals(WEAVE_DAT_FILE)) {
            KaitaiInsidiousClassWeaveParser classWeave = new KaitaiInsidiousClassWeaveParser(
                    new ByteBufferKaitaiStream(bytes));

            for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeave.classInfo()) {
                classInfoMap.put(classInfo.className().value(), classInfo);
            }
        }

        return new ArchiveIndex(typeInfoIndex, stringInfoIndex, objectInfoIndex);
    }

    @Override
    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) {
        return null;
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
