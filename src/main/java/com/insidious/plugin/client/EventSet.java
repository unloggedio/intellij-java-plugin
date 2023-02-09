package com.insidious.plugin.client;

import com.insidious.common.parser.KaitaiInsidiousEventParser;
import com.insidious.plugin.pojo.dao.LogFile;

import java.util.List;

public class EventSet {
    private final List<KaitaiInsidiousEventParser.Block> events;
    private final LogFile logFile;

    public EventSet(LogFile logFile, List<KaitaiInsidiousEventParser.Block> events) {
        this.logFile = logFile;
        this.events = events;
    }

    public List<KaitaiInsidiousEventParser.Block> getEvents() {
        return events;
    }

    public LogFile getLogFile() {
        return logFile;
    }

}
