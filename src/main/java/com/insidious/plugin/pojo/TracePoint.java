package com.insidious.plugin.pojo;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.Util;
import com.insidious.common.parser.KaitaiInsidiousClassWeaveParser;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.pojo.ClassInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ObjectInfo;

import java.util.Collections;

public class TracePoint {

    private static final byte ATTRIBUTE_SEPARATOR = ',';
    private static final byte ATTRIBUTE_KEYVALUE_SEPARATOR = '=';
    private final long recordedAt;
    long classId, linenum, threadId, value;
    int dataId;
    String executionSessionId;
    String filename;
    String classname;
    String exceptionClass;
    private long nanoTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TracePoint that = (TracePoint) o;

        if (recordedAt != that.recordedAt) return false;
        if (classId != that.classId) return false;
        if (linenum != that.linenum) return false;
        if (threadId != that.threadId) return false;
        if (value != that.value) return false;
        if (dataId != that.dataId) return false;
        if (nanoTime != that.nanoTime) return false;
        if (!executionSessionId.equals(that.executionSessionId)) return false;
        if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
        if (!classname.equals(that.classname)) return false;
        return exceptionClass != null ? exceptionClass.equals(that.exceptionClass) : that.exceptionClass == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (recordedAt ^ (recordedAt >>> 32));
        result = 31 * result + (int) (classId ^ (classId >>> 32));
        result = 31 * result + (int) (linenum ^ (linenum >>> 32));
        result = 31 * result + (int) (threadId ^ (threadId >>> 32));
        result = 31 * result + (int) (value ^ (value >>> 32));
        result = 31 * result + dataId;
        result = 31 * result + executionSessionId.hashCode();
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + classname.hashCode();
        result = 31 * result + (exceptionClass != null ? exceptionClass.hashCode() : 0);
        result = 31 * result + (int) (nanoTime ^ (nanoTime >>> 32));
        return result;
    }

    public TracePoint(long classId, long linenum,
                      int dataId,
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


    public FilteredDataEventsRequest toFilterDataEventRequest() {
        FilteredDataEventsRequest filteredDataEventsRequest = new FilteredDataEventsRequest();
        filteredDataEventsRequest.setSessionId(this.getExecutionSessionId());
        filteredDataEventsRequest.setProbeId(this.getDataId());
        filteredDataEventsRequest.setThreadId(this.getThreadId());
        filteredDataEventsRequest.setNanotime(this.getNanoTime());
        filteredDataEventsRequest.setValueId(Collections.singletonList(this.getValue()));
        filteredDataEventsRequest.setPageSize(200);
        filteredDataEventsRequest.setPageNumber(0);
        filteredDataEventsRequest.setDebugPoints(Collections.emptyList());
        filteredDataEventsRequest.setSortOrder("DESC");
        return filteredDataEventsRequest;
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
            if (Util.getAttribute(dataInfoObject.getAttributes(), "Type", "").length() == 1) {
                return null;
            }


            return new TracePoint(dataInfoObject.getClassId(),
                    dataInfoObject.getLine(),
                    dataEvent.getDataId(),
                    dataEvent.getThreadId(),
                    dataEvent.getValue(),
                    dataEvent.getSessionId(),
                    classInfo.getFilename(),
                    classInfo.getClassName(),
                    exceptionClass,
                    dataEvent.getRecordedAt().getTime(),
                    dataEvent.getNanoTime());

        }

        return null;


    }


    /**
     * Access a particular attribute of the instruction, assuming the "KEY=VALUE" format.
     *
     * @param key          specifies an attribute key
     * @param defaultValue is returned if the key is unavailable.
     * @return the value corresponding to the key.
     */
    public String getAttribute(String attributes, String key, String defaultValue) {
        int index = attributes.indexOf(key);
        while (index >= 0) {
            if (index == 0 || attributes.charAt(index - 1) == ATTRIBUTE_SEPARATOR) {
                int keyEndIndex = attributes.indexOf(ATTRIBUTE_KEYVALUE_SEPARATOR, index);
                if (keyEndIndex == index + key.length()) {
                    int valueEndIndex = attributes.indexOf(ATTRIBUTE_SEPARATOR, keyEndIndex);
                    if (valueEndIndex > keyEndIndex) {
                        return attributes.substring(index + key.length() + 1, valueEndIndex);
                    } else {
                        return attributes.substring(index + key.length() + 1);
                    }
                }
            }
            index = attributes.indexOf(key, index + 1);
        }
        return defaultValue;
    }

    public long getClassId() {
        return classId;
    }

    public long getLinenum() {
        return linenum;
    }

    public int getDataId() {
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
