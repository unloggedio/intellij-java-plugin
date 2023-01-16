package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;

public class VMoptionsConstructionService {

    private String JVMoptionsBase = "";
    private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH;
    public boolean addopens = false;

    public VMoptionsConstructionService()
    {
        this.JVMoptionsBase = getJVMoptionsBase();

    }
    public String getVMParameters(String basepackage) {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(JVMoptionsBase);
        newVMParams.append("i=" + basepackage);
        newVMParams.append("\"");
        return newVMParams.toString();
    }

    private String getJVMoptionsBase() {
        String vmoptions = javaAgentString + "=";
        return vmoptions;
    }

    public String getPrettyVMtext(String basepackage) {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(JVMoptionsBase);
        newVMParams.append("\ni=" + basepackage);
        newVMParams.append("\"");
        if(this.addopens)
        {
            newVMParams.append("\n--add-opens=java.base/java.util=ALL-UNNAMED");
        }
        return newVMParams.toString();
    }

    public void setAddopens(boolean addopens)
    {
        this.addopens = addopens;
    }
}
