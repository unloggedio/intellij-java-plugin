package com.insidious.plugin.util;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

public class LoggerUtil {

    public static Logger getInstance(@NotNull Class<?> cl) {
        return Logger.getInstance(cl);
    }

    public static Logger getInstance(String className) {
        return Logger.getInstance(className);
    }
}
