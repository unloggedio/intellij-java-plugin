package com.insidious.plugin.pojo;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;

/**
 * Parameter is a value (long id or string) with a name and type information (class name). It could
 * be a variable passed as a method argument, or the
 * test subject or the return value from the function. Store the corresponding probeInfo and
 * event also from where the information was identified
 */
public class Parameter {
    /**
     * Value is either a long number or a string value if the value was actually a Ljava/lang/String
     */
    Object value;
    /**
     * name should be a valid java variable name. this will be used inside the generated test cases
     */
    String name;
    String type;
    DataEventWithSessionId prob;
    private int index;
    private DataInfo probeInfo;
    private ConstructorType constructorType;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public DataEventWithSessionId getProb() {
        return prob;
    }

    public void setProb(DataEventWithSessionId prob) {
        this.prob = prob;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public DataInfo getProbeInfo() {
        return probeInfo;
    }

    public void setProbeInfo(DataInfo probeInfo) {
        this.probeInfo = probeInfo;
    }

    public ConstructorType getConstructorType() {
        return constructorType;
    }

    public void setConstructorType(ConstructorType constructorType) {
        this.constructorType = constructorType;
    }
}
