package com.insidious.plugin.util;

import java.util.Vector;

public class VectorUtils {

    public static Vector<Vector<Object>> convertToVector(Object[][] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector<Vector<Object>> v = new Vector<>(anArray.length);
        for (Object[] o : anArray) {
            v.addElement(convertToVector(o));
        }
        return v;
    }

    public static Vector<Object> convertToVector(Object[] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector<Object> v = new Vector<>(anArray.length);
        for (Object o : anArray) {
            v.addElement(o);
        }
        return v;
    }

}
