package com.insidious.plugin.UITests.wrapper;

import java.util.List;

public class FileMethodWrapper {

    List<MethodWiseGutterIcons> methodWiseGutterIconsList;

    public FileMethodWrapper(List<MethodWiseGutterIcons> methodWiseGutterIconsList) {
        this.methodWiseGutterIconsList = methodWiseGutterIconsList;
    }

    public List<MethodWiseGutterIcons> getMethodWiseGutterIconsList() {
        return methodWiseGutterIconsList;
    }
}
