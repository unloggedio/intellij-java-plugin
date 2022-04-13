package com.insidious.plugin.extension.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.intellij.openapi.util.text.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * This object is to record attributes of a data ID.
 */
public class DataInfo {

    private static final String SEPARATOR = ",";
    private static final char ATTRIBUTE_KEYVALUE_SEPARATOR = '=';
    private static final char ATTRIBUTE_SEPARATOR = ',';

    private int classId;
    private int methodId;

    private int dataId;

    private int line;
    @JsonIgnore
    private int instructionIndex;

    private EventType eventType;
    private Descriptor valueDesc;
    @JsonIgnore
    private String attributes;
    private Map<String, String> attributesMap = new HashMap<>();

    private String sessionId;


    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }


    public DataInfo() {
    }

    /**
     * Create an instance recording the data ID.
     *
     * @param classId          is a class ID assigned by the weaver.
     * @param methodId         is a method ID assigned by the weaver.
     * @param dataId           is a data ID assigned by the weaver.
     * @param line             is the line number of the instruction (if available).
     * @param instructionIndex is the location of the bytecode instruction in the ASM's InsnList.
     * @param eventType        is the event type.
     * @param valueDesc        is the value type observed by the event.
     * @param attributes       specifies additional attributes statically obtained from the instruction.
     */
    public DataInfo(int classId, int methodId, int dataId, int line, int instructionIndex, EventType eventType, Descriptor valueDesc, String attributes) {
        this.classId = classId;
        this.methodId = methodId;
        this.dataId = dataId;
        this.line = line;
        this.instructionIndex = instructionIndex;
        this.eventType = eventType;
        this.valueDesc = valueDesc;

        if (attributes != null) {
            String[] attributesList = attributes.split(",");
            for (String attributePair : attributesList) {
                if (StringUtil.isEmpty(attributePair)) {
                    continue;
                }
                String[] attributeParts = attributePair.split("=");
                if (attributeParts.length == 2) {
                    attributesMap.put(attributeParts[0], attributeParts[1]);
                } else {
                    attributesMap.put(attributeParts[0], "true");
                }
            }
        }

        this.attributes = attributes;
    }

    /**
     * Create an instance from a string representation created by DataInfo.toString.
     *
     * @param s is the string representation
     * @return a created instance
     */
    public static DataInfo parse(String s) {
        Scanner sc = new Scanner(s);
        sc.useDelimiter(SEPARATOR);
        int dataId = sc.nextInt();
        int classId = sc.nextInt();
        int methodId = sc.nextInt();
        int line = sc.nextInt();
        int instructionIndex = sc.nextInt();
        EventType t = EventType.valueOf(sc.next());
        Descriptor d = Descriptor.get(sc.next());
        StringBuilder b = new StringBuilder();
        while (sc.hasNext()) {
            b.append(sc.next());
            b.append(DataInfo.ATTRIBUTE_SEPARATOR);
        }
        String attributes = b.toString();
        sc.close();
        return new DataInfo(classId, methodId, dataId, line, instructionIndex, t, d, attributes);
    }

    public int getClassId() {
        return classId;
    }

    public int getMethodId() {
        return methodId;
    }

    public int getDataId() {
        return dataId;
    }

    public int getLine() {
        return line;
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Descriptor getValueDesc() {
        return valueDesc;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
        attributesMap = new HashMap<>();
        if (attributes != null) {
            String[] attributesList = attributes.split(",");
            for (String attributePair : attributesList) {
                if (StringUtil.isEmpty(attributePair)) {
                    continue;
                }
                String[] attributeParts = attributePair.split("=");
                if (attributeParts.length == 2) {
                    attributesMap.put(attributeParts[0], attributeParts[1]);
                } else {
                    attributesMap.put(attributeParts[0], "true");
                }
            }
        }
    }

    public Map<String, String> getAttributesMap() {
        return attributesMap;
    }

    /**
     * Access a particular attribute of the instruction, assuming the "KEY=VALUE" format.
     *
     * @param key          specifies an attribute key
     * @param defaultValue is returned if the key is unavailable.
     * @return the value corresponding to the key.
     */
    public String getAttribute(String key, String defaultValue) {
        if (attributesMap.containsKey(key)) {
            return attributesMap.get(key);
        }
//        while (index >= 0) {
//            if (index == 0 || attributes.charAt(index - 1) == ATTRIBUTE_SEPARATOR) {
//                int keyEndIndex = attributes.indexOf(ATTRIBUTE_KEYVALUE_SEPARATOR, index);
//                if (keyEndIndex == index + key.length()) {
//                    int valueEndIndex = attributes.indexOf(ATTRIBUTE_SEPARATOR, keyEndIndex);
//                    if (valueEndIndex > keyEndIndex) {
//                        return attributes.substring(index + key.length() + 1, valueEndIndex);
//                    } else {
//                        return attributes.substring(index + key.length() + 1);
//                    }
//                }
//            }
//            index = attributes.indexOf(key, index + 1);
//        }
        return defaultValue;
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return dataId +
                SEPARATOR +
                classId +
                SEPARATOR +
                methodId +
                SEPARATOR +
                line +
                SEPARATOR +
                instructionIndex +
                SEPARATOR +
                eventType.name() +
                SEPARATOR +
                (valueDesc != null ? valueDesc.getString() : " [] ") +
                SEPARATOR +
                attributes;
    }


}
