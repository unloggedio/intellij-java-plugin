package com.insidious.plugin.pojo;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import org.apache.commons.lang.StringUtils;

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
    private final List<String> names = new LinkedList<>();
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
    private String stringValue = null;
    private int index;
    private DataInfo dataInfo;
    private ConstructorType constructorType;
    private MethodCallExpression creatorExpression;
    private Map<String, Parameter> templateMap = new HashMap<>();
    private boolean isContainer = false;

    public Parameter(Long value) {
        this.value = value;
    }

    public Parameter() {
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
        if (this.type == null) {
            this.type = type;
        }
    }

    public String getName() {
        if (names.size() == 0) {
            return null;
        }
        return names.get(0);
    }

    public void setName(String name) {
        if (name.startsWith("(")) {
            return;
        }
        if (name != null && !this.names.contains(name)) {
            name = name.replace('$', 'D');
            this.names.add(0, name);
        }
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

    public ConstructorType getConstructorType() {
        return constructorType;
    }

    public void setConstructorType(ConstructorType constructorType) {
        this.constructorType = constructorType;
    }

    public MethodCallExpression getCreatorExpression() {
        return creatorExpression;
    }

    public void setCreator(MethodCallExpression createrExpression) {

        this.creatorExpression = createrExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Parameter parameter = (Parameter) o;

//        if (value != null ? !value.equals(parameter.value) : parameter.value != null) return false;
        return type != null ? type.equals(parameter.type) : parameter.type == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    public FieldSpec.Builder toFieldSpec() {
        String fieldType = getType();
        if (fieldType.contains("$")) {
            fieldType = fieldType.substring(0, fieldType.indexOf('$'));
        }
        return FieldSpec.builder(
                ClassName.bestGuess(fieldType),
                getName(), Modifier.PRIVATE
        );
    }

    public void setTemplateParameter(String e, Parameter nextValueParam) {
        isContainer = true;
        this.templateMap.put(e, nextValueParam);
    }

    public void addName(String nameForParameter) {
        if (nameForParameter == null || this.names.contains(nameForParameter) || nameForParameter.startsWith("(") || nameForParameter.length() < 1) {
            return;
        }
        nameForParameter = nameForParameter.replace('$', 'D');
        this.names.add(nameForParameter);
    }

    public boolean hasName(String name) {
        if (name == null || this.names.contains(name) || name.startsWith("(") || name.length() < 1) {
            return true;
        }
        return false;
    }

    public List<String> getNames() {
        return names;
    }

    public String getClosestName(String type, String methodName) {
        if (names.size() < 1) {
            return null;
        }
        if (names.size() < 2) {
            return names.get(0);
        }
        int matchedDistance = 99999;
        String matchedString = names.get(0);
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

        return matchedString;
    }

    public boolean isBooleanType() {
        return type != null && (type.equals("Z") || type.equals("java.lang.Boolean"));
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
