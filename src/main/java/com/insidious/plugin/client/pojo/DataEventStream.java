package com.insidious.plugin.client.pojo;

public class DataEventStream {
    byte[] stream;

    public DataEventStream() {
    }

    public DataEventStream(byte[] stream) {
        this.stream = stream;
    }

    public byte[] getStream() {
        return stream;
    }

    public void setStream(byte[] stream) {
        this.stream = stream;
    }
}
