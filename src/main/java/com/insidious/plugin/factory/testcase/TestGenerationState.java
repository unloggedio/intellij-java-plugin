package com.insidious.plugin.factory.testcase;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.insidious.plugin.client.ParameterNameFactory;
import com.insidious.plugin.factory.testcase.parameter.VariableContainer;
import com.insidious.plugin.factory.testcase.util.ClassTypeUtils;
import com.insidious.plugin.pojo.Parameter;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.apache.commons.codec.digest.DigestUtils;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This container will hold the intermediate state of the script being generated, to keep track of
 * - created variables
 * - mocked calls
 */
public class TestGenerationState {
    private static final Gson gson = new Gson();
    private static final Pattern ENDS_WITH_DIGITS = Pattern.compile("(.+)([0-9]+)$");
    private static final Logger logger = LoggerUtil.getInstance(TestGenerationState.class);
    private final Map<String, Object> valueResourceMap = new HashMap<>();
    private final Map<String, String> valueResourceStringMap = new HashMap<>();
    private final ParameterNameFactory parameterNameFactory;
    private VariableContainer variableContainer;
    private Map<String, Boolean> mockedCallsMap = new HashMap<>();
    private boolean setupNeedsJsonResources;

    public TestGenerationState(ParameterNameFactory parameterNameFactory) {
        this.parameterNameFactory = parameterNameFactory;
    }

    public ParameterNameFactory getParameterNameFactory() {
        return parameterNameFactory;
    }

    public VariableContainer getVariableContainer() {
        return variableContainer;
    }

    public void setVariableContainer(VariableContainer variableContainer) {
        this.variableContainer = variableContainer;
    }

    public Map<String, Boolean> getMockedCallsMap() {
        return mockedCallsMap;
    }

    public void setMockedCallsMap(Map<String, Boolean> mockedCallsMap) {
        this.mockedCallsMap = mockedCallsMap;
    }

    public Map<String, Object> getValueResourceMap() {
        return valueResourceMap;
    }

    public String addObjectToResource(Parameter lhsExpression) {
        String targetObjectName = parameterNameFactory.getNameForUse(lhsExpression, null);
        Matcher matcher = ENDS_WITH_DIGITS.matcher(targetObjectName);
        if (matcher.matches()) {
            targetObjectName = matcher.group(1);
        }

        String value = new String(lhsExpression.getProb()
                .getSerializedValue());
        String valueHash = DigestUtils.md5Hex(value);
        if (valueResourceStringMap.containsKey(valueHash)) {
            valueResourceStringMap.get(valueHash);
        }
        String referenceNameForValue = null;
        for (int i = 0; i < 100; i++) {
            referenceNameForValue = targetObjectName + i;
            if (!valueResourceMap.containsKey(referenceNameForValue)) {
                Object value1;
                try {
                    value1 = gson.fromJson(value, JsonElement.class);
                } catch (JsonSyntaxException jse) {
                    value1 = new String(lhsExpression.getProb()
                            .getSerializedValue());
                    logger.warn("Object was not serialized properly: " + value1 + " -> " + jse.getMessage());
                }
                valueResourceMap.put(referenceNameForValue, value1);
                valueResourceStringMap.put(valueHash, referenceNameForValue);
                break;
            }
        }
        return referenceNameForValue;
    }

    public FieldSpec.Builder toFieldSpec(Parameter parameter) {
        String fieldType = parameter.getType();
        if (fieldType.contains("$")) {
            fieldType = fieldType.substring(0, fieldType.indexOf('$'));
        }
        TypeName fieldTypeName = ClassName.bestGuess(fieldType);
        if (parameter.isContainer()) {
            TypeName[] typeArguments =
                    parameter.getTemplateMap()
                            .stream()
                            .map(e -> ClassTypeUtils.createTypeFromNameString(e.getType()))
                            .toArray(TypeName[]::new);
            fieldTypeName = ParameterizedTypeName.get((ClassName) fieldTypeName, typeArguments);
        }

        return FieldSpec.builder(
                fieldTypeName, parameterNameFactory.getNameForUse(parameter, null), Modifier.PRIVATE
        );
    }

    public void generateParameterName(Parameter returnValue, String methodName) {
        String variableName = ClassTypeUtils.createVariableNameFromMethodName(methodName, returnValue.getType());

        Object value = returnValue.getValue();

        Parameter existingVariableById = variableContainer.getParametersById((long) value);
        if (existingVariableById != null) {
            if (!Objects.equals(returnValue.getName(), existingVariableById.getName())) {
                returnValue.setName(existingVariableById.getName());
                parameterNameFactory.setNameForParameter(returnValue, existingVariableById.getName());
            }
        } else {
            if (parameterNameFactory.getNameForUse(returnValue, methodName) == null) {
                returnValue.setName(variableName);
                parameterNameFactory.setNameForParameter(returnValue, variableName);
            }
        }

    }

    public boolean isSetupNeedsJsonResources() {
        return setupNeedsJsonResources;
    }

    public void setSetupNeedsJsonResources(boolean setupNeedsJsonResources) {
        this.setupNeedsJsonResources = setupNeedsJsonResources;
    }
}
