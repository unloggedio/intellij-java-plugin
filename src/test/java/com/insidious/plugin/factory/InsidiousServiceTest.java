package com.insidious.plugin.factory;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class InsidiousServiceTest {

    @Test
    public void addAgentToRunConfig() {

//        Project project = Mockito.mock(Project.class);
//        InsidiousService insidiousService = new InsidiousService(project);
//
        String[] vmParams = new String[]{"no", "param"};

        String newVmParam = VideobugUtils.addAgentToVMParams(String.join(" ", vmParams), "--add-opens=java.base/java.util=ALL-UNNAMED -javaagent:\"agent.jar=i=com/videobug,token=localhost-token\"");
        String[] newVmParams = newVmParam.split(" ");

        Assert.assertTrue(vmParams.length + 2 == newVmParams.length);

        List<String> newParamsList = Arrays.asList(newVmParams);

        Assert.assertTrue(newParamsList.contains("--add-opens=java.base/java.util=ALL-UNNAMED"));
        Assert.assertTrue(newParamsList.contains("-javaagent:\"agent.jar=i=com/videobug,token=localhost-token\""));

        vmParams = VideobugUtils.addAgentToVMParams(newVmParam,
                "--add-opens=java.base/java.util=ALL-UNNAMED -javaagent:\"agent.jar=i=com/videobug,token=localhost-token\"").split(" ");
        Assert.assertTrue(vmParams.length == newVmParams.length);

        vmParams = VideobugUtils.addAgentToVMParams(newVmParam,
                "-javaagent:\"agent.jar=i=com/videobug,token=localhost-token\"").split(" ");
        Assert.assertTrue(vmParams.length == newVmParams.length);

        vmParams = VideobugUtils.addAgentToVMParams(newVmParam,
                "--add-opens=java.base/java.util=ALL-UNNAMED").split(" ");
        Assert.assertTrue(vmParams.length == 3);

    }
}