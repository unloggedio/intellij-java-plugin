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
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.DataInfo;
import com.insidious.plugin.extension.model.Descriptor;
import com.insidious.plugin.extension.model.EventType;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.client.cache.ArchiveIndex;
import com.insidious.plugin.client.pojo.*;
import com.insidious.plugin.client.pojo.local.ObjectInfoDocument;
import com.insidious.plugin.client.pojo.local.StringInfoDocument;
import com.insidious.plugin.client.pojo.local.TypeInfoDocument;
import com.intellij.openapi.util.io.StreamUtil;
import io.kaitai.struct.ByteBufferKaitaiStream;

import java.io.*;
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
            createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_TYPE_DAT_FILE, null);
            ArchiveIndex index = readArchiveIndex(sessionArchive.getAbsolutePath() + "/", INDEX_TYPE_DAT_FILE.getFileName());

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

    private void createFileOnDiskFromSessionArchiveFile(File sessionFile,
                                                        DatFileType indexType, String pathName) throws IOException {
        ZipInputStream indexArchive = new ZipInputStream(new FileInputStream(sessionFile));
        String outDirName = sessionFile.getName().split(".zip")[0];
        File outDirFile = new File(this.pathToSessions + session.getName() + "/" + outDirName);
        outDirFile.mkdirs();

        ZipEntry entry = null;
        while ((entry = indexArchive.getNextEntry()) != null) {

            switch (indexType) {
                case INDEX_EVENTS_DAT_FILE:

                    if (entry.getName().equals(INDEX_EVENTS_DAT_FILE.getFileName())) {
                        FileOutputStream fos = new FileOutputStream(
                                Path.of(outDirFile.getAbsolutePath(),
                                        INDEX_EVENTS_DAT_FILE.getFileName()).toFile());
                        StreamUtil.copy(indexArchive, fos);
                        fos.close();
                    } else if (entry.getName().equals(pathName)) {
                        FileOutputStream fos = new FileOutputStream(
                                Path.of(outDirFile.getAbsolutePath(), pathName).toFile());
                        StreamUtil.copy(indexArchive, fos);
                        fos.close();
                    }


                    break;
                case INDEX_OBJECT_DAT_FILE:

                    if (entry.getName().equals(INDEX_OBJECT_DAT_FILE.getFileName())) {
                        FileOutputStream fos = new FileOutputStream(
                                Path.of(outDirFile.getAbsolutePath(),
                                        INDEX_OBJECT_DAT_FILE.getFileName()).toFile());
                        StreamUtil.copy(indexArchive, fos);
                        fos.close();
                    }


                    break;
                case INDEX_STRING_DAT_FILE:


                    if (entry.getName().equals(INDEX_STRING_DAT_FILE.getFileName())) {
                        FileOutputStream fos = new FileOutputStream(
                                Path.of(outDirFile.getAbsolutePath(), INDEX_STRING_DAT_FILE.getFileName()).toFile());
                        StreamUtil.copy(indexArchive, fos);
                        fos.close();
                    }


                    break;
                case INDEX_TYPE_DAT_FILE:


                    if (entry.getName().equals(INDEX_TYPE_DAT_FILE.getFileName())) {
                        FileOutputStream fos = new FileOutputStream(
                                Path.of(outDirFile.getAbsolutePath(), INDEX_TYPE_DAT_FILE.getFileName()).toFile());
                        StreamUtil.copy(indexArchive, fos);
                        fos.close();
                    }


                    break;
                case WEAVE_DAT_FILE:

                    if (entry.getName().equals(pathName)) {
                        FileOutputStream fos = new FileOutputStream(
                                Path.of(outDirFile.getAbsolutePath(), pathName).toFile());
                        StreamUtil.copy(indexArchive, fos);
                        fos.close();
                    }

                    break;
            }


        }

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

            createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_STRING_DAT_FILE, null);
            logger.info("initialize index for archive: " + sessionArchive.getAbsolutePath());
            ArchiveIndex index =
                    readArchiveIndex(sessionArchive.getAbsolutePath() + "/", INDEX_STRING_DAT_FILE.getFileName());


            Query<StringInfoDocument> query = equal(StringInfoDocument.STRING_VALUE, value);
            ResultSet<StringInfoDocument> searchResult = index.Strings().retrieve(query);

            if (searchResult.size() > 0) {
                ArchiveFilesIndex archiveFileIndex = null;
                try {
                    archiveFileIndex = readEventIndex(sessionArchive.getAbsolutePath());
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

                        createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE, e.getPath());
                        List<DataEventWithSessionId> dataEvents
                                = getDataEventsFromPath(sessionArchive, e.getPath(), e.getValueIds());

                        tracePointList.addAll(
                                dataEvents.stream().map(e1 -> {

                                    try {
                                        getProbeInfo(sessionArchive, e.getPath(), Set.of(e1.getDataId()));
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }

                                    return new TracePoint(1, 1, 1,
                                            1, 1, "asdf", "asdf",
                                            "asdf", "asdf", 1, 1);
                                }).collect(Collectors.toList()));

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

            }
        }
        getProjectSessionErrorsCallback.success(tracePointList);

    }

    private List<DataInfo> getProbeInfo(File sessionFile, String filePath, Set<Integer> dataId) throws IOException {

        createFileOnDiskFromSessionArchiveFile(sessionFile, WEAVE_DAT_FILE, filePath);
        KaitaiInsidiousClassWeaveParser classWeaveInfo = new KaitaiInsidiousClassWeaveParser(
                new ByteBufferKaitaiStream(
                        new FileInputStream(
                                Path.of(sessionFile.getAbsolutePath(), filePath).toFile()).readAllBytes()));


        return classWeaveInfo.classInfo().parallelStream()
                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
                .flatMap(Collection::stream)
                .filter(e -> dataId.contains(Math.toIntExact(e.dataId())))
                .map(e -> new DataInfo(
                        Math.toIntExact(e.classId()),
                        Math.toIntExact(e.methodId()),
                        Math.toIntExact(e.dataId()),
                        Math.toIntExact(e.lineNumber()),
                        Math.toIntExact(e.instructionIndex()),
                        EventType.valueOf(e.eventType().value()),
                        Descriptor.valueOf(e.valueDescriptor().value()),
                        e.attributes().value()
                )).collect(Collectors.toList());

    }

    private List<DataEventWithSessionId> getDataEventsFromPath(File sessionFile,
                                                               String filepath, Long[] valueIds) throws IOException {

        Set<Long> ids = Set.of(valueIds);
        File eventsFile = Path.of(sessionFile.getAbsolutePath(), filepath).toFile();
        assert eventsFile.exists();

        KaitaiInsidiousEventParser dataEvents
                = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(new FileInputStream(eventsFile).readAllBytes()));

        return dataEvents.event().entries().stream().filter(
                e -> e.magic() == 4 && ids.contains(((KaitaiInsidiousEventParser.DataEventBlock) ids).valueId())
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

    private ArchiveFilesIndex readEventIndex(String outDirFile) throws IOException {
        File typeIndexFile = new File(outDirFile + INDEX_EVENTS_DAT_FILE);

        KaitaiInsidiousIndexParser archiveIndex = new KaitaiInsidiousIndexParser(
                new ByteBufferKaitaiStream(
                        new FileInputStream(typeIndexFile).readAllBytes()));

        return new ArchiveFilesIndex(archiveIndex);
    }

    private ArchiveIndex readArchiveIndex(String outDirFile, String indexFilterType) throws IOException {

        ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex = null;

        if (indexFilterType.equals(INDEX_TYPE_DAT_FILE.getFileName())) {
            File typeIndexFile = new File(outDirFile + INDEX_TYPE_DAT_FILE.getFileName());
            DiskPersistence<TypeInfoDocument, String> typeInfoDocumentStringDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(TypeInfoDocument.TYPE_NAME, typeIndexFile);
            typeInfoIndex = new ConcurrentIndexedCollection<>(typeInfoDocumentStringDiskPersistence);
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_NAME));
        }


        ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex = null;
        if (indexFilterType.equals(INDEX_STRING_DAT_FILE.getFileName())) {
            File stringIndexFile = new File(outDirFile + INDEX_STRING_DAT_FILE.getFileName());
            DiskPersistence<StringInfoDocument, Long> stringInfoDocumentStringDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(StringInfoDocument.STRING_ID, stringIndexFile);
            stringInfoIndex = new ConcurrentIndexedCollection<>(stringInfoDocumentStringDiskPersistence);
            stringInfoIndex.addIndex(InvertedRadixTreeIndex.onAttribute(StringInfoDocument.STRING_VALUE));
        }

        ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex = null;
        if (indexFilterType.equals(INDEX_OBJECT_DAT_FILE.getFileName())) {
            File objectIndexFile = new File(outDirFile + INDEX_OBJECT_DAT_FILE.getFileName());
            DiskPersistence<ObjectInfoDocument, Integer> objectInfoDocumentIntegerDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(ObjectInfoDocument.OBJECT_TYPE_ID, objectIndexFile);

            objectInfoIndex = new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_TYPE_ID));
        }

        if (indexFilterType.equals(WEAVE_DAT_FILE.getFileName())) {
            File objectIndexFile = new File(outDirFile + WEAVE_DAT_FILE.getFileName());
            KaitaiInsidiousClassWeaveParser classWeave = new KaitaiInsidiousClassWeaveParser(
                    new ByteBufferKaitaiStream(
                            new FileInputStream(objectIndexFile).readAllBytes()));

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
