package com.insidious.plugin.client;

import com.insidious.plugin.pojo.dao.ArchiveFile;
import com.insidious.plugin.pojo.dao.LogFile;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.insidious.plugin.Constants.PENDING;

class ZipConsumer implements Runnable {

    private static final Logger logger = LoggerUtil.getInstance(ZipConsumer.class);
    private final DaoService daoService;
    private final Map<String, ArchiveFile> archiveFileMap;
    private final File sessionDirectory;
    private final SessionInstance sessionInstance;
    private final Map<String, LogFile> existingLogFilesMap;
    private final HashSet<String> checkedArchivesList;
    private Map<String, Long> checkedTimeMap = new HashMap<>();
    private AtomicBoolean isChecking = new AtomicBoolean(false);
    private boolean stop;

    ZipConsumer(DaoService daoService, File sessionDirectory, SessionInstance sessionInstance) {
        this.daoService = daoService;
        this.sessionInstance = sessionInstance;
        this.sessionDirectory = sessionDirectory;
        this.archiveFileMap = daoService.getArchiveFileMap();
        existingLogFilesMap = daoService.getLogFiles().stream().collect(Collectors.toMap(LogFile::getName, e -> e));
        checkedArchivesList = new HashSet<String>();
        for (LogFile value : existingLogFilesMap.values()) {
            checkedArchivesList.add(value.getArchiveName());
        }

    }

    private static int getThreadIdFromFileName(String archiveFile) {
        return Integer.parseInt(archiveFile.substring(archiveFile.lastIndexOf("-") + 1, archiveFile.lastIndexOf(".")));
    }

    @Override
    public void run() {
        logger.warn("zip consumer started for path: " + sessionDirectory.getPath());
        while (true) {
            if (stop) {
                break;
            }
            try {
                Thread.sleep(1000);
                checkNewFiles();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void close() {
        stop = true;
    }

    public void checkNewFiles() throws IOException {
        if (!isChecking.compareAndSet(false, true)) {
            return;
        }
        try {
            List<File> sessionArchiveList = Arrays.stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                    .sorted(Comparator.comparing(File::getName))
                    .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                    .collect(Collectors.toList());
            int total = sessionArchiveList.size();
            checkProgressIndicator("Loading thread data from logs", null);
            boolean hasNewFiles = false;
            for (int i = 0; i < sessionArchiveList.size(); i++) {
                File sessionArchive = sessionArchiveList.get(i);
                if (checkedArchivesList.contains(sessionArchive.getName())) {
                    continue;
                }
                ArchiveFile dbEntry = archiveFileMap.get(sessionArchive.getName());
                if (dbEntry == null) {

                    List<String> logFilesNameList;
                    try {
                        logFilesNameList = listArchiveFiles(sessionArchive);
                    } catch (Exception e) {
                        // probably an incomplete zip file
                        // we will read it later when it is complete
                        continue;
                    }
                    if (logFilesNameList.size() == 0) {
                        // probably an incomplete zip file
                        // we will read it later when it is complete
                        continue;
                    }


//                checkProgressIndicator(null,
//                        "Reading archive: " + sessionArchive.getName() + " [" + i + " / " + total + "]");
                    logger.warn("Reading archive: " + sessionArchive.getName());

                    dbEntry = new ArchiveFile();
                    dbEntry.setName(sessionArchive.getName());
                    dbEntry.setStatus(PENDING);
                    archiveFileMap.put(sessionArchive.getName(), dbEntry);
                    daoService.createArchiveFileEntry(dbEntry);


                    String archiveName = sessionArchive.getName();
                    for (String logFileName : logFilesNameList) {
                        LogFile logFile = new LogFile(logFileName, archiveName, PENDING);
                        int threadId = getThreadIdFromFileName(logFileName);
                        logFile.setThreadId(threadId);
                        daoService.createLogFileEntry(logFile);
                        existingLogFilesMap.put(logFile.getName(), logFile);
                        hasNewFiles = true;
                    }
                }
                checkedArchivesList.add(sessionArchive.getName());
            }
            if (hasNewFiles) {
                sessionInstance.scanDataAndBuildReplay();
            }
        } finally {
            boolean wasSetToFalse = isChecking.compareAndSet(true, false);
            if (!wasSetToFalse) {
                logger.warn("existing value eas not [true] for is checking");
            }
        }

    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .isCanceled()) {
                throw new ProcessCanceledException();
            }
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


    private List<String> listArchiveFiles(File sessionFile) throws IOException {

        long lastModified = sessionFile.lastModified();
        Long lastCheckedTime = checkedTimeMap.getOrDefault(sessionFile.getAbsolutePath(), 0L);
        if (lastCheckedTime >= lastModified) {
            return Collections.emptyList();
        }
        logger.info(
                "open archive [" + sessionFile + "] last modified=[" + lastModified + "], lastChecked = [" + lastCheckedTime + "]");
        checkedTimeMap.put(sessionFile.getAbsolutePath(), lastModified);

        List<String> files = new LinkedList<>();

        try (ZipInputStream indexArchive = new ZipInputStream(new FileInputStream(sessionFile))) {
            ZipEntry entry;
            while ((entry = indexArchive.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (!entryName.contains("@")) {
                    continue;
                }
                indexArchive.closeEntry();
                files.add(entryName.split("@")[1]);
            }
        }
        Collections.sort(files);
        return files;
    }


}
