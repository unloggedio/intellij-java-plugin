package com.insidious.plugin.pojo;


import com.intellij.openapi.module.Module;

public class ModuleInformation {

    private String name;
    private String type;
    private String path;
    private Module module;

    public ModuleInformation() {
    }

    public ModuleInformation(String name, String type, String path) {
        this.name = filerModuleName(name);
        this.type = type;
        this.path = filterPath(path);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = filerModuleName(name);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = filterPath(path);
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    private String filterPath(String path) {
        //project
        if (path.contains("/.idea")) {
            return path.split("/\\.idea")[0];
        } else {
            //internal module
            return path.substring(0, path.lastIndexOf("/"));
        }
    }

    private String filerModuleName(String modulename) {
        if (modulename.contains(".")) {
            String parts[] = modulename.split("\\.");
            return parts[parts.length - 1];
        }
        return modulename;
    }
}
