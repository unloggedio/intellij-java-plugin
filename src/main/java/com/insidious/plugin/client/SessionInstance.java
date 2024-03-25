package com.insidious.plugin.client;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.radixinverted.InvertedRadixTreeIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.insidious.common.ChronicleUtils;
import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.UploadFile;
import com.insidious.common.cqengine.ObjectInfoDocument;
import com.insidious.common.cqengine.StringInfoDocument;
import com.insidious.common.cqengine.TypeInfoDocument;
import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.parser.KaitaiInsidiousEventParser;
import com.insidious.common.weaver.*;
import com.insidious.plugin.Constants;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.MethodSignatureParser;
import com.insidious.plugin.client.cache.ArchiveIndex;
import com.insidious.plugin.client.exception.ClassInfoNotFoundException;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.NameWithBytes;
import com.insidious.plugin.coverage.ClassCoverageData;
import com.insidious.plugin.coverage.CodeCoverageData;
import com.insidious.plugin.coverage.MethodCoverageData;
import com.insidious.plugin.coverage.PackageCoverageData;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.factory.CandidateSearchQuery;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.factory.testcase.parameter.ChronicleVariableContainer;
import com.insidious.plugin.pojo.ClassWeaveInfo;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.pojo.ThreadProcessingState;
import com.insidious.plugin.pojo.atomic.MethodUnderTest;
import com.insidious.plugin.pojo.dao.ClassDefinition;
import com.insidious.plugin.pojo.dao.LogFile;
import com.insidious.plugin.pojo.dao.MethodDefinition;
import com.insidious.plugin.ui.NewTestCandidateIdentifiedListener;
import com.insidious.plugin.util.*;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.RandomAccessFileKaitaiStream;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static com.insidious.common.weaver.EventType.*;
import static com.insidious.plugin.client.DatFileType.*;

public class SessionInstance implements Runnable {
    private static final Logger logger = LoggerUtil.getInstance(SessionInstance.class);
    private final File sessionDirectory;
    private final ExecutionSession executionSession;
    private final Map<String, String> cacheEntries = new HashMap<>();
    //    private final DatabasePipe databasePipe;
    private final DaoService daoService;
    private final Map<String, List<String>> zipFileListMap = new HashMap<>();
    private final ExecutorService executorPool;
    private final Map<String, Boolean> objectIndexRead = new HashMap<>();
    private final ZipConsumer zipConsumer;
    private final Project project;
    private final Map<String, Boolean> classNotFound = new HashMap<>();
    private final Map<String, ClassInfo> classInfoIndexByName = new HashMap<>();
    private final Map<Long, com.insidious.plugin.pojo.dao.MethodCallExpression> methodCallMap = new HashMap<>();
    private final Map<Long, String> methodCallSubjectTypeMap = new HashMap<>();
    private final String processorId;
    private final Map<Integer, Integer> methodLineCount = new HashMap<>();
    private final Map<Integer, Boolean> methodUsesFields = new HashMap<>();
    private final List<SessionScanEventListener> sessionScanEventListeners = new ArrayList<>();
    private boolean scanEnable = false;
    private List<File> sessionArchives = new ArrayList<>();
    private ArchiveIndex archiveIndex;
    private ChronicleMap<Long, ObjectInfoDocument> objectInfoIndex;
    private ChronicleMap<Integer, DataInfo> probeInfoIndex;
    private ChronicleMap<Integer, TypeInfoDocument> typeInfoIndex;
    private ChronicleMap<Integer, MethodInfo> methodInfoIndex;
    private ChronicleMap<String, MethodInfo> methodInfoByNameIndex;
    private ChronicleMap<Integer, ClassInfo> classInfoIndex;
    private ConcurrentIndexedCollection<ObjectInfoDocument> objectIndexCollection;
    private List<NewTestCandidateIdentifiedListener> testCandidateListener = new ArrayList<>();
    private File currentSessionArchiveBeingProcessed;
    private ChronicleVariableContainer parameterContainer;
    //    private Date lastScannedTimeStamp;
    private boolean isSessionCorrupted = false;
    private boolean hasShownCorruptedNotification = false;
    private BlockingQueue<Integer> scanLock;
    private boolean shutdown = false;

    public SessionInstance(ExecutionSession executionSession, Project project) throws SQLException, IOException {
        this.project = project;
        this.sessionDirectory = FileSystems.getDefault().getPath(executionSession.getPath()).toFile();
        this.processorId = UUID.randomUUID().toString();

        File sessionLockFile = FileSystems.getDefault().getPath(executionSession.getPath(), "lock").toFile();
        try {
            boolean created = sessionLockFile.createNewFile();
            if (created) {
                scanEnable = true;
                sessionLockFile.deleteOnExit();
                logger.warn("scan lock file created: " + project.getName());
            } else {
                logger.warn("scan lock file wasn't created, scanning is disabled: " + project.getName());
            }
        } catch (IOException e) {
            logger.warn("exception while trying to create scan lock file, scanning is disabled " + project.getName(),
                    e);
            // lockFile failed to create, probably already exists
            // no scanning to be done from this session instance

        }

        File cacheDir = new File(this.sessionDirectory + "/cache/");
        cacheDir.mkdirs();
        this.executionSession = executionSession;

        File file = FileSystems.getDefault()
                .getPath(executionSession.getDatabasePath())
                .toFile();
        boolean dbFileExists = file.exists();
        JdbcConnectionSource connectionSource = new JdbcConnectionSource(
                executionSession.getDatabaseConnectionString());

        ChronicleMap<Long, Parameter> parameterIndex = createParameterIndex();
        parameterContainer = new ChronicleVariableContainer(parameterIndex);

        ParameterProvider parameterProvider = value -> parameterContainer.getParameterByValue(value);
        daoService = new DaoService(connectionSource, parameterProvider, ObjectMapperInstance.getInstance());


        checkProgressIndicator("Opening Zip Files", null);
        if (scanEnable) {
            logger.warn("Starting zip consumer: " + processorId);
            zipConsumer = new ZipConsumer(daoService, sessionDirectory, this);
            scanLock = new ArrayBlockingQueue<Integer>(1);
            executorPool = Executors.newFixedThreadPool(4,
                    new DefaultThreadFactory("UnloggedSessionThreadPool", true));
            executorPool.submit(this);
            executorPool.submit(zipConsumer);
            executorPool.submit(() -> {
                try {
                    publishEvent(ScanEventType.START);
                    this.sessionArchives = refreshSessionArchivesList(false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            this.sessionArchives = refreshSessionArchivesList(false);
            zipConsumer = null;
            executorPool = null;
        }

    }

    private static int getThreadIdFromFileName(String archiveFile) {
        return Integer.parseInt(archiveFile.substring(archiveFile.lastIndexOf("-") + 1, archiveFile.lastIndexOf(".")));
    }

    private static DataEventWithSessionId createDataEventFromBlock(int fileThreadId, KaitaiInsidiousEventParser.DetailedEventBlock eventBlock) {
        DataEventWithSessionId dataEvent = new DataEventWithSessionId(fileThreadId);
        dataEvent.setProbeId(eventBlock.probeId());
        dataEvent.setEventId(eventBlock.eventId());
        dataEvent.setRecordedAt(eventBlock.timestamp());
        dataEvent.setValue(eventBlock.valueId());
        dataEvent.setSerializedValue(eventBlock.serializedData());
        // use the following instead if the serialized data is in binary form (eg using fst/kryo serializer)
        //         dataEvent.setSerializedValue(Base64.getEncoder().encodeToString(eventBlock.serializedData()).getBytes(
        //                StandardCharsets.UTF_8));
        return dataEvent;
    }

    public boolean isScanEnable() {
        return scanEnable;
    }

    private void publishEvent(ScanEventType scanEventType) {
        switch (scanEventType) {

            case START:
                sessionScanEventListeners.
                        parallelStream()
                        .forEach(SessionScanEventListener::started);
                break;
            case PAUSED:
                sessionScanEventListeners.
                        parallelStream()
                        .forEach(SessionScanEventListener::paused);
                break;
            case WAITING:
                sessionScanEventListeners.
                        parallelStream()
                        .forEach(SessionScanEventListener::waiting);
                break;
            case ENDED:
                sessionScanEventListeners.
                        parallelStream()
                        .forEach(SessionScanEventListener::ended);
                break;
            case PROGRESS:
                sessionScanEventListeners.
                        parallelStream()
                        .forEach(SessionScanEventListener::started);
                break;
        }
    }

    private void publishProgressEvent(ScanProgress scanProgress) {
        sessionScanEventListeners.
                parallelStream()
                .forEach(e -> e.progress(scanProgress));
    }

    private Map<String, LogFile> getLogFileMap() {
        Map<String, LogFile> logFileMap = new HashMap<>();
        List<LogFile> logFiles = daoService.getLogFiles();
        for (LogFile logFile : logFiles) {
            logFileMap.put(logFile.getName(), logFile);
        }
        return logFileMap;
    }

    public ExecutionSession getExecutionSession() {
        return executionSession;
    }


    private List<File> refreshSessionArchivesList(boolean forceRefresh) throws IOException {
        long start = new Date().getTime();
        if (sessionDirectory.listFiles() == null) {
            return Collections.emptyList();
        }
        List<File> sessionFiles = Arrays.stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .sorted((a, b) -> -1 * a.getName().compareTo(b.getName()))
                .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                .collect(Collectors.toList());
        logger.info("found [" + sessionFiles.size() + "] session archives");

        List<File> filesToRemove = new LinkedList<>();
        int i;
        KaitaiInsidiousClassWeaveParser classWeaveInfo = null;
        if (typeInfoIndex != null && typeInfoIndex.size() > 0 && !forceRefresh) {
            return sessionFiles;
        }

        typeInfoIndex = createTypeInfoIndex();
        objectInfoIndex = createObjectInfoIndex();

        probeInfoIndex = createProbeInfoIndex();
        methodInfoIndex = createMethodInfoIndex();
        methodInfoByNameIndex = createMethodInfoByNameIndex();
        classInfoIndex = createClassInfoIndex();
        try {
            classInfoIndex.values().forEach(classInfo1 ->
                    classInfoIndexByName.put(ClassTypeUtils.getDottedClassName(classInfo1.getClassName()),
                            ChronicleUtils.ClassInfoFromClassInfo(classInfo1)));

        } catch (Throwable e) {
//            e.printStackTrace();
            if (e instanceof RuntimeException && e.getCause() instanceof InvalidClassException) {
                typeInfoIndex.close();
                objectInfoIndex.close();
                probeInfoIndex.close();
                methodInfoIndex.close();
                classInfoIndex.close();
                e.printStackTrace();
                List<String> indexFiles = Arrays.asList(
                        "index.class.dat",
                        "index.method.name.dat",
                        "index.method.dat",
                        "index.object.dat",
                        "index.probe.dat",
                        "index.type.dat"
                );
                for (String indexFile : indexFiles) {
                    File toDelete = FileSystems.getDefault()
                            .getPath(executionSession.getPath(), indexFile)
                            .toFile();
                    toDelete.delete();
                }
                typeInfoIndex = createTypeInfoIndex();
                objectInfoIndex = createObjectInfoIndex();
                probeInfoIndex = createProbeInfoIndex();
                methodInfoByNameIndex = createMethodInfoByNameIndex();
                methodInfoIndex = createMethodInfoIndex();
                classInfoIndex = createClassInfoIndex();
            }
        }


        for (i = 0; i < sessionFiles.size(); i++) {
            try {
                readClassWeaveInfoStream(sessionFiles.get(i));
                break;
            } catch (FailedToReadClassWeaveException | EOFException e) {
//                e.printStackTrace();
                logger.warn("failed to read class weave info from: " + sessionFiles.get(i) + " => " + e.getMessage());
                filesToRemove.add(sessionFiles.get(i));
            }
        }


        sessionFiles.removeAll(filesToRemove);
        Collections.sort(sessionFiles);
        long end = new Date().getTime();
        logger.info("refresh session archives list in  [" + (end - start) + "] ms");
        return sessionFiles;
    }


    private void refreshWeaveInformationStream(String fileName) throws IOException {
        logger.warn("reading class weave info from: " + fileName);
        long start = new Date().getTime();
        AtomicInteger counter = new AtomicInteger(0);

        checkProgressIndicator("Loading class mappings to scan events", null);

        ByteBufferKaitaiStream kaitaiStream = new ByteBufferKaitaiStream(fileName);
        KaitaiInsidiousClassWeaveParser classWeaveInfo = new KaitaiInsidiousClassWeaveParser(kaitaiStream);

        Map<Integer, DataInfo> probeListCache = new HashMap<>();

        List<MethodDefinition> existingMethodDefinitions = daoService.getAllMethodDefinitions();
        Map<Integer, MethodDefinition> methodDefinitionMap = existingMethodDefinitions.stream()
                .collect(Collectors.toMap(e -> (int) e.getId(), e -> e));

        while (true) {
            KaitaiInsidiousClassWeaveParser.ClassInfo classInfo = classWeaveInfo.nextClass();
            if (classInfo == null) {
                break;
            }
            int current = counter.addAndGet(1);
//            checkProgressIndicator(null, "Loading " + current + " / " + totalClassCount + " class information");

            ClassInfo existingClassInfo = classInfoIndex.get((int) classInfo.classId());
            if (existingClassInfo != null) {
                continue;
            }

            ClassInfo classInfo1 = KaitaiUtils.toClassInfo(classInfo);


            final String className = classInfo.className().value();

            List<DataInfo> dataInfoList = classInfo.probeList()
                    .stream()
                    .map(KaitaiUtils::toDataInfo)
                    .collect(Collectors.toList());

            Map<Integer, List<DataInfo>> dataInfoByMethodId = dataInfoList.stream()
                    .collect(Collectors.groupingBy(DataInfo::getMethodId, Collectors.toList()));

            List<MethodInfo> methodInfoStream = classInfo.methodList()
                    .stream()
                    .map(methodInfo -> KaitaiUtils.toMethodInfo(methodInfo, className))
                    .collect(Collectors.toList());
            Map<Integer, MethodInfo> methodInfoMap = methodInfoStream.stream()
                    .collect(Collectors.toMap(MethodInfo::getMethodId, e -> e));

            Map<String, MethodInfo> methodByNameMap = methodInfoStream.stream()
                    .collect(Collectors.toMap(e -> e.getClassName() + e.getMethodName() + e.getMethodDesc(), e -> e));
            methodInfoByNameIndex.putAll(methodByNameMap);

            boolean isEnum = false;
            boolean isPojo = true;
            if (methodInfoMap.size() == 0) {
                // ?
                isPojo = false;
            }
            int getterCount = 0;
            int setterCount = 0;
            for (MethodInfo methodInfo : methodInfoMap.values()) {
                String methodName = methodInfo.getMethodName();
                if (methodName.equals("values") &&
                        methodInfo.getMethodDesc().equals("()[L" + methodInfo.getClassName() + ";") &&
                        methodInfo.getAccess() == 9) {
                    isEnum = true;
                }
                if (methodName.startsWith("set")) {
                    setterCount++;
                }
                if (methodName.startsWith("get") || methodName.startsWith("is")) {
                    getterCount++;
                }
                if (methodName.startsWith("set")
                        || methodName.startsWith("get")
                        || methodName.startsWith("is")) {
                    List<DataInfo> methodProbes = dataInfoByMethodId.get(methodInfo.getMethodId());
                    Optional<DataInfo> hasCallEvent = methodProbes.stream()
                            .filter(e -> e.getEventType().equals(CALL))
                            .findAny();
                    if (hasCallEvent.isPresent()) {
                        isPojo = false;
                    }
                } else {
                    if (methodName.equals("toString") && !classInfo1.getFilename()
                            .equals("<generated>")) {
                        isPojo = true;
                        classInfo1.setPojo(isPojo);
                        break;
                    } else if (methodName.equals("equals")
                            || methodName.equals("canEqual")
                            || methodName.equals("hashCode")
                            || methodName.startsWith("<")) {
                    } else {
                        List<String> descriptorItemsList = MethodSignatureParser.parseMethodSignature(
                                methodInfo.getMethodDesc());
                        if (descriptorItemsList.size() > 1) {
                            isPojo = false;
                        }
                    }
                }
            }
            if (getterCount > 0 && setterCount > 0 && !classInfo1.getFilename()
                    .equals("<generated>")) {
                classInfo1.setPojo(isPojo);
            }
            classInfo1.setEnum(isEnum);
            classInfoIndex.put(classInfo1.getClassId(), classInfo1);
            classInfoIndexByName.put(ClassTypeUtils.getDottedClassName(classInfo1.getClassName()), classInfo1);

            for (MethodInfo value : methodInfoMap.values()) {
                List<DataInfo> methodProbes = dataInfoByMethodId.get(value.getMethodId());
                Set<Integer> lineNumbers = new HashSet<>();
                for (int i = 0; i < methodProbes.size(); i++) {
                    DataInfo dataInfo = methodProbes.get(i);
                    int line = dataInfo.getLine();
                    if (line != 0 && line != -1) {
                        lineNumbers.add(line);
                    }
                    if (dataInfoList.get(i).getEventType() == GET_INSTANCE_FIELD_RESULT ||
                            dataInfoList.get(i).getEventType() == GET_STATIC_FIELD) {
                        methodUsesFields.put(value.getMethodId(), true);
                    }
                }
                methodLineCount.put(value.getMethodId(), lineNumbers.size());
            }

            methodInfoIndex.putAll(methodInfoMap);

            Map<Integer, DataInfo> probesMap = dataInfoList.stream()
                    .collect(Collectors.toMap(DataInfo::getDataId, e -> e));
//            logger.debug("Add [" + probesMap.size() + "] probes for class: " + counter.get() + "/" + totalClassCount);
            probeListCache.putAll(probesMap);
            if (probeListCache.size() > 10000) {
                probeInfoIndex.putAll(probeListCache);
                probeListCache.clear();
            }
        }
        kaitaiStream.close();
        probeInfoIndex.putAll(probeListCache);
        probeListCache.clear();

        daoService.createOrUpdateClassDefinitions(classInfoIndex.values().stream()
                .map(ClassDefinition::fromClassInfo)
                .collect(Collectors.toList()));
        daoService.createOrUpdateMethodDefinitions(methodInfoIndex.values().stream()
                .map(e -> {
                            MethodDefinition existingMethodDefinition = methodDefinitionMap.get(e.getMethodId());
                            return MethodDefinition.fromMethodInfo(e,
                                    classInfoIndex.get(e.getClassId()),
                                    methodUsesFields.getOrDefault(e.getMethodId(),
                                            existingMethodDefinition != null && existingMethodDefinition.isUsesFields()),
                                    methodLineCount.getOrDefault(e.getMethodId(),
                                            existingMethodDefinition != null ? existingMethodDefinition.getLineCount() : 0));
                        }
                )
                .collect(Collectors.toList()));
        classWeaveInfo._io().close();
        long end = new Date().getTime();
        logger.warn("refresh weave information took: " + (end - start) + " ms");

    }

    private ChronicleMap<Integer, DataInfo> createProbeInfoIndex() throws IOException {

        checkProgressIndicator(null, "Loading probe info index");
        File probeIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.probe.dat")
                .toFile();
        ChronicleMapBuilder<Integer, DataInfo> probeInfoMapBuilder = ChronicleMapBuilder.of(Integer.class,
                        DataInfo.class)
                .name("probe-info-map")
                .averageValue(new DataInfo())
                .entries(5_000_000);
        return probeInfoMapBuilder.createPersistedTo(probeIndexFile);

    }

    private ChronicleMap<Long, DataEventWithSessionId> createEventIndex() throws IOException {

        checkProgressIndicator(null, "Loading event info index");
        File probeIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.event.dat")
                .toFile();
        DataEventWithSessionId averageValue = new DataEventWithSessionId();
        averageValue.setSerializedValue(new byte[10000]);
        ChronicleMapBuilder<Long, DataEventWithSessionId> probeInfoMapBuilder = ChronicleMapBuilder.of(Long.class,
                        DataEventWithSessionId.class)
                .name("event-info-map")
                .averageValue(averageValue)
                .entries(100_000);
        return probeInfoMapBuilder.createPersistedTo(probeIndexFile);

    }

    private ChronicleMap<Integer, TypeInfoDocument> createTypeInfoIndex() throws IOException {

        checkProgressIndicator(null, "Loading type info index");
        File typeIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.type.dat")
                .toFile();

        int entries = 20000;
        if (executionSession.getSessionId().equals("na")) {
            entries = 200;
        }

        ChronicleMapBuilder<Integer, TypeInfoDocument> probeInfoMapBuilder = ChronicleMapBuilder.of(Integer.class,
                        TypeInfoDocument.class)
                .name("type-info-map")
                .averageValue(new TypeInfoDocument(1, "Type-name-class", new byte[100]))
                .entries(entries);
        return probeInfoMapBuilder.createPersistedTo(typeIndexFile);

    }

    private ChronicleMap<Long, ObjectInfoDocument> createObjectInfoIndex() throws IOException {

        checkProgressIndicator(null, "Loading object info index");
        File objectIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.object.dat")
                .toFile();

        int entries = 1_000_000;
        if (executionSession.getSessionId().equals("na")) {
            entries = 500;
        }

        ChronicleMapBuilder<Long, ObjectInfoDocument> probeInfoMapBuilder = ChronicleMapBuilder.of(Long.class,
                        ObjectInfoDocument.class)
                .name("object-info-map")
                .averageValue(new ObjectInfoDocument(1, 1))
                .entries(entries);
        return probeInfoMapBuilder.createPersistedTo(objectIndexFile);

    }

    private ChronicleMap<Long, Parameter> createParameterIndex() throws IOException {

        File parameterIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.parameter.dat")
                .toFile();

        Parameter averageValue = new Parameter(1L);
        averageValue.setType("com.package.class.sub.package.ClassName");
        DataEventWithSessionId prob = new DataEventWithSessionId(1L);
        prob.setEventId(1L);
        prob.setSerializedValue(new byte[2000]);
        prob.setRecordedAt(1L);
        prob.setProbeId(1);
        prob.setThreadId(1L);
        averageValue.setProbeAndProbeInfo(prob, new DataInfo(1, 2, 3, 4, 5,
                EventType.CALL, Descriptor.Boolean, "some=attributes,here=fornothing,here=fornothing,here=fornothing"));

        averageValue.setName("name1-name1-name1");
        averageValue.setName("name2-name2");
        averageValue.setName("name4");
        List<Parameter> transformedTemplateMap = new ArrayList<>();

        Parameter param1 = new Parameter(1L);
        param1.setName("E");
        transformedTemplateMap.add(param1);

        Parameter param2 = new Parameter(1L);
        param2.setName("F");
        transformedTemplateMap.add(param2);

        Parameter param3 = new Parameter(1L);
        param3.setName("G");
        transformedTemplateMap.add(param3);

        averageValue.setTemplateMap(transformedTemplateMap);

        int entries = 1_500_000;
        if (executionSession.getSessionId().equals("na")) {
            entries = 500;
        }
        ChronicleMapBuilder<Long, Parameter> parameterInfoMapBuilder = ChronicleMapBuilder.of(Long.class,
                        Parameter.class)
                .name("parameter-info-map")
                .averageValue(averageValue)
                .entries(entries);
        return parameterInfoMapBuilder.createPersistedTo(parameterIndexFile);

    }

    private ChronicleMap<Integer, MethodInfo> createMethodInfoIndex() throws IOException {

        checkProgressIndicator(null, "Loading method info index");
        File methodIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.method.dat")
                .toFile();
        int entries = 100_000;
        if (executionSession.getSessionId().equals("na")) {
            entries = 10;
        }
        ChronicleMapBuilder<Integer, MethodInfo> probeInfoMapBuilder = ChronicleMapBuilder.of(Integer.class,
                        MethodInfo.class)
                .name("method-info-map")
                .averageValue(
                        new MethodInfo(1, 2, "class-name", "method-name", "methoddesc", 5, "source-file-name",
                                "method-hash"))
                .entries(entries);
        return probeInfoMapBuilder.createPersistedTo(methodIndexFile);

    }

    private ChronicleMap<String, MethodInfo> createMethodInfoByNameIndex() throws IOException {

        checkProgressIndicator(null, "Loading method info by name index");
        File methodIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.method.name.dat")
                .toFile();
        ChronicleMapBuilder<String, MethodInfo> probeInfoMapBuilder = ChronicleMapBuilder.of(String.class,
                        MethodInfo.class)
                .name("method-info-name-map")
                .averageKey("methodNameIsALong(Laudhfiudfhadsufhasdoufhaofuahdsofudashfuiadshfufakdsufhd")
                .averageValue(
                        new MethodInfo(1, 2, "class-name", "method-name", "methoddesc", 5, "source-file-name",
                                "method-hash"))
                .entries(100_000);
        return probeInfoMapBuilder.createPersistedTo(methodIndexFile);

    }

    private ChronicleMap<Integer, ClassInfo> createClassInfoIndex() throws IOException {

        checkProgressIndicator(null, "Loading class info index");
        File classIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.class.dat")
                .toFile();
        ChronicleMapBuilder<Integer, ClassInfo> probeInfoMapBuilder = ChronicleMapBuilder.of(Integer.class,
                        ClassInfo.class)
                .name("class-info-map")
                .averageValue(
                        new ClassInfo(1, "container-name", "file-name", "class-name", LogLevel.Normal, "hashvalue",
                                "class-loader-identifier", new String[]{"classinterface-1"}, "super-class-name",
                                "signaure"))
                .entries(10_000);
        return probeInfoMapBuilder.createPersistedTo(classIndexFile);

    }

    private ChronicleMap<String, ClassInfo> createClassInfoNameIndex() throws IOException {

        checkProgressIndicator(null, "Loading class info index");
        File classIndexFile = FileSystems.getDefault()
                .getPath(executionSession.getPath(), "index.classname.dat")
                .toFile();
        ChronicleMapBuilder<String, ClassInfo> probeInfoMapBuilder = ChronicleMapBuilder.of(String.class,
                        ClassInfo.class)
                .name("class-info-name-map")
                .averageKey("aco.asfe.asfijaisd.avsdiv$ausdhf$adfaadsfa.adfadf")
                .averageValue(
                        new ClassInfo(1, "container-name", "file-name", "class-name", LogLevel.Normal, "hashvalue",
                                "class-loader-identifier", new String[]{"classinterface-1"}, "super-class-name",
                                "signaure"))
                .entries(10_000);
        return probeInfoMapBuilder.createPersistedTo(classIndexFile);

    }

//    public Collection<TracePoint> queryTracePointsByValue(SearchQuery searchQuery) {
//        List<TracePoint> tracePointList = new LinkedList<>();
//        for (File sessionArchive : this.sessionArchives) {
//
//            NameWithBytes bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
//                    INDEX_STRING_DAT_FILE.getFileName());
//            if (bytes == null) {
//                logger.warn("archive [" + sessionArchive.getAbsolutePath() + "] is not complete or is corrupt.");
//                continue;
//            }
//            logger.info("initialize index for archive: " + sessionArchive.getAbsolutePath());
//
//
//            ArchiveIndex index;
//            try {
//                index = readArchiveIndex(bytes.getBytes(), INDEX_STRING_DAT_FILE);
//            } catch (Exception e) {
////                e.printStackTrace();
//                logger.error("failed to read type index file  [" + sessionArchive.getName() + "]", e);
//                continue;
//            }
//            Set<Long> valueIds = new HashSet<>(index.getStringIdsFromStringValues((String) searchQuery.getQuery()));
//
//
//            checkProgressIndicator(null,
//                    "Loaded " + valueIds.size() + " strings from archive " + sessionArchive.getName());
//
//            if (valueIds.size() > 0) {
//                tracePointList.addAll(getTracePointsByValueIds(sessionArchive, valueIds));
//            }
//        }
//        tracePointList.forEach(e -> e.setExecutionSession(executionSession));
//        return tracePointList;
//    }

//    private List<TracePoint> getTracePointsByValueIds(File sessionArchive, Set<Long> valueIds) {
//        logger.info("Query for valueIds [" + valueIds.toString() + "]");
//        List<TracePoint> tracePointList = new LinkedList<>();
//        NameWithBytes bytes;
//        ArchiveFilesIndex eventsIndex = null;
//        ArchiveIndex objectIndex;
//        Map<String, TypeInfo> typeInfoMap;
//        Map<Long, ObjectInfo> objectInfoMap = new HashMap<>();
//        try {
//            checkProgressIndicator(null,
//                    "Loading events index " + sessionArchive.getName() + " " + "to match against " + valueIds.size() + " values");
//
//
//            bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
//            assert bytes != null;
//            eventsIndex = readEventIndex(bytes.getBytes());
//
//
//            checkProgressIndicator(null,
//                    "Loading objects from " + sessionArchive.getName() + " to match against " + valueIds.size() + " values");
//            NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
//                    INDEX_OBJECT_DAT_FILE.getFileName());
//            assert objectIndexBytes != null;
//            objectIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
//            logger.info("object index has [" + objectIndex.Objects()
//                    .size() + "] objects");
//            objectInfoMap = objectIndex.getObjectsByObjectId(valueIds);
//
//            Set<Integer> types = objectInfoMap.values()
//                    .stream()
//                    .map(ObjectInfo::getTypeId)
//                    .collect(Collectors.toSet());
//
//            checkProgressIndicator(null, "Loading types from " + sessionArchive.getName());
//            typeInfoMap = archiveIndex.getTypesById(types);
//            logger.info("[" + typeInfoMap.size() + "] typeInfo found");
//
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//
//
//        ArchiveFilesIndex finalEventsIndex = eventsIndex;
//        HashMap<String, UploadFile> matchedFiles = new HashMap<>();
//        AtomicInteger counter = new AtomicInteger();
//        valueIds.forEach(valueId -> {
//            int currentIndex = counter.addAndGet(1);
//            assert finalEventsIndex != null;
//
//            checkProgressIndicator(null, "Matching events for item " + currentIndex + " of " + valueIds.size());
//
//            boolean archiveHasSeenValue = finalEventsIndex.hasValueId(valueId);
//            List<UploadFile> matchedFilesForString;
//            logger.info("value [" + valueId + "] found in archive: [" + archiveHasSeenValue + "]");
//
//            if (archiveHasSeenValue) {
//                checkProgressIndicator(null, "Events matched in " + sessionArchive.getName());
//                matchedFilesForString = finalEventsIndex.querySessionFilesByValueId(valueId);
//                for (UploadFile uploadFile : matchedFilesForString) {
//                    String filePath = uploadFile.getPath();
//                    int threadId = getThreadIdFromFileName(FileSystems.getDefault()
//                            .getPath(filePath)
//                            .getFileName()
//                            .toString());
//                    UploadFile uploadFileToAdd = new UploadFile(filePath, threadId, null, null);
//                    uploadFileToAdd.setValueIds(new Long[]{valueId});
//                    matchedFiles.put(filePath, uploadFile);
//                }
//            }
//        });
//        Map<Long, ObjectInfo> finalObjectInfoMap = objectInfoMap;
//        logger.info("matched [" + matchedFiles.size() + "] files");
//
//        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
//            ProgressIndicatorProvider.getGlobalProgressIndicator()
//                    .setText("Found " + matchedFiles.size() + " archives with matching values");
//            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
//                    .isCanceled()) {
//                throw new ProcessCanceledException();
//            }
//        }
//
//
//        for (UploadFile matchedFile : matchedFiles.values()) {
//            try {
//
//                checkProgressIndicator(null, "Loading events data from " + matchedFile.getPath());
//                String fileName = FileSystems.getDefault()
//                        .getPath(matchedFile.getPath())
//                        .getFileName()
//                        .toString();
//
//                NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, fileName);
//                if (fileBytes == null) {
//                    List<String> fileList = listArchiveFiles(sessionArchive);
//                    logger.error(
//                            String.format("matched file not found " + "inside the session archive [%s] -> [%s] -> [%s]",
//                                    fileName, sessionArchive, fileList));
//                    throw new RuntimeException("matched file not found inside the session archive -> " + fileList);
//                }
//                long timestamp = Long.parseLong(fileBytes.getName()
//                        .split("@")[0]);
//                int threadId = Integer.parseInt(fileBytes.getName()
//                        .split("-")[2].split("\\.")[0]);
//
//
//                List<DataEventWithSessionId> dataEvents = getDataEventsFromPathByValueIds(fileBytes.getBytes(),
//                        matchedFile.getValueIds());
//                checkProgressIndicator(null,
//                        "Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
//                List<TracePoint> matchedTracePoints = dataEvents.stream()
//                        .map(e1 -> {
//
//                            try {
//                                List<DataInfo> dataInfoList = getProbeInfo(new HashSet<>(
//                                        Collections.singletonList(e1.getProbeId())));
//                                logger.debug(
//                                        "data info list by data id [" + e1.getProbeId() + "] => [" + dataInfoList + "]");
//
//                                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
//                                    if (ProgressIndicatorProvider.getGlobalProgressIndicator()
//                                            .isCanceled()) {
//                                        return null;
//                                    }
//                                }
//
//
//                                DataInfo dataInfo = dataInfoList.get(0);
//                                int classId = dataInfo.getClassId();
//                                KaitaiInsidiousClassWeaveParser.ClassInfo classInfo = getClassInfo(classId);
//
//                                ObjectInfo objectInfo = finalObjectInfoMap.get(e1.getValue());
//                                TypeInfo typeInfo = getTypeInfo(objectInfo.getTypeId());
//
//                                TracePoint tracePoint = new TracePoint(classId, dataInfo.getLine(),
//                                        dataInfo.getDataId(),
//                                        threadId, e1.getValue(), classInfo.fileName()
//                                        .value(), classInfo.className()
//                                        .value(),
//                                        typeInfo.getTypeNameFromClass(), timestamp, e1.getEventId());
//                                tracePoint.setExecutionSession(executionSession);
//                                return tracePoint;
//                            } catch (ClassInfoNotFoundException | Exception ex) {
//                                ex.printStackTrace();
//                                logger.error("failed to get data probe information", ex);
//                            }
//                            return null;
//
//
//                        })
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toList());
//                tracePointList.addAll(matchedTracePoints);
//
//                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
//                    if (tracePointList.size() > 0) {
//                        ProgressIndicatorProvider.getGlobalProgressIndicator()
//                                .setText(tracePointList.size() + " matched...");
//                    }
//                    if (ProgressIndicatorProvider.getGlobalProgressIndicator()
//                            .isCanceled()) {
//                        return tracePointList;
//                    }
//                }
//
//
//            } catch (IOException ex) {
////                ex.printStackTrace();
//            } catch (Exception e) {
////                e.printStackTrace();
//                throw e;
//            }
//
//        }
//
//        return tracePointList;
//    }

    private List<DataInfo> getProbeInfo(Set<Long> dataId) throws IOException {

        throw new RuntimeException("who is using this");

//        return classWeaveInfo.classInfo().stream()
//                .map(KaitaiInsidiousClassWeaveParser.ClassInfo::probeList)
//                .flatMap(Collection::stream)
//                .filter(e -> dataId.contains(e.dataId()))
//                .map(KaitaiUtils::toDataInfo).collect(Collectors.toList());

    }

//    private KaitaiInsidiousClassWeaveParser readClassWeaveInfo(File sessionFile) throws IOException {
//
//        KaitaiInsidiousClassWeaveParser classWeaveInfo1;
//        logger.warn("creating class weave info from scratch from file [1012]: " + sessionFile.getName());
//        NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionFile, WEAVE_DAT_FILE.getFileName());
//        if (fileBytes == null) {
//            logger.debug("failed to read class weave info from " + "sessionFile [" + sessionFile.getName() + "]");
//            return null;
//        }
//        ByteBufferKaitaiStream io = new ByteBufferKaitaiStream(fileBytes.getBytes());
//        classWeaveInfo1 = new KaitaiInsidiousClassWeaveParser(io);
//        io.close();
//        return classWeaveInfo1;
//    }

    private void readClassWeaveInfoStream(File sessionFile) throws IOException, FailedToReadClassWeaveException {

        try {
            logger.warn("creating class weave info from scratch from file [1026]: " + sessionFile.getName());
            String classWeaveFileStream = getFileStreamFromArchive(sessionFile, WEAVE_DAT_FILE.getFileName());
            refreshWeaveInformationStream(classWeaveFileStream);

            NameWithBytes typeIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionFile,
                    INDEX_TYPE_DAT_FILE.getFileName());
            if (typeIndexBytes == null) {
                throw new FailedToReadClassWeaveException("index.type.dat not found in: " + sessionFile.getName());
            }

            archiveIndex = readArchiveIndex(typeIndexBytes.getBytes(), INDEX_TYPE_DAT_FILE);
            ConcurrentIndexedCollection<TypeInfoDocument> typeIndex = archiveIndex.getTypeInfoIndex();
            typeIndex.parallelStream().forEach(e -> typeInfoIndex.put(e.getTypeId(), e));
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("failed to read archive for types index: " + e.getMessage()
                    + " from file [" + sessionFile + "]");
            throw new FailedToReadClassWeaveException("Failed to read " + INDEX_TYPE_DAT_FILE + " in "
                    + sessionFile.getPath() + " -> " + e.getMessage(), e);
        }
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

        Set<Long> ids = new HashSet<>(Arrays.asList(valueIds));

        KaitaiInsidiousEventParser dataEvents = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(bytes));

        return dataEvents.event()
                .entries()
                .stream()
                .filter(e -> ids.contains(e.block()
                        .valueId()))
                .map(e -> {
                    long valueId = e.block()
                            .valueId();
                    int probeId = e.block()
                            .probeId();
                    long eventId = e.block()
                            .eventId();
                    long timestamp = e.block()
                            .timestamp();

                    DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                    dataEvent.setProbeId(probeId);
                    dataEvent.setValue(valueId);
                    dataEvent.setEventId(eventId);
                    dataEvent.setRecordedAt(timestamp);
                    return dataEvent;
                })
                .collect(Collectors.toList());
    }

    public TypeInfo getTypeInfo(Integer typeId) {

        Map<String, TypeInfo> result = archiveIndex.getTypesById(new HashSet<>(Collections.singletonList(typeId)));
        if (result.size() > 0) {
            return result.get(String.valueOf(typeId));
        }

        return new TypeInfo(typeId, "unidentified type", "", 0, 0, "", new int[0]);
    }

    public TypeInfo getTypeInfo(String name) {

        if (archiveIndex != null) {
            TypeInfo result = archiveIndex.getTypesByName(name);
            if (result != null) {
                return result;
            }
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

        try (FileInputStream fileInputStream = new FileInputStream(sessionFile)) {
            try (ZipInputStream indexArchive = new ZipInputStream(fileInputStream)) {
                ZipEntry entry;
                while ((entry = indexArchive.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    indexArchive.closeEntry();
                    files.add(entryName);
                }
            }
        }

        files = files.stream()
                .filter(e -> e.contains("@"))
                .map(e -> e.split("@")[1])
                .collect(Collectors.toList());
        Collections.sort(files);
        zipFileListMap.put(sessionFile.getName(), files);
        return files;

    }

//    private ArchiveFilesIndex readEventIndex(byte[] bytes) throws IOException {
//        KaitaiInsidiousIndexParser archiveIndex = new KaitaiInsidiousIndexParser(new ByteBufferKaitaiStream(bytes));
//
//        return new ArchiveFilesIndex(archiveIndex);
//    }

    private ArchiveIndex readArchiveIndex(byte[] bytes, DatFileType indexFilterType) throws IOException {

        ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex = null;
        String cacheKey = bytesHex(bytes, indexFilterType.getFileName());


        Path path = FileSystems.getDefault()
                .getPath(this.sessionDirectory.getAbsolutePath(), cacheKey, indexFilterType.getFileName());
        Path parentPath = path.getParent();
        parentPath.toFile().mkdirs();

        if (!path.toFile()
                .exists()) {
            Files.write(path, bytes);
        }

        if (indexFilterType.equals(INDEX_TYPE_DAT_FILE)) {
            DiskPersistence<TypeInfoDocument, Integer> typeInfoDocumentStringDiskPersistence = DiskPersistence.onPrimaryKeyInFile(
                    TypeInfoDocument.TYPE_ID, path.toFile());
            typeInfoIndex = new ConcurrentIndexedCollection<>(typeInfoDocumentStringDiskPersistence);
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_NAME));
            typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_ID));
        }


        ConcurrentIndexedCollection<StringInfoDocument> sII = null;
        if (indexFilterType.equals(INDEX_STRING_DAT_FILE)) {
            DiskPersistence<StringInfoDocument, Long> stringInfoDocumentStringDiskPersistence = DiskPersistence.onPrimaryKeyInFile(
                    StringInfoDocument.STRING_ID, path.toFile());
            sII = new ConcurrentIndexedCollection<>(stringInfoDocumentStringDiskPersistence);

            sII.addIndex(InvertedRadixTreeIndex.onAttribute(StringInfoDocument.STRING_VALUE));
        }

        ConcurrentIndexedCollection<ObjectInfoDocument> oII = null;
        if (indexFilterType.equals(INDEX_OBJECT_DAT_FILE)) {
            DiskPersistence<ObjectInfoDocument, Long> objectInfoDocumentIntegerDiskPersistence = DiskPersistence.onPrimaryKeyInFile(
                    ObjectInfoDocument.OBJECT_ID, path.toFile());

            oII = new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);
            oII.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_TYPE_ID));
            oII.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_ID));
        }

        if (indexFilterType.equals(WEAVE_DAT_FILE)) {
//            KaitaiInsidiousClassWeaveParser classWeave = new KaitaiInsidiousClassWeaveParser(
//                    new ByteBufferKaitaiStream(bytes));
            throw new RuntimeException("this is not to be used");
//            for (KaitaiInsidiousClassWeaveParser.ClassInfo classInfo : classWeave.classInfo()) {
//                classInfoMap.put(classInfo.classId(), KaitaiUtils.toClassInfo(classInfo));
//            }
        }

        return new ArchiveIndex(typeInfoIndex, sII, oII, null);
    }


    private String bytesHex(byte[] bytes, String indexFilterType) {
        String md5Hex = DigestUtils.md5Hex(bytes);
        return md5Hex + "-" + indexFilterType;
    }

    public NameWithBytes createFileOnDiskFromSessionArchiveFile(File sessionFile, String pathName) {
        logger.debug(String.format("get file[%s] from archive[%s]", pathName, sessionFile.getName()));
        String cacheKey = sessionFile.getName() + pathName;
        String cacheFileLocation = this.sessionDirectory + "/cache/" + cacheKey + ".dat";
        try {

            if (cacheEntries.containsKey(cacheKey)) {
                String name = cacheEntries.get(cacheKey);
                File cacheFile = new File(cacheFileLocation);
                try (FileInputStream inputStream = new FileInputStream(cacheFile)) {
                    byte[] bytes = IOUtils.toByteArray(inputStream);
                    return new NameWithBytes(name, bytes);
                }
            }

            try (ZipFile zipFile = new ZipFile(sessionFile)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().contains(pathName)) {
                        try (InputStream entryStream = zipFile.getInputStream(entry)) {
                            File cacheFile = new File(cacheFileLocation);

                            byte[] fileBytes = IOUtils.toByteArray(entryStream);

                            FileUtils.writeByteArrayToFile(cacheFile, fileBytes);
                            cacheEntries.put(cacheKey, entry.getName());

                            NameWithBytes nameWithBytes = new NameWithBytes(entry.getName(), fileBytes);
                            logger.info(
                                    pathName + " file from " + sessionFile.getName() + " is " + nameWithBytes.getBytes().length + " bytes");
                            return nameWithBytes;
                        }
                    }
                }
            }


//            try (FileInputStream sessionFileInputStream = new FileInputStream(sessionFile)) {
//                try (ZipInputStream indexArchive = new ZipInputStream(sessionFileInputStream)) {
//                    ZipEntry entry;
//                    while ((entry = indexArchive.getNextEntry()) != null) {
//                        String entryName = entry.getName();
////                        logger.info(String.format("file entry in archive [%s] -> [%s]", sessionFile.getName(),
////                                entryName));
//                        if (entryName.contains(pathName)) {
//                            byte[] fileBytes = IOUtils.toByteArray(indexArchive);
//
//                            File cacheFile = new File(cacheFileLocation);
//                            FileUtils.writeByteArrayToFile(cacheFile, fileBytes);
//                            indexArchive.closeEntry();
//                            indexArchive.close();
//
//                            cacheEntries.put(cacheKey, entryName);
//
//                            NameWithBytes nameWithBytes = new NameWithBytes(entryName, fileBytes);
//                            logger.info(
//                                    pathName + " file from " + sessionFile.getName() + " is " + nameWithBytes.getBytes().length + " bytes");
//                            return nameWithBytes;
//                        }
//                    }
//                } catch (Exception e) {
////                    e.printStackTrace();
//                    logger.error("Failed to open zip archive: " + e.getMessage(), e);
//                }
//            }

        } catch (Exception e) {
//            e.printStackTrace();
            logger.warn(
                    "failed to create file [" + pathName + "] on disk from" + " archive[" + sessionFile.getName() + "]");
            return null;
        }
        return null;
    }

    /**
     * @param sessionFile Archive file from which file contents need to be extracted
     * @param pathName    name of the file which needs to be extracted
     * @return absolute file path to cached file the disk which can be read
     */
    private String createFileOnDiskFromSessionArchiveFileV2(File sessionFile, String pathName) {
        logger.debug(String.format("get file[%s] from archive[%s]", pathName, sessionFile.getName()));
        String cacheKey = sessionFile.getName() + pathName;
        String cacheFileLocation = this.sessionDirectory + "/cache/" + cacheKey + ".dat";
        try {

            if (cacheEntries.containsKey(cacheKey)) {
//                String name = cacheEntries.get(cacheKey);
//                File cacheFile = new File(cacheFileLocation);
//                try (FileInputStream inputStream = new FileInputStream(cacheFile)) {
                return cacheFileLocation;
//                }
            }

            try (ZipFile zipFile = new ZipFile(sessionFile)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.getName().contains(pathName)) {
                        try (InputStream entryStream = zipFile.getInputStream(entry)) {
                            File cacheFile = new File(cacheFileLocation);
                            try (FileOutputStream outputStream = new FileOutputStream(cacheFile)) {
                                StreamUtil.copy(entryStream, outputStream);
                                outputStream.close();
                                entryStream.close();
                                cacheEntries.put(cacheKey, entry.getName());
                                return cacheFileLocation;
                            }
                        }
                    }
                }
            }

//            try (FileInputStream sessionFileInputStream = new FileInputStream(sessionFile)) {
//                try (ZipInputStream indexArchive = new ZipInputStream(sessionFileInputStream)) {
//                    ZipEntry entry;
//                    while ((entry = indexArchive.getNextEntry()) != null) {
//                        String entryName = entry.getName();
////                        logger.info(String.format("file entry in archive [%s] -> [%s]", sessionFile.getName(),
////                                entryName));
//                        if (entryName.contains(pathName)) {
//                            File cacheFile = new File(cacheFileLocation);
//                            FileOutputStream outputStream = new FileOutputStream(cacheFile);
//                            StreamUtil.copy(indexArchive, outputStream);
//                            outputStream.close();
//                            indexArchive.closeEntry();
//                            indexArchive.close();
//                            cacheEntries.put(cacheKey, entryName);
//                            return cacheFileLocation;
//                        }
//                    }
//                }
//            }

        } catch (Exception e) {
//            e.printStackTrace();
            logger.warn(
                    "failed to create file [" + pathName + "] on disk from" + " archive[" + sessionFile.getName() + "]");
            return null;
        }
        return null;
    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
//            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
//                try {
//                    // we want a close call here, otherwise the chronicle map might remain locked, and we will not be
//                    // able ot read it on next refresh/load
//                    close();
//                } catch (Exception e) {
//                    // now this is just very weird
//                    throw new RuntimeException(e);
//                }
//                throw new ProcessCanceledException();
//            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText(text1);
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

        return Collections.emptyList();

    }

//    public void queryTracePointsByEventType(SearchQuery searchQuery, ClientCallBack<TracePoint> tracePointsCallback) {
//
//
//        List<DataInfo> probeIds = null;
//        for (File sessionArchive : sessionArchives) {
//            logger.info("check archive [" + sessionArchive.getName() + "] for " + "probes");
//            if (probeIds == null || probeIds.size() == 0) {
//                Collection<EventType> eventTypes = (Collection<EventType>) searchQuery.getQuery();
//                probeIds = queryProbeFromFileByEventType(sessionArchive, eventTypes);
//            }
//
//            checkProgressIndicator(null,
//                    "Loaded " + probeIds.size() + " objects from archive " + sessionArchive.getName());
//
//
//            if (probeIds.size() > 0) {
//                getTracePointsByProbeIds(sessionArchive,
//                        probeIds.stream()
//                                .map(DataInfo::getDataId)
//                                .collect(Collectors.toSet()), tracePointsCallback);
//            }
//        }
//        tracePointsCallback.completed();
//
//    }

//    private void getTracePointsByProbeIds(File sessionArchive, Set<Integer> probeIds, ClientCallBack<TracePoint> tracePointsCallback) {
//        logger.info("Query for probeIds [" + probeIds.toString() + "]");
//        List<TracePoint> tracePointList = new LinkedList<>();
//        NameWithBytes bytes;
//        Map<String, TypeInfo> typeInfoMap = new HashMap<>();
//        Map<String, ObjectInfo> objectInfoMap = new HashMap<>();
//
//
//        ArchiveFilesIndex eventsIndex = null;
//        ArchiveIndex objectIndex = null;
//        try {
//            checkProgressIndicator(null,
//                    "Loading events index " + sessionArchive.getName() + " to match against " + probeIds.size() + " values");
//
//
//            bytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, INDEX_EVENTS_DAT_FILE.getFileName());
//            if (bytes == null) {
//                return;
//            }
//            eventsIndex = readEventIndex(bytes.getBytes());
//
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//
//
//        ArchiveFilesIndex finalEventsIndex = eventsIndex;
//        HashMap<String, UploadFile> matchedFiles = new HashMap<>();
//        AtomicInteger counter = new AtomicInteger();
//        probeIds.forEach(probeId -> {
//            int currentIndex = counter.addAndGet(1);
//            assert finalEventsIndex != null;
//
//            checkProgressIndicator(null, "Matching events for probe " + currentIndex + " of " + probeIds.size());
//
//            boolean archiveHasSeenValue = finalEventsIndex.hasProbeId(probeId);
//            List<UploadFile> matchedFilesForString = new LinkedList<>();
//            logger.info("probeId [" + probeId + "] found in archive: [" + archiveHasSeenValue + "]");
//
//            if (archiveHasSeenValue) {
//                checkProgressIndicator(null, "Events matched in " + sessionArchive.getName());
//                matchedFilesForString = finalEventsIndex.querySessionFilesByProbeId(probeId);
//                for (UploadFile uploadFile : matchedFilesForString) {
//                    String filePath = uploadFile.getPath();
//                    int threadId = getThreadIdFromFileName(FileSystems.getDefault()
//                            .getPath(filePath)
//                            .getFileName()
//                            .toString());
//                    UploadFile uploadFileToAdd = new UploadFile(filePath, threadId, null, null);
//                    uploadFileToAdd.setProbeIds(new Integer[]{probeId});
//
//                    if (matchedFiles.containsKey(filePath)) {
//                        Integer[] existingProbes = matchedFiles.get(filePath)
//                                .getProbeIds();
//                        ArrayList<Integer> arrayList = new ArrayList<>(Arrays.asList(existingProbes));
//                        if (!arrayList.contains(probeId)) {
//                            arrayList.add(probeId);
//                            Integer[] matchedProbeIds = new Integer[arrayList.size()];
//                            for (int i = 0; i < arrayList.size(); i++) {
//                                Integer integer = arrayList.get(i);
//                                matchedProbeIds[i] = integer;
//                            }
//
//                            matchedFiles.get(filePath)
//                                    .setProbeIds(matchedProbeIds);
//                        }
//
//                    } else {
//                        matchedFiles.put(filePath, uploadFile);
//                    }
//
//                }
//            }
//        });
//        logger.info("matched [" + matchedFiles.size() + "] files");
//
//        checkProgressIndicator("Found " + matchedFiles.size() + " archives with matching values", null);
//
//
//        for (UploadFile matchedFile : matchedFiles.values()) {
//            try {
//
//                checkProgressIndicator(null, "Loading events data from " + matchedFile.getPath());
//                String fileName = FileSystems.getDefault()
//                        .getPath(matchedFile.getPath())
//                        .getFileName()
//                        .toString();
//
//                NameWithBytes fileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, fileName);
//                if (fileBytes == null) {
//                    List<String> fileList = listArchiveFiles(sessionArchive);
//                    logger.error(
//                            String.format("matched file not found " + "inside the session archive [%s] -> [%s] -> [%s]",
//                                    fileName, sessionArchive, fileList));
//                    throw new RuntimeException("matched file not found inside the session archive -> " + fileList);
//                }
//                long timestamp = Long.parseLong(fileBytes.getName()
//                        .split("@")[0]);
//                int threadId = Integer.parseInt(fileBytes.getName()
//                        .split("-")[2].split("\\.")[0]);
//
//
//                List<DataEventWithSessionId> dataEvents = getDataEventsFromPathByProbeIds(fileBytes.getBytes(),
//                        matchedFile.getProbeIds());
//
//                checkProgressIndicator(null,
//                        "Filtering " + dataEvents.size() + " events from file " + matchedFile.getPath());
//                List<TracePoint> matchedTracePoints = dataEvents.stream()
//                        .map(e1 -> {
//
//                            try {
//                                List<DataInfo> dataInfoList = getProbeInfo(new HashSet<>(
//                                        Collections.singletonList(e1.getProbeId())));
//                                logger.debug(
//                                        "data info list by data id [" + e1.getProbeId() + "] => [" + dataInfoList + "]");
//
//                                if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
//                                    if (ProgressIndicatorProvider.getGlobalProgressIndicator()
//                                            .isCanceled()) {
//                                        return null;
//                                    }
//                                }
//
//
//                                DataInfo dataInfo = dataInfoList.get(0);
//                                int classId = dataInfo.getClassId();
//                                KaitaiInsidiousClassWeaveParser.ClassInfo classInfo = getClassInfo(classId);
//
//                                ObjectInfo objectInfo = objectInfoMap.get(String.valueOf(e1.getValue()));
//                                String typeName = "<na>";
//                                if (objectInfo != null) {
//                                    TypeInfo typeInfo = getTypeInfo(objectInfo.getTypeId());
//                                    typeName = typeInfo.getTypeNameFromClass();
//                                }
//
//                                TracePoint tracePoint = new TracePoint(classId, dataInfo.getLine(),
//                                        dataInfo.getDataId(),
//                                        threadId, e1.getValue(), classInfo.fileName()
//                                        .value(), classInfo.className()
//                                        .value(),
//                                        typeName, timestamp, e1.getEventId());
//
//                                tracePoint.setExecutionSession(executionSession);
//                                return tracePoint;
//                            } catch (ClassInfoNotFoundException | Exception ex) {
//                                ex.printStackTrace();
//                                logger.error("failed to get data probe information", ex);
//                            }
//                            return null;
//
//
//                        })
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toList());
//                tracePointList.addAll(matchedTracePoints);
//
//                checkProgressIndicator(null, tracePointList.size() + " matched...");
//
//
//            } catch (IOException ex) {
//                logger.error("exception while creating trace points in file[" + matchedFile.path + "]", ex);
//            }
//
//        }
//        if (tracePointList.size() != 0) {
//            tracePointList.forEach(e -> e.setExecutionSession(executionSession));
//            tracePointsCallback.success(tracePointList);
//        }
//    }

    private List<DataEventWithSessionId> getDataEventsFromPathByProbeIds(byte[] bytes, Integer[] probeIds) {

        Set<Integer> ids = new HashSet<>(Arrays.asList(probeIds));
        KaitaiInsidiousEventParser dataEvents = new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(bytes));

        return dataEvents.event()
                .entries()
                .stream()
                .filter(e -> ids.contains(e.block()
                        .probeId()))
                .map(e -> {
                    long valueId = e.block()
                            .valueId();
                    int probeId = e.block()
                            .probeId();
                    long eventId = e.block()
                            .eventId();
                    long timestamp = e.block()
                            .timestamp();

                    DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                    dataEvent.setProbeId(probeId);
                    dataEvent.setValue(valueId);
                    dataEvent.setEventId(eventId);
                    dataEvent.setRecordedAt(timestamp);
                    return dataEvent;


                })
                .collect(Collectors.toList());
    }

    public ReplayData fetchDataEvents(FilteredDataEventsRequest filteredDataEventsRequest) {
        File archiveToServe = null;
        for (File sessionArchive : this.sessionArchives) {
            long timestamp = Long.parseLong(sessionArchive.getName()
                    .split("-")[2].split("\\.")[0]);
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


        checkProgressIndicator(null, "Mapping " + eventsContainer.event()
                .entries()
                .size() + " events ");

        List<DataEventWithSessionId> dataEventList = eventsContainer.event()
                .entries()
                .stream()
                .map(e -> {
                    long valueId = e.block()
                            .valueId();
                    int probeId = e.block()
                            .probeId();
                    long eventId = e.block()
                            .eventId();
                    long timestamp = e.block()
                            .timestamp();

                    DataEventWithSessionId dataEvent = new DataEventWithSessionId();
                    dataEvent.setProbeId(probeId);
                    dataEvent.setValue(valueId);
                    dataEvent.setEventId(eventId);
                    dataEvent.setRecordedAt(timestamp);
                    return dataEvent;
                })
                .collect(Collectors.toList());

        Collections.reverse(dataEventList);


        Map<Long, ClassInfo> classInfo = new HashMap<>();
        Map<Long, DataInfo> dataInfo = new HashMap<>();

        checkProgressIndicator(null, "Loading class mappings for data events");

        Set<Long> valueIds = dataEventList.stream()
                .map(DataEventWithSessionId::getValue)
                .collect(Collectors.toSet());


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
//                e.printStackTrace();
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
//                e.printStackTrace();
                logger.error("failed to read string index from session bytes: " + e.getMessage(), e);
                continue;
            }
            Map<Long, StringInfo> sessionStringInfo = stringIndex.getStringsByIdWithLongKeys(
                    valueIds.stream()
                            .filter(e -> e > 10)
                            .collect(Collectors.toSet()));
            stringInfo.putAll(sessionStringInfo);


            Set<Integer> typeIds = objectInfo.values()
                    .stream()
                    .map(ObjectInfo::getTypeId)
                    .collect(Collectors.toSet());


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

//    private Set<ObjectInfoDocument> queryObjectsByTypeFromSessionArchive(SearchQuery searchQuery, File sessionArchive) {
//
//        checkProgressIndicator(null, "querying type names from: " + sessionArchive.getName());
//
//
//        Set<Integer> typeIds = archiveIndex.queryTypeIdsByName(searchQuery);
//
//
//        checkProgressIndicator(null, "Loading matched objects");
//
//
//        NameWithBytes objectIndexFileBytes;
//        objectIndexFileBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
//                INDEX_OBJECT_DAT_FILE.getFileName());
//        if (objectIndexFileBytes == null) {
//            logger.warn("object index file bytes are empty, skipping");
//            return new HashSet<>();
//        }
//
//
//        ArchiveIndex objectIndex;
//        try {
//            objectIndex = readArchiveIndex(objectIndexFileBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
//        } catch (IOException e) {
////            e.printStackTrace();
//            logger.warn("failed to read object index file: " + e.getMessage());
//            return new HashSet<>();
//
//        }
//        Query<ObjectInfoDocument> query = in(ObjectInfoDocument.OBJECT_TYPE_ID, typeIds);
//        ResultSet<ObjectInfoDocument> typeInfoSearchResult = objectIndex.Objects()
//                .retrieve(query);
//        Set<ObjectInfoDocument> objects = typeInfoSearchResult.stream()
//                .collect(Collectors.toSet());
//        typeInfoSearchResult.close();
//
//        checkProgressIndicator(null,
//                sessionArchive.getName() + " matched " + objects.size() + " objects of total " + objectIndex.Objects()
//                        .size());
//
//
//        return objects;
//    }

    public ClassWeaveInfo getClassWeaveInfo() {


        List<ClassInfo> classInfoList = new LinkedList<>();
        List<MethodInfo> methodInfoList = new LinkedList<>();
        List<DataInfo> dataInfoList = new LinkedList<>();

        return new ClassWeaveInfo(classInfoList, methodInfoList, dataInfoList);
    }


//    private ArchiveFilesIndex getEventIndex(File sessionArchive) {
//        NameWithBytes eventIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
//                INDEX_EVENTS_DAT_FILE.getFileName());
//        if (eventIndexBytes == null) {
//            logger.warn("failed to read events index from : " + sessionArchive.getName());
//            return null;
//        }
//        ArchiveFilesIndex eventsIndex;
//        try {
//            eventsIndex = readEventIndex(eventIndexBytes.getBytes());
//        } catch (IOException e) {
////            e.printStackTrace();
//            logger.warn("failed to read events index from : " + sessionArchive.getName());
//            return null;
//        }
//        return eventsIndex;
//    }

    private List<KaitaiInsidiousEventParser.Block> getEventsFromFile(File sessionArchive, String archiveFile) throws IOException {
        long start = new Date().getTime();
        logger.warn("Read events from file [1799]: " + archiveFile);
        String eventFile = createFileOnDiskFromSessionArchiveFileV2(sessionArchive, archiveFile);
        RandomAccessFileKaitaiStream io = new RandomAccessFileKaitaiStream(eventFile);
        KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(io);
        ArrayList<KaitaiInsidiousEventParser.Block> events = eventsContainer.event()
                .entries();
        io.close();
        long end = new Date().getTime();
        logger.warn("Read events took: " + (end - start) + " ms");
        return events;
    }

//    private List<KaitaiInsidiousEventParser.Block> getEventsFromFileOld(File sessionArchive, String archiveFile) throws IOException {
//        long start = new Date().getTime();
//        logger.warn("Read events from file [1813]: " + archiveFile);
//        NameWithBytes nameWithBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, archiveFile);
//        assert nameWithBytes != null;
//        KaitaiInsidiousEventParser eventsContainer =
//                new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(nameWithBytes.getBytes()));
//        ArrayList<KaitaiInsidiousEventParser.Block> events = eventsContainer.event()
//                .entries();
//        long end = new Date().getTime();
//        logger.warn("Read events took: " + (end - start) + " ms");
//        return events;
//    }

    private String getFileStreamFromArchive(File sessionArchive, String archiveFile) throws FailedToReadClassWeaveException {
        long start = new Date().getTime();
        String eventFile = createFileOnDiskFromSessionArchiveFileV2(sessionArchive, archiveFile);
        if (eventFile == null) {
            throw new FailedToReadClassWeaveException(archiveFile + " not found in " + sessionArchive.getName());
        }

        long end = new Date().getTime();
        logger.warn("Read events from file [1826]: " + archiveFile + " took " + (end - start) + " ms");
        return eventFile;
    }

    private List<KaitaiInsidiousEventParser.Block> getEventsFromFileV2(File sessionArchive, String archiveFile) throws IOException {
        long start = new Date().getTime();
        logger.warn("Read events from file [1836]: " + archiveFile);
//        ZipInputStream stream = readEventFile(sessionArchive, archiveFile);
        NameWithBytes bytesWithName = createFileOnDiskFromSessionArchiveFile(sessionArchive, archiveFile);
        assert bytesWithName != null;
        ByteBufferKaitaiStream kaitaiStream = new ByteBufferKaitaiStream(bytesWithName.getBytes());

        KaitaiInsidiousEventParser eventsContainer = new KaitaiInsidiousEventParser(kaitaiStream);


        ArrayList<KaitaiInsidiousEventParser.Block> events = eventsContainer.event()
                .entries();
        kaitaiStream.close();
        long end = new Date().getTime();
        logger.warn("Read events took: " + ((end - start) / 1000));
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

        final AtomicLong previousEventAt = new AtomicLong(-1);

        Set<Long> remainingObjectIds = new HashSet<>();
        Set<Long> remainingStringIds = new HashSet<>();

        Map<String, SELogFileMetadata> fileEventIdPairs = new HashMap<>();

        for (File sessionArchive : sessionArchivesLocal) {
            logger.warn("open archive [" + sessionArchive.getName() + "]");


            Map<String, UploadFile> matchedFiles = new HashMap<>();
//            if (objectId != -1) {
//                ArchiveFilesIndex eventsIndex = getEventIndex(sessionArchive);
//                if (eventsIndex == null) continue;
//                if (!eventsIndex.hasValueId(objectId)) {
//                    continue;
//                }
//
//                List<UploadFile> matchedFilesForString = eventsIndex.querySessionFilesByValueId(objectId);
//                for (UploadFile uploadFile : matchedFilesForString) {
//                    String filePath = uploadFile.getPath();
//                    logger.info("File matched for object id [" + objectId + "] -> " + uploadFile + " -> " + filePath);
//                    int threadId = getThreadIdFromFileName(FileSystems.getDefault()
//                            .getPath(filePath)
//                            .getFileName()
//                            .toString());
//                    UploadFile uploadFileToAdd = new UploadFile(filePath, threadId, null, null);
//                    uploadFileToAdd.setValueIds(new Long[]{objectId});
//                    matchedFiles.put(filePath, uploadFile);
//                }
//                if (matchedFiles.size() == 0) {
//                    continue;
//                }
//            }


            try {
                List<String> archiveFiles;

                if (objectId != -1) {
//                    logger.info("Files were matched: " + matchedFiles);

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
                    if (archiveIndex == null) {
                        try {
                            readClassWeaveInfoStream(sessionArchive);
                        } catch (FailedToReadClassWeaveException e) {
                            throw new RuntimeException(e);
                        }
                    }

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

                    if (metadata != null) {
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
                    List<DataEventWithSessionId> dataEventGroupedList = eventsSublist.stream()
                            .filter(e -> {
                                long currentFirstEventAt = previousEventAt.get();

                                long currentEventId;
                                long valueId;

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
                            .map(e -> createDataEventFromBlock(finalMetadata.getThreadId(), e.block()))
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
//                e.printStackTrace();
                logger.warn("failed to read archive [" + sessionArchive.getName() + "]");
                continue;
            }
            if (dataEventList.size() == 0) {
                continue;
            }


            Set<Long> valueIds = dataEventList.stream()
                    .map(DataEventWithSessionId::getValue)
                    .collect(Collectors.toSet());


            NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                    INDEX_STRING_DAT_FILE.getFileName());
            assert stringsIndexBytes != null;


            ArchiveIndex stringIndex;
            try {
                stringIndex = readArchiveIndex(stringsIndexBytes.getBytes(), INDEX_STRING_DAT_FILE);
            } catch (IOException e) {
//                e.printStackTrace();
                logger.error("failed to read string index from session bytes: " + e.getMessage(), e);
                continue;
            }
            Set<Long> potentialStringIds = valueIds.stream()
                    .filter(e -> e > 10)
                    .collect(Collectors.toSet());
            Map<Long, StringInfo> sessionStringInfo = stringIndex.getStringsByIdWithLongKeys(potentialStringIds);
            if (potentialStringIds.size() != sessionStringInfo.size()) {

                sessionStringInfo.values()
                        .stream()
                        .map(StringInfo::getStringId)
                        .collect(Collectors.toList())
                        .forEach(potentialStringIds::remove);


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
            ArchiveIndex objectsIndex;
            try {
                objectsIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
            } catch (IOException e) {
//                e.printStackTrace();
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

                sessionObjectsInfo.values()
                        .stream()
                        .map(ObjectInfo::getObjectId)
                        .collect(Collectors.toList())
                        .forEach(objectIds::remove);
                remainingObjectIds.addAll(objectIds);
            }
            objectInfoMap.putAll(sessionObjectsInfo);


            Set<Integer> typeIds = objectInfoMap.values()
                    .stream()
                    .map(ObjectInfo::getTypeId)
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

            for (File sessionArchive : sessionArchivesLocal) {

                NameWithBytes objectIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                        INDEX_OBJECT_DAT_FILE.getFileName());
                assert objectIndexBytes != null;
                ArchiveIndex objectsIndex;
                try {
                    objectsIndex = readArchiveIndex(objectIndexBytes.getBytes(), INDEX_OBJECT_DAT_FILE);
                } catch (IOException e) {
//                    e.printStackTrace();
                    logger.error("failed to read object index from session archive", e);
                    continue;
                }


                Map<Long, ObjectInfo> sessionObjectsInfo = objectsIndex.getObjectsByObjectIdWithLongKeys(
                        remainingObjectIds);
                if (sessionObjectsInfo.size() > 0) {
//                    logger.warn("expected [" + objectIds.size() + "] results but got only " + sessionObjectsInfo.size());

                    sessionObjectsInfo.values()
                            .stream()
                            .map(ObjectInfo::getObjectId)
                            .collect(Collectors.toList())
                            .forEach(remainingObjectIds::remove);
                }
                objectInfoMap.putAll(sessionObjectsInfo);


                NameWithBytes stringsIndexBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive,
                        INDEX_STRING_DAT_FILE.getFileName());
                assert stringsIndexBytes != null;


                ArchiveIndex stringIndex;
                try {
                    stringIndex = readArchiveIndex(stringsIndexBytes.getBytes(), INDEX_STRING_DAT_FILE);
                } catch (IOException e) {
//                    e.printStackTrace();
                    logger.error("failed to read string index from session bytes: " + e.getMessage(), e);
                    continue;
                }
                Map<Long, StringInfo> sessionStringInfo = stringIndex.getStringsByIdWithLongKeys(remainingStringIds);
                if (remainingStringIds.size() != sessionStringInfo.size()) {

                    sessionStringInfo.values()
                            .stream()
                            .map(StringInfo::getStringId)
                            .collect(Collectors.toList())
                            .forEach(remainingStringIds::remove);
                }

                stringInfoMap.putAll(sessionStringInfo);


                Set<Integer> typeIds = objectInfoMap.values()
                        .stream()
                        .map(ObjectInfo::getTypeId)
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

    public void unlockNextScan() {
        scanLock.offer(1);
    }

    private void scanDataAndBuildReplay() {
        if (probeInfoIndex == null) {
            logger.warn("probe info index is not ready: " + this.executionSession.getPath());
            return;
        }
        if (isSessionCorrupted) {
            logger.warn("session is corrupted, not scanning new files: " + project.getName());
            if (!hasShownCorruptedNotification) {
                hasShownCorruptedNotification = true;
                InsidiousNotification.notifyMessage(
                        "Session is corrupted, please restart application or contact us" +
                                " at Discord [ https://discord.gg/Hhwvay8uTa ] if the issue persists",
                        NotificationType.ERROR);
            }

            return;
        }
        try {

            long scanStart = System.currentTimeMillis();

            List<LogFile> logFilesToProcess = daoService.getPendingLogFilesToProcess(processorId);
            final int logFileCount = logFilesToProcess.size();

            Map<Integer, List<LogFile>> logFilesByThreadMap = logFilesToProcess.stream()
                    .collect(Collectors.groupingBy(LogFile::getThreadId));
            if (logFilesByThreadMap.size() == 0) {
                return;
            }

            if (parameterContainer == null) {
                ChronicleMap<Long, Parameter> parameterIndex = createParameterIndex();
                parameterContainer = new ChronicleVariableContainer(parameterIndex);
            }

            boolean newCandidateIdentified = false;

            Set<Integer> allThreads = logFilesByThreadMap.keySet();
            int i = 0;
            int processedCount = 0;
            for (Integer threadId : allThreads) {
                i++;
                checkProgressIndicator("Processing files for thread " + i + " / " + allThreads.size(), null);
                List<LogFile> logFiles = logFilesByThreadMap.get(threadId);
                ThreadProcessingState threadState = daoService.getThreadState(threadId);
//                publishProgressEvent(new ScanProgress(processedCount + logFiles.size(), logFileCount));
                boolean newCandidateIdentifiedNew = processPendingThreadFiles(threadState, logFiles,
                        parameterContainer);
                newCandidateIdentified = newCandidateIdentified | newCandidateIdentifiedNew;
                processedCount += logFiles.size();
            }


//            Collection<Parameter> allParameters = new ArrayList<>(parameterIndex.values());
//            checkProgressIndicator("Saving " + allParameters.size() + " parameters", "");
//            daoService.createOrUpdateParameter(allParameters);
            if (newCandidateIdentified && testCandidateListener != null) {
                final int finalProcessedCount = processedCount;
                testCandidateListener.forEach(e -> {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        e.onNewTestCandidateIdentified(finalProcessedCount,
                                logFilesToProcess.size());
                    });
                });
            }


//            long scanEndTime = System.currentTimeMillis();
//            float scanTime = ((float) scanEndTime - (float) scanStart) / (float) 1000;
//            File sessionDir = new File(this.sessionDirectory.getParent());
//            long size_folder = getFolderSize(sessionDir);
//            JSONObject eventProperties = new JSONObject();
//            eventProperties.put("session_scan_time", scanTime);
//            eventProperties.put("session_folder_size", (size_folder / 1000000));
//            UsageInsightTracker.getInstance().RecordEvent("ScanMetrics", eventProperties);
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn("Exception in scan and build session", e);
            JSONObject properties = new JSONObject();
            properties.put("project", this.project.getName());
            properties.put("session", executionSession.getPath());
            properties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("SESSION_CORRUPT", properties);
            if (shutdown) {
                scanEnable = false;
                return;
            }
            isSessionCorrupted = true;
        }
    }

    private boolean processPendingThreadFiles(
            ThreadProcessingState threadState,
            List<LogFile> archiveLogFiles,
            ChronicleVariableContainer parameterContainer
    ) throws IOException, SQLException, FailedToReadClassWeaveException, InterruptedException {

        boolean newTestCaseIdentified = false;

        Set<Integer> existingProbes = new HashSet<>(daoService.getProbes());

//        updateObjectInfoIndex();

        int eventBufferSize = 100000;
        SessionEventReader eventsReader = new SessionEventReader(executionSession, archiveLogFiles, cacheEntries,
                eventBufferSize, this);
        executorPool.submit(eventsReader);

        int totalFile = archiveLogFiles.size();
        int currentFileIndex = 0;
        List<KaitaiInsidiousEventParser.Block> eventsList = new ArrayList<>();
        while (currentFileIndex < totalFile) {
            eventsList.clear();
            checkProgressIndicator(null, "Processing file " + currentFileIndex + " / " + archiveLogFiles.size());

            LogFile currentLogFile = null;
            List<LogFile> logFiles = new ArrayList<>();
            while (eventsList.size() < eventBufferSize && currentFileIndex < totalFile) {
                EventSet eventSet = eventsReader.getNextEventSet();
                currentLogFile = eventSet.getLogFile();
                logFiles.add(currentLogFile);
                List<KaitaiInsidiousEventParser.Block> events = eventSet.getEvents();
                eventsList.addAll(events);
                currentFileIndex++;
            }

            this.currentSessionArchiveBeingProcessed = FileSystems.getDefault()
                    .getPath(executionSession.getPath(), currentLogFile.getArchiveName())
                    .toFile();

//            List<KaitaiInsidiousEventParser.Block> eventsFromFileOld =
//                    getEventsFromFileOld(sessionArchive, logFile.getName());
            try {
                newTestCaseIdentified = processLogFile(logFiles, threadState, parameterContainer, existingProbes,
                        eventsList);
            } catch (NeedMoreLogsException e) {
                return newTestCaseIdentified;
            } catch (StackMismatchException e) {
                throw new RuntimeException(e);
            }
        }

        return newTestCaseIdentified;

    }

    private void updateObjectInfoIndex(Long eventValue) throws IOException {
        if (this.sessionArchives == null) {
            this.sessionArchives = refreshSessionArchivesList(true);
        }
        logger.warn("objectInfoIndex size = " + objectInfoIndex.size());
        if (objectInfoIndex.size() > 400000) {
            objectInfoIndex.clear();
        }
        List<String> archiveList = this.sessionArchives.stream()
                .map(File::getName)
                .collect(Collectors.toList());
        Collections.reverse(archiveList);

        // try to read the most recent object index from the archives we are about to process
        for (String lastArchiveName : archiveList) {
//            int archiveIndexNumber = Integer.parseInt(lastArchiveName.split("-")[1]);
            File lastSessionArchive = FileSystems.getDefault()
                    .getPath(executionSession.getPath(), lastArchiveName)
                    .toFile();
//            int latestIndexReadNumber = -1;
//            List<String> latestIndexRead = objectIndexRead.keySet()
//                    .stream()
//                    .sorted()
//                    .collect(Collectors.toList());
//            if (latestIndexRead.size() > 0) {
//                latestIndexReadNumber = Integer.parseInt(latestIndexRead.get(latestIndexRead.size() - 1)
//                        .split("-")[1]);
//            }


//            if (!objectIndexRead.containsKey(lastSessionArchive.getName())) {
//                objectIndexRead.put(lastSessionArchive.getName(), true);
            NameWithBytes objectIndex = createFileOnDiskFromSessionArchiveFile(lastSessionArchive,
                    INDEX_OBJECT_DAT_FILE.getFileName());
            if (objectIndex == null) {
                logger.error("failed to read object info index from: " + lastSessionArchive);
                continue;
            }
//                assert objectIndex != null;
            ArchiveIndex archiveObjectIndex = readArchiveIndex(objectIndex.getBytes(), INDEX_OBJECT_DAT_FILE);
//            if (objectIndexCollection != null) {
//                objectIndexCollection = null;
//            }
            objectIndexCollection = archiveObjectIndex.getObjectIndex();
            if (archiveObjectIndex.getObjectByObjectId(eventValue) == null) {
                continue;
            }
            logger.warn("adding [" + objectIndexCollection.size() + "] objects to index");
            archiveObjectIndex.getObjectIndex()
                    .parallelStream()
                    .forEach(e -> objectInfoIndex.put(e.getObjectId(), e));
            objectIndexCollection = null;
            return;
//            }
        }
    }

    private boolean processLogFile(
            List<LogFile> logFileList,
            ThreadProcessingState threadState,
            ChronicleVariableContainer parameterContainer,
            Set<Integer> existingProbes,
            List<KaitaiInsidiousEventParser.Block> eventsSublist
    ) throws IOException, FailedToReadClassWeaveException, NeedMoreLogsException, StackMismatchException {
        boolean newTestCaseIdentified = false;
        int threadId = threadState.getThreadId();
        long currentCallId = daoService.getMaxCallId();


        if (eventsSublist.size() == 0) {
            logFileList.forEach(logFile -> {
                logFile.setStatus(Constants.COMPLETED);
                daoService.updateLogFileEntry(logFile);
            });
            return newTestCaseIdentified;
        }

        List<DataEventWithSessionId> eventsToSave = new ArrayList<>();
        List<DataInfo> probesToSave = new ArrayList<>();
        Set<com.insidious.plugin.pojo.dao.MethodCallExpression> callsToSave = new HashSet<>();
        Set<com.insidious.plugin.pojo.dao.MethodCallExpression> callsToUpdate = new HashSet<>();
        List<com.insidious.plugin.pojo.dao.TestCandidateMetadata> candidatesToSave = new ArrayList<>();
        Date start = new Date();
//            Parameter parameterInstance = new Parameter();
        String nameFromProbe;
        String typeFromProbe;
        boolean isModified = false;
        com.insidious.plugin.pojo.dao.TestCandidateMetadata completedExceptional;
        com.insidious.plugin.pojo.dao.MethodCallExpression methodCall;
        com.insidious.plugin.pojo.dao.MethodCallExpression topCall;

        String existingParameterType;
        Parameter parameterInstance = new Parameter();
        logger.warn("processing [" + eventsSublist.size() + "] events from [" + logFileList.size() + "] log files");
        for (KaitaiInsidiousEventParser.Block e : eventsSublist) {

            KaitaiInsidiousEventParser.DetailedEventBlock eventBlock = e.block();
            DataEventWithSessionId dataEvent = null;


            final long eventValue = eventBlock.valueId();
// 70849
            DataInfo probeInfo = probeInfoIndex.get(eventBlock.probeId());
            if (probeInfo == null) {
                try {
                    logger.warn(
                            "probe info is null, reading latest classWeaveInfo: " + currentSessionArchiveBeingProcessed.getName() + " => " + eventBlock.probeId());
                    readClassWeaveInfoStream(currentSessionArchiveBeingProcessed);
                    probeInfo = probeInfoIndex.get(eventBlock.probeId());
                    if (probeInfo == null) {
                        throw new NeedMoreLogsException("probe info is null for id: " + eventBlock.probeId());
                    }
                } catch (FailedToReadClassWeaveException ex) {
                    // we have logs from new zip, which has new probes, but we could not read new class weave info
                    // cant proceed
                    logger.warn("Failed to read class weave file", ex);
                    throw new NeedMoreLogsException("probe info is null for id: " + eventBlock.probeId());
                }
            }
            Parameter existingParameter = parameterInstance;
            boolean saveProbe = false;
            isModified = false;
//            if (eventBlock.valueId() == 391737481) {
//                logger.warn("here: " + logFile); // 170446
//            }
            if (threadState.isSkipTillNextMethodExit()) {
                switch (probeInfo.getEventType()) {
//                    case METHOD_NORMAL_EXIT:
                    case METHOD_ENTRY:
                        threadState.setSkipTillNextMethodExit(false);
                        break;
                    default:
                        continue;
                        //nothing
                }
            }
            int line = probeInfo.getLine();
            if (threadState.candidateSize() != 0 && line != 0) {
                threadState.getTopCandidate().addLineCovered(line);
            }
            switch (probeInfo.getEventType()) {

                case LABEL:
//                    logger.warn("label: " + probeInfo);
                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);


//                    isModified = false;
//                    if (existingParameter.getProb() == null) {
                        existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
//                        isModified = true;
//                    }
                    saveProbe = true;

//                    if (!isModified) {
//                        existingParameter = null;
//                    }

                    // nothing to do
                    break;
                case LINE_NUMBER:
                    // we always have this information in the probeInfo
                    // nothing to do
                    break;

                case LOCAL_STORE:

                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    if (existingParameter != null) {
//                        nameFromProbe = probeInfo.getAttribute("Name",
//                                probeInfo.getAttribute("FieldName", null));
//                        if (!existingParameter.hasName(nameFromProbe)) {
//                            existingParameter.addName(nameFromProbe);
//                            isModified = true;
//                        }
                    }
                    if (existingParameter.getType() == null) {
//                        ObjectInfoDocument objectInfo = objectInfoIndex.get(existingParameter.getValue());
//                        if (objectInfo != null) {
//                            TypeInfoDocument typeInfo = typeInfoIndex.get(objectInfo.getTypeId());
//                            if (!typeInfo.getTypeName().startsWith("com.sun.proxy")) {
//                                existingParameter.setType(typeInfo.getTypeName());
//                            } else {
//                                existingParameter.setType(ClassTypeUtils.getDottedClassName(
//                                        probeInfo.getAttribute("Type", null)));
//                            }
//                            isModified = true;
//                        } else {
                        typeFromProbe = probeInfo.getAttribute("Type", null);
                        existingParameter.setType(ClassTypeUtils.getDottedClassName(typeFromProbe));
                        isModified = true;
//                        }
                    }
                    if (!isModified) {
                        existingParameter = null;
                    }

                    break;

                case LOCAL_LOAD:
                    if (eventValue == 0) {
                        continue;
                    }
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);

//                    String nameForParameter = probeInfo.getAttribute("Name",
//                            probeInfo.getAttribute("FieldName", null));
//                    if (!existingParameter.hasName(nameForParameter)) {
//                        existingParameter.addName(nameForParameter);
//                        isModified = true;
//                    }
                    existingParameterType = existingParameter.getType();
                    if (eventValue != 0 && (existingParameterType == null
                            || existingParameterType.equals("java.lang.Object")
                            || existingParameterType.endsWith("$1")
                            || existingParameterType.endsWith("$2")
                            || existingParameterType.endsWith("$3"))
                    ) {
//                        ObjectInfoDocument objectInfo = objectInfoIndex.get(existingParameter.getValue());
//                        if (objectInfo != null) {
//                            TypeInfoDocument typeInfo = typeInfoIndex.get(objectInfo.getTypeId());
//                            if (!typeInfo.getTypeName().startsWith("com.sun.proxy")) {
//                                existingParameter.setType(typeInfo.getTypeName());
//                            } else {
//                                existingParameter.setType(ClassTypeUtils.getDottedClassName(
//                                        probeInfo.getAttribute("Type", null)));
//                            }
//
//                        } else {
                        existingParameter.setType(
                                ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", null)));
//                        }
                        isModified = true;
                    }
                    if (!isModified) {
                        existingParameter = null;
                    }


                    break;

                case GET_STATIC_FIELD:
                    String fieldType1 = ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", null));
                    if (fieldType1.startsWith("org.slf4j")
                            || fieldType1.startsWith("com.google")
                            || fieldType1.startsWith("org.joda.time")) {
                        // nothing to do
                    } else {
                        if (fieldType1.endsWith("]")) {
                            fieldType1 = fieldType1.substring(0, fieldType1.indexOf("["));
                        }
                        ClassInfo classInfo = classInfoIndexByName.get(fieldType1);
                        if (classInfo != null && !classInfo.isEnum()) {
                            threadState.getTopCall().setUsesFields(true);
                        }
                    }
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    if (existingParameter != null && eventValue != 0) {
//                        nameFromProbe = probeInfo.getAttribute("Name",
//                                probeInfo.getAttribute("FieldName", null));
                        isModified = false;
//                        if (!existingParameter.hasName(nameFromProbe)) {
//                            existingParameter.addName(nameFromProbe);
//                            isModified = true;
//                        }
                        if (existingParameter.getProbeInfo() == null) {
//                            ObjectInfoDocument objectInfo = objectInfoIndex.get(existingParameter.getValue());
//                            if (objectInfo != null) {
//                                TypeInfoDocument typeInfo = typeInfoIndex.get(objectInfo.getTypeId());
//                                if (!typeInfo.getTypeName().startsWith("com.sun.proxy")) {
//                                    existingParameter.setType(typeInfo.getTypeName());
//                                } else {
//                                    existingParameter.setType(ClassTypeUtils.getDottedClassName(
//                                            probeInfo.getAttribute("Type", null)));
//                                }
//                            } else {
                            existingParameter.setType(
                                    ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", null)));
//                            }

                            dataEvent = createDataEventFromBlock(threadId, eventBlock);
                            existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);

                            saveProbe = true;
                            isModified = true;
                        }
                        if (!isModified) {
                            existingParameter = null;
                        }
                    }


                    break;

                case GET_INSTANCE_FIELD_RESULT:
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
//                    nameFromProbe = probeInfo.getAttribute("Name",
//                            probeInfo.getAttribute("FieldName", null));
                    isModified = false;
//                    if (!existingParameter.hasName(nameFromProbe)) {
//                        isModified = true;
//                        existingParameter.addName(nameFromProbe);
//                    }
                    typeFromProbe = ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V"));
                    existingParameterType = existingParameter.getType();
                    if (eventValue != 0 && (existingParameterType == null
                            || existingParameterType.equals("java.lang.Object")
                            || existingParameterType.endsWith("$1")
                            || existingParameterType.endsWith("$2")
                            || existingParameterType.endsWith("$3"))
                    ) {
//                        ObjectInfoDocument objectInfo = objectInfoIndex.get(existingParameter.getValue());
//                        if (objectInfo != null) {
//                            TypeInfoDocument typeInfo = typeInfoIndex.get(objectInfo.getTypeId());
//                            if (!typeInfo.getTypeName().startsWith("com.sun.proxy")) {
//                                existingParameter.setType(typeInfo.getTypeName());
//                            } else {
//                                existingParameter.setType(ClassTypeUtils.getDottedClassName(typeFromProbe));
//                            }
//
//                        } else {
                        existingParameter.setType(ClassTypeUtils.getDottedClassName(typeFromProbe));
//                        }
                    }
                    saveProbe = true;
                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
                    existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);

                    threadState.getTopCandidate().addField(existingParameter.getValue());
                    if (typeFromProbe.startsWith("org.slf4j") || typeFromProbe.startsWith("com.google")) {
                        // nothing to do
                    } else {
                        ClassInfo classInfo = classInfoIndex.get(probeInfo.getClassId());
                        if (!classInfo.isEnum()) {
                            threadState.getTopCall().setUsesFields(true);
                        }
                    }
                    if (!isModified) {
                        existingParameter = null;
                    }

                    break;

                case PUT_INSTANCE_FIELD:


                    // we are going to set this field in the next event
                    threadState.pushValue(eventValue);
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    if (eventValue != 0 && existingParameter != null && existingParameter.getProb() != null) {
                        if (existingParameter.getType() == null || existingParameter.getType().contains(".Object")) {
//                            ObjectInfoDocument objectInfo = objectInfoIndex.get(existingParameter.getValue());
//                            if (objectInfo != null) {
//                                TypeInfoDocument typeInfo = typeInfoIndex.get(objectInfo.getTypeId());
//                                if (!typeInfo.getTypeName().startsWith("com.sun.proxy")) {
//                                    existingParameter.setType(typeInfo.getTypeName());
//                                } else {
//                                    existingParameter.setType(ClassTypeUtils.getDottedClassName(
//                                            probeInfo.getAttribute("Owner", null)));
//                                }
//                            } else {
                            existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                    probeInfo.getAttribute("Owner", null)));
//                            }

                        } else {
                            existingParameter = null;
                        }
                    } else {
                        // new variable identified ?
                        dataEvent = createDataEventFromBlock(threadId, eventBlock);
                        existingParameter = parameterContainer.getParameterByValueUsing(eventValue,
                                existingParameter);
                        existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                        existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                probeInfo.getAttribute("Type", "V")));

//                        existingParameter.addName(
//                                probeInfo.getAttribute("Name", probeInfo.getAttribute("FieldName", null)));
                    }

                    break;

                case PUT_INSTANCE_FIELD_VALUE:


                    threadState.popValue();


                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    if (existingParameter != null) {
//                        nameFromProbe = probeInfo.getAttribute("Name",
//                                probeInfo.getAttribute("FieldName", null));
//                        if (!existingParameter.hasName(nameFromProbe)) {
//                            existingParameter.addName(nameFromProbe);
//                            isModified = true;
//                        }
                        existingParameterType = existingParameter.getType();
                        if (eventValue != 0 && (existingParameterType == null
                                || existingParameterType.equals("java.lang.Object")
                                || existingParameterType.endsWith("$1")
                                || existingParameterType.endsWith("$2")
                                || existingParameterType.endsWith("$3"))
                        ) {
//                            ObjectInfoDocument objectInfo = objectInfoIndex.get(existingParameter.getValue());
//                            if (objectInfo != null) {
//                                TypeInfoDocument typeInfo = typeInfoIndex.get(objectInfo.getTypeId());
//                                if (!typeInfo.getTypeName().startsWith("com.sun.proxy")) {
//                                    existingParameter.setType(typeInfo.getTypeName());
//                                } else {
//                                    existingParameter.setType(ClassTypeUtils.getDottedClassName(
//                                            probeInfo.getAttribute("Type", null)));
//                                }
//                            } else {
                            existingParameter.setType(
                                    ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", null)));
//                            }
                            isModified = true;
                        }
                    } else {
                        // new field
                        dataEvent = createDataEventFromBlock(threadId, eventBlock);
                        existingParameter = parameterContainer.getParameterByValueUsing(eventValue,
                                existingParameter);
                        existingParameter.setType(
                                ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Type", "V")));

//                        existingParameter.addName(
//                                probeInfo.getAttribute("Name", probeInfo.getAttribute("FieldName", null)));
                        existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);

                        saveProbe = true;
                        isModified = true;
                    }
                    if (!isModified) {
                        existingParameter = null;
                    }
                    break;


                case PUT_STATIC_FIELD:


                    isModified = false;
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    if (existingParameter != null) {
                        // field is already present, and we are overwriting it here
                        // setting this to null, so it is not inserted into the database again
                        existingParameter = null;
                    } else {
                        existingParameter = parameterContainer.getParameterByValueUsing(eventValue,
                                existingParameter);
                        if (existingParameter.getProb() == null) {
                            // we are coming across this field for the first time
                            dataEvent = createDataEventFromBlock(threadId, eventBlock);
                            existingParameter = parameterContainer.getParameterByValueUsing(eventValue,
                                    existingParameter);
//                            existingParameter.addName(probeInfo.getAttribute("Name",
//                                    probeInfo.getAttribute("FieldName", null)));
                            existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                    probeInfo.getAttribute("Type", "V")));

                            existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                            saveProbe = true;

                        } else {
//                            nameFromProbe = probeInfo.getAttribute("Name",
//                                    probeInfo.getAttribute("FieldName", null));
//                            if (!existingParameter.hasName(nameFromProbe)) {
//                                existingParameter.addName(nameFromProbe);
//                                isModified = true;
//                            }
                            typeFromProbe = ClassTypeUtils.getDottedClassName(
                                    probeInfo.getAttribute("Type", "V"));
                            if (existingParameter.getType() == null ||
                                    !existingParameter.getType().equals(typeFromProbe)) {
                                existingParameter.setType(ClassTypeUtils.getDottedClassName(typeFromProbe));
                                isModified = true;
                            }
                            if (!isModified) {
                                existingParameter = null;
                            }
                        }
                    }

                    break;

                case CALL:
//                        if (eventBlock.eventId() == 165161L) {
//                            logger.warn("in file: " + logFile);
//                        }
                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    saveProbe = true;
                    isModified = false;

                    if (existingParameter.getProbeInfo() == null) {
                        existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                        isModified = eventValue != 0;
                    }
                    if ((existingParameter.getType() == null || existingParameter.getType()
                            .equals("java.lang.Object"))) {
                        existingParameter.setType(
                                ClassTypeUtils.getDottedClassName(probeInfo.getAttribute("Owner", "V")));
                        isModified = eventValue != 0;
                    }


                    String methodName = probeInfo.getAttribute("Name", null);


                    if (existingParameter.getValue() == 0
                            && "Static".equals(probeInfo.getAttribute("CallType", null))
                            && !methodName.startsWith("<") && !methodName.contains("$")) {
                        String ownerClass = ClassTypeUtils.getJavaClassName(
                                probeInfo.getAttribute("Owner", null));
                        existingParameter.setValue(ownerClass.hashCode());
                        isModified = true;
                    }

                    currentCallId++;
                    methodCall = new com.insidious.plugin.pojo.dao.MethodCallExpression(methodName,
                            existingParameter.getValue(), new LinkedList<>(),
                            0, threadState.getCallStackSize());
                    methodCall.setThreadId(threadId);
                    methodCall.setId(currentCallId);
                    methodCall.setEnterNanoTime(dataEvent.getRecordedAt());
                    methodCallMap.put(currentCallId, methodCall);
                    methodCallSubjectTypeMap.put(currentCallId, ClassTypeUtils.getJavaClassName(
                            probeInfo.getAttribute("Owner", null)));
                    methodCall.setEntryProbeInfoId(probeInfo.getDataId());
                    methodCall.setEntryProbeId(dataEvent.getEventId());

                    MethodInfo methodDescription = methodInfoByNameIndex.get(
                            probeInfo.getAttribute("Owner", null) + probeInfo.getAttribute("Name",
                                    null) + probeInfo.getAttribute("Desc", null));
                    if (methodDescription != null) {
                        methodCall.setMethodDefinitionId(methodDescription.getMethodId());
                    }

                    ClassInfo methodClassInfo = classInfoIndexByName.get(existingParameter.getType());
                    if (methodClassInfo != null) {
                        if (Arrays.asList(methodClassInfo.getInterfaces())
                                .contains("org/springframework/data/jpa/repository/JpaRepository") ||
                                Arrays.asList(methodClassInfo.getInterfaces())
                                        .contains("org/springframework/data/repository/CrudRepository")) {
                            methodCall.setMethodAccess(1);
                        }
                    }

                    if ("Static".equals(probeInfo.getAttribute("CallType", null))) {
                        methodCall.setStaticCall(true);
                        methodCall.setSubject(existingParameter.getValue());
                    }
                    methodCall.setMethodAccess(1);


                    if (threadState.getCallStackSize() > 0) {
                        methodCall.setParentId(threadState.getTopCall()
                                .getId());
                    }
                    threadState.pushCall(methodCall);
                    if (!isModified) {
                        existingParameter = null;
                    }


                    break;


                case CALL_PARAM:
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
                    topCall = threadState.getTopCall();
                    isModified = false;
                    if ((existingParameter.getType() == null || existingParameter.getType().endsWith(".Object"))) {
//                        ObjectInfoDocument objectInfo = objectInfoIndex.get(existingParameter.getValue());
//                        if (objectInfo != null) {
//                            TypeInfoDocument typeInfo = typeInfoIndex.get(objectInfo.getTypeId());
//                            if (!typeInfo.getTypeName().startsWith("com.sun.proxy")) {
//                                existingParameter.setType(typeInfo.getTypeName());
//                            } else {
//                                existingParameter.setType(ClassTypeUtils.getDottedClassName(
//                                        probeInfo.getAttribute("Type", null)));
//                            }
//                        } else {
                        typeFromProbe = probeInfo.getAttribute("Type", null);
                        String typeNameForValue = ClassTypeUtils.getDottedClassName(typeFromProbe);
                        if (typeNameForValue != null && !typeNameForValue.equals("java.lang.Object")) {
                            existingParameter.setType(
                                    ClassTypeUtils.getDottedClassName(typeFromProbe));
                        }
//                        }
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
                        if (existingParameter.getProbeInfo() == null || (
                                !existingParameter.getProbeInfo().getEventType().equals(PUT_INSTANCE_FIELD_VALUE) &&
                                        !existingParameter.getProbeInfo().getEventType()
                                                .equals(GET_INSTANCE_FIELD_RESULT)
                        )) {
                            existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                            isModified = true;
                        }
                    }
                    saveProbe = true;
                    topCall.addArgument(existingParameter.getValue());
                    topCall.addArgumentProbe(dataEvent.getEventId());
                    if (!isModified) {
                        existingParameter = null;
                    }
                    break;

                case METHOD_ENTRY:
                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
                    MethodInfo methodInfo = methodInfoIndex.get(probeInfo.getMethodId());
                    methodCall = null;
                    // a method_entry event can come in without a corresponding event for call,
                    // in which case this is actually a separate method call
                    if (threadState.getCallStackSize() > 0) {
                        methodCall = threadState.getTopCall();
                        String expectedClassName = ClassTypeUtils.getDottedClassName(
                                methodInfo.getClassName());
                        String owner = ClassTypeUtils.getDottedClassName(
                                probeInfoIndex.get(methodCall.getEntryProbeInfo_id()).getAttribute("Owner", null));
                        if (owner == null) {
                            methodCall = null;
                        } else {
                            // sometimes we can enter a method_entry without a call
                            if (
                                    methodCall.getEntryProbe_id() ==
                                            (eventBlock.eventId() - methodCall.getArgumentProbes().size() - 1)
                                            && methodCall.getSubject() == dataEvent.getValue()
                            ) {
                                // we are inside a method call on a class intercepted by spring or cglib companions
                                // but this is the actual method call
                                // the current methodCall on stack is the actual one we have entered in
                                // we are not setting methodCall to null since else we will create a new call for
                                // this methodEntry and eventually bad things will happen (while popping calls)
                            } else if (!methodCallSubjectTypeMap.get(methodCall.getId()).startsWith(expectedClassName)
                                    || !methodInfo.getMethodName().equals(methodCall.getMethodName())) {
                                methodCall = null;
                            }
                        }
                    }

                    com.insidious.plugin.pojo.dao.TestCandidateMetadata newCandidate =
                            new com.insidious.plugin.pojo.dao.TestCandidateMetadata();

                    newCandidate.setEntryProbeIndex(eventBlock.eventId());
                    isModified = false;


                    if (methodCall == null) {
                        existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                        if (existingParameter.getValue() == 0
                                && ((methodInfo.getAccess() & 8) == 8)
                                && !methodInfo.getMethodName().startsWith("<")
                                && !methodInfo.getMethodName().contains("$")) {
                            String ownerClass = ClassTypeUtils.getJavaClassName(
                                    classInfoIndex.get(probeInfo.getClassId())
                                            .getClassName());
                            existingParameter.setValue(ownerClass.hashCode());
                            isModified = true;
                            existingParameter.setType(ownerClass);
                            existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                        }
                        if (existingParameter.getProb() == null) {

                            existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                            existingParameter.setType(ClassTypeUtils.getDottedClassName(methodInfo.getClassName()));
                            isModified = true;
                        }
                        if (existingParameter.getType() == null ||
                                existingParameter.getType().equals("java.lang.Object")) {
                            existingParameter.setType(ClassTypeUtils.getDottedClassName(methodInfo.getClassName()));


                            isModified = true;
                        }


//                            logger.warn("Method " + methodInfo.getMethodName() + ": " + currentCallId);
                        methodCall = new com.insidious.plugin.pojo.dao.MethodCallExpression(methodInfo.getMethodName(),
                                existingParameter.getValue(), new LinkedList<>(),
                                0, threadState.getCallStackSize());
//                        if (methodInfo.getMethodName().equals("triggerMethodExecutorRefresh")) {
//                            int x = 1;
//                        }
//                        logger.warn("MethodEnter [" + methodInfo.getMethodName() + "]");

                        saveProbe = true;
                        methodCall.setThreadId(threadId);
                        methodCall.setEntryProbeInfoId(probeInfo.getDataId());
                        methodCall.setEntryProbeId(dataEvent.getEventId());
                        methodCall.setEnterNanoTime(dataEvent.getRecordedAt());
                        methodCall.setMethodDefinitionId(probeInfo.getMethodId());
                        methodCall.setStaticCall((methodInfo.getAccess() & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC);

                        currentCallId++;
                        methodCall.setId(currentCallId);
                        methodCallMap.put(currentCallId, methodCall);
                        methodCallSubjectTypeMap.put(currentCallId,
                                ClassTypeUtils.getJavaClassName(methodInfo.getClassName()));
//                            if (threadState.candidateSize() > 0) {
//                                addMethodToCandidate(threadState, methodCall);
//                            }
                        if (threadState.getCallStackSize() > 0) {
                            methodCall.setParentId(threadState.getTopCall().getId());
                        }

                        threadState.pushCall(methodCall);
                    } else {
                        if (methodCall.getMethodDefinitionId() == 0) {
                            methodCall.setMethodDefinitionId(probeInfo.getMethodId());
                        }
                        saveProbe = true;
                    }
                    newCandidate.setMainMethod(methodCall.getId());
                    threadState.pushTopCandidate(newCandidate);

//                    if (existingParameter == null) {
//                        existingParameter = parameterContainer.getParameterByValueUsing(eventValue,
//                                existingParameter);
//                    }


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

                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);


                    isModified = false;
                    if (existingParameter.getProb() == null) {
                        existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                        isModified = true;
                    }
//                    if (existingParameter.getType() == null) {
//                        ObjectInfoDocument objectInfo = objectInfoIndex.get(existingParameter.getValue());
//                        if (objectInfo != null) {
//                            TypeInfoDocument typeInfo = typeInfoIndex.get(objectInfo.getTypeId());
//                            if (!typeInfo.getTypeName().contains(".$")) {
//                                existingParameter.setType(typeInfo.getTypeName());
//                            }
//                            isModified = true;
//                        }
//                    }
                    saveProbe = true;

                    topCall = threadState.getTopCall();

                    EventType entryProbeEventType = probeInfoIndex.get(topCall.getEntryProbeInfo_id()).getEventType();
                    if (entryProbeEventType == EventType.CALL) {
                        // not adding these since we will record method_params only for cases in which we dont have a method_entry probe
                    } else if (entryProbeEventType == EventType.METHOD_ENTRY) {
                        topCall.addArgument(existingParameter.getValue());
                        topCall.addArgumentProbe(dataEvent.getEventId());
                    } else {
                        throw new RuntimeException("unexpected entry probe event type");
                    }
                    if (!isModified) {
                        existingParameter = null;
                    }
                    break;

                case CATCH:
                    if (threadState.getCallStackSize() == 0) {
                        threadState.getCallStack().clear();
                        threadState.getCandidateStack().clear();
                        threadState.setSkipTillNextMethodExit(true);
                        continue;
                    }

                    if (threadState.candidateSize() < threadState.getCallStackSize()
                            && threadState.getTopCandidate().getMainMethod() != threadState.getTopCall().getId()) {

                        dataEvent = createDataEventFromBlock(threadId, eventBlock);
                        existingParameter = parameterContainer.getParameterByValueUsing(eventValue,
                                existingParameter);
                        saveProbe = true;
                        existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                        topCall = threadState.popCall();
                        topCall.setReturnValue_id(existingParameter.getValue());
                        topCall.setReturnDataEvent(dataEvent.getEventId());

                        topCall.setReturnNanoTime(existingParameter.getProb().getRecordedAt());
                        callsToSave.add(topCall);
                        methodCallMap.remove(topCall.getId());
                        methodCallSubjectTypeMap.remove(topCall.getId());
                        threadState.setMostRecentReturnedCall(topCall);

                    }
                    break;

                case METHOD_EXCEPTIONAL_EXIT:
                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
//                                LoggerUtil.logEvent("SCAN", callStack.size(), instructionIndex, dataEvent, probeInfo, classInfo, methodInfo);

//                    topCall = threadState.getTopCall();
//                    entryProbeEventType = probeInfoIndex.get(topCall.getEntryProbeInfo_id())
//                            .getEventType();

                    if (threadState.candidateSize() == 0) {
                        threadState.getCallStack().clear();
                        threadState.getCandidateStack().clear();
                        threadState.setSkipTillNextMethodExit(true);
                        continue;
                    }

                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    if (existingParameter.getType() == null && eventValue != 0) {
                        ObjectInfoDocument objectInfoDocument = objectInfoIndex.get(existingParameter.getValue());
                        if (objectInfoDocument == null) {
                            this.sessionArchives = refreshSessionArchivesList(true);
                            updateObjectInfoIndex(eventValue);
                            objectInfoDocument = objectInfoIndex.get(existingParameter.getValue());
                            if (objectInfoDocument == null) {
                                logger.warn(
                                        "object info document is null for [" + existingParameter.getValue() + "] in " +
                                                "log file: [" + "] in archive [" +
                                                "]");
                                throw new NeedMoreLogsException(
                                        "object info document is null for [" + existingParameter.getValue() + "] in " +
                                                "log file: [" + "] in archive [" +
                                                "]");
                            }
                        }
                        if (objectInfoDocument != null) {
                            TypeInfoDocument typeFromTypeIndex = getTypeFromTypeIndex(objectInfoDocument.getTypeId());
                            String typeName = ClassTypeUtils.getDottedClassName(typeFromTypeIndex.getTypeName());
                            existingParameter.setType(typeName);
                        }
                    }

                    existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                    isModified = true;
                    saveProbe = true;


//                    if (entryProbeEventType == EventType.METHOD_ENTRY) {
                    topCall = threadState.popCall();
                    topCall.setReturnValue_id(existingParameter.getValue());
                    topCall.setReturnDataEvent(dataEvent.getEventId());
                    topCall.setReturnNanoTime(existingParameter.getProb().getRecordedAt());
                    callsToSave.add(topCall);
                    threadState.setMostRecentReturnedCall(topCall);
//                    }
                    // we need to pop only 1 call here from the stack

                    completedExceptional = threadState.popTopCandidate();

                    completedExceptional.setExitProbeIndex(dataEvent.getEventId());
                    com.insidious.plugin.pojo.dao.MethodCallExpression completedMainMethod =
                            methodCallMap.get(completedExceptional.getMainMethod());

                    if (completedMainMethod != null) {
                        completedExceptional.setTestSubject(completedMainMethod.getSubject());
                        completedExceptional.setCallTimeNanoSecond(completedMainMethod.getCallTimeNano());
                    } else {
                        logger.error("completedMainMethod is null");
                    }


                    if (threadState.candidateSize() > 0) {
                        com.insidious.plugin.pojo.dao.TestCandidateMetadata newCurrent = threadState.getTopCandidate();
                        if (methodCallSubjectTypeMap.get(newCurrent.getMainMethod())
                                .equals(methodCallSubjectTypeMap.get(completedMainMethod.getId()))) {
                            for (long parameterValue : completedExceptional.getFields()) {
                                newCurrent.addField(parameterValue);
                            }
                        }

                    } else {
                        if (threadState.getCallStackSize() > 0) {
                            logger.warn("inconsistent call stack state, flushing calls list");
                            threadState.getCallStack().clear();
                        }
                    }

                    addMethodAsTestCandidate(candidatesToSave, completedExceptional);
//                    if (entryProbeEventType == METHOD_ENTRY) {
                    methodCallMap.remove(completedExceptional.getMainMethod());
                    methodCallSubjectTypeMap.remove(completedExceptional.getMainMethod());
//                    }

                    if (!isModified) {
                        existingParameter = null;
                    }
                    break;


                case METHOD_NORMAL_EXIT:

                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
//                                LoggerUtil.logEvent("SCAN", callStack.size(), instructionIndex, dataEvent, probeInfo, classInfo, methodInfo);
                    if (threadState.candidateSize() == 0) {
                        threadState.getCallStack().clear();
                        threadState.getCandidateStack().clear();
                        threadState.setSkipTillNextMethodExit(true);
                        continue;
                    }

                    topCall = threadState.getTopCall();
//                    if (topCall.getMethodName().equals("triggerMethodExecutorRefresh")) {
//                    logger.warn("MethodExit [" + topCall.getMethodName() + "]");
//                        int x = 1;
//                    }
                    entryProbeEventType = probeInfoIndex.get(topCall.getEntryProbeInfo_id())
                            .getEventType();
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);
                    isModified = false;
                    saveProbe = true;
                    if (existingParameter.getProb() == null || existingParameter.getProbeInfo() == null) {
                        existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                        isModified = eventValue != 0;
                    }
                    if (existingParameter.getType()  != null
                     && existingParameter.getType().contains("Mono")) {
                        long longValue = ByteBuffer.wrap(eventBlock.serializedData()).getLong();

                    }

                    if (existingParameter.getType() == null && eventValue != 0) {
                        ObjectInfoDocument objectInfoDocument = objectInfoIndex.get(existingParameter.getValue());
                        if (probeInfo.getValueDesc() == Descriptor.Object) {
                            if (objectInfoDocument == null) {
                                this.sessionArchives = refreshSessionArchivesList(true);
                                updateObjectInfoIndex(eventValue);
                                objectInfoDocument = objectInfoIndex.get(existingParameter.getValue());
                                if (objectInfoDocument == null) {
                                    logger.warn(
                                            "object info document is null for [" + existingParameter.getValue() + "] in " +
                                                    "log file: [" + "] in archive [" +
                                                    "]");
                                    throw new NeedMoreLogsException(
                                            "object info document is null for [" + existingParameter.getValue() + "] in " +
                                                    "log file: [" + "] in archive [" +
                                                    "]");
                                }
                            }
                            if (objectInfoDocument != null) {
                                TypeInfoDocument typeFromTypeIndex = getTypeFromTypeIndex(
                                        objectInfoDocument.getTypeId());
                                String typeName = ClassTypeUtils.getDottedClassName(typeFromTypeIndex.getTypeName());
                                existingParameter.setType(typeName);
                                isModified = true;
                            }
                        } else {
                            existingParameter.setType(
                                    ClassTypeUtils.getJavaClassName(probeInfo.getValueDesc().getString()));
                            isModified = true;
                        }

                    }


                    if (entryProbeEventType == EventType.CALL) {
                        // we don't pop it here, wait for the CALL_RETURN to pop the call

                    } else if (entryProbeEventType == EventType.METHOD_ENTRY) {
                        // we can pop the current call here since we never had the CALL event in the first place
                        // this might be going out of our hands
                        topCall = threadState.popCall();


                        if (topCall.getMethodName().startsWith("<")) {
                            topCall.setReturnValue_id(topCall.getSubject());

                        } else {
                            topCall.setReturnValue_id(existingParameter.getValue());
                        }
                        topCall.setReturnDataEvent(dataEvent.getEventId());
                        topCall.setReturnNanoTime(dataEvent.getRecordedAt());
                        callsToSave.add(topCall);

                        threadState.setMostRecentReturnedCall(topCall);

                    } else {
                        throw new RuntimeException(
                                "unexpected entry probe event type [" + entryProbeEventType + "]");
                    }


                    com.insidious.plugin.pojo.dao.TestCandidateMetadata completed = threadState.popTopCandidate();

                    completed.setExitProbeIndex(eventBlock.eventId());
                    if (completed.getMainMethod() != 0) {
                        com.insidious.plugin.pojo.dao.MethodCallExpression methodCallExpression = methodCallMap.get(
                                completed.getMainMethod());
                        completed.setCallTimeNanoSecond(methodCallExpression.getCallTimeNano());
                        completed.setTestSubject(methodCallExpression.getSubject());
                    }

                    if (threadState.candidateSize() > 0) {
                        com.insidious.plugin.pojo.dao.TestCandidateMetadata newCurrent = threadState.getTopCandidate();
                        com.insidious.plugin.pojo.dao.MethodCallExpression newCurrentMainMethod = methodCallMap.get(
                                newCurrent.getMainMethod());

                        if (methodCallSubjectTypeMap.get(newCurrentMainMethod.getId())
                                .equals(methodCallSubjectTypeMap.get(completed.getMainMethod()))) {
                            for (long parameter : completed.getFields()) {
                                newCurrent.addField(parameter);
                            }
                        }


//                                newCurrent.getFields().all().addAll(completed.gertFields().all());
                    } else {
                        if (threadState.getCallStackSize() > 0) {
                            logger.warn(
                                    "inconsistent call stack state, flushing calls list: " + threadState.getCallStack());
//                                        callStack.clear();
                        }
                    }

                    addMethodAsTestCandidate(candidatesToSave, completed);
                    if (entryProbeEventType == METHOD_ENTRY) {
                        methodCallMap.remove(completed.getMainMethod());
                        methodCallSubjectTypeMap.remove(completed.getMainMethod());
                    }

                    if (!isModified) {
                        existingParameter = null;
                    }
                    break;

                case CALL_RETURN:

                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
                    existingParameter = parameterContainer.getParameterByValueUsing(eventValue, existingParameter);

                    isModified = false;
                    saveProbe = true;
                    if ((existingParameter.getType() == null || existingParameter.getType().endsWith(".Object"))) {
                        existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
                        saveProbe = true;
                        existingParameter.setType(ClassTypeUtils.getDottedClassName(
                                probeInfo.getAttribute("Type", "V")));

                        isModified = true;
                    }
                    if (threadState.getCallStackSize() == 0) {
                        threadState.getCallStack().clear();
                        threadState.getCandidateStack().clear();
                        threadState.setSkipTillNextMethodExit(true);
                        continue;
                    }

                    com.insidious.plugin.pojo.dao.MethodCallExpression callExpression = threadState.getTopCall();
                    EventType entryEventType = probeInfoIndex.get(callExpression.getEntryProbeInfo_id())
                            .getEventType();
                    if (entryEventType == EventType.CALL) {
                        // we pop it now

                        topCall = threadState.popCall();
                        topCall.setReturnValue_id(existingParameter.getValue());
                        topCall.setReturnDataEvent(dataEvent.getEventId());
                        topCall.setReturnNanoTime(dataEvent.getRecordedAt());
                        callsToSave.add(topCall);

                        methodCallMap.remove(topCall.getId());
                        methodCallSubjectTypeMap.remove(topCall.getId());

                        if (threadState.getTopCandidate().getMainMethod() == topCall.getId()) {

                        }

                        threadState.setMostRecentReturnedCall(topCall);

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
                    String nextNewObjectType = ClassTypeUtils.getDottedClassName(
                            probeInfo.getAttribute("Type", "V"));
                    threadState.pushNextNewObjectType(nextNewObjectType);
                    if (nextNewObjectType.equals("java.util.Date")) {
                        threadState.getTopCall().setUsesFields(true);
                    }
                    break;
                case NEW_OBJECT_CREATED:
                    com.insidious.plugin.pojo.dao.MethodCallExpression theCallThatJustEnded = threadState.getMostRecentReturnedCall();
                    String upcomingObjectType = threadState.popNextNewObjectType();
                    existingParameter = parameterContainer.getParameterByValue(theCallThatJustEnded.getSubject());
                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
//                    existingParameter.setValue(0L);
//                    if (existingParameter.getProb() == null) {
                    existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
//                    }
                    existingParameter.setType(ClassTypeUtils.getDottedClassName(upcomingObjectType));
                    theCallThatJustEnded.setReturnValue_id(existingParameter.getValue());
                    theCallThatJustEnded.setSubject(existingParameter.getValue());
                    if (!callsToSave.contains(theCallThatJustEnded)) {
                        callsToUpdate.add(theCallThatJustEnded);
                    }
                    saveProbe = true;
                    break;

                case METHOD_OBJECT_INITIALIZED:
                    topCall = threadState.getTopCall();
                    existingParameter = parameterContainer.getParameterByValue(topCall.getSubject());
                    dataEvent = createDataEventFromBlock(threadId, eventBlock);
//                    if (existingParameter.getProb() == null) {
                    existingParameter.setProbeAndProbeInfo(dataEvent, probeInfo);
//                    }
                    saveProbe = true;
                    topCall.setSubject(existingParameter.getValue());
                    topCall.setReturnValue_id(existingParameter.getValue());

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
        long eventsPerSecond = 1000 * (eventsSublist.size() / timeInMs);
        logger.warn(
                "[3466] Took [" + timeInMs + " ms] for [" + eventsSublist.size() + " events" +
                        "]  => " + eventsPerSecond + " EPS");
        logger.debug("parameterContainer = " + parameterContainer.all().size()
                + ",  eventsToSave = " + eventsToSave.size()
                + ",  probesToSave = " + probesToSave.size());
        daoService.createOrUpdateProbeInfo(probesToSave);


        if (threadState.getCallStack().size() > 1000) {
            StringBuilder infiniteRecursionDetectedMessage = new StringBuilder(
                    "<html>There was an infinite recursion " +
                            "detected. Events cannot be processed further for thread [" + threadId + "]. " +
                            "You need to add @JsonManagedReference/@JsonBackReference annotation to identify " +
                            "POJOs references forming a closed loop. Here is a list of last few calls from the stack: " +
                            "<br/><br/>");
            int internalCounter = 0;
            Map<Long, Integer> matchedProbe = new HashMap<>();
            for (int j = 0; j < threadState.getCallStack().size(); j++) {
                com.insidious.plugin.pojo.dao.MethodCallExpression call = threadState.getCallStack().get(j);
                long dataId = probeInfoIndex.get(call.getEntryProbeInfo_id()).getDataId();
                Parameter subjectParameter = parameterContainer.getParameterByValue(call.getSubject());
                infiniteRecursionDetectedMessage
                        .append("[")
                        .append(dataId)
                        .append("][")
                        .append(call.getEntryProbe_id())
                        .append("]").append(" call: ");

                if (subjectParameter.getType() != null && subjectParameter.getType().length() > 0) {
                    infiniteRecursionDetectedMessage.append(subjectParameter.getType()).append(".");
                }
                infiniteRecursionDetectedMessage.append(call.getMethodName()).append("(")
                        .append(call.getArguments().size()).append(" args").append(")");

                infiniteRecursionDetectedMessage.append("<br />");
                internalCounter++;
                if (matchedProbe.containsKey(dataId) && matchedProbe.get(dataId) > 3) {
                    break;
                }
                if (!matchedProbe.containsKey(dataId)) {
                    matchedProbe.put(dataId, 0);
                }
                matchedProbe.put(dataId, matchedProbe.get(dataId) + 1);
                if (internalCounter > 30) {
                    break;
                }
            }
            infiniteRecursionDetectedMessage.append("</html>");
            logger.warn("infinite recursion detected: " + infiniteRecursionDetectedMessage.toString());
            logger.warn("was going to save: [" + threadState.getCallStack().size() + " calls ]");
            InsidiousNotification.notifyMessage(infiniteRecursionDetectedMessage.toString(), NotificationType.ERROR);
//            return newTestCaseIdentified;
        }

        daoService.createOrUpdateDataEvent(eventsToSave);
        daoService.createOrUpdateCall(callsToSave);
        daoService.createOrUpdateIncompleteCall(threadState.getCallStack());
        daoService.updateCalls(callsToUpdate);
        daoService.createOrUpdateTestCandidate(candidatesToSave);

        if (testCandidateListener != null && candidatesToSave.size() > 0) {
            for (NewTestCandidateIdentifiedListener newTestCandidateIdentifiedListener : testCandidateListener) {
                newTestCandidateIdentifiedListener.onNewTestCandidateIdentified(1, 1);
            }
        }


        if (candidatesToSave.size() > 0) {
            newTestCaseIdentified = true;
        }

        logFileList.forEach(logFile -> {
            logFile.setStatus(Constants.COMPLETED);
            daoService.updateLogFileEntry(logFile);
        });

        daoService.createOrUpdateThreadState(threadState);
        return newTestCaseIdentified;
    }

    private void addMethodAsTestCandidate(
            List<com.insidious.plugin.pojo.dao.TestCandidateMetadata> candidatesToSave,
            com.insidious.plugin.pojo.dao.TestCandidateMetadata completedCandidated
    ) {
        if (completedCandidated.getTestSubject() != 0) {
            com.insidious.plugin.pojo.dao.MethodCallExpression mainMethod = methodCallMap.get(
                    completedCandidated.getMainMethod());
            ClassInfo subjectClassInfo = classInfoIndexByName.get(methodCallSubjectTypeMap.get(mainMethod.getId()));
            String candidateMethodName = mainMethod.getMethodName();

            if ((subjectClassInfo != null && subjectClassInfo.isPojo()) ||
                    candidateMethodName.equals("getTargetClass")
                    || candidateMethodName.equals("getTargetSource")
                    || candidateMethodName.equals("intercept")
                    || candidateMethodName.equals("isFrozen")
                    || candidateMethodName.equals("invoke")
                    || candidateMethodName.equals("doFilter")
                    || candidateMethodName.equals("resolveToken")
                    || candidateMethodName.equals("toString")
                    || (!candidateMethodName.startsWith("lambda$") && candidateMethodName.contains("$"))
                    || candidateMethodName.equals("getIndex")
                    || candidateMethodName.equals("values")
                    || candidateMethodName.equals("hashCode")
                    || candidateMethodName.startsWith("<")
                    || candidateMethodName.equals("setBeanFactory")
                    || candidateMethodName.equals("setCallbacks")) {
                // don't save these methods as test candidates, since they are created by spring
            } else {
                candidatesToSave.add(completedCandidated);
            }
        }
    }

    public CodeCoverageData createCoverageData() {

        List<MethodDefinition> allMethods = daoService.getAllMethodDefinitions();
        allMethods.sort(Collections.reverseOrder(MethodDefinition::compareTo));

        List<MethodDefinition> onlyUpdatedMethod = new ArrayList<>();
//        Map<String, Integer> classNameToIdMap = new HashMap<>();
        Map<String, Boolean> methodByKeyMap = new HashMap<>();

        for (MethodDefinition methodDefinition : allMethods) {
            String className = methodDefinition.getOwnerType();
            String methodHashKey = className + methodDefinition.getMethodName() + methodDefinition.getMethodDescriptor();
            if (methodByKeyMap.containsKey(methodHashKey)) {
                continue;
            }
            methodByKeyMap.put(methodHashKey, true);
            onlyUpdatedMethod.add(methodDefinition);
        }


        Map<String, List<ClassCoverageData>> byPackageMap = new HashMap<>();
        Map<String, List<MethodCoverageData>> byClassMap = new HashMap<>();

        for (MethodDefinition methodDefinition : onlyUpdatedMethod) {
            String className = methodDefinition.getOwnerType();

            List<MethodCoverageData> classCoverageData = byClassMap.get(className);
            if (classCoverageData == null) {
                classCoverageData = new ArrayList<>();
                byClassMap.put(className, classCoverageData);
            }

            classCoverageData.add(new MethodCoverageData(
                    methodDefinition.getMethodName(), methodDefinition.getMethodDescriptor(),
                    methodDefinition.getLineCount(), 0
            ));

        }

        for (String className : byClassMap.keySet()) {
            String packageName = "";
            if (className.contains(".")) {
                packageName = className.substring(0, className.lastIndexOf("."));
            }
            List<MethodCoverageData> methodCoverageData = byClassMap.get(className);
            methodCoverageData.sort(Comparator.comparing(MethodCoverageData::getMethodName));
            ClassCoverageData classCoverageData = new ClassCoverageData(className.substring(
                    className.lastIndexOf(".") + 1
            ), methodCoverageData);
            List<ClassCoverageData> packageCoverageData = byPackageMap.get(packageName);
            if (packageCoverageData == null) {
                packageCoverageData = new ArrayList<>();
                byPackageMap.put(packageName, packageCoverageData);
            }
            packageCoverageData.add(classCoverageData);
        }


        List<PackageCoverageData> packageCoverageList = new ArrayList<>();

        for (String packageName : byPackageMap.keySet()) {
            List<ClassCoverageData> classCoverageDataList = byPackageMap.get(packageName);
            classCoverageDataList.sort(Comparator.comparing(ClassCoverageData::getClassName));
            packageCoverageList.add(new PackageCoverageData(packageName, classCoverageDataList));
        }


        return new CodeCoverageData(packageCoverageList);
    }

    private TypeInfoDocument getTypeFromTypeIndex(int typeId) throws FailedToReadClassWeaveException, IOException {
        TypeInfoDocument typeInfoDocument = typeInfoIndex.get(typeId);
        if (typeInfoDocument == null) {
            logger.warn(
                    "typeInfo is null for [" + typeId + "] reading latest classWeaveInfo from: " + currentSessionArchiveBeingProcessed.getName());
            readClassWeaveInfoStream(this.currentSessionArchiveBeingProcessed);
            typeInfoDocument = typeInfoIndex.get(typeId);
            if (typeInfoDocument == null) {
                throw new RuntimeException("Type info not found: " + typeId);
            }
        }
        return typeInfoDocument;
    }

//    private ObjectInfoDocument getObjectInfoDocumentRaw(long parameterValue) {
//        ResultSet<ObjectInfoDocument> result = objectIndexCollection.retrieve(
//                equal(ObjectInfoDocument.OBJECT_ID, parameterValue));
//        if (result.size() == 0) {
//            return null;
//        }
//        ObjectInfoDocument objectInfo = result.uniqueResult();
//        result.close();
//        return objectInfo;
//    }

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

    private long eventId(KaitaiInsidiousEventParser.Block lastEvent) {
        return lastEvent.block().eventId();
    }

//    public void queryTracePointsByTypes(SearchQuery searchQuery, ClientCallBack<TracePoint> clientCallBack) {
//
//        int totalMatched = 0;
//
//        for (File sessionArchive : sessionArchives) {
//
//            checkProgressIndicator("Checking archive " + sessionArchive.getName(), null);
//
//            Collection<Long> objectIds = queryObjectsByTypeFromSessionArchive(searchQuery, sessionArchive).stream()
//                    .map(ObjectInfoDocument::getObjectId)
//                    .collect(Collectors.toSet());
//
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                break;
//            }
//
//
//            if (objectIds.size() > 0) {
//                List<TracePoint> tracePointsByValueIds = getTracePointsByValueIds(sessionArchive,
//                        new HashSet<>(objectIds));
//                tracePointsByValueIds.forEach(e -> e.setExecutionSession(executionSession));
//                clientCallBack.success(tracePointsByValueIds);
//                totalMatched += tracePointsByValueIds.size();
//
//                checkProgressIndicator("Matched " + totalMatched + " events", null);
//            }
//
//        }
//        clientCallBack.completed();
//    }

//    public void queryTracePointsByValue(SearchQuery searchQuery, ClientCallBack<TracePoint> getProjectSessionErrorsCallback) {
//        checkProgressIndicator("Searching locally by value [" + searchQuery.getQuery() + "]", null);
//        Collection<TracePoint> tracePointList;
//
//        tracePointList = queryTracePointsByValue(searchQuery);
//
//        getProjectSessionErrorsCallback.success(tracePointList);
//
//    }

    public List<VideobugTreeClassAggregateNode> getTestCandidateAggregates() {
        return daoService.getTestCandidateAggregates();
    }

    public List<TestCandidateMethodAggregate> getTestCandidateAggregatesByClassName(String className) {
        return daoService.getTestCandidateAggregatesForType(className);
    }

//    public List<TestCandidateMetadata> getTestCandidatesForPublicMethod(String className, String methodName, boolean loadCalls) {
//        return daoService.getTestCandidatesForPublicMethod(className, methodName, loadCalls);
//    }

    public List<TestCandidateMetadata> getTestCandidatesForAllMethod(CandidateSearchQuery candidateSearchQuery) {
        try {
            return daoService.getTestCandidatesForAllMethod(candidateSearchQuery);
        } catch (Exception e) {
            // probably database doesnt exist
            logger.warn("failed to get test candidates for method [" +
                    candidateSearchQuery.getClassName() + "." + candidateSearchQuery.getMethodName() + "()]", e);
            return new ArrayList<>();
        }
    }

    public List<MethodCallExpression> getMethodCallExpressions(CandidateSearchQuery candidateSearchQuery) {
        try {
            return daoService.getMethodCallExpressions(candidateSearchQuery);
        } catch (Exception e) {
            // probably database doesnt exist
            logger.warn("failed to get test candidates for method [" +
                    candidateSearchQuery.getClassName() + "." + candidateSearchQuery.getMethodName() + "()]", e);
            return new ArrayList<>();
        }
    }

    public TestCandidateMetadata getConstructorCandidate(Parameter parameter) throws Exception {
        return daoService.getConstructorCandidate(parameter);
    }

    public TestCandidateMetadata getTestCandidateById(Long testCandidateId, boolean loadCalls) {
        TestCandidateMetadata testCandidateMetadata = daoService.getTestCandidateById(testCandidateId, loadCalls);
        if (testCandidateMetadata == null) {
            return null;
        }
        TestCandidateMetadata tm = ApplicationManager.getApplication()
                .runReadAction((Computable<TestCandidateMetadata>) () -> {
                    ClassUtils.resolveTemplatesInCall(testCandidateMetadata.getMainMethod(), project);
                    return testCandidateMetadata;
                });
        // check if the param are ENUM
        createParamEnumPropertyTrueIfTheyAre(testCandidateMetadata.getMainMethod());

        if (loadCalls) {
            for (MethodCallExpression methodCallExpression : testCandidateMetadata.getCallsList()) {
                MethodCallExpression finalMethodCallExpression = methodCallExpression;
                methodCallExpression = ApplicationManager.getApplication().runReadAction(
                        (Computable<MethodCallExpression>) () -> ClassUtils.resolveTemplatesInCall(
                                finalMethodCallExpression, project));
                createParamEnumPropertyTrueIfTheyAre(methodCallExpression);
            }
        }
        return testCandidateMetadata;
    }

    private void createParamEnumPropertyTrueIfTheyAre(MethodCallExpression methodCallExpression) {
        List<Parameter> methodArguments = methodCallExpression.getArguments();

        for (Parameter methodArgument : methodArguments) {
            //param is enum then we set it to enum type
            checkAndSetParameterEnumIfYesMakeNameCamelCase(methodArgument);
        }
        // check for the return value type if its enum
        checkAndSetParameterEnumIfYesMakeNameCamelCase(methodCallExpression.getReturnValue());
    }

    private void checkAndSetParameterEnumIfYesMakeNameCamelCase(Parameter param) {
        if (param == null || param.getType() == null)
            return;

        String currParamType = param.getType();
        ClassInfo currClass = this.classInfoIndexByName.get(currParamType);

        if (currClass != null && currClass.isEnum()) {
            param.setIsEnum(true);

            // curr param name converted to camelCase
            List<String> names = param.getNamesList();
            if (names != null && names.size() > 0) {
                String modifiedName = StringUtils.convertSnakeCaseToCamelCase(names.get(0));
                names.remove(0);
                names.add(0, modifiedName);
            }
        }
    }


    public synchronized void close() {
        if (shutdown) {
            // already shutdown
            return;
        }
        shutdown = true;
        logger.warn("Closing session instance: " + executionSession.getPath());
        publishEvent(ScanEventType.ENDED);
        try {
            if (zipConsumer != null) {
                zipConsumer.close();
            }
        } catch (Exception e) {
            logger.error("Failed to close database pipe", e);
            InsidiousNotification.notifyMessage("Failed to close database pipe: " + e.getMessage(),
                    NotificationType.ERROR);
        }
        try {
            if (executorPool != null && !executorPool.isShutdown()) {
                executorPool.shutdownNow();
            }
        } catch (Exception e) {
            logger.error("Failed to close executor pool", e);
            InsidiousNotification.notifyMessage("Failed to close executor pool: " + e.getMessage(),
                    NotificationType.ERROR);
        }
        try {
            daoService.close();
        } catch (Exception e) {
            logger.error("Failed to close database", e);
            InsidiousNotification.notifyMessage("Failed to close database: " + e.getMessage(),
                    NotificationType.ERROR);
        }
        if (classInfoIndex != null) {
            classInfoIndex.close();
        }
        if (probeInfoIndex != null) {
            probeInfoIndex.close();
        }
        if (methodInfoIndex != null) {
            methodInfoIndex.close();
        }
        if (typeInfoIndex != null) {
            typeInfoIndex.close();
        }
        if (objectInfoIndex != null) {
            objectInfoIndex.close();
        }
        if (parameterContainer != null) {
            parameterContainer.close();
        }
        if (archiveIndex != null) {
            archiveIndex.close();
            archiveIndex = null;
        }
    }

    public void addTestCandidateListener(NewTestCandidateIdentifiedListener testCandidateListener) {
        this.testCandidateListener.add(testCandidateListener);
    }

    public void removeTestCandidateListener(NewTestCandidateIdentifiedListener testCandidateListener) {
        this.testCandidateListener.remove(testCandidateListener);
    }

    public Map<String, ClassInfo> getClassIndex() {
        return classInfoIndexByName;
    }


    public void getTopLevelTestCandidates(Consumer<List<TestCandidateMetadata>> testCandidateReceiver, long afterEventId) {


        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {

                int page = 0;
                int limit = 50;
                int count = 0;
                while (true) {
                    if (shutdown) {
                        return;
                    }
                    List<TestCandidateMetadata> testCandidateMetadataList = daoService
                            .getTopLevelTestCandidatePaginated(afterEventId, page, limit);
                    testCandidateReceiver.accept(testCandidateMetadataList);
                    count += testCandidateMetadataList.size();
                    page++;
                    if (testCandidateMetadataList.size() < limit || count > 100) {
                        break;
                    }
                }

            } catch (SQLException e) {
                // failed to load candidates hmm
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });


    }

    public AtomicInteger getTestCandidates(Consumer<List<TestCandidateMetadata>> testCandidateReceiver, long afterEventId) {

        AtomicInteger cdl = new AtomicInteger(1);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {


                int page = 0;
                int limit = 50;
                int count = 0;
                int attempt = 0;
                long currentAfterEventId = afterEventId;
                while (true) {
                    attempt++;
                    if (cdl.get() == 0) {
                        logger.warn(
                                "shutting down query started at [" + afterEventId + "] currently at item [" + count +
                                        "] => [" + currentAfterEventId + "] attempt [" + attempt + "]");
                        return;
                    }
                    List<TestCandidateMetadata> testCandidateMetadataList = daoService
                            .getTestCandidatePaginated(currentAfterEventId, 0, limit);
                    if (testCandidateMetadataList.size() > 0) {
                        count += testCandidateMetadataList.size();
                        testCandidateReceiver.accept(testCandidateMetadataList);
                        currentAfterEventId =
                                testCandidateMetadataList.get(testCandidateMetadataList.size() - 1)
                                        .getEntryProbeIndex() + 1;
                    }
                    if (testCandidateMetadataList.size() < limit) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            } catch (SQLException e) {
                // failed to load candidates hmm
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });

        return cdl;

    }

    public Project getProject() {
        return project;
    }

    public ClassMethodAggregates getClassMethodAggregates(String qualifiedName) {
        return daoService.getClassMethodCallAggregates(qualifiedName);
    }

    @Override
    public void run() {
        while (true) {
            try {
                scanLock.take();
                if (scanEnable && !isSessionCorrupted) {
                    scanDataAndBuildReplay();
                    publishEvent(ScanEventType.WAITING);
                }
            } catch (InterruptedException ie) {
//                ie.printStackTrace();
                logger.warn("scan checker interrupted");
//                return;
            } catch (Exception e) {
                logger.warn("scan checker interruption", e);
//                e.printStackTrace();
                //
            }
        }
    }

    public MethodDefinition getMethodDefinition(MethodUnderTest methodUnderTest1) {
        return daoService.getAllMethodDefinitionBySignature(methodUnderTest1.getClassName(),
                methodUnderTest1.getName(), methodUnderTest1.getSignature());
    }

    public int getMethodCallCountBetween(long start, long end) {
        return daoService.getCallCountBetween(start, end);
    }

    public void addSessionScanEventListener(SessionScanEventListener listener) {
        this.sessionScanEventListeners.add(listener);
        if (scanEnable) {
            listener.started();
        }
    }

    public List<TestCandidateMetadata> getTestCandidateBetween(long eventId, long eventId1) throws SQLException {
        return daoService.getTestCandidateBetween(eventId, eventId1);
    }

    public List<MethodCallExpression> getMethodCallsBetween(long start, long end) {
        return daoService.getCallsBetween(start, end);
    }

    public int getProcessedFileCount() {
        return daoService.getProcessedFileCount();
    }

    public int getTotalFileCount() {
        return daoService.getTotalFileCount();
    }

}
