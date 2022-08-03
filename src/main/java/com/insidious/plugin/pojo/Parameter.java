package com.insidious.plugin.pojo;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;

public class Parameter {
    Object value;
    String name;
    private int index;

    @Override
    public String toString() {
        return "Parameter{" +
                "value=" + value +
                ", name='" + name + '\'' +
                ", index=" + index +
                ", type='" + type + '\'' +
                ", probeInfo=" + probeInfo +
                ", prob=" + prob +
                '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    String type;
    private DataInfo probeInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    DataEventWithSessionId prob;


    public void setValue(Object value) {
        this.value = value;
    }

    public void setProb(DataEventWithSessionId prob) {
        this.prob = prob;
    }


    public Object getValue() {
        return value;
    }

    public DataEventWithSessionId getProb() {
        return prob;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }


    public void setProbeInfo(DataInfo probeInfo) {
        this.probeInfo = probeInfo;
    }

    public DataInfo getProbeInfo() {
        return probeInfo;
    }
}
