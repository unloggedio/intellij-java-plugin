package com.insidious.plugin.autoexecutor;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.management.OperatingSystemMXBean;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AutoExecutionConsumer implements Runnable {

    private boolean consuming = false;
    private InsidiousService insidiousService;
    private AutoExecutionRecordQueue queue;
    public long addcounts = 0;
    private static final Logger logger = LoggerUtil.getInstance(AutoExecutionConsumer.class);
    private String source;

    public AutoExecutionConsumer(InsidiousService insidiousService, AutoExecutionRecordQueue queue) {
        this.insidiousService = insidiousService;
        this.queue = queue;
    }

    @Override
    public void run() {
        consuming = true;
        consume();
    }

    public void stop() {
        consuming = false;
        queue.notifyIsNotEmpty();
    }

    private void consume() {
        while (consuming) {
            if (queue.isEmpty()) {
                try {
                    queue.waitIsNotEmpty();
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (!consuming) {
                break;
            }
            addcounts++;
//            System.out.println("[P-Autex] " + source + " Consumer adding record : " + addcounts);
            AutoExecutorReportRecord record = queue.poll();
            insidiousService.addExecutionRecord(record);
        }
    }

    public void setSource(String source) {
        this.source = source;
    }
}
