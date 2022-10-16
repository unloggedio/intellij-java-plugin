package com.insidious.plugin.pojo.dao;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.jetbrains.annotations.NotNull;

@DatabaseTable(tableName = "log_file")
public class LogFile implements Comparable<LogFile> {

    @DatabaseField(id = true)
    String name;
    @DatabaseField(canBeNull = false)
    String archiveName;
    @DatabaseField
    String status;

    public LogFile() {
    }

    public LogFile(String name, String archiveName, String status) {
        this.name = name;
        this.archiveName = archiveName;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int compareTo(@NotNull LogFile o) {
        return this.name.compareTo(o.name);
    }
}
