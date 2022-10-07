package com.insidious.plugin.pojo;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;

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
    /**
     * Value is either a long number or a string value if the value was actually a Ljava/lang/String
     */
    long value = 0;
    /**
     * name should be a valid java variable name. this will be used inside the generated test cases
     */
    String type;
    private final List<String> names = new LinkedList<>();
    private String stringValue;

    public boolean getException() {
        return exception;
    }

    public Parameter(Long value) {
        this.value = value;
    }

    public Parameter() {
    }

    boolean exception;
    DataEventWithSessionId prob;
    private int index;
    private DataInfo dataInfo;
    private ConstructorType constructorType;
    private MethodCallExpression creatorExpression;
    private final VariableContainer variableContainer = new VariableContainer();

    public void addField(Parameter parameter) {
        this.variableContainer.add(parameter);
    }

    public VariableContainer getFields() {
        return this.variableContainer;
    }

    public void setContainer(boolean container) {
        isContainer = container;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public Map<String, Parameter> getTemplateMap() {
        return templateMap;
    }

    private Map<String, Parameter> templateMap = new HashMap<>();
    private boolean isContainer = false;

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
        if (type.contains("$")) {
            type = type.replace('$', '.');
        }
        this.type = type;
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

    public void addNames(Collection<String> name) {
        name = name.stream().filter(e -> !e.startsWith("(")).collect(Collectors.toList());
        this.names.addAll(name);
    }

    public Object getValue() {
        return stringValue == null ? value : stringValue;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public void setValue(String value) {
        this.stringValue = value;
    }

    public DataEventWithSessionId getProb() {
        return prob;
    }

    public void setProb(DataEventWithSessionId prob) {
//        byte[] existingSerializedData;
//        if (this.prob != null) {
//            existingSerializedData = this.prob.getSerializedValue();
//            if ((existingSerializedData == null || existingSerializedData.length == 0) &&
//                    (prob.getSerializedValue() != null && prob.getSerializedValue().length > 0)) {
//                this.prob = prob;
//            }
//        } else {
            this.prob = prob;
            if (value == 0) {
                value = prob.getValue();
            }
//        }
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
        this.dataInfo = probeInfo;
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
        int result = Math.toIntExact(value);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public FieldSpec.Builder toFieldSpec() {
        return FieldSpec.builder(
                ClassName.bestGuess(getType()),
                getName(), Modifier.PRIVATE
        );
    }

    public void setTemplateParameter(String e, Parameter nextValueParam) {
        isContainer = true;
        this.templateMap.put(e, nextValueParam);
    }

    public void addName(String nameForParameter) {
        if (nameForParameter == null || this.names.contains(nameForParameter) || nameForParameter.startsWith("(")) {
            return;
        }
        nameForParameter = nameForParameter.replace('$', 'D');
        this.names.add(nameForParameter);
    }

    public boolean hasName(String name) {
        return this.names.contains(name);
    }

    public void setTemplateMap(Map<String, Parameter> transformedTemplateMap) {
        this.templateMap = transformedTemplateMap;
    }

    public List<String> getNames() {
        return names;
    }
}
