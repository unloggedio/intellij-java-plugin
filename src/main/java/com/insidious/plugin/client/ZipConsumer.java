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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.insidious.plugin.Constants.PENDING;

class ZipConsumer implements Runnable {

    private static final Logger logger = LoggerUtil.getInstance(ZipConsumer.class);
    private final DaoService daoService;
    private final Map<String, ArchiveFile> archiveFileMap;
    private final File sessionDirectory;
    private Map<String, Long> checkedTimeMap = new HashMap<>();

    ZipConsumer(DaoService daoService, File sessionDirectory) throws IOException {
        this.daoService = daoService;
        this.sessionDirectory = sessionDirectory;
        this.archiveFileMap = daoService.getArchiveFileMap();
//        checkNewFiles();
    }

    private static int getThreadIdFromFileName(String archiveFile) {
        return Integer.parseInt(archiveFile.substring(archiveFile.lastIndexOf("-") + 1, archiveFile.lastIndexOf(".")));
    }

    @Override
    public void run() {
        logger.info("zip consumer started for path: " + sessionDirectory.getPath());
        while (true) {
            try {
                Thread.sleep(5000);
                checkNewFiles();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void checkNewFiles() throws IOException {
        List<File> sessionArchiveList = Arrays.stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .sorted(Comparator.comparing(File::getName))
                .filter(e -> e.getName()
                        .endsWith(".zip") && e.getName()
                        .startsWith("index-"))
                .collect(Collectors.toList());
        int total = sessionArchiveList.size();
        checkProgressIndicator("Loading thread data from logs", null);
        for (int i = 0; i < sessionArchiveList.size(); i++) {
            File sessionArchive = sessionArchiveList.get(i);
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


                checkProgressIndicator(null,
                        "Reading archive: " + sessionArchive.getName() + " [" + i + " / " + total + "]");
                logger.warn("Reading archive: " + sessionArchive.getName());

                dbEntry = new ArchiveFile();
                dbEntry.setName(sessionArchive.getName());
                dbEntry.setStatus(PENDING);
                archiveFileMap.put(sessionArchive.getName(), dbEntry);
                daoService.updateArchiveFile(dbEntry);


                for (String logFileName : logFilesNameList) {
                    LogFile logFile = new LogFile();
                    logFile.setName(logFileName);
                    logFile.setArchiveName(sessionArchive.getName());
                    logFile.setStatus(PENDING);
                    int threadId = getThreadIdFromFileName(logFileName);
                    logFile.setThreadId(threadId);
                    daoService.updateLogFile(logFile);
                }
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
            return List.of();
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
