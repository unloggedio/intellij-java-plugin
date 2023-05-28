package com.insidious.plugin.pojo;

import com.insidious.common.FilteredDataEventsRequest;
import com.insidious.common.PageInfo;
import com.insidious.common.Util;
import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.ObjectInfo;
import com.insidious.common.weaver.TypeInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;

import java.util.Collections;

public class TracePoint {

    private static final byte ATTRIBUTE_SEPARATOR = ',';
    private static final byte ATTRIBUTE_KEYVALUE_SEPARATOR = '=';
    private long recordedAt;
    private long classId, lineNumber, threadId, matchedValueId;
    private long dataId;
    private String filename;
    private String classname;
    private String exceptionClass;
    private long nanoTime;
    private ExecutionSession executionSession;

    public TracePoint() {
    }

    public TracePoint(long classId, long lineNumber,
                      long dataId,
                      long threadId,
                      long matchedValueId,
                      String filename,
                      String classname,
                      String exceptionClass,
                      long recordedAt,
                      long nanoTime
    ) {
        this.classId = classId;
        this.lineNumber = lineNumber;
        this.dataId = dataId;
        this.threadId = threadId;
        this.matchedValueId = matchedValueId;
        this.filename = filename;
        this.classname = classname;
        this.exceptionClass = exceptionClass;
        this.recordedAt = recordedAt;
        this.nanoTime = nanoTime;
    }

    public static TracePoint fromDataEvent(DataEventWithSessionId dataEvent, DataResponse<DataEventWithSessionId> traceResponse) {

        DataInfo dataInfoObject = traceResponse.getDataInfo(String.valueOf(dataEvent.getProbeId()));
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
            }
            if (Util.getAttribute(dataInfoObject.getAttributes(), "Type", "").length() == 1) {
                return null;
            }


            TracePoint tracePoint = new TracePoint(dataInfoObject.getClassId(),
                    dataInfoObject.getLine(),
                    dataEvent.getProbeId(),
                    dataEvent.getThreadId(),
                    dataEvent.getValue(),
                    classInfo.getFilename(),
                    classInfo.getClassName(),
                    exceptionClass,
                    dataEvent.getRecordedAt(),
                    dataEvent.getEventId());
            ExecutionSession executionSession1 = new ExecutionSession();
            executionSession1.setSessionId(dataEvent.getSessionId());
            tracePoint.setExecutionSession(executionSession1);
            return tracePoint;

        }

        return null;


    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TracePoint that = (TracePoint) o;

        if (recordedAt != that.recordedAt) return false;
        if (classId != that.classId) return false;
        if (lineNumber != that.lineNumber) return false;
        if (threadId != that.threadId) return false;
        if (matchedValueId != that.matchedValueId) return false;
        if (dataId != that.dataId) return false;
        if (nanoTime != that.nanoTime) return false;
        if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
        if (!classname.equals(that.classname)) return false;
        return exceptionClass != null ? exceptionClass.equals(that.exceptionClass) : that.exceptionClass == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (recordedAt ^ (recordedAt >>> 32));
        result = 31 * result + (int) (classId ^ (classId >>> 32));
        result = 31 * result + (int) (lineNumber ^ (lineNumber >>> 32));
        result = 31 * result + (int) (threadId ^ (threadId >>> 32));
        result = 31 * result + (int) (matchedValueId ^ (matchedValueId >>> 32));
        result = 31 * result + (int) (dataId ^ (dataId >>> 32));
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + classname.hashCode();
        result = 31 * result + (exceptionClass != null ? exceptionClass.hashCode() : 0);
        result = 31 * result + (int) (nanoTime ^ (nanoTime >>> 32));
        return result;
    }

    public FilteredDataEventsRequest toFilterDataEventRequest() {
        FilteredDataEventsRequest filteredDataEventsRequest = new FilteredDataEventsRequest();
        filteredDataEventsRequest.setSessionId(this.getExecutionSession().getSessionId());
        filteredDataEventsRequest.setProbeId((int)this.getDataId());
        filteredDataEventsRequest.setThreadId(this.getThreadId());
        filteredDataEventsRequest.setNanotime(this.getRecordedAt());
        filteredDataEventsRequest.setValueId(Collections.singletonList(this.getMatchedValueId()));
        filteredDataEventsRequest.setPageInfo(new PageInfo(0, 50000, PageInfo.Order.DESC));
        filteredDataEventsRequest.setDebugPoints(Collections.emptyList());
        return filteredDataEventsRequest;
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

    public long getLineNumber() {
        return lineNumber;
    }

    public long getDataId() {
        return dataId;
    }

    public long getThreadId() {
        return threadId;
    }

    public long getMatchedValueId() {
        return matchedValueId;
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

    public void setExecutionSession(ExecutionSession executionSession) {
        this.executionSession = executionSession;
    }

    public ExecutionSession getExecutionSession() {
        return executionSession;
    }
}
