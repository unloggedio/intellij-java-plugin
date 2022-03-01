package com.insidious.plugin.util;

import com.intellij.openapi.application.PathManager;
import org.apache.log4j.*;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.varia.LevelRangeFilter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LoggerUtil {

    static {
        LoggerRepository repository = LogManager.getLoggerRepository();
        org.apache.log4j.Logger insidiousLogger = repository.getLogger("com.insidious");
        PatternLayout layout = new PatternLayout("%d [%7r] %6p - %30.30c - %m \n");

        try {
            String strTmp = System.getProperty("java.io.tmpdir");
            String logFilePath = strTmp + "/insidious.log";
            System.out.println("Logging to file - " + logFilePath);
            RollingFileAppender insidiousAppender = new RollingFileAppender((Layout)layout, logFilePath, true);
            insidiousAppender.setEncoding(StandardCharsets.UTF_8.name());
            insidiousAppender.setMaxBackupIndex(12);
            insidiousAppender.setMaximumFileSize(10000000L);
            LevelRangeFilter filter = new LevelRangeFilter();
            filter.setLevelMin(Level.DEBUG);
            insidiousAppender.addFilter((Filter)filter);
            insidiousLogger.addAppender((Appender)insidiousAppender);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static {
//        @NotNull Logger baseLogger = LoggerFactory.getLoggerInstance("#com.insidious");
    }

    public static Logger getInstance(@NotNull Class<?> cl) {
        return LoggerFactory.getLogger(cl);
    }

    public static Logger getInstance(String className) {
        return LoggerFactory.getLogger(className);
    }
}
