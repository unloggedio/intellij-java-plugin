package com.insidious.plugin.extension.thread;

import com.insidious.common.Util;
import com.insidious.common.weaver.*;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.model.ReplayData;
import com.insidious.plugin.extension.thread.types.InsidiousClassTypeReference;
import com.insidious.plugin.extension.thread.types.InsidiousObjectReference;
import com.insidious.plugin.extension.thread.types.InsidiousTypeFactory;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.pojo.TracePoint;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class InsidiousThreadReference implements ThreadReference {


    public static final int MAX_HISTORY_LENGTH = 1000;
    private static final Logger logger = LoggerUtil.getInstance(InsidiousThreadReference.class);
    private final ThreadGroupReference threadGroupReference;
    private final ReplayData replayData;
    private final Map<Long, StringInfo> stringInfoMap;
    private final Map<String, InsidiousField> typeFieldMap = new HashMap<>();
    private final Map<String, InsidiousClassTypeReference> classTypeMap;
    private final TracePoint tracePoint;
    private Map<String, InsidiousLocalVariable> variableMap;
    private Map<Long, InsidiousObjectReference> objectReferenceMap;
    private Integer position;
    private LinkedList<InsidiousStackFrame> stackFrames;
    private Map<Long, List<InsidiousLocalVariable>> objectFieldMap;

    public InsidiousThreadReference(ThreadGroupReference threadGroupReference,
                                    ReplayData replayData, TracePoint tracePoint) {
        this.threadGroupReference = threadGroupReference;
        this.replayData = replayData;
        this.tracePoint = tracePoint;


        JSONObject eventProperties = new JSONObject();
        eventProperties.put("classname", tracePoint.getClassname());
        eventProperties.put("sessionId", tracePoint.getExecutionSession().getSessionId());
        eventProperties.put("filename", tracePoint.getFilename());
        eventProperties.put("eventsCount", replayData.getDataEvents().size());
        eventProperties.put("objectsCount", replayData.getObjectInfoMap().size());
        UsageInsightTracker.getInstance().RecordEvent("ConstructThreadReference", eventProperties);

        int i = 0;
        long lowestTimestamp = 99999999999999L;
        long highestTimestamp = 0;

        List<DataEventWithSessionId> matches = replayData.getDataEvents().stream()
                .filter(e -> e.getNanoTime() == tracePoint.getNanoTime())
                .collect(Collectors.toList());
        position = replayData.getDataEvents().indexOf(
                matches.get(0)
        );


        stringInfoMap = this.replayData.getStringInfoMap();

        this.classTypeMap = buildClassTypeReferences();

        try {
            calculateFrames();
        } catch (ClassNotLoadedException e) {
            // should never happen
            e.printStackTrace();
        }
    }

    private void calculateFrames() throws ClassNotLoadedException {
        LinkedList<InsidiousStackFrame> stackFrames = new LinkedList<>();

        InsidiousObjectReference thisObject = new InsidiousObjectReference(this);

        InsidiousStackFrame currentFrame = new InsidiousStackFrame(
                null,
                this,
                thisObject,
                this.virtualMachine());


        int methodsToSkip = 0;
        ReplayData replayDataLocal = this.replayData;
        List<DataEventWithSessionId> dataEventsList = replayDataLocal.getDataEvents();

        List<DataEventWithSessionId> subList =
                dataEventsList.subList(position, position + Math.min(MAX_HISTORY_LENGTH, dataEventsList.size() - position));
        int currentClassId = -1; // dataInfoMap.get(String.valueOf(subList.get(0).getDataId())).getClassId();

        List<InsidiousLocalVariable> danglingFieldList = new LinkedList<>();
        objectFieldMap = new HashMap<>();
        variableMap = new HashMap<>();
        objectReferenceMap = new HashMap<>();
        Map<String, List<InsidiousLocalVariable>> childrenObjectMap = new HashMap<>();

        boolean isMethodParam = false;
        boolean isReturnParam = false;

        //        int instructionCount = 0;

        for (int index = 0; index < subList.size(); index++) {
            DataEventWithSessionId dataEvent = subList.get(index);
            String dataId = String.valueOf(dataEvent.getDataId());
            DataInfo probeInfo = replayData.getProbeInfo(dataEvent.getDataId());
            int classId = probeInfo.getClassId();
            ClassInfo classInfo = replayData.getClassInfo(classId);
//            ObjectInfo objectInfo = this.replayData.getObjectInfoMap().get(dataEvent.getValue());
//            TypeInfo typeInfo = null;
//            if (objectInfo != null) {
//                typeInfo = this.replayData.getTypeInfoMap().get(String.valueOf(objectInfo.getTypeId()));
//            }
            long receiverObjectId = 0;
            InsidiousObjectReference receiverObject;

            logger.trace("[" + (index + position) + "] Build [" + dataEvent.getNanoTime()
                    + "] line [" + probeInfo.getLine() + "][" +
                    probeInfo.getEventType()
                    + "]  of class [" +
                    classInfo.getFilename() + "] => [" + dataEvent.getValue() + "]");
            String fieldName = Util.getAttribute(probeInfo.getAttributes(), "FieldName", null);


            switch (probeInfo.getEventType()) {
                case PUT_INSTANCE_FIELD:
                case GET_INSTANCE_FIELD:
                    receiverObjectId = dataEvent.getValue();
                    // instructionCount++;

                    if (methodsToSkip == 0 && thisObject.uniqueID() == 0) {
                        thisObject.setObjectId(receiverObjectId);
                        objectReferenceMap.put(receiverObjectId, thisObject);
                    }

                    if (objectReferenceMap.containsKey(receiverObjectId)) { // object has been created and we want to add identified variables back to it
                        InsidiousObjectReference variable = objectReferenceMap.get(receiverObjectId);
                        for (InsidiousLocalVariable insidiousLocalVariable : danglingFieldList) {
                            if (!variable.getValues().containsKey(insidiousLocalVariable.name())) {
                                variable.getValues().put(insidiousLocalVariable.name(), insidiousLocalVariable);
                            }
                        }
                        danglingFieldList = new LinkedList<>();

                    } else {
                        if (!objectFieldMap.containsKey(receiverObjectId)) {
                            objectFieldMap.put(receiverObjectId, new LinkedList<>());
                        }
                        if (danglingFieldList.size() > 0) {
                            objectFieldMap.get(receiverObjectId).addAll(danglingFieldList);
                            danglingFieldList = new LinkedList<>();
                        }
                    }


                    break;

                case PUT_INSTANCE_FIELD_VALUE:
                case GET_INSTANCE_FIELD_RESULT:


                    InsidiousLocalVariable fieldVariableInstance = buildLocalVariable(fieldName, dataEvent, probeInfo);


                    if (methodsToSkip == 0) {
                        thisObject.getValues().put(fieldName, fieldVariableInstance);
                    }
                    // instructionCount++;


                    String parentId = Util.getAttribute(
                            probeInfo.getAttributes(), "Parent", "");
                    if (!Objects.equals(parentId, "")) {
                        if (!childrenObjectMap.containsKey(parentId)) {
                            childrenObjectMap.put(parentId, new LinkedList<>());
                        }
                        childrenObjectMap.get(parentId).add(fieldVariableInstance);
                    }
                    danglingFieldList.add(fieldVariableInstance);

                    break;

                case LINE_NUMBER:

                    if (methodsToSkip == 0) {
                        currentClassId = classId;
                        if (currentFrame.location() == null || currentFrame.location().lineNumber() == -1) {
                            InsidiousLocation currentLocation = getClassLocationFromProbe(probeInfo, classInfo);
                            currentFrame.setLocation(currentLocation);
                        }
                        if (thisObject.referenceType() == null) {
                            thisObject.setReferenceType(
                                    buildClassTypeReferenceFromName(
                                            classInfo.getClassName()));
                        }

                    }
                    break;

                case METHOD_NORMAL_EXIT:
                    methodsToSkip++;
                    break;
//                    currentFrame = stackFrames.pop();

                case METHOD_ENTRY:
                    if (methodsToSkip == 0) {
//                        thisObject.setObjectId(dataEvent.getValue());
//                        objectReferenceMap.put(dataEvent.getValue(), thisObject);
                        stackFrames.add(currentFrame);
                        thisObject = new InsidiousObjectReference(this);
                        thisObject.setObjectId(dataEvent.getValue());
                        objectReferenceMap.put(dataEvent.getValue(), thisObject);
                        @NotNull InsidiousLocation location = getClassLocationFromProbe(probeInfo, classInfo);
                        currentFrame = new InsidiousStackFrame(location, this, thisObject, this.virtualMachine());
                    } else {
                        methodsToSkip--;
                    }
                    break;
                case LOCAL_STORE:
                case LOCAL_LOAD:

                    if (currentClassId != -1 && currentClassId != classId) {
                        continue;
                    }


                    String variableName = Util.getAttribute(
                            probeInfo.getAttributes(), "Name", null);
//                    instructionCount++;

                    if (variableMap.containsKey(variableName) || variableName == null) {
                        continue;
                    }

                    InsidiousLocalVariable newVariable = buildLocalVariable(variableName, dataEvent, probeInfo);

                    currentFrame.getLocalVariables().add(newVariable);
                    variableMap.put(variableName, newVariable);
                    break;

                case NEW_OBJECT_CREATED:
                    long objectObjectCreatedId = dataEvent.getValue();
//                    instructionCount++;
                    if (objectObjectCreatedId == 0) {
                        continue;
                    }
                    DataInfo parentDataInfo
                            = this.replayData.getProbeInfo(
                            Long.parseLong(Util.getAttribute(probeInfo.getAttributes(), "NewParent", "0")));
                    String classTypeOfNewObject = "java/lang/Object";
                    if (parentDataInfo == null) {
                        logger.warn("no data info for parent of new object created - " + probeInfo);
                    } else {
                        classTypeOfNewObject = Util.getAttribute(
                                parentDataInfo.getAttributes(), "Type", classTypeOfNewObject);
                    }

                    buildDataObjectFromIdAndTypeValue(
                            "L" + classTypeOfNewObject,
                            dataEvent.getValue());

                    break;

                case METHOD_PARAM:
                    isMethodParam = true;

                case CALL_RETURN:
                    isReturnParam = true;
                case CALL_PARAM:


                    if (methodsToSkip != 0) {
                        continue;
                    }


                    String paramType = Util.getAttribute(
                            probeInfo.getAttributes(), "Type", "Ljava/lang/Object;");
                    if ("V".equals(paramType)) { // void return type
                        continue;
                    }
                    Object dataValue = dataEvent.getValue();
                    long newParamObjectId = 0L;
                    newParamObjectId = Long.parseLong(dataValue.toString());

                    try {
                        dataValue = buildDataObjectFromIdAndTypeValue(paramType, dataValue);
                        if (dataValue instanceof StringInfo) {
                            newParamObjectId = ((StringInfo) dataValue).getStringId();
                        } else if (dataValue instanceof InsidiousObjectReference) {
                            newParamObjectId = ((InsidiousObjectReference) dataValue).uniqueID();
                        }
                    } catch (Exception e) {
                        // ignore
                    }


                    newVariable = new InsidiousLocalVariable(
                            "",
                            paramType, paramType, newParamObjectId,
                            new InsidiousValue(InsidiousTypeFactory.typeFrom(
                                    paramType, paramType, virtualMachine()),
                                    dataValue, virtualMachine()),
                            virtualMachine()
                    );
                    if (isMethodParam) {
//                        currentFrame.getLocalVariables().add(newVariable);
                        isMethodParam = false;
                    }

                    parentId = Util.getAttribute(
                            probeInfo.getAttributes(), "CallParent", "0");
                    if (!"0".equals(parentId)) {
                        List<InsidiousLocalVariable> list = childrenObjectMap.get(parentId);
                        if (!childrenObjectMap.containsKey(parentId)) {
                            childrenObjectMap.put(parentId, new LinkedList<>());
                        }
                        childrenObjectMap.get(parentId).add(newVariable);
                    }


                    break;

                case CALL:


                    // instructionCount++;
                    String interfaceOwner = Util.getAttribute(probeInfo.getAttributes(), "Owner", "");
                    String methodName = Util.getAttribute(probeInfo.getAttributes(), "Name", "");

                    Object value = buildDataObjectFromIdAndTypeValue("L" + interfaceOwner, dataEvent.getValue());
                    if (value instanceof InsidiousObjectReference) {
                        receiverObject = (InsidiousObjectReference) value;
                    } else {
                        receiverObject = new InsidiousObjectReference(this);
                        receiverObject.setReferenceType(buildClassTypeReferenceFromName(interfaceOwner));
                    }

                    List<InsidiousLocalVariable> paramsArgsList = childrenObjectMap.get(dataId);
                    childrenObjectMap.remove(dataId);

                    switch (interfaceOwner) {

                        default:

                            if (paramsArgsList == null || paramsArgsList.size() == 0) {
                                logger.debug("call with no params or return values, skipping [{}]", methodName);
                                continue;
                            }
                            if (!(methodName.startsWith("get")
                                    || methodName.startsWith("set")
                                    || methodName.startsWith("put")
                            )) {
                                continue;
                            }

                            // yikes
                            // get fieldName from methodName if it looks like putXXX/getXXX/setXXX
                            if (methodName.length() > 3 && methodName.charAt(2) == 't') {
                                fieldName = methodName.substring(3);
                                fieldName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
                            }

                            // if fieldName is empty, it can potentially be the first item of the call params
                            // getInt("fieldName") set("FieldName", value)
                            if (StringUtil.isEmpty(fieldName) && paramsArgsList.size() > 1) {
                                InsidiousLocalVariable key = paramsArgsList.get(0);
                                fieldName = String.valueOf(key.getValue().getActualValue());
                                paramsArgsList.remove(0);
                            }


                            if (methodName.startsWith("get")) {
                                InsidiousLocalVariable returnValue = paramsArgsList.get(0);
                                receiverObject.getValues().put(fieldName, returnValue);

                            } else if (methodName.startsWith("put") || methodName.startsWith("set")) {


                                InsidiousValue key = paramsArgsList.get(0).getValue();

                                if (receiverObject.getValues().containsKey(fieldName)) {
                                    continue;
                                }


                                InsidiousLocalVariable insidiousLocalVariable =
                                        paramsArgsList.get(paramsArgsList.size() - 1);
                                insidiousLocalVariable.setName(fieldName);
                                receiverObject.getValues().put(fieldName, insidiousLocalVariable);

                            }


                            break;

                        case "java/util/List":


                            if (paramsArgsList == null || paramsArgsList.size() == 0) {
                                continue;
                            } else {

                                // only handle add/get method for list,
                                // TODO: need to support other methods in the list interface like clear()
                                if (!methodName.equals("add") && !methodName.equals("get")) {
                                    continue;
                                }

                                if (paramsArgsList.size() > 1) {
                                    // calls to list methods return a value which we need to ignore
                                    paramsArgsList.remove(0);
                                }

                                for (int i = 0; i < paramsArgsList.size(); i++) {
                                    InsidiousLocalVariable insidiousLocalVariable = paramsArgsList.get(paramsArgsList.size() - i - 1);

                                    String name = insidiousLocalVariable.name();
                                    if (paramsArgsList.size() == 1) {
                                        name = methodName + "(" + (receiverObject.getValues().keySet().size() + 1) + ")";
                                    }

                                    if (receiverObject.getValues().containsKey(name)) {
                                        name = name + "[" + receiverObject.getValues().size() + "]";
                                    }
                                    receiverObject.getValues().put(name, insidiousLocalVariable);
                                }

                            }

                            break;


                        case "java/util/Map":


                            if (paramsArgsList != null) {
                                if ("put".equals(methodName)) { // put call

                                    if (paramsArgsList.size() > 1) {
                                        // remove the return value of the put call
                                        paramsArgsList.remove(0);
                                    }

                                    InsidiousValue key = paramsArgsList.get(paramsArgsList.size() - 1).getValue();
                                    String fieldKey;
                                    if (key.getActualValue() instanceof StringInfo) {

                                        fieldKey = ((StringInfo) key.getActualValue()).getContent();
                                    } else {
                                        fieldKey = key.getActualValue().toString();
                                    }

                                    if (receiverObject.getValues().containsKey(fieldKey)) {
                                        continue;
                                    }


                                    InsidiousLocalVariable insidiousLocalVariable = paramsArgsList.get(0);

                                    receiverObject.getValues().put(fieldKey, insidiousLocalVariable);


                                } else if ("get".equals(methodName)) { // get call

                                    String signature = Util.getAttribute(
                                            probeInfo.getAttributes(), "Desc", ")Object").split("\\)")[1];

//                                    objectReferenceMap.put(receiverObjectId, receiverObject);
//
                                    InsidiousLocalVariable keyVariable = paramsArgsList.get(0);
                                    InsidiousValue key = keyVariable.getValue();
                                    String keyString;
                                    if (key.getActualValue() instanceof StringInfo) {
                                        keyString = ((StringInfo) key.getActualValue()).getContent();
                                    } else {
                                        keyString = key.getActualValue().toString();
                                    }

//                                    InsidiousLocalVariable insidiousLocalVariable = new InsidiousLocalVariable(
//                                            keyString,
//                                            signature,
//                                            signature, receiverObjectId,
//                                            new InsidiousValue(
//                                                    typeReferenceFromName(signature), new InsidiousObjectReference(this), this.virtualMachine()
//                                            ),
//                                            this.virtualMachine()
//                                    );
//
//
//                                    receiverObject.getValues().put(keyString, insidiousLocalVariable);

                                }


                            }

//                            objectReferenceMap.put(objectId, objectInstance);


//                            String typeSignature = probeInfo.getAttribute("Owner", "String");
//                            String[] typeString = typeSignature.split("\\/");
//                            String newVariableTypeName = typeString[typeString.length - 1];
//                            newVariable = new InsidiousLocalVariable(
//                                    newVariableTypeName,
//                                    newVariableTypeName, typeSignature, objectId,
//                                    new InsidiousValue(InsidiousTypeFactory.typeFrom(
//                                            "", "", virtualMachine()),
//                                            objectInstance, virtualMachine()),
//                                    virtualMachine()
//                            );
//
//                            currentFrame.getLocalVariables().add(newVariable);
                            break;

                    }

                    break;


            }

        }

        if (currentFrame.location() != null) {
            stackFrames.add(currentFrame);
        }

        this.stackFrames = stackFrames;

    }

    @NotNull
    private InsidiousLocation getClassLocationFromProbe(DataInfo probeInfo, ClassInfo classInfo) {
        String[] packageParts = classInfo.getClassName().split("/");
        packageParts[packageParts.length - 1] = classInfo.getFilename();
        return new InsidiousLocation(StringUtil.join(packageParts, "/"),
                probeInfo.getLine() - 1);
    }

    @Nullable
    private Object buildDataObjectFromIdAndTypeValue(String paramType, Object dataValue) {

        if (Long.parseLong(String.valueOf(dataValue)) == 0L) {
            return dataValue;
        }

        if (paramType.length() == 1 && !paramType.equals("L")) {
            return dataValue;
        }

        String dataValueString = String.valueOf(dataValue);
        if (stringInfoMap.containsKey(dataValueString) && Integer.valueOf(String.valueOf(dataValue)) > 10) {
            dataValue = stringInfoMap.get(dataValueString);
        } else if (
                (paramType.startsWith("L")
                        || replayData.getObjectInfoMap().containsKey(dataValueString)
                        || objectReferenceMap.containsKey(Long.valueOf(dataValueString))
                ) && (!paramType.startsWith("Ljava/lang") || paramType.startsWith("Ljava/lang/Object"))
        ) {
            Long paramObjectId = Long.valueOf(dataValueString);
            if (objectReferenceMap.containsKey(paramObjectId)) {
                dataValue = objectReferenceMap.get(paramObjectId);
                return dataValue;
            } else {
                if (replayData.getObjectInfoMap().containsKey(dataValueString)) {
                    try {
                        ObjectInfo objectInfo = replayData.getObjectInfoMap().get(dataValueString);
                        TypeInfo typeInfo = replayData.getTypeInfoMap().get(String.valueOf(objectInfo.getTypeId()));
                        if (typeInfo != null) {
                            paramType = typeInfo.getTypeNameFromClass();
                        } else {
                            logger.warn("failed to get type for object: " + objectInfo.getTypeId());
                        }
                    } catch (Exception e) {
                        logger.warn("failed to identify type for value", e);
                    }
                }
                InsidiousObjectReference newInsidiousDataValue = new InsidiousObjectReference(this);
                newInsidiousDataValue.setObjectId(paramObjectId);
                newInsidiousDataValue.setReferenceType(buildClassTypeReferenceFromName(paramType));
                if (Long.parseLong(String.valueOf(dataValue)) != 0) {
                    objectReferenceMap.put(paramObjectId, newInsidiousDataValue);
                }
                return newInsidiousDataValue;
            }
        }
        return dataValue;
    }

    private Map<String, InsidiousClassTypeReference> buildClassTypeReferences() {
        Map<String, InsidiousClassTypeReference> classMap = new HashMap<>();
        Map<String, Map<String, String>> classFieldsMap = new HashMap<>();

        for (Map.Entry<Integer, ClassInfo> classInfoEntry : replayData.getClassInfoMap().entrySet()) {

            Integer classId = classInfoEntry.getKey();
            ClassInfo classInfo = classInfoEntry.getValue();
            // note for future reader: we are breaking this to make the classInfo class serializable (maybe)
            List<DataInfo> probes = List.of(); // classInfo.getDataInfoList();
            if (probes == null) {
                continue;
            }

            Map<String, String> classFields = new HashMap<>();

            for (DataInfo probe : probes) {
                switch (probe.getEventType()) {
                    case PUT_INSTANCE_FIELD:
                    case GET_INSTANCE_FIELD:
                        String fieldName = Util.getAttribute(probe.getAttributes(), "FieldName", "");
                        if (classFields.containsKey(fieldName)) {
                            continue;
                        }
                        String fieldType = Util.getAttribute(
                                probe.getAttributes(), "Type", "");
                        classFields.put(fieldName, fieldType);
                        break;
                }
            }

            String className = classInfo.getClassName();
            classFieldsMap.put(className, classFields);

            InsidiousClassTypeReference classTypeReference = buildClassTypeReferenceFromName(className);
            classMap.put(className, classTypeReference);
        }

        for (Map.Entry<String, InsidiousClassTypeReference> classTypeEntry : classMap.entrySet()) {
            Map<String, Field> fieldMap = buildFieldMap(classFieldsMap.get(classTypeEntry.getKey()));
            classTypeEntry.getValue().setFields(fieldMap);
        }

        java.lang.reflect.Field[] fields = NotAClass.class.getFields();
        for (java.lang.reflect.Field field : fields) {
            Class<?> type = field.getType();
            InsidiousClassTypeReference classTypeReference = new InsidiousClassTypeReference(
                    type.getName(), type.getCanonicalName(), type.getName(), Map.of(), this.virtualMachine()
            );
            classMap.put(type.getName().replace('.', '/'), classTypeReference);
        }


        return classMap;

    }

    @NotNull
    private InsidiousClassTypeReference buildClassTypeReferenceFromName(String className) {

        List<String> signatureParts = Arrays.asList(className.split("/"));
        String qualifiedClassName = className.replaceAll("/", ".");
        return new InsidiousClassTypeReference(
                qualifiedClassName, className, "L" + className, null, this.virtualMachine()
        );
    }

    private Map<String, Field> buildFieldMap(Map<String, String> fieldToTypeMap) {
        Map<String, Field> fieldValueMap = new HashMap<>();
        for (Map.Entry<String, String> nameTypeEntry : fieldToTypeMap.entrySet()) {
            Field field = buildFieldFromType(nameTypeEntry.getValue());
            fieldValueMap.put(nameTypeEntry.getKey(), field);
        }

        return fieldValueMap;
    }

    private Field buildFieldFromType(String typeName) {
        if (typeFieldMap.containsKey(typeName)) {
            return typeFieldMap.get(typeName);
        }
        InsidiousField field = InsidiousField.from(typeName, this.virtualMachine());
        typeFieldMap.put(typeName, field);
        return field;
    }

    private InsidiousLocalVariable buildLocalVariable(String variableName, DataEventWithSessionId dataEvent, DataInfo probeInfo) {


        String variableSignature = Util.getAttribute(probeInfo.getAttributes(), "Type", null);
//        ClassInfo classInfo = this.replayData.getClassInfo(probeInfo.getClassId());
        long objectId = 0;

        char typeFirstCharacter = variableSignature.charAt(0);
        String qualifiedClassName = variableSignature.substring(1);

        boolean isArrayType = false;
        String typeName = qualifiedClassName.split(";")[0];
        if (typeFirstCharacter == '[') {
            isArrayType = true;
            typeName = variableSignature.substring(2).split(";")[0];
            typeFirstCharacter = variableSignature.charAt(1);
        }

        Object value = dataEvent.getValue();

        switch (typeFirstCharacter) {
            case 'L':
                // class
                objectId = dataEvent.getValue();

                String packageName = typeName.replaceAll("/", ".");
                String[] signatureParts = typeName.split("/");

                if (!typeName.startsWith("java/lang")) {

                    if (objectReferenceMap.containsKey(objectId)) {
                        value = objectReferenceMap.get(objectId);
                    } else {
                        InsidiousObjectReference objectValue = new InsidiousObjectReference(this);

                        objectValue.setObjectId(objectId);
                        objectValue.setReferenceType(classTypeMap.get(typeName));

                        if (objectFieldMap.containsKey(objectId)) {
                            List<InsidiousLocalVariable> fieldVariables = objectFieldMap.get(objectId);
                            Map<String, InsidiousLocalVariable> fieldMap = objectValue.getValues();
                            for (InsidiousLocalVariable fieldVariable : fieldVariables) {
                                fieldMap.put(fieldVariable.name(), fieldVariable);
                            }
                        }

                        objectReferenceMap.put(objectId, objectValue);


                        value = objectValue;
                    }
                }
                break;
            case 'Z':
                typeName = "boolean";
                // boolean
                break;
            case 'B':
                // byte
                typeName = "byte";
                break;
            case 'C':
                // char
                typeName = "char";
                break;
            case 'S':
                // short
                typeName = "short";
                break;
            case 'I':
                // int
                typeName = "int";
                break;
            case 'J':
                // long
                typeName = "long";
                break;
            case 'F':
                // float
                typeName = "float";
                break;
            case 'D':
                // double
                typeName = "double";
                break;
            default:
                System.out.println("Invalid type defining character: " + typeFirstCharacter);
                break;
        }


        if (variableSignature.contains("java/lang/String")) {
            StringInfo stringInfo = stringInfoMap.get(String.valueOf(value));
            if (stringInfo != null) {
                value = stringInfo.getContent();
            }
        }

        InsidiousLocalVariable newVariable = new InsidiousLocalVariable(
                variableName,
                typeName,
                variableSignature,
                objectId,
                new InsidiousValue(typeReferenceFromName(typeName),
                        value,
                        this.virtualMachine()
                ),
                this.virtualMachine());

        return newVariable;

    }

    private Type typeReferenceFromName(String typeName) {
        if (classTypeMap.containsKey(typeName)) {
            return classTypeMap.get(typeName);
        }

        @NotNull InsidiousClassTypeReference reference = buildClassTypeReferenceFromName(typeName);
        classTypeMap.put(typeName, reference);
        return reference;
    }

    @Override
    public String name() {
        return "Insidious thread reference";
    }

    @Override
    public void suspend() {
        new Exception().printStackTrace();

    }

    @Override
    public void resume() {
        new Exception().printStackTrace();

    }

    @Override
    public int suspendCount() {
        return 0;
    }

    @Override
    public void stop(ObjectReference objectReference) throws InvalidTypeException {
        new Exception().printStackTrace();

    }

    @Override
    public void interrupt() {
        new Exception().printStackTrace();

    }

    @Override
    public int status() {
        return ThreadReference.THREAD_STATUS_MONITOR;
    }

    @Override
    public boolean isSuspended() {
        return true;
    }

    @Override
    public boolean isAtBreakpoint() {
        return true;
    }

    @Override
    public ThreadGroupReference threadGroup() {
        return this.threadGroupReference;
    }

    @Override
    public int frameCount() throws IncompatibleThreadStateException {
        return stackFrames.size();
    }

    @Override
    public List<StackFrame> frames() throws IncompatibleThreadStateException {
        return stackFrames.stream().collect(Collectors.toList());
    }

    @Override
    public StackFrame frame(int i) throws IncompatibleThreadStateException {
        return stackFrames.get(i);
    }

    @Override
    public List<StackFrame> frames(int i, int i1) throws IncompatibleThreadStateException {
        return stackFrames.subList(i, i1).stream().collect(Collectors.toList());
    }

    @Override
    public List<ObjectReference> ownedMonitors() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public List<MonitorInfo> ownedMonitorsAndFrames() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public ObjectReference currentContendedMonitor() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public void popFrames(StackFrame stackFrame) throws IncompatibleThreadStateException {
        new Exception().printStackTrace();

    }

    @Override
    public void forceEarlyReturn(Value value) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException {
        new Exception().printStackTrace();

    }

    @Override
    public ReferenceType referenceType() {
        return null;
    }

    @Override
    public Value getValue(Field field) {
        return null;
    }

    @Override
    public Map<Field, Value> getValues(List<? extends Field> list) {
        return null;
    }

    @Override
    public void setValue(Field field, Value value) throws InvalidTypeException, ClassNotLoadedException {
        new Exception().printStackTrace();

    }

    @Override
    public Value invokeMethod(ThreadReference threadReference, Method method, List<? extends Value> list, int i) throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
        return null;
    }

    @Override
    public void disableCollection() {
        new Exception().printStackTrace();

    }

    @Override
    public void enableCollection() {
        new Exception().printStackTrace();

    }

    @Override
    public boolean isCollected() {
        return false;
    }

    @Override
    public long uniqueID() {
        return 0;
    }

    @Override
    public List<ThreadReference> waitingThreads() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public ThreadReference owningThread() throws IncompatibleThreadStateException {
        return null;
    }

    @Override
    public int entryCount() throws IncompatibleThreadStateException {
        return 0;
    }

    @Override
    public List<ObjectReference> referringObjects(long l) {
        return null;
    }

    @Override
    public Type type() {
        return null;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return threadGroupReference.virtualMachine();
    }

    public void doStep(int size, int depth, RequestHint requestHint) {
//        buildClassTypeReferences();

//        JSONObject eventProperties = new JSONObject();
//        eventProperties.put("size", size);
//        eventProperties.put("depth", depth);
//        UsageInsightTracker.getInstance().RecordEvent("DebugStep", eventProperties);

        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();
        int currentLineNumber = replayData.getProbeInfo(dataEvents.get(position).getDataId()).getLine();

        List<DataEventWithSessionId> subList = dataEvents.subList(position, dataEvents.size());
        if (size < 0) {
            for (int i = position; i < dataEvents.size(); i++) {
                DataEventWithSessionId dataEventWithSessionId = dataEvents.get(i);
                DataInfo dataInfo = replayData.getProbeInfo(dataEventWithSessionId.getDataId());
                if (!EventType.LINE_NUMBER.equals(dataInfo.getEventType())) {
                    continue;
                }
                if (dataInfo.getLine() != currentLineNumber) {
                    position = i;
                    break;
                }
            }
        } else {
            for (int i = position; i > 0; i--) {
                DataEventWithSessionId dataEventWithSessionId = dataEvents.get(i);
                DataInfo dataInfo
                        = replayData.getProbeInfo(dataEventWithSessionId.getDataId());
                if (!dataInfo.getEventType().equals(EventType.LINE_NUMBER)) {
                    continue;
                }
                if (dataInfo.getLine() != currentLineNumber) {
                    position = i;
                    break;
                }
            }
        }


        try {
            calculateFrames();
        } catch (Exception e) {
            // should never happen
            e.printStackTrace();
        }

    }
}
