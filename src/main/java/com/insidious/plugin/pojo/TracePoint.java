package com.insidious.plugin.pojo;

import com.insidious.plugin.extension.model.DataInfo;
import com.insidious.plugin.extension.model.TypeInfo;
import com.insidious.plugin.network.pojo.ClassInfo;
import com.insidious.plugin.network.pojo.DataEventWithSessionId;
import com.insidious.plugin.network.pojo.DataResponse;
import com.insidious.plugin.network.pojo.ObjectInfo;

public class TracePoint {

    private final long recordedAt;
    long classId, linenum, dataId, threadId, value;
    String executionSessionId;
    String filename;
    String classname;
    String exceptionClass;
    private long nanoTime;

    public TracePoint(long classId, long linenum,
                      long dataId,
                      long threadId,
                      long value,
                      String executionSessionId,
                      String filename,
                      String classname,
                      String exceptionClass,
                      long recordedAt,
                      long nanoTime
    ) {
        this.classId = classId;
        this.linenum = linenum;
        this.dataId = dataId;
        this.threadId = threadId;
        this.value = value;
        this.executionSessionId = executionSessionId;
        this.filename = filename;
        this.classname = classname;
        this.exceptionClass = exceptionClass;
        this.recordedAt = recordedAt;
        this.nanoTime = nanoTime;
    }

    public static TracePoint fromDataEvent(DataEventWithSessionId dataEvent, DataResponse<DataEventWithSessionId> traceResponse) {

        DataInfo dataInfoObject = traceResponse.getDataInfo(String.valueOf(dataEvent.getDataId()));
        if (dataInfoObject != null) {

            ClassInfo classInfo = traceResponse.getClassInfo(String.valueOf(dataInfoObject.getClassId()));

            ObjectInfo errorKeyValueJson = traceResponse.getObjectInfo(String.valueOf(dataEvent.getValue()));
            long exceptionType = errorKeyValueJson.getTypeId();
            TypeInfo exceptionClassJson = traceResponse.getTypeInfo(String.valueOf(exceptionType));
            String exceptionClass = "";
            if (exceptionClassJson != null) {
                exceptionClass = exceptionClassJson.getTypeNameFromClass();
            }
            switch (dataInfoObject.getEventType()) {
                case LABEL:
                case CATCH:
                    return null;
//                case METHOD_EXCEPTIONAL_EXIT:
//                case METHOD_THROW:
            }
            if (dataInfoObject.getAttribute("Type", "").length() == 1) {
                return null;
            }


            return new TracePoint(dataInfoObject.getClassId(),
                    dataInfoObject.getLine(),
                    dataEvent.getDataId(),
                    dataEvent.getThreadId(),
                    dataEvent.getValue(),
                    dataEvent.getExecutionSessionId(),
                    classInfo.getFilename(),
                    classInfo.getClassName(),
                    exceptionClass,
                    dataEvent.getRecordedAt().getTime(),
                    dataEvent.getNanoTime());

        }

        return null;


    }

    public long getClassId() {
        return classId;
    }

    public long getLinenum() {
        return linenum;
    }

    public long getDataId() {
        return dataId;
    }

    public long getThreadId() {
        return threadId;
    }

    public long getValue() {
        return value;
    }

    public String getExecutionSessionId() {
        return executionSessionId;
    }

    public void setExecutionSessionId(String executionSessionId) {
        this.executionSessionId = executionSessionId;
    }

    public String getFilename() {
        return filename;
    }

    public String getClassname() {
        return classname;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public long getRecordedAt() {
        return recordedAt;
    }

    public long getNanoTime() {
        return nanoTime;
    }

    public void setNanoTime(long nanoTime) {
        this.nanoTime = nanoTime;
    }
}