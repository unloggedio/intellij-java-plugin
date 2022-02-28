package network.pojo;


import com.fasterxml.jackson.annotation.JsonIgnore;
import extension.model.DataInfo;

import java.util.List;
import java.util.Scanner;

public class ClassInfo {
    private static final String SEPARATOR = ",";
    String sessionId;
    private int classId;
    @JsonIgnore
    private String container;
    private String filename;
    private String className;
    private List<DataInfo> dataInfoList;
    @JsonIgnore
    private LogLevel loglevel;
    @JsonIgnore
    private String hash;
    @JsonIgnore
    private String classLoaderIdentifier;

    public ClassInfo() {
    }

    /**
     * Create an instance to record the information.
     *
     * @param classId               specifies the ID assigned by the weaver.
     * @param container             is the name of a JAR file if the class is loaded from a JAR.
     * @param filename              is a class file name.
     * @param className             is a class name.
     * @param loglevel              is the level of the inserted logging code.
     * @param hash                  is a file hash of bytecode.
     * @param classLoaderIdentifier is a string representing a class loader that loaded the original class
     */
    public ClassInfo(int classId, String container, String filename, String className, LogLevel loglevel, String hash, String classLoaderIdentifier) {
        this.classId = classId;
        this.container = container;
        this.filename = filename;
        this.className = className;
        this.loglevel = loglevel;
        this.hash = hash;
        this.classLoaderIdentifier = classLoaderIdentifier;
    }

    /**
     * Create an instance from a string representation created by
     * ClassInfo.toString.
     *
     * @param s is the string representation.
     * @return an instance.
     */
    public static ClassInfo parse(String s) {
        Scanner sc = new Scanner(s);
        sc.useDelimiter(SEPARATOR);
        int classId = sc.nextInt();
        String container = sc.next();
        String filename = sc.next();
        String className = sc.next();
        LogLevel level = LogLevel.valueOf(sc.next());
        String hash = sc.next();
        String id = sc.next();
        sc.close();
        return new ClassInfo(classId, container, filename, className, level, hash, id);
    }

    public List<DataInfo> getDataInfoList() {
        return dataInfoList;
    }

    public void setDataInfoList(List<DataInfo> dataInfoList) {
        this.dataInfoList = dataInfoList;
    }

    public int getClassId() {
        return classId;
    }

    public String getContainer() {
        return container;
    }

    public String getFilename() {
        return filename;
    }

    public String getClassName() {
        return className;
    }

    public LogLevel getLoglevel() {
        return loglevel;
    }

    public String getHash() {
        return hash;
    }

    public String getClassLoaderIdentifier() {
        return classLoaderIdentifier;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * @return a string representation of the information.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(classId);
        buf.append(SEPARATOR);
        buf.append(container);
        buf.append(SEPARATOR);
        buf.append(filename);
        buf.append(SEPARATOR);
        buf.append(className);
        buf.append(SEPARATOR);
        buf.append(loglevel != null ? loglevel.name() : "");
        buf.append(SEPARATOR);
        buf.append(hash);
        buf.append(SEPARATOR);
        buf.append(classLoaderIdentifier);
        return buf.toString();
    }

}
