package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.pojo.ProjectTypeInfo;

import java.util.*;

public class VMoptionsConstructionService {

    private String JVMoptionsBase = "";
    //private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH;
    public boolean addopens = false;
    private final String addOpensOption = "--add-opens=java.base/java.util=ALL-UNNAMED";
    public String basePackage = "your.package";
    private Map<ProjectTypeInfo.RUN_TYPES,String> runType_CommandMap = new TreeMap<>();
    String quoute_type = "\""; // use "\"" or "'"
    private Set<String> included_packages = new TreeSet<>();

    public VMoptionsConstructionService()
    {
        runType_CommandMap.put(ProjectTypeInfo.RUN_TYPES.MAVEN_CLI,
                "mvn spring-boot:run -Dspring-boot.run.jvmArguments=\"{PARAMS}\" {OPENS}");
        runType_CommandMap.put(ProjectTypeInfo.RUN_TYPES.GRADLE_CLI,
                "apply plugin: \"java\"\n" +
                        "apply plugin: \"application\"\n" +
                        "\n" +
                        "application {\n" +
                        "\tapplicationDefaultJvmArgs = [\"{PARAMS}\",\n" +
                        "\"{OPENS}\"]\n" +
                        "}");
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
        List<String> packages = new ArrayList<>(this.included_packages);
        for(String pack : packages)
        {
            newVMParams.append("i=" + pack+",");
        }
        newVMParams.deleteCharAt(newVMParams.length()-1);
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
        List<String> packages = new ArrayList<>(this.included_packages);
        for(String pack : packages)
        {
            newVMParams.append("i=" + pack+",");
        }
        newVMParams.deleteCharAt(newVMParams.length()-1);
        newVMParams.append(quoute_type);
        return newVMParams.toString();
    }

    public String getVMParametersWithQuoteType(String quoteSample) {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(getJVMoptionsBase(quoteSample));
        List<String> packages = new ArrayList<>(this.included_packages);
        for(String pack : packages)
        {
            newVMParams.append("i=" + pack+",");
        }
        newVMParams.deleteCharAt(newVMParams.length()-1);
        newVMParams.append(quoteSample);
        return newVMParams.toString();
    }

    private String getJVMoptionsBase() {
        String vmoptions = "-javaagent:" +quoute_type+ Constants.AGENT_PATH + "=";
        return vmoptions;
    }

    private String getJVMoptionsBase(String quoute_type) {
        String vmoptions = "-javaagent:" +quoute_type+ Constants.AGENT_PATH + "=";
        return vmoptions;
    }

    public String getPrettyVMtext() {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(JVMoptionsBase);
        List<String> packages = new ArrayList<>(this.included_packages);
        for(String pack : packages)
        {
            newVMParams.append("\ni=" + pack+",");
        }
        newVMParams.deleteCharAt(newVMParams.length()-1);
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

        this.basePackage = basePackage.trim();
        if(basePackage.contains(","))
        {
            String[] parts = basePackage.trim().split(",");
            this.included_packages = new TreeSet<>(Arrays.asList(parts));
        }
        else
        {
            this.included_packages = new TreeSet<>();
            this.included_packages.add(basePackage);
        }
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
        return command.trim();
    }
}
