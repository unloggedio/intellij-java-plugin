package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.pojo.ProjectTypeInfo;

import java.util.Map;
import java.util.TreeMap;

public class VMoptionsConstructionService {

    private String JVMoptionsBase = "";
    //private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH;
    public boolean addopens = false;
    private final String addOpensOption = "--add-opens=java.base/java.util=ALL-UNNAMED";
    public String basePackage = "your.package";
    private Map<ProjectTypeInfo.RUN_TYPES,String> runType_CommandMap = new TreeMap<>();
    String quoute_type = "\""; // use "\"" or "'"

    public VMoptionsConstructionService()
    {
        runType_CommandMap.put(ProjectTypeInfo.RUN_TYPES.MAVEN_CLI,
                "mvn spring-boot:run -Dspring-boot.run.jvmArguments=\"{PARAMS}\" {OPENS}");
        runType_CommandMap.put(ProjectTypeInfo.RUN_TYPES.GRADLE_CLI,
                "applicationDefaultJvmArgs = [\"{PARAMS}\",\n" +
                        "\"{OPENS}\"]");
        runType_CommandMap.put(ProjectTypeInfo.RUN_TYPES.INTELLIJ_APPLICATION,
                "{PARAMS} {OPENS}");
        runType_CommandMap.put(ProjectTypeInfo.RUN_TYPES.JAVA_JAR_CLI,
                "java {PARAMS} " +
                        "{OPENS} -jar {target.jar}");

        this.JVMoptionsBase = getJVMoptionsBase();

    }
    public String getVMParametersFull() {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(JVMoptionsBase);
        newVMParams.append("i=" + basePackage);
        newVMParams.append(quoute_type);
        if(this.addopens)
        {
            newVMParams.append(" "+addOpensOption);
        }
        return newVMParams.toString();
    }
    public String getVMParameters() {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(JVMoptionsBase);
        newVMParams.append("i=" + basePackage);
        newVMParams.append(quoute_type);
        return newVMParams.toString();
    }

    public String getVMParametersWithQuoteType(String quoteSample) {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(getJVMoptionsBase(quoteSample));
        newVMParams.append("i=" + basePackage);
        newVMParams.append(quoteSample);
        return newVMParams.toString();
    }

    private String getJVMoptionsBase() {
        String vmoptions = "-javaagent:" +quoute_type+ Constants.VIDEOBUG_AGENT_PATH+ "=";
        return vmoptions;
    }

    private String getJVMoptionsBase(String quoute_type) {
        String vmoptions = "-javaagent:" +quoute_type+ Constants.VIDEOBUG_AGENT_PATH+ "=";
        return vmoptions;
    }

    public String getPrettyVMtext() {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(JVMoptionsBase);
        newVMParams.append("\ni=" + basePackage);
        newVMParams.append(quoute_type);
        if(this.addopens)
        {
            newVMParams.append("\n"+addOpensOption);
        }
        return newVMParams.toString();
    }

    public void setAddopens(boolean addopens)
    {
        this.addopens = addopens;
    }

    public String getOpensStringIfNeeded()
    {
        if(this.addopens)
        {
            return addOpensOption;
        }
        return "";
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getVMOptionsForRunType(ProjectTypeInfo.RUN_TYPES runType)
    {
        String quoteType="\"";
        if(runType.equals(ProjectTypeInfo.RUN_TYPES.MAVEN_CLI))
        {
            quoteType = "\\\"";
        }
        else if(runType.equals(ProjectTypeInfo.RUN_TYPES.GRADLE_CLI))
        {
            quoteType = "";
        }
        String command = runType_CommandMap.get(runType);
        command = command.replace("{PARAMS}",getVMParametersWithQuoteType(quoteType));
        command = command.replace("{OPENS}",getOpensStringIfNeeded());
        return command;
    }

}
