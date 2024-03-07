package com.insidious.plugin.pojo.dao;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.MethodSignatureParser;
import com.insidious.plugin.util.ClassTypeUtils;
import com.insidious.plugin.util.StringUtils;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.stream.Collectors;

@DatabaseTable(tableName = "method_definition")
public class MethodDefinition implements Comparable<MethodDefinition> {

    @DatabaseField(id = true)
    private int id;

    @DatabaseField
    private String argumentTypes;
    @DatabaseField
    private String methodName;
    @DatabaseField
    private boolean isStatic;
    @DatabaseField
    private boolean isPublic;

    @DatabaseField
    private boolean usesFields;

    @DatabaseField(index = true)
    private String ownerType;

    @DatabaseField
    private String returnType;
    @DatabaseField(index = true)
    private int methodAccess;

    @DatabaseField
    private String methodDescriptor;
    @DatabaseField
    private int lineCount;

    private String methodHash;
    private String sourceFileName;
    private int classId;

    public MethodDefinition(int id, String argumentTypes, String methodName, boolean isStatic,
                            boolean usesFields, String ownerType, String returnType, int methodAccess,
                            String methodDescriptor) {
        this.id = id;
        this.argumentTypes = argumentTypes;
        this.methodName = methodName;
        this.isStatic = isStatic;
        this.usesFields = usesFields;
        this.ownerType = ownerType;
        this.returnType = returnType;
        this.methodAccess = methodAccess;
        this.methodDescriptor = methodDescriptor;
    }

    public MethodDefinition() {
    }

    public static MethodDefinition fromMethodInfo(
            MethodInfo methodInfo, ClassInfo classInfo,
            boolean usesFields, int lineCount) {
        MethodDefinition methodDefinition = new MethodDefinition();
        methodDefinition.setMethodAccess(methodInfo.getAccess());
        methodDefinition.setMethodName(methodInfo.getMethodName());
        methodDefinition.setId(methodInfo.getMethodId());
        methodDefinition.setLineCount(lineCount);


        methodDefinition.setMethodHash(methodInfo.getMethodHash());
        methodDefinition.setSourceFileName(methodInfo.getSourceFileName());
        methodDefinition.setOwnerType(ClassTypeUtils.getJavaClassName(methodInfo.getClassName()));

        List<String> descriptorParsed = MethodSignatureParser.parseMethodSignature(methodInfo.getMethodDesc());

        methodDefinition.setReturnType(
                ClassTypeUtils.getJavaClassName(descriptorParsed.get(descriptorParsed.size() - 1)));
        descriptorParsed.remove(descriptorParsed.size() - 1);
        methodDefinition.setArgumentTypes(StringUtils.join(descriptorParsed.stream()
                .map(ClassTypeUtils::getJavaClassName)
                .collect(Collectors.toList()), ","));

        methodDefinition.setClassId(classInfo.getClassId());
        methodDefinition.setUsesFields(usesFields);
        methodDefinition.setStatic((methodInfo.getAccess() & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC);
        methodDefinition.setPublic((methodInfo.getAccess() & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC);

        methodDefinition.setMethodDescriptor(methodInfo.getMethodDesc());

        return methodDefinition;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getArgumentTypes() {
        return argumentTypes;
    }

    public void setArgumentTypes(String argumentTypes) {
        this.argumentTypes = argumentTypes;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public boolean isUsesFields() {
        return usesFields;
    }

    public void setUsesFields(boolean usesFields) {
        this.usesFields = usesFields;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public int getMethodAccess() {
        return methodAccess;
    }

    public void setMethodAccess(int methodAccess) {
        this.methodAccess = methodAccess;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public void setMethodDescriptor(String methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public String getMethodHash() {
        return methodHash;
    }

    public void setMethodHash(String methodHash) {
        this.methodHash = methodHash;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    @Override
    public int compareTo(MethodDefinition o) {
        return Integer.compare(this.id, o.id);
    }
}
