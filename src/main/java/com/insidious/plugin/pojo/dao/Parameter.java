package com.insidious.plugin.pojo.dao;

import com.insidious.common.weaver.DataInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.insidious.plugin.util.Strings;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parameter is a variable (long id or string) with a name and type information (class name). It could
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
    @DatabaseField
    String type;
    @DatabaseField
    boolean exception;
    @DatabaseField
    long prob_id;
    /**
     * name should be a valid java variable name. this will be used inside the generated test cases
     */
    @DatabaseField
    private String names = "";
    @DatabaseField
    private int probeInfo_id;
    private List<Parameter> templateMap = new ArrayList<>();
    private boolean isContainer = false;

    public static Parameter fromParameter(com.insidious.plugin.pojo.Parameter e) {
        if (e == null) {
            return null;
        }
        Parameter newParam = new Parameter();
        newParam.setContainer(e.isContainer());
        newParam.setException(e.isException());
        newParam.setProb_id(e.getProb());
        newParam.setType(e.getType());
        List<com.insidious.plugin.pojo.Parameter> templateMap1 = e.getTemplateMap();
        List<Parameter> transformedTemplateMap = new ArrayList<>();
        for (com.insidious.plugin.pojo.Parameter param : templateMap1) {
            transformedTemplateMap.add(Parameter.fromParameter(param));
        }
        newParam.setNames(e.getNames());

        newParam.setTemplateMap(transformedTemplateMap);
        newParam.setProbeInfo_id(e.getProbeInfo());
        newParam.setValue(e.getValue());
        return newParam;
    }

    public static com.insidious.plugin.pojo.Parameter toParameter(Parameter parameter) {
        if (parameter == null) {
            return null;
        }


        com.insidious.plugin.pojo.Parameter newParam = new com.insidious.plugin.pojo.Parameter();


        newParam.setContainer(parameter.isContainer());
        newParam.setType(parameter.getType());
        newParam.addNames(Arrays.asList(parameter.getNames()));
        List<Parameter> templateMap1 = parameter.getTemplateMap();
        List<com.insidious.plugin.pojo.Parameter> transformedTemplateMap = new ArrayList<>();
        for (Parameter param : templateMap1) {
            transformedTemplateMap.add(Parameter.toParameter(param));
        }

        newParam.setTemplateMap(transformedTemplateMap);
        newParam.setValue(parameter.getValue());


        return newParam;
    }

    public boolean getException() {
        return exception;
    }

    public void setException(boolean exception) {
        this.exception = exception;
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

    public void setTemplateMap(List<Parameter> templateMap) {
        this.templateMap = templateMap;
    }

    @Override
    public String toString() {
        String[] namesList = names.split(",");
        return
                (namesList.length > 0 ? namesList[0] : "") +
                        (type == null ? "" : "new " + type.substring(type.lastIndexOf('.') + 1) + "(); // ") +
                        "{" + "value=" + value +
                        ", probeInfo=" + probeInfo_id +
                        ", prob=" + prob_id +
                        '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        if (names.length() == 0) {
            return null;
        }
        return names.split(",")[0];
    }

    public String[] getNames() {
        return names.split(",");
    }

    public void setNames(List<String> names) {
        if (names.size() == 0) {
            return;
        }
        this.names = Strings.join(names.stream().filter(e -> e.length() > 1).collect(Collectors.toList()), ",");
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public long getProb_id() {
        return prob_id;
    }

    public void setProb_id(DataEventWithSessionId prob_id) {
        this.prob_id = prob_id.getNanoTime();
    }

    public int getProbeInfo_id() {
        return probeInfo_id;
    }

    public void setProbeInfo_id(DataInfo probeInfo_id) {
        this.probeInfo_id = probeInfo_id.getDataId();
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

}
