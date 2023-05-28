package com.insidious.plugin.util;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.pojo.DataEventWithSessionId;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

public class LoggerUtil {

    private static final Logger logger = getInstance(LoggerUtil.class);

    public static Logger getInstance(@NotNull Class<?> cl) {
        return Logger.getInstance(cl);
    }

    public static Logger getInstance(String className) {
        return Logger.getInstance(className);
    }

    public static void logEvent(
            String tag,
            int callStack, int i,
            DataEventWithSessionId historyEvent,
            DataInfo historyEventProbe,
            ClassInfo currentClassInfo,
            MethodInfo methodInfoLocal
    ) {
        logger.warn("[" + tag + "] #" + i +
                ", T=" + historyEvent.getEventId() +
                ", P=" + String.format("%-7s", historyEvent.getProbeId()) +
                ", V=" + String.format("%-10s", historyEvent.getValue()) +
                " [Stack:" + callStack + "]" +
                " " + String.format("%-25s", historyEventProbe.getEventType())
                + " in " + String.format("%-25s",
                currentClassInfo.getClassName().substring(currentClassInfo.getClassName().lastIndexOf("/") + 1) + ".java")
                + ":" + historyEventProbe.getLine()
                + " in " + String.format("%-20s", methodInfoLocal.getMethodName())
                + "  -> " + historyEventProbe.getAttributes());
    }
}
