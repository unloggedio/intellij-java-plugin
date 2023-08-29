package com.insidious.plugin;

import com.intellij.AbstractBundle;

import org.jetbrains.annotations.PropertyKey;

public class InsidiousBundle extends AbstractBundle {
    public static final String PATH_TO_BUNDLE = "META-INF.configuration";
    private static final AbstractBundle ourInstance = new InsidiousBundle();

    private InsidiousBundle() {
        super("META-INF.configuration");
    }


    
    public static String message( @PropertyKey(resourceBundle = "META-INF.configuration") String key,  Object... params) {
        return ourInstance.getMessage(key, params);
    }
}


