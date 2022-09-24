package com.insidious.plugin.pojo.dao;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.pojo.ConstructorType;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Parameter is a value (long id or string) with a name and type information (class name). It could
 * be a variable passed as a method argument, or the
 * test subject or the return value from the function. Store the corresponding probeInfo and
 * event also from where the information was identified
 */
@DatabaseTable(tableName = "parameter")
public class Parameter {
    /**
     * Value is either a long number or a string value if the value was actually a Ljava/lang/String
     */
    @DatabaseField(id = true)
    Long value;
    /**
     * name should be a valid java variable name. this will be used inside the generated test cases
     */
    @DatabaseField
    String type;
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private final String[] names = new String[0];
    @DatabaseField
    private String stringValue;


    @DatabaseField
    boolean exception;
    @DatabaseField(foreign = true)
    DataEventWithSessionId prob;
    @DatabaseField
    private int index;
    @DatabaseField(foreign = true)
    private ProbeInfo probeInfo;
    private ConstructorType constructorType;
    @DatabaseField(foreign = true)
    private MethodCallExpression creatorExpression;

    public static Parameter fromParameter(com.insidious.plugin.pojo.Parameter e) {
        if (e == null) {
            return null;
        }
        Parameter newParam = new Parameter();
        newParam.setContainer(e.isContainer());
        newParam.setCreator(MethodCallExpression.FromMCE(e.getCreatorExpression()));
        newParam.setException(e.getException());
        newParam.setProb(e.getProb());
        newParam.setType(e.getType());
        Map<String, com.insidious.plugin.pojo.Parameter> templateMap1 = e.getTemplateMap();
        Map<String, Parameter> transformedTemplateMap = new HashMap<>();
        for (String s : templateMap1.keySet()) {
            com.insidious.plugin.pojo.Parameter param = templateMap1.get(s);
            transformedTemplateMap.put(s, Parameter.fromParameter(param));
        }

        newParam.setTemplateMap(transformedTemplateMap);
        newParam.setConstructorType(e.getConstructorType());
        newParam.setProbeInfo(e.getProbeInfo());
        newParam.setValue((long) e.getValue());
        return newParam;
    }

    public static com.insidious.plugin.pojo.Parameter toParameter(Parameter parameter) {
        if (parameter == null) {
            return null;
        }


        com.insidious.plugin.pojo.Parameter newParam = new com.insidious.plugin.pojo.Parameter();


        newParam.setContainer(parameter.isContainer());
//        newParam.setCreator(MethodCallExpression.FromMCE(e.getCreatorExpression()));
//        newParam.setException(e.getException());
        newParam.setProb(parameter.getProb());
        newParam.setType(parameter.getType());
        newParam.addNames(Arrays.asList(parameter.getNames()));
        Map<String, Parameter> templateMap1 = parameter.getTemplateMap();
        Map<String, com.insidious.plugin.pojo.Parameter> transformedTemplateMap = new HashMap<>();
        for (String s : templateMap1.keySet()) {
            Parameter param = templateMap1.get(s);
            transformedTemplateMap.put(s, Parameter.toParameter(param));
        }

        newParam.setTemplateMap(transformedTemplateMap);
        newParam.setConstructorType(parameter.getConstructorType());
        newParam.setValue((long) parameter.getValue());


        return newParam;
    }

    public boolean getException() {
        return exception;
    }

    public void setException(boolean exception) {
        this.exception = exception;
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

    public void setTemplateMap(Map<String, Parameter> templateMap) {
        this.templateMap = templateMap;
    }

    private Map<String, Parameter> templateMap = new HashMap<>();
    private boolean isContainer = false;

    @Override
    public String toString() {
        return
                (names.length > 0 ? names[0] : "") +
                        (type == null ? "" : "new " + type.substring(type.lastIndexOf('.') + 1) + "(); // ") +
                        "{" + "value=" + value +
                        ", index=" + index +
                        ", probeInfo=" + probeInfo +
                        ", prob=" + prob +
                        '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type != null && type.contains("$")) {
            type = type.replace('$', '.');
        }
        this.type = type;
    }

    public String getName() {
        if (names.length == 0) {
            return null;
        }
        return names[0];
    }

    public String[] getNames() {
        return names;
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
        if (value == null && prob != null) {
            value = prob.getValue();
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ProbeInfo getProbeInfo() {
        return probeInfo;
    }

    public void setProbeInfo(DataInfo probeInfo) {
        this.probeInfo = ProbeInfo.FromProbeInfo(probeInfo);
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


}
