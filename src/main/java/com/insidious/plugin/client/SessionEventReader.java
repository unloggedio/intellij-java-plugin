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
    private SessionInstance sessionInstance;

    public SessionEventReader(ExecutionSession executionSession,
                              List<LogFile> logFilesToRead,
                              Map<String, String> cacheEntries,
                              int eventBufferSize,
                              SessionInstance sessionInstance) {
        this.executionSession = executionSession;
        this.sessionInstance = sessionInstance;
        this.cacheEntries = cacheEntries;
        this.logFilesToRead = new ArrayList<>(logFilesToRead);
        this.eventBufferSize = eventBufferSize * 3;
        this.sessionDirectory = FileSystems.getDefault()
                .getPath(executionSession.getPath())
                .toFile();

    }

    @Override
    public void run() {
        long startTime = new Date().getTime();
        int totalFilesToRead = logFilesToRead.size();
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
                logger.warn("Reading data from file [" + logFile.getName() + "] => " + eventsFromFileOld.size() + " events ");

                currentBufferSize += eventsFromFileOld.size();
                eventSetQueue.put(eventSetFromLogFile);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        long endTime = new Date().getTime();

        logger.warn("Finished reading events [" + totalFilesToRead + "] log files in [" + (endTime - startTime) + " " +
                "ms ]");
    }

    public EventSet getNextEventSet() throws InterruptedException {
        EventSet take = eventSetQueue.take();
        currentBufferSize -= take.getEvents().size();
        return take;
    }

    private List<KaitaiInsidiousEventParser.Block> getEventsFromFileOld(File sessionArchive, String archiveFile) throws IOException {
        long start = new Date().getTime();
//        logger.warn("Read events from file: " + archiveFile);
        NameWithBytes nameWithBytes = sessionInstance.createFileOnDiskFromSessionArchiveFile(sessionArchive,
                archiveFile);
        assert nameWithBytes != null;
        KaitaiInsidiousEventParser eventsContainer =
                new KaitaiInsidiousEventParser(new ByteBufferKaitaiStream(nameWithBytes.getBytes()));
        ArrayList<KaitaiInsidiousEventParser.Block> events = eventsContainer.event()
                .entries();
        int count = events.size();
        long end = new Date().getTime();
        long timeInMs = end - start;
        if (timeInMs > 100) {
            logger.warn("Read events[" + count + "] took: " + timeInMs + " ms" + " from [" + archiveFile + "]");
        }
        return events;
    }



}
