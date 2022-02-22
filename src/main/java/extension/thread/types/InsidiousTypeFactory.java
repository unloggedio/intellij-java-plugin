package extension.thread.types;

import com.sun.jdi.Type;
import com.sun.jdi.VirtualMachine;

import java.util.HashMap;
import java.util.Map;

public class InsidiousTypeFactory {

    private static final Map<String, Type> typeMap = new HashMap<>();

    public static Type typeFrom(String typeName, String variableSignature, VirtualMachine virtualMachine) {
        if (typeMap.containsKey(typeName)) {
            return typeMap.get(typeName);
        }
        InsidiousType type = new InsidiousType(typeName, variableSignature, virtualMachine);
        typeMap.put(typeName, type);
        return type;
    }
}
