package pojo;

public class Bugs {

    public Bugs(long classId, long linenum, long dataId, long threadId, long value, String executionSessionId, String filename, String classname) {
        this.classId = classId;
        this.linenum = linenum;
        this.dataId = dataId;
        this.threadId = threadId;
        this.value = value;
        this.executionSessionId = executionSessionId;
        this.filename = filename;
        this.classname = classname;
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

    public String getFilename() {
        return filename;
    }

    public String getClassname() {
        return classname;
    }

    long classId, linenum, dataId, threadId, value;
    String executionSessionId,filename,classname;

}
