package com.insidious.plugin.videobugclient;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.radixinverted.InvertedRadixTreeIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.plugin.callbacks.*;
import com.insidious.plugin.extension.connector.model.ProjectItem;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.index.InsidiousIndexParser;
import com.insidious.plugin.parser.ClassWeaveByteFormat;
import com.insidious.plugin.pojo.SessionCache;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.videobugclient.cache.ArchiveIndex;
import com.insidious.plugin.videobugclient.pojo.*;
import com.insidious.plugin.videobugclient.pojo.local.ObjectInfoDocument;
import com.insidious.plugin.videobugclient.pojo.local.StringInfoDocument;
import com.insidious.plugin.videobugclient.pojo.local.TypeInfoDocument;
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

public class VideobugLocalClient implements VideobugClientInterface {

    public static final String WEAVE_DAT_FILE = "class.weave.dat";
    public static final String INDEX_TYPE_DAT_FILE = "index.type.dat";
    public static final String INDEX_OBJECT_DAT_FILE = "index.object.dat";
    public static final String INDEX_EVENTS_DAT_FILE = "index.events.dat";
    public static final String INDEX_STRING_DAT_FILE = "index.string.dat";


    private static final Logger logger = java.util.logging.Logger.getLogger(VideobugLocalClient.class.getName());
    private final String pathToSessions;
    private final Map<String, ClassWeaveByteFormat.ClassInfo> classInfoMap = new HashMap<>();
    private ExecutionSession session;
    private ProjectItem currentProject;
    private SessionCache sessionCache;

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
        List<File> sessionFiles = Arrays
                .stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                .collect(Collectors.toList());


        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionFile : sessionFiles) {

            logger.info("initialize index for archive: " + sessionFile.getAbsolutePath());
            buildIndexFromSessionFile(sessionFile, INDEX_TYPE_DAT_FILE);
            ArchiveIndex index = readArchiveIndex(sessionFile.getAbsolutePath() + "/", INDEX_TYPE_DAT_FILE);

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

    private void buildIndexFromSessionFile(File sessionFile, String indexType) throws IOException {
        ZipInputStream indexArchive = new ZipInputStream(new FileInputStream(sessionFile));
        String outDirName = sessionFile.getName().split(".zip")[0];
        File outDirFile = new File(this.pathToSessions + session.getName() + "/" + outDirName);
        outDirFile.mkdirs();

        ZipEntry entry = null;
        while ((entry = indexArchive.getNextEntry()) != null) {

            if (entry.getName().equals(INDEX_EVENTS_DAT_FILE) && INDEX_EVENTS_DAT_FILE.equals(indexType)) {
                FileOutputStream fos = new FileOutputStream(Path.of(outDirFile.getAbsolutePath(), INDEX_EVENTS_DAT_FILE).toFile());
                StreamUtil.copy(indexArchive, fos);
                fos.close();
            }
            if (entry.getName().equals(INDEX_OBJECT_DAT_FILE) && INDEX_OBJECT_DAT_FILE.equals(indexType)) {
                FileOutputStream fos = new FileOutputStream(Path.of(outDirFile.getAbsolutePath(), INDEX_OBJECT_DAT_FILE).toFile());
                StreamUtil.copy(indexArchive, fos);
                fos.close();
            }


            if (entry.getName().equals(INDEX_STRING_DAT_FILE) && INDEX_STRING_DAT_FILE.equals(indexType)) {
                FileOutputStream fos = new FileOutputStream(Path.of(outDirFile.getAbsolutePath(), INDEX_STRING_DAT_FILE).toFile());
                StreamUtil.copy(indexArchive, fos);
                fos.close();
            }

            if (entry.getName().equals(INDEX_TYPE_DAT_FILE) && INDEX_TYPE_DAT_FILE.equals(indexType)) {
                FileOutputStream fos = new FileOutputStream(Path.of(outDirFile.getAbsolutePath(), INDEX_TYPE_DAT_FILE).toFile());
                StreamUtil.copy(indexArchive, fos);
                fos.close();
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
        List<File> sessionFiles = Arrays
                .stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                .collect(Collectors.toList());


        List<TracePoint> tracePointList = new LinkedList<>();
        for (File sessionFile : sessionFiles) {

            buildIndexFromSessionFile(sessionFile, INDEX_STRING_DAT_FILE);
            logger.info("initialize index for archive: " + sessionFile.getAbsolutePath());
            ArchiveIndex index = readArchiveIndex(sessionFile.getAbsolutePath() + "/", INDEX_STRING_DAT_FILE);


            Query<StringInfoDocument> query = equal(StringInfoDocument.STRING_VALUE, value);
            ResultSet<StringInfoDocument> searchResult = index.Strings().retrieve(query);

            if (searchResult.size() > 0) {
                List<TracePoint> tracePoints = searchResult.stream()
                        .map(e -> {

                            long stringId = e.getStringId();
                            ArchiveIndex eventsIndex = null;
                            try {
                                eventsIndex = readEventIndex(sessionFile.getAbsolutePath());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            eventsIndex.query


                            return tracePoint;
                        })
                        .collect(Collectors.toList());
                tracePointList.addAll(tracePoints);
            }
        }
        getProjectSessionErrorsCallback.success(tracePointList);

    }

    private ArchiveFilesIndex readEventIndex(String outDirFile) throws IOException {
        ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex = null;
        File typeIndexFile = new File(outDirFile + INDEX_EVENTS_DAT_FILE);

        InsidiousIndexParser archiveIndex = new InsidiousIndexParser(
                new ByteBufferKaitaiStream(
                        new FileInputStream(typeIndexFile).readAllBytes()));

        ArchiveFilesIndex archiveFilesIndex = new ArchiveFilesIndex(archiveIndex);

        return archiveFilesIndex;
    }

    private ArchiveIndex readArchiveIndex(String outDirFile, String indexFilterType) throws IOException {

        ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex = null;
        if (indexFilterType.equals(INDEX_TYPE_DAT_FILE)) {
            File typeIndexFile = new File(outDirFile + INDEX_TYPE_DAT_FILE);
            DiskPersistence<TypeInfoDocument, String> typeInfoDocumentStringDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(TypeInfoDocument.TYPE_NAME, typeIndexFile);
            typeInfoIndex = new ConcurrentIndexedCollection<>(typeInfoDocumentStringDiskPersistence);
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_NAME));
        }


        ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex = null;
        if (indexFilterType.equals(INDEX_STRING_DAT_FILE)) {
            File stringIndexFile = new File(outDirFile + INDEX_STRING_DAT_FILE);
            DiskPersistence<StringInfoDocument, Long> stringInfoDocumentStringDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(StringInfoDocument.STRING_ID, stringIndexFile);
            stringInfoIndex = new ConcurrentIndexedCollection<>(stringInfoDocumentStringDiskPersistence);
            stringInfoIndex.addIndex(InvertedRadixTreeIndex.onAttribute(StringInfoDocument.STRING_VALUE));
        }

        ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex = null;
        if (indexFilterType.equals(INDEX_OBJECT_DAT_FILE)) {
            File objectIndexFile = new File(outDirFile + INDEX_OBJECT_DAT_FILE);
            DiskPersistence<ObjectInfoDocument, Integer> objectInfoDocumentIntegerDiskPersistence
                    = DiskPersistence.onPrimaryKeyInFile(ObjectInfoDocument.OBJECT_TYPE_ID, objectIndexFile);

            objectInfoIndex = new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
            objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_TYPE_ID));
        }

        if (indexFilterType.equals(WEAVE_DAT_FILE)) {
            File objectIndexFile = new File(outDirFile + WEAVE_DAT_FILE);
            ClassWeaveByteFormat classWeave = new ClassWeaveByteFormat(
                    new ByteBufferKaitaiStream(
                            new FileInputStream(objectIndexFile).readAllBytes()));

            for (ClassWeaveByteFormat.ClassInfo classInfo : classWeave.classInfo()) {
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
