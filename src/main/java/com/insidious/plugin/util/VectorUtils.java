package com.insidious.plugin.util;

import com.insidious.plugin.pojo.TracePoint;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
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

    public static Vector<Object> tracePointToRowVector(TracePoint tracePoint) {
        String className = tracePoint.getClassname().substring(
                tracePoint.getClassname().lastIndexOf('/') + 1);
        return new Vector<>(
                List.of(
                        getUnqualifiedClassName(tracePoint.getExceptionClass()),
                        className,
                        String.valueOf(tracePoint.getLineNumber()),
                        String.valueOf(tracePoint.getThreadId()),
                        new Date(tracePoint.getRecordedAt()).toString()
                )
        );
    }

    @NotNull
    private static String getUnqualifiedClassName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf('.') + 1);
        } else if (className.contains("/")) {
            return className.substring(className.lastIndexOf('/') + 1);
        }
        return className;
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
