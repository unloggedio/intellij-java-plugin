package com.insidious.plugin.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parameter is a value (long id or string) with a name and type information (class name). It could
 * be a variable passed as a method argument, or the
 * test subject or the return value from the function. Store the corresponding probeInfo and
 * event also from where the information was identified
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Parameter implements Serializable, BytesMarshallable {
    public static final byte[] COMMA_CHARACTER = ",".getBytes();
    /**
     * Value is either a long number or a string value if the value was actually a Ljava/lang/String
     */
    long value = 0;
    /**
     * name should be a valid java variable name. this will be used inside the generated test cases
     */
    String type;
    boolean exception;
    DataEventWithSessionId prob;
    private List<String> names = new ArrayList<>();
    private String stringValue = null;
    private int index;
    private DataInfo dataInfo;
    private MethodCallExpression creatorExpression;
    private List<Parameter> templateMap = new ArrayList<>();
    private boolean isContainer = false;
    private boolean isEnum = false;

    public Parameter(Long value) {
        this.value = value;
    }

    public Parameter() {
    }

    public Parameter(Parameter original) {

        names = new ArrayList<>(original.names);

        List<Parameter> deepCopyTemplateMap = new ArrayList<>();
        for (Parameter p : original.templateMap) {
            deepCopyTemplateMap.add(new Parameter(p));
        }
        templateMap = deepCopyTemplateMap;


        type = original.type;
        isContainer = original.isContainer;
        dataInfo = original.dataInfo;
        value = original.value;
        prob = original.prob;
        isEnum = original.isEnum;
    }

    @Override
    public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        value = bytes.readLong();
        int typeLength = bytes.readInt();
        if (typeLength > 0) {
            byte[] typeBytes = new byte[typeLength];
            bytes.read(typeBytes);
            type = new String(typeBytes);
        } else {
            type = null;
        }
        boolean hasProbe = bytes.readBoolean();
        if (hasProbe) {
            prob = new DataEventWithSessionId();
            prob.readMarshallable(bytes);
        } else {
            prob = null;
        }
        int namesLength = bytes.readInt();
        if (namesLength > 0) {
            byte[] nameBytes = new byte[namesLength];
            bytes.read(nameBytes);
            names = new ArrayList<>(List.of(new String(nameBytes).split(",")));
        } else {
            names = new ArrayList<>();
        }
        boolean hasInfo = bytes.readBoolean();
        if (hasInfo) {
            dataInfo = new DataInfo();
            dataInfo.readMarshallable(bytes);
        } else {
            dataInfo = null;
        }
    }

    @Override
    public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
        bytes.writeLong(value);
        if (type == null) {
            bytes.writeInt(0);
        } else {
            bytes.writeInt(type.length());
            bytes.write(type.getBytes());
        }
        if (prob != null) {
            bytes.writeBoolean(true);
            prob.writeMarshallable(bytes);
        } else {
            bytes.writeBoolean(false);
        }

        int totalLength = 0;
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            totalLength += name.length();
            if (i > 0) {
                totalLength += 1;
            }
        }


        bytes.writeInt(totalLength);
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (i > 0) {
                bytes.write(COMMA_CHARACTER);
            }
            bytes.write(name.getBytes());
        }


        if (dataInfo != null) {
            bytes.writeBoolean(true);
            dataInfo.writeMarshallable(bytes);
        } else {
            bytes.writeBoolean(false);
        }


//        BytesMarshallable.super.writeMarshallable(bytes);
    }

    public boolean getIsEnum() {
        return isEnum;
    }

    public void setIsEnum(boolean isEnum) {
        this.isEnum = isEnum;
    }

    public boolean isException() {
        return exception;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public void setContainer(boolean container) {
        isContainer = container;
    }

    public List<Parameter> getTemplateMap() {
        return templateMap;
    }

    public void setTemplateMap(List<Parameter> transformedTemplateMap) {
        this.templateMap = transformedTemplateMap;
    }

    @Override
    public String toString() {
        return
                (names != null ? names.stream()
                        .findFirst()
                        .orElse("<n/a>") : "<n/a>") +
                        (type == null ? "</na>" : " = new " + (type != null ?
                                type.substring(type.lastIndexOf('.') + 1) : "naType") + "(); // ") +
                        "{" + "value=" + value +
                        ", index=" + index +
                        ", probeInfo=" + dataInfo +
                        ", prob=" + prob +
                        '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null) {
            return;
        }
        if (this.type == null
                || this.type.endsWith(".Object")
                || this.type.endsWith("$1")
                || this.type.endsWith("$2")
                || this.type.endsWith("$3")
                || this.type.endsWith("$4")
                || this.type.endsWith("$5")
        ) {
            this.type = type;
        }
    }

    public void setTypeForced(String type) {
        this.type = type;
    }

    public String getName() {
        if (names.size() == 0) {
            return null;
        }
        return names.get(0);
    }

    public void setName(String name) {
        if (name == null) {
            return;
        }
        if (name.startsWith("(", 0) || name.startsWith("CGLIB", 0)) {
            return;
        }
        if (!this.names.contains(name)) {
            name = name.replace('$', 'D');
            this.names.add(0, name);
        }
    }

    public List<String> getNamesList() {
        return names;
    }

    public void setNamesList(List<String> namesList) {
        this.names = namesList;
    }

    public void clearNames() {
        this.names.clear();
    }

    public void addNames(Collection<String> name) {
        name = name.stream()
                .filter(e -> !e.startsWith("(", 0) && e.length() > 0)
                .collect(Collectors.toList());
        this.names.addAll(name);
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void setValue(String value) {
        this.stringValue = value;
    }

    public String getStringValue() {
        return stringValue;
    }

    public DataEventWithSessionId getProb() {
        return prob;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public DataInfo getProbeInfo() {
        return dataInfo;
    }

    public void setProbeAndProbeInfo(DataEventWithSessionId prob, DataInfo probeInfo) {
        if (value == 0 && prob != null) {
            this.prob = prob;
            value = prob.getValue();
        } else {
            if (this.prob == null) {
                this.prob = prob;
            }
        }
        if (probeInfo == null) {
            this.dataInfo = null;
            return;
        }


        if (this.dataInfo == null
                || !this.dataInfo.getEventType().equals(EventType.METHOD_EXCEPTIONAL_EXIT)
                || (probeInfo.getEventType() != null && probeInfo.getEventType()
                .equals(EventType.METHOD_EXCEPTIONAL_EXIT))
        ) {
            this.dataInfo = probeInfo;
            this.prob = prob;
        }
        if (probeInfo.getEventType() == EventType.METHOD_EXCEPTIONAL_EXIT) {
            this.exception = true;
        }

    }

    public MethodCallExpression getCreatorExpression() {
        return creatorExpression;
    }

    public void setCreator(MethodCallExpression createrExpression) {

        this.creatorExpression = createrExpression;
    }


    public void addName(String nameForParameter) {
        if (nameForParameter == null
                || this.names.contains(nameForParameter)
                || nameForParameter.startsWith("(", 0)
                || nameForParameter.length() < 1) {
            return;
        }
        nameForParameter = nameForParameter.replace('$', 'D');
        this.names.add(nameForParameter);
    }

    public boolean hasName(String name) {
        if (name == null) {
            return false;
        }
        name = name.replace('$', 'D');
        if (this.names.contains(name) || name.startsWith("(") || name.length() < 1) {
            return true;
        }
        return false;
    }

    public List<String> getNames() {
        return names;
    }


    public boolean isBooleanType() {
        return type != null && (type.equals("Z") || type.equals("java.lang.Boolean"));
    }

    public boolean isStringType() {
        return type != null && (type.equals("java.lang.String"));
    }

    public boolean isOptionalType() {
        return type != null && type.equals("java.util.Optional");
    }

    public boolean isPrimitiveType() {
        // types which are java can build just using their values
        String typeCopy = type;
        while (typeCopy != null && typeCopy.startsWith("[") && typeCopy.length() > 1) {
            typeCopy = typeCopy.substring(1);
        }
        return type != null && (typeCopy.length() == 1 || isBoxedPrimitiveType());

    }

    public boolean isBoxedPrimitiveType() {
        return type != null && (type.startsWith("java.lang.Boolean")
                || type.startsWith("java.lang.Integer")
                || type.startsWith("java.lang.Long")
                || type.startsWith("java.lang.Short")
                || type.startsWith("java.lang.Char")
                || type.startsWith("java.lang.Double")
                || type.startsWith("java.lang.Float")
                || type.startsWith("java.lang.Number")
                || type.startsWith("java.lang.Void")
                || type.startsWith("java.lang.Byte")
        );
    }

}
