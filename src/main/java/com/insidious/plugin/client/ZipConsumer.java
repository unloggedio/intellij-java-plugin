package com.insidious.plugin.client;

import com.insidious.plugin.pojo.dao.ArchiveFile;
import com.insidious.plugin.pojo.dao.LogFile;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

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
    final DaoService daoService;
    final Map<String, ArchiveFile> archiveFileMap;
    private final File sessionDirectory;

    ZipConsumer(DaoService daoService, File sessionDirectory) throws IOException {
        this.daoService = daoService;
        this.sessionDirectory = sessionDirectory;
        this.archiveFileMap = daoService.getArchiveFileMap();
        checkNewFiles();
    }

    private static int getThreadIdFromFileName(String archiveFile) {
        return Integer.parseInt(archiveFile.substring(archiveFile.lastIndexOf("-") + 1, archiveFile.lastIndexOf(".")));
    }

    @Override
    public void run() {
        logger.info("zip consumer started for path: " + sessionDirectory.getPath());
        while (true) {
            try {
                Thread.sleep(500);
                checkNewFiles();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

    private void checkNewFiles() throws IOException {
        List<File> sessionArchiveList = Arrays.stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .sorted(Comparator.comparing(File::getName))
                .filter(e -> e.getName().endsWith(".zip") && e.getName().startsWith("index-"))
                .collect(Collectors.toList());
        for (File sessionArchive : sessionArchiveList) {
            ArchiveFile dbEntry = archiveFileMap.get(sessionArchive.getName());
            if (dbEntry == null) {
                dbEntry = new ArchiveFile();
                dbEntry.setName(sessionArchive.getName());
                dbEntry.setStatus(PENDING);
                archiveFileMap.put(sessionArchive.getName(), dbEntry);
                daoService.updateArchiveFile(dbEntry);

                List<String> logFilesNameList = listArchiveFiles(sessionArchive);

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

    private List<String> listArchiveFiles(File sessionFile) throws IOException {
        logger.warn("open archive [" + sessionFile + "]");
        List<String> files = new LinkedList<>();

        ZipInputStream indexArchive = new ZipInputStream(new FileInputStream(sessionFile));

        ZipEntry entry;
        while ((entry = indexArchive.getNextEntry()) != null) {
            String entryName = entry.getName();
            if (!entryName.contains("@")) {
                continue;
            }
            indexArchive.closeEntry();
            files.add(entryName.split("@")[1]);
        }
        indexArchive.close();
        Collections.sort(files);
        return files;
    }


}
