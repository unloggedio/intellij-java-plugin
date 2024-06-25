package com.insidious.plugin.client.pojo;

public class NameWithBytes {
    private final String cacheFileLocation;
    private String name;
    private byte[] bytes;


    public NameWithBytes(String name, byte[] bytes, String cacheFileLocation) {
        this.name = name;
        this.bytes = bytes;
        this.cacheFileLocation = cacheFileLocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCacheFileLocation() {
        return cacheFileLocation;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

}
