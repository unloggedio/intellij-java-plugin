package com.insidious.plugin.factory;

import com.insidious.plugin.Constants;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.intellij.pom.java.LanguageLevel;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class VMoptionsConstructionService {

    private static final String addOpensOption = "--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java" +
            ".base/java" +
            ".lang=ALL-UNNAMED";
    //private String javaAgentString = "-javaagent:\"" + Constants.VIDEOBUG_AGENT_PATH;
//    public boolean addOpens = false;
//    public String basePackage = "your.package";
//    String quoteType = "\""; // use "\"" or "'"
//    private String JVMoptionsBase = "";
    private Map<ProjectTypeInfo.RUN_TYPES, String> runType_CommandMap = new TreeMap<>();
//    private Set<String> included_packages = new TreeSet<>();

    public VMoptionsConstructionService() {
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

//        this.JVMoptionsBase = getJVMoptionsBase(quoteType);

    }

//    public String getVMParametersFull(boolean addOpens) {
//        StringBuilder newVMParams = new StringBuilder();
//        newVMParams.append(JVMoptionsBase);
//        List<String> packages = new ArrayList<>(this.included_packages);
//        for (String pack : packages) {
//            newVMParams.append("i=" + pack + ",");
//        }
//        newVMParams.deleteCharAt(newVMParams.length() - 1);
//        newVMParams.append(quoteType);
//        if (addOpens) {
//            newVMParams.append(" " + addOpensOption);
//        }
//        return newVMParams.toString();
//    }

//    public String getVMParameters() {
//        StringBuilder newVMParams = new StringBuilder();
//        newVMParams.append(JVMoptionsBase);
//        List<String> packages = new ArrayList<>(this.included_packages);
//        for (String pack : packages) {
//            newVMParams.append("i=").append(pack).append(",");
//        }
//        newVMParams.deleteCharAt(newVMParams.length() - 1);
//        newVMParams.append(quoute_type);
//        return newVMParams.toString();
//    }

    public String getVMParametersWithQuoteType(String quoteSample, List<String> packages) {
        StringBuilder newVMParams = new StringBuilder();
        newVMParams.append(getJVMoptionsBase(quoteSample));
        for (String pack : packages) {
            newVMParams.append("i=").append(pack).append(",");
        }
        newVMParams.deleteCharAt(newVMParams.length() - 1);
        newVMParams.append(quoteSample);
        return newVMParams.toString();
    }

    private String getJVMoptionsBase(String quoteType) {
        return "-javaagent:" + quoteType + Constants.AGENT_PATH + "=";
    }


    public String getOpensStringIfNeeded(boolean addOpens) {
        if (addOpens) {
            return addOpensOption;
        }
        return "";
    }

    public String getVMOptionsForRunType(
            ProjectTypeInfo.RUN_TYPES runType,
            LanguageLevel languageLevel,
            List<String> includedPackages) {
        boolean addOpens = languageLevel.isAtLeast(LanguageLevel.JDK_11);
        String quoteType = "\"";
        if (runType.equals(ProjectTypeInfo.RUN_TYPES.MAVEN_CLI)) {
            quoteType = "\\\"";
        } else if (runType.equals(ProjectTypeInfo.RUN_TYPES.GRADLE_CLI)) {
            quoteType = "";
        }
        String command = runType_CommandMap.get(runType);
        command = command.replace("{PARAMS}", getVMParametersWithQuoteType(quoteType, includedPackages));
        command = command.replace("{OPENS}", getOpensStringIfNeeded(addOpens));
        return command.trim();
    }
}
