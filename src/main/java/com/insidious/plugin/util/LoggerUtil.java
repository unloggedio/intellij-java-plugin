package com.insidious.plugin.util;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggerRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;

public class LoggerUtil {

    private static String logFilePath;

    static {
//        LoggerRepository repository = LogManager.getLoggerRepository();
//        org.apache.log4j.Logger insidiousLogger = repository.getLogger("com.insidious");
//        PatternLayout layout = new PatternLayout("%d [%7r] %6p - %30.30c - %m \n");
//
//        try {
//            String strTmp = System.getProperty("java.io.tmpdir");
//            String projectName = "videobug";
//            logFilePath = strTmp + FileSystems.getDefault().getSeparator() + "insidious-" + projectName + ".log";
//
//            RollingFileAppender insidiousAppender = new RollingFileAppender(layout, logFilePath, true);
//            insidiousAppender.setEncoding(StandardCharsets.UTF_8.name());
//            insidiousAppender.setMaxBackupIndex(12);
//            insidiousAppender.setMaximumFileSize(10000000L);
//            LevelRangeFilter filter = new LevelRangeFilter();
//            filter.setLevelMin(Level.DEBUG);
//            insidiousAppender.addFilter(filter);
//            insidiousLogger.addAppender(insidiousAppender);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public static String getLogFilePath() {
        return logFilePath;
    }

    public static Logger getInstance(@NotNull Class<?> cl) {
        return LoggerFactory.getLogger(cl);
    }

    public static Logger getInstance(String className) {
        return LoggerFactory.getLogger(className);
    }
}
