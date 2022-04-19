package com.insidious.plugin.client;

public enum DatFileType {

    WEAVE_DAT_FILE("class.weave.dat"),
    INDEX_TYPE_DAT_FILE("index.type.dat"),
    INDEX_OBJECT_DAT_FILE("index.object.dat"),
    INDEX_EVENTS_DAT_FILE("index.events.dat"),
    INDEX_STRING_DAT_FILE("index.string.dat");

    private final String fileName;

    DatFileType(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }


}
