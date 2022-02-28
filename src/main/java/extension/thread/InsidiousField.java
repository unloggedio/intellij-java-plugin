package extension.thread;

import com.sun.jdi.*;
import extension.model.DataInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsidiousField implements Field {

    private final DataInfo dataInfo;

    private InsidiousField(DataInfo dataInfo) {
        this.dataInfo = dataInfo;
    }

    @Override
    public String typeName() {
        return dataInfo.getAttribute("FieldName", "n/a");
    }

    @Override
    public Type type() throws ClassNotLoadedException {
        return null;
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public boolean isVolatile() {
        return false;
    }

    @Override
    public boolean isEnumConstant() {
        return false;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String signature() {
        return null;
    }

    @Override
    public String genericSignature() {
        return null;
    }

    @Override
    public ReferenceType declaringType() {
        return null;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isSynthetic() {
        return false;
    }

    @Override
    public int modifiers() {
        return 0;
    }

    @Override
    public boolean isPrivate() {
        return false;
    }

    @Override
    public boolean isPackagePrivate() {
        return false;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public VirtualMachine virtualMachine() {
        return null;
    }

    @Override
    public int compareTo(@NotNull Field o) {
        return 0;
    }

    public static class Factory {
        public static Map<String, Field> mapToFields(List<DataInfo> dataInfoList) {
            Map<String, Field> fieldMap = new HashMap<>();
            return fieldMap;
        }

        public InsidiousField createField(DataInfo dataInfo) {
            return new InsidiousField(dataInfo);
        }
    }
}
