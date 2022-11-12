package com.insidious.plugin.pojo;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parameter is a value (long id or string) with a name and type information (class name). It could
 * be a variable passed as a method argument, or the
 * test subject or the return value from the function. Store the corresponding probeInfo and
 * event also from where the information was identified
 */
public class Parameter {
    private final VariableContainer variableContainer = new VariableContainer();
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
    private List<String> names = new LinkedList<>();
    private String stringValue = null;
    private int index;
    private DataInfo dataInfo;
    //    private ConstructorType constructorType;
    private MethodCallExpression creatorExpression;
    private Map<String, Parameter> templateMap = new HashMap<>();
    private boolean isContainer = false;
    private String nameUsed;

    public Parameter(Long value) {
        this.value = value;
    }

    public Parameter() {
    }

    @NotNull
    public static Parameter cloneParameter(Parameter parameter) {
        Parameter buildWithJson = new Parameter();
        buildWithJson.setNamesList(new ArrayList<>(parameter.getNamesList()));
        buildWithJson.setTemplateMap(parameter.getTemplateMap());
        buildWithJson.setType(parameter.getType());
        buildWithJson.setContainer(parameter.isContainer());
        buildWithJson.setProbeInfo(parameter.getProbeInfo());
        buildWithJson.setProb(parameter.getProb());
        return buildWithJson;
    }

    public boolean getException() {
        return exception;
    }

    public void addField(Parameter parameter) {
        this.variableContainer.add(parameter);
    }

    public VariableContainer getFields() {
        return this.variableContainer;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public void setContainer(boolean container) {
        isContainer = container;
    }

    public Map<String, Parameter> getTemplateMap() {
        return templateMap;
    }

    public void setTemplateMap(Map<String, Parameter> transformedTemplateMap) {
        this.templateMap = transformedTemplateMap;
    }

    @Override
    public String toString() {
        return
                names.stream().findFirst().orElse("<n/a>") +
                        (type == null ? "</na>" : " = new " + type.substring(type.lastIndexOf('.') + 1) + "(); // ") +
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
        if (this.type == null || this.type.endsWith(".Object")) {
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
        if (name.startsWith("(")) {
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
        name = name.stream().filter(e -> !e.startsWith("(") && e.length() > 0).collect(Collectors.toList());
        this.names.addAll(name);
    }

    public long getValue() {
        return value;
    }

    public void setValue(Long value) {
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

    public void setProb(DataEventWithSessionId prob) {
        this.prob = prob;
        if (value == 0) {
            value = prob.getValue();
        }
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

//    public ConstructorType getConstructorType() {
//        return constructorType;
//    }
//
//    public void setConstructorType(ConstructorType constructorType) {
//        this.constructorType = constructorType;
//    }

    public void setProbeInfo(DataInfo probeInfo) {
        if (this.dataInfo == null
                || !this.dataInfo.getEventType().equals(EventType.METHOD_EXCEPTIONAL_EXIT)
                || probeInfo.getEventType().equals(EventType.METHOD_EXCEPTIONAL_EXIT)
        ) {
            this.dataInfo = probeInfo;
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

    public FieldSpec.Builder toFieldSpec() {
        String fieldType = getType();
        if (fieldType.contains("$")) {
            fieldType = fieldType.substring(0, fieldType.indexOf('$'));
        }
        TypeName fieldTypeName = ClassName.bestGuess(fieldType);
        if (isContainer) {
            fieldTypeName = ParameterizedTypeName.get((ClassName) fieldTypeName,
                    ClassName.bestGuess(getTemplateMap().get("E").getType()));
        }

        FieldSpec.Builder builder = FieldSpec.builder(
                fieldTypeName,
                getName(), Modifier.PRIVATE
        );
        return builder;
    }

    public void setTemplateParameter(String e, Parameter nextValueParam) {
        isContainer = true;
        this.templateMap.put(e, nextValueParam);
    }

    public void addName(String nameForParameter) {
        if (nameForParameter == null || this.names.contains(nameForParameter) || nameForParameter.startsWith(
                "(") || nameForParameter.length() < 1) {
            return;
        }
        nameForParameter = nameForParameter.replace('$', 'D');
        this.names.add(nameForParameter);
    }

    public boolean hasName(String name) {
        if (nameUsed != null) {
            return nameUsed.equals(name);
        }
        if (name == null || this.names.contains(name) || name.startsWith("(") || name.length() < 1) {
            return true;
        }
        return false;
    }

    public List<String> getNames() {
        return names;
    }

    public String getNameForUse(String methodName) {
        if (nameUsed != null) {
            return nameUsed;
        }
        if (names.size() < 1) {
            return null;
        }
        if (names.size() < 2) {
            return names.get(0);
        }
        int matchedDistance = 99999;
        String matchedString = names.get(0);
        if (methodName == null) {
            methodName = matchedString;
        }
        for (String name : names) {
            int distance = StringUtils.getLevenshteinDistance(name, methodName);
            if (distance < matchedDistance) {
                matchedString = name;
                matchedDistance = distance;
            }
        }
        if (!matchedString.equals(names.get(0))) {
            names.remove(matchedString);
            names.add(0, matchedString);
        }

        nameUsed = matchedString;

        return matchedString;
    }

    public boolean isBooleanType() {
        return type != null && (type.equals("Z") || type.equals("java.lang.Boolean"));
    }

    public boolean isStringType() {
        return type != null && (type.equals("java.lang.String"));
    }

    public boolean isPrimitiveType() {
        // types which are java can build just using their values
        return type != null &&
                (type.length() == 1
                        || type.startsWith("java.lang.Boolean")
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
