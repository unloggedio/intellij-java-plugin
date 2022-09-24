package com.insidious.plugin.pojo;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.EventType;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.pojo.dao.ProbeInfo;
import com.j256.ormlite.field.DatabaseField;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;

import javax.lang.model.element.Modifier;
import java.util.*;

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
    Long value;
    /**
     * name should be a valid java variable name. this will be used inside the generated test cases
     */
    String type;
    private final List<String> names = new LinkedList<>();
    private String stringValue;

    public boolean getException() {
        return exception;
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
                names.stream().findFirst() +
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
        this.names.add(0, name);
    }

    public void addNames(Collection<String> name) {
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
        this.prob = prob;
        if (value == null || value == 0) {
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

        if (value != null ? !value.equals(parameter.value) : parameter.value != null) return false;
        return type != null ? type.equals(parameter.type) : parameter.type == null;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
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
        this.names.add(nameForParameter);
    }

    public boolean hasName(String name) {
        return this.names.contains(name);
    }

    public void setTemplateMap(Map<String, Parameter> transformedTemplateMap) {
        this.templateMap = transformedTemplateMap;
    }
}
