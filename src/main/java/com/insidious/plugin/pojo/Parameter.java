package com.insidious.plugin.pojo;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.Map;

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
    private MethodCallExpression creatorExpression;

    public boolean isContainer() {
        return isContainer;
    }

    public Map<String, Parameter> getTemplateMap() {
        return templateMap;
    }

    private final Map<String, Parameter> templateMap = new HashMap<>();
    private boolean isContainer = false;

    @Override
    public String toString() {
        return "[Name="+ name +"][Type="+type+"]{" +
                "value=" + value +
                ", index=" + index +
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
        if (name != null ? !name.equals(parameter.name) : parameter.name != null) return false;
        return type != null ? type.equals(parameter.type) : parameter.type == null;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
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
}
