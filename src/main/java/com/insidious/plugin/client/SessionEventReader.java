package com.insidious.plugin.client;

import com.insidious.common.parser.KaitaiInsidiousEventParser;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.NameWithBytes;
import com.insidious.plugin.pojo.dao.LogFile;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import io.kaitai.struct.ByteBufferKaitaiStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SessionEventReader implements Runnable {
    private final static Logger logger = LoggerUtil.getInstance(SessionEventReader.class);
    private final List<LogFile> logFilesToRead;
    private final ExecutionSession executionSession;
    private final File sessionDirectory;
    private final Map<String, String> cacheEntries;
    private final ArrayBlockingQueue<EventSet> eventSetQueue = new ArrayBlockingQueue<>(10);
    private final int eventBufferSize;
    private int currentBufferSize = 0;

    public SessionEventReader(ExecutionSession executionSession, List<LogFile> logFilesToRead, Map<String, String> cacheEntries, int eventBufferSize) {
        this.executionSession = executionSession;
        this.cacheEntries = cacheEntries;
        this.logFilesToRead = new ArrayList<>(logFilesToRead);
        this.eventBufferSize = eventBufferSize * 2;
        this.sessionDirectory = FileSystems.getDefault()
                .getPath(executionSession.getPath())
                .toFile();

    }

    @Override
    public void run() {
        while (logFilesToRead.size() > 0) {
            try {
                if (currentBufferSize > eventBufferSize) {
                    Thread.sleep(10);
                    continue;
                }
                LogFile logFile = logFilesToRead.remove(0);
                File sessionArchive = FileSystems.getDefault()
                        .getPath(executionSession.getPath(), logFile.getArchiveName())
                        .toFile();
                List<KaitaiInsidiousEventParser.Block> eventsFromFileOld = getEventsFromFileOld(sessionArchive,
                        logFile.getName());
                EventSet eventSetFromLogFile = new EventSet(logFile, eventsFromFileOld);
                currentBufferSize += eventsFromFileOld.size();
                eventSetQueue.put(eventSetFromLogFile);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean hasRemaining() {
        return logFilesToRead.size() > 0;
    }

    public EventSet getNextEventSet() throws InterruptedException {
        EventSet take = eventSetQueue.take();
        currentBufferSize -= take.getEvents().size();
        return take;
    }

    private List<KaitaiInsidiousEventParser.Block> getEventsFromFileOld(File sessionArchive, String archiveFile) throws IOException {
        long start = new Date().getTime();
//        logger.warn("Read events from file: " + archiveFile);
        NameWithBytes nameWithBytes = createFileOnDiskFromSessionArchiveFile(sessionArchive, archiveFile);
        assert nameWithBytes != null;
        KaitaiInsidiousEventParser eventsContainer =
                new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(nameWithBytes.getBytes()));
        ArrayList<KaitaiInsidiousEventParser.Block> events = eventsContainer.event()
                .entries();
        long end = new Date().getTime();
        long timeInMs = end - start;
        if (timeInMs > 5) {
            logger.warn("Read events took: " + timeInMs + " ms" + " from [" + archiveFile + "]");
        }
        return events;
    }

    private NameWithBytes createFileOnDiskFromSessionArchiveFile(File sessionFile, String pathName) {
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

            try (FileInputStream sessionFileInputStream = new FileInputStream(sessionFile)) {
                try (ZipInputStream indexArchive = new ZipInputStream(sessionFileInputStream)) {
                    ZipEntry entry;
                    while ((entry = indexArchive.getNextEntry()) != null) {
                        String entryName = entry.getName();
//                        logger.info(String.format("file entry in archive [%s] -> [%s]", sessionFile.getName(),
//                                entryName));
                        if (entryName.contains(pathName)) {
                            byte[] fileBytes = IOUtils.toByteArray(indexArchive);

                            File cacheFile = new File(cacheFileLocation);
                            FileUtils.writeByteArrayToFile(cacheFile, fileBytes);
                            indexArchive.closeEntry();
                            indexArchive.close();

                            cacheEntries.put(cacheKey, entryName);

                            NameWithBytes nameWithBytes = new NameWithBytes(entryName, fileBytes);
                            logger.info(
                                    pathName + " file from " + sessionFile.getName() + " is " + nameWithBytes.getBytes().length + " bytes");
                            return nameWithBytes;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to open zip archive: " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            logger.warn(
                    "failed to create file [" + pathName + "] on disk from" + " archive[" + sessionFile.getName() + "]");
            return null;
        }
        return null;
    }


}
