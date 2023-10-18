package com.insidious.plugin.factory;

import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.autoexecutor.AutoExecutorReportRecord;
import com.insidious.plugin.ui.methodscope.DiffResultType;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.intellij.notification.NotificationType;
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

public class ReportingService {

    private final InsidiousService insidiousService;
    public boolean notify = true;
    private boolean reportingEnabled = false;
    private String output_file_name;
    private int count = 1;

    public ReportingService(InsidiousService service) {
        this.insidiousService = service;
    }

    public boolean toggleReportMode() {
        this.reportingEnabled = !this.reportingEnabled;
        if (notify) {
            InsidiousNotification.notifyMessage("Reporting mode is now : " + (reportingEnabled ?
                    "ACTIVE" : "INACTIVE"), NotificationType.INFORMATION);
        }
        return this.reportingEnabled;
    }

    public boolean isReportingEnabled() {
        return reportingEnabled;
    }

    public void setReportingEnabled(boolean reportingEnabled) {
        this.reportingEnabled = reportingEnabled;
    }

    public void addRecord(AutoExecutorReportRecord autoExecutorReportRecord) {
        if (!this.reportingEnabled) {
            return;
        }
//        System.out.println("Execution record added count : " + count++);
        DifferenceResult result = autoExecutorReportRecord.getDifferenceResult();
//        System.out.println("Adding record [***] " + result.toString());
        boolean isException = false;
        boolean isAgentException = false;
        boolean pluginException = false;

        XSSFWorkbook workbook = getWorkbook();
        if (workbook == null) {
            System.out.println("Workbook is null : " + result.toString());
            return;
        }
        XSSFSheet sheet = workbook.getSheetAt(0);
        int rowNum = sheet.getLastRowNum() + 1;
        XSSFRow row = sheet.createRow(rowNum);

//        System.out.println("Report entry Start ----");
        AgentCommandRequest agentCommandRequest = result.getCommand();
        String classname = agentCommandRequest.getClassName();
//        System.out.println("Class : "+classname);

        Cell cell = row.createCell(0);
        cell.setCellValue(classname);

        String method = agentCommandRequest.getMethodName() + "\n" + agentCommandRequest.getMethodSignature();
//        System.out.println("Method : "+method);
        cell = row.createCell(1);
        cell.setCellValue(method);

        String executionMode = result.getExecutionMode().toString();
//        System.out.println("Execution mode : "+executionMode);
        cell = row.createCell(2);
        cell.setCellValue(executionMode);

        String executionStatus = "";
        if (result.getExecutionMode().equals(DifferenceResult.EXECUTION_MODE.DIRECT_INVOKE)) {
            try {
                if (result.getResponse().getResponseType().equals(ResponseType.NORMAL)) {
                    executionStatus = "NORMAL";
                } else {
                    executionStatus = "EXCEPTION";
                    isException = true;
                }
            } catch (Exception e) {
                //plugin exception
                pluginException = true;
            }
        } else {
//            System.out.println("Execution Status : "+result.getDiffResultType());
            executionStatus = result.getDiffResultType().toString();
            if (result.getDiffResultType().equals(DiffResultType.BOTH_EXCEPTION)
                    || result.getDiffResultType().equals(DiffResultType.ACTUAL_EXCEPTION)) {
                isException = true;
            }
        }
        cell = row.createCell(3);
        cell.setCellValue(executionStatus);

        List<String> input = getInputs(result.getCommand());
//        System.out.println("Input Serialized : "+input.toString());
        cell = row.createCell(4);
        cell.setCellValue(input.toString());

        String output;
        if (result.getResponse() == null || result.getResponse().getMethodReturnValue() == null) {
            output = null;
            if (result.getResponse().getTimestamp() == 0) {
                isException = true;
                pluginException = true;
            }
        } else {
            output = result.getResponse().getMethodReturnValue().toString();
        }
//        System.out.println("Output Serialized : "+output);
        if (isException) {
            if (result.getResponse().getResponseType().equals(ResponseType.FAILED)) {
                isAgentException = true;
            }
        }
        cell = row.createCell(5);
        cell.setCellValue(output);

        //make check for plugin exception
        String exceptionTrace = isException ? result.getResponse().getMethodReturnValue().toString() : "";
//        System.out.println("Exception trace : "+exceptionTrace);
        if (pluginException) {
            exceptionTrace = "plugin exception, no response";
        }
        cell = row.createCell(6);
        cell.setCellValue(exceptionTrace);

        String exceptionType = "";
        if (isException) {
            if (isAgentException) {
                exceptionType = "Agent Exception";
            } else if (pluginException) {
                exceptionType = "Plugin Exception";
            } else {
                exceptionType = "Expected Exception";
            }
        }
//        System.out.println("Exception type : "+exceptionType);
        cell = row.createCell(7);
        cell.setCellValue(exceptionType);

        long timestamp = result.getResponse().getTimestamp();
        String time = "";
        if (!pluginException) {
            time = convertTimestamp(timestamp);
        }
//        System.out.println("Timestamp : "+time);
        cell = row.createCell(8);
        cell.setCellValue(time);

        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                OperatingSystemMXBean.class);

        cell = row.createCell(9);
        cell.setCellValue(osBean.getProcessCpuLoad());

        MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        heapMemoryUsage.getUsed();

        cell = row.createCell(10);
        cell.setCellValue(heapMemoryUsage.getUsed() / 1000000);

        cell = row.createCell(11);
        cell.setCellValue(autoExecutorReportRecord.getScannedFileCount());

        cell = row.createCell(12);
        cell.setCellValue(autoExecutorReportRecord.getTotalFileCount());

        try {
            FileOutputStream out = new FileOutputStream(new File(insidiousService.getProject().getBasePath()
                    + "/" + this.output_file_name));
            workbook.write(out);
            out.close();
//            System.out.println("Execution record added [+] : " + result.toString());
        } catch (Exception e) {
            System.out.println("Exception writing record to file " + e);
            e.printStackTrace();
        }
    }

    private List<String> getInputs(AgentCommandRequest command) {
        List<String> types = command.getParameterTypes();
        List<String> inputList = new ArrayList<>();

        for (int i = 0; i < command.getMethodParameters().size(); i++) {
            inputList.add(types.get(i) + " : " + command.getMethodParameters().get(i) + "\n");
        }
        return inputList;
    }

    private XSSFWorkbook getWorkbook() {
        Date today = new Date();
        LocalDate now = LocalDate.now();
        String filename = insidiousService.getProject().getName() + "_" + now.getYear() + "_" + now.getMonth()
                + "_" + now.getDayOfMonth() + ".xlsx";
        this.output_file_name = filename;
        File file = new File(insidiousService.getProject().getBasePath() + "/" + filename);
        if (!file.exists()) {
            try {
                //create sheet
                XSSFWorkbook workbook = new XSSFWorkbook();

                XSSFSheet spreadsheet
                        = workbook.createSheet("Executions");
                XSSFRow row = spreadsheet.createRow(0);

                Cell cell = row.createCell(0);
                cell.setCellValue("Class");

                cell = row.createCell(1);
                cell.setCellValue("Method");

                cell = row.createCell(2);
                cell.setCellValue("Execution mode");

                cell = row.createCell(3);
                cell.setCellValue("Execution status");

                cell = row.createCell(4);
                cell.setCellValue("Serialized Input");

                cell = row.createCell(5);
                cell.setCellValue("Serialized Output");

                cell = row.createCell(6);
                cell.setCellValue("Exception Trace");

                cell = row.createCell(7);
                cell.setCellValue("Exception Type");

                cell = row.createCell(8);
                cell.setCellValue("Timestamp");

                cell = row.createCell(9);
                cell.setCellValue("Cpu usage");

                cell = row.createCell(10);
                cell.setCellValue("Memory used (MB)");

                cell = row.createCell(11);
                cell.setCellValue("Number of files scanned");

                cell = row.createCell(12);
                cell.setCellValue("Total number of files");

                FileOutputStream out = new FileOutputStream(
                        new File(insidiousService.getProject().getBasePath() + "/" + filename));

                workbook.write(out);
                out.close();

                return workbook;
            } catch (Exception e) {
                System.out.println("Exception when creating excel file " + e);
                e.printStackTrace();
                return null;
            }
        } else {
            try {
                XSSFWorkbook workbook = new XSSFWorkbook(
                        new FileInputStream(insidiousService.getProject().getBasePath() + "/" + filename));
                return workbook;
            } catch (Exception e) {
                System.out.println("Exception opening existing excel file " + e);
                e.printStackTrace();
                return null;
            }
        }
    }

    private String convertTimestamp(long timestamp) {
        ZonedDateTime dateTime = Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Asia/Kolkata"));
        return dateTime.toString();
    }
}
