package com.insidious.plugin.extension.thread;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.util.text.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.sun.jdi.*;
import com.insidious.plugin.extension.connector.RequestHint;
import com.insidious.plugin.extension.model.*;
import com.insidious.plugin.extension.thread.types.InsidiousClassTypeReference;
import com.insidious.plugin.extension.thread.types.InsidiousObjectReference;
import com.insidious.plugin.extension.thread.types.InsidiousTypeFactory;
import com.insidious.plugin.network.pojo.ClassInfo;
import com.insidious.plugin.network.pojo.DataEventWithSessionId;
import com.insidious.plugin.network.pojo.ObjectInfo;
import com.insidious.plugin.pojo.TracePoint;

import java.util.*;
import java.util.stream.Collectors;

public class InsidiousThreadReference implements ThreadReference {


    private static final Logger logger = LoggerUtil.getInstance(InsidiousThreadReference.class);
    private final ThreadGroupReference threadGroupReference;
    private final ReplayData replayData;
    private final Map<String, DataInfo> dataInfoMap;
    private final Map<String, ClassInfo> classInfoMap;
    private final Map<String, StringInfo> stringInfoMap;
    private final Map<String, InsidiousField> typeFieldMap = new HashMap<>();
    private final Map<String, InsidiousClassTypeReference> classTypeMap;
    private Map<String, InsidiousLocalVariable> variableMap;
    private Map<Long, InsidiousObjectReference> objectReferenceMap;
    private Integer position;
    private LinkedList<InsidiousStackFrame> stackFrames;
    private Map<Long, List<InsidiousLocalVariable>> objectFieldMap;

    public InsidiousThreadReference(ThreadGroupReference threadGroupReference,
                                    ReplayData replayData, TracePoint tracePoint) {
        this.threadGroupReference = threadGroupReference;
        this.replayData = replayData;
        position = 0;


        dataInfoMap = this.replayData.getDataInfoMap();
        classInfoMap = this.replayData.getClassInfoMap();
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
        List<DataEventWithSessionId> dataEventsList = this.replayData.getDataEvents();

        List<DataEventWithSessionId> subList = dataEventsList.subList(position, dataEventsList.size());
        int currentClassId = -1; // dataInfoMap.get(String.valueOf(subList.get(0).getDataId())).getClassId();

        List<InsidiousLocalVariable> danglingFieldList = new LinkedList<>();
        objectFieldMap = new HashMap<>();
        variableMap = new HashMap<>();
        objectReferenceMap = new HashMap<>();
        Map<String, List<InsidiousLocalVariable>> childrenObjectMap = new HashMap<>();

        boolean isMethodParam = false;

        for (int index = 0; index < subList.size(); index++) {
            DataEventWithSessionId dataEvent = subList.get(index);
            String dataId = String.valueOf(dataEvent.getDataId());
            DataInfo probeInfo = dataInfoMap.get(dataId);
            int classId = probeInfo.getClassId();
            ClassInfo classInfo = classInfoMap.get(String.valueOf(classId));
            ObjectInfo objectInfo = replayData.getObjectInfo().get(String.valueOf(dataEvent.getValue()));
            TypeInfo typeInfo = null;
            if (objectInfo != null) {
                typeInfo = replayData.getTypeInfo().get(String.valueOf(objectInfo.getTypeId()));
            }
            long receiverObjectId = 0;
            InsidiousObjectReference receiverObject;

            logger.info("[" + (index + position) + "] Build [" + dataEvent.getNanoTime()
                    + "] line [" + probeInfo.getLine() + "][" + probeInfo.getEventType()
                    + "]  of class [" + classInfo.getFilename() + "] => [" + dataEvent.getValue() + "]");


            switch (probeInfo.getEventType()) {
                case PUT_INSTANCE_FIELD:
                case GET_INSTANCE_FIELD:
                    receiverObjectId = dataEvent.getValue();

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


                    String fieldName = probeInfo.getAttribute("FieldName", null);
                    InsidiousLocalVariable fieldVariableInstance = buildLocalVariable(fieldName, dataEvent, probeInfo);


                    if (methodsToSkip == 0) {
                        thisObject.getValues().put(fieldName, fieldVariableInstance);
                    }


                    String parentId = probeInfo.getAttribute("Parent", "");
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
                        if (currentFrame.location() == null) {
                            InsidiousLocation currentLocation = new InsidiousLocation(classInfo.getFilename(), probeInfo.getLine() - 1);
                            currentFrame.setLocation(currentLocation);
                        }
                        if (thisObject.referenceType() == null) {
                            thisObject.setReferenceType(buildClassTypeReferenceFromName(classInfo.getClassName()));
                        }

                    }
                    break;

                case METHOD_NORMAL_EXIT:
                    methodsToSkip++;
                    break;
//                    currentFrame = stackFrames.pop();

                case METHOD_ENTRY:
                    if (methodsToSkip == 0) {
                        thisObject.setObjectId(dataEvent.getValue());
                        objectReferenceMap.put(dataEvent.getValue(), thisObject);
                        stackFrames.add(currentFrame);
                        thisObject = new InsidiousObjectReference(this);
                        currentFrame = new InsidiousStackFrame(null, this, thisObject, this.virtualMachine());
                    } else {
                        methodsToSkip--;
                    }
                    break;
                case LOCAL_STORE:
                case LOCAL_LOAD:

                    if (currentClassId != -1 && currentClassId != classId) {
                        continue;
                    }


                    String variableName = probeInfo.getAttribute("Name", null);


                    if (variableMap.containsKey(variableName) || variableName == null) {
                        continue;
                    }

                    InsidiousLocalVariable newVariable = buildLocalVariable(variableName, dataEvent, probeInfo);

                    currentFrame.getLocalVariables().add(newVariable);
                    variableMap.put(variableName, newVariable);
                    break;

                case CALL_RETURN:
                    long returnValueObjectId = dataEvent.getValue();
                    if (returnValueObjectId == 0) {
                        continue;
                    }
                    buildDataObjectFromIdAndTypeValue(probeInfo.getAttribute("Type", "Ljava.lang.Object"), dataEvent.getValue());

                    break;

                case NEW_OBJECT_CREATED:
                    long objectObjectCreatedId = dataEvent.getValue();
                    if (objectObjectCreatedId == 0) {
                        continue;
                    }
                    DataInfo parentDataInfo = replayData.getDataInfoMap().get(probeInfo.getAttribute("NewParent", "0"));
                    buildDataObjectFromIdAndTypeValue("L" + parentDataInfo.getAttribute("Type", "java.lang.Object"), dataEvent.getValue());

                    break;

                case METHOD_PARAM:
                    isMethodParam = true;
                case CALL_PARAM:


                    if (methodsToSkip != 0) {
                        continue;
                    }


                    String paramType = probeInfo.getAttribute("Type", "LObject");
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
                            "String", "String", newParamObjectId,
                            new InsidiousValue(InsidiousTypeFactory.typeFrom(
                                    "", "", virtualMachine()),
                                    dataValue, virtualMachine()),
                            virtualMachine()
                    );
                    if (isMethodParam) {
//                        currentFrame.getLocalVariables().add(newVariable);
                        isMethodParam = false;
                    }

                    parentId = probeInfo.getAttribute("CallParent", "0");
                    if (!"0".equals(parentId)) {
                        List<InsidiousLocalVariable> list = childrenObjectMap.get(parentId);
                        if (!childrenObjectMap.containsKey(parentId)) {
                            childrenObjectMap.put(parentId, new LinkedList<>());
                        }
                        childrenObjectMap.get(parentId).add(newVariable);
                    }


                    break;

                case CALL:

                    String interfaceOwner = probeInfo.getAttribute("Owner", "");
                    String methodName = probeInfo.getAttribute("Name", "");

                    Object value = buildDataObjectFromIdAndTypeValue("L" + interfaceOwner, dataEvent.getValue());
                    if (value instanceof InsidiousObjectReference) {
                        receiverObject = (InsidiousObjectReference) value;
                    } else {
                        receiverObject = new InsidiousObjectReference(this);
                        receiverObject.setReferenceType(buildClassTypeReferenceFromName(interfaceOwner));
                    }

                    List<InsidiousLocalVariable> paramsList = childrenObjectMap.get(dataId);

                    switch (interfaceOwner) {

                        default:

                            if (methodName.startsWith("get")) {

                            }

                            if (methodName.startsWith("put")) {


                                List<InsidiousLocalVariable> paramsArgsList = childrenObjectMap.get(dataId);
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


                                InsidiousLocalVariable insidiousLocalVariable = childrenObjectMap.get(dataId).get(0);

                                receiverObject.getValues().put(fieldKey, insidiousLocalVariable);

                                childrenObjectMap.remove(dataId);

                            }

                            if (methodName.startsWith("set")) {


                                List<InsidiousLocalVariable> paramsArgsList = childrenObjectMap.get(dataId);
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


                                InsidiousLocalVariable insidiousLocalVariable = childrenObjectMap.get(dataId).get(0);

                                receiverObject.getValues().put(fieldKey, insidiousLocalVariable);

                                childrenObjectMap.remove(dataId);

                            }


                            break;

                        case "java/util/List":


                            if (paramsList == null || paramsList.size() == 0) {
                                continue;
                            } else {


                                for (int i = 0; i < paramsList.size(); i++) {
                                    InsidiousLocalVariable insidiousLocalVariable = paramsList.get(paramsList.size() - i - 1);

                                    String name = insidiousLocalVariable.name();
                                    if (paramsList.size() == 1) {
                                        name = methodName + "(" + (receiverObject.getValues().keySet().size() + 1) + ")";
                                    }

                                    if (receiverObject.getValues().containsKey(name)) {
                                        name = name + "[" + receiverObject.getValues().size() + "]";
                                    }

                                    long longLocalValue = 0l;
                                    try {
                                        longLocalValue = Long.parseLong(insidiousLocalVariable.toString());
                                    } catch (Exception e) {
                                        //
                                    }

                                    if (longLocalValue > 100 && objectReferenceMap.containsKey(longLocalValue)) {
                                        InsidiousObjectReference existingObject = objectReferenceMap.get(longLocalValue);
                                        insidiousLocalVariable = new InsidiousLocalVariable(name, existingObject.referenceType().name(),
                                                existingObject.referenceType().genericSignature(), longLocalValue,
                                                new InsidiousValue(existingObject.type(), existingObject, this.virtualMachine()), this.virtualMachine());
                                    }


                                    receiverObject.getValues().put(name, insidiousLocalVariable);
                                }

                            }

                            break;


                        case "java/util/Map":


                            if (childrenObjectMap.containsKey(dataId)) {
                                if ("put".equals(methodName)) { // put call

                                    List<InsidiousLocalVariable> paramsArgsList = childrenObjectMap.get(dataId);
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


                                    InsidiousLocalVariable insidiousLocalVariable = childrenObjectMap.get(dataId).get(0);

                                    receiverObject.getValues().put(fieldKey, insidiousLocalVariable);

                                    childrenObjectMap.remove(dataId);

                                } else if ("get".equals(methodName)) { // get call

                                    String signature = probeInfo.getAttribute("Desc", ")Object").split("\\)")[1];

//                                    objectReferenceMap.put(receiverObjectId, receiverObject);
//
                                    InsidiousLocalVariable keyVariable = childrenObjectMap.get(dataId).get(0);
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
                                    childrenObjectMap.remove(dataId);
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

    @Nullable
    private Object buildDataObjectFromIdAndTypeValue(String paramType, Object dataValue) {
        String dataValueString = String.valueOf(dataValue);
        if (stringInfoMap.containsKey(dataValueString)) {
            dataValue = stringInfoMap.get(dataValueString);
        } else if (paramType.startsWith("L") || replayData.getObjectInfo().containsKey(dataValueString)
                || objectReferenceMap.containsKey(Long.valueOf(dataValueString))) {
            Long paramObjectId = Long.valueOf(dataValueString);
            if (objectReferenceMap.containsKey(paramObjectId)) {
                dataValue = objectReferenceMap.get(paramObjectId);
                return dataValue;
            } else {
                if (replayData.getObjectInfo().containsKey(dataValueString)) {
                    try {
                        ObjectInfo objectInfo = replayData.getObjectInfo().get(dataValueString);
                        TypeInfo typeInfo = replayData.getTypeInfo().get(String.valueOf(objectInfo.getTypeId()));
                        paramType = typeInfo.getTypeNameFromClass();
                    } catch (Exception e) {
                        logger.warn("failed to identify type for value", e);
                    }
                }
                InsidiousObjectReference newInsidiousDataValue = new InsidiousObjectReference(this);
                newInsidiousDataValue.setObjectId(paramObjectId);
                newInsidiousDataValue.setReferenceType(buildClassTypeReferenceFromName(paramType));
                objectReferenceMap.put(paramObjectId, newInsidiousDataValue);
                return newInsidiousDataValue;
            }
        }
        return dataValue;
    }

    private Map<String, InsidiousClassTypeReference> buildClassTypeReferences() {
        Map<String, InsidiousClassTypeReference> classMap = new HashMap<>();
        Map<String, Map<String, String>> classFieldsMap = new HashMap<>();

        for (Map.Entry<String, ClassInfo> classInfoEntry : replayData.getClassInfoMap().entrySet()) {

            Long classId = Long.valueOf(classInfoEntry.getKey());
            ClassInfo classInfo = classInfoEntry.getValue();
            List<DataInfo> probes = classInfo.getDataInfoList();

            Map<String, String> classFields = new HashMap<>();

            for (DataInfo probe : probes) {
                switch (probe.getEventType()) {
                    case PUT_INSTANCE_FIELD:
                    case GET_INSTANCE_FIELD:
                        String fieldName = probe.getAttribute("FieldName", "");
                        if (classFields.containsKey(fieldName)) {
                            continue;
                        }
                        String fieldType = probe.getAttribute("Type", "");
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
//            signatureParts.remove(signatureParts.size() - 1);
        String packageName = Strings.join(signatureParts.subList(0, signatureParts.size() - 1), ".");

//            String nonQualifiedClassName = signatureParts.get(signatureParts.size() - 1);


        InsidiousClassTypeReference classTypeReference = new InsidiousClassTypeReference(
                qualifiedClassName, className, "L" + className, null, this.virtualMachine()
        );
        return classTypeReference;
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

    private InsidiousClassTypeReference getClassTypeReference(long classId) {
        return null;
    }

    private boolean isNativeType(String className) {
        return className.startsWith("java.lang") || className.indexOf('.') == -1;
    }

    private InsidiousLocalVariable buildLocalVariable(String variableName, DataEventWithSessionId dataEvent, DataInfo probeInfo) {


        String variableSignature = probeInfo.getAttribute("Type", null);
        ClassInfo classInfo = this.replayData.getClassInfoMap().get(String.valueOf(probeInfo.getClassId()));
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

                String packageName = variableSignature.substring(1);
                String[] signatureParts = packageName.split("/");

                if (!qualifiedClassName.startsWith("java/lang")) {

                    if (objectReferenceMap.containsKey(objectId)) {
                        value = objectReferenceMap.get(objectId);
                    } else {
                        InsidiousObjectReference objectValue = new InsidiousObjectReference(this);

                        objectValue.setObjectId(objectId);
                        objectValue.setReferenceType(classTypeMap.get(classInfo.getClassName()));

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
        buildClassTypeReferences();

        List<DataEventWithSessionId> dataEvents = replayData.getDataEvents();
        int currentLineNumber = replayData.getDataInfoMap().get(String.valueOf(dataEvents.get(position).getDataId())).getLine();

        List<DataEventWithSessionId> subList = dataEvents.subList(position, dataEvents.size());
        if (size < 0) {
            for (int i = position; i < dataEvents.size(); i++) {
                DataEventWithSessionId dataEventWithSessionId = dataEvents.get(i);
                DataInfo dataInfo = replayData.getDataInfoMap().get(String.valueOf(dataEventWithSessionId.getDataId()));
                if (!dataInfo.getEventType().equals(EventType.LINE_NUMBER)) {
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
                DataInfo dataInfo = replayData.getDataInfoMap().get(String.valueOf(dataEventWithSessionId.getDataId()));
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
