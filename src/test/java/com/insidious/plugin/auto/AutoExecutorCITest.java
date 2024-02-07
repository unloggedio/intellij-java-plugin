package com.insidious.plugin.auto;

import com.insidious.plugin.agent.AgentCommand;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.auto.autoCIUtils.AgentClientLite;
import com.insidious.plugin.auto.autoCIUtils.AssertionUtils;
import com.insidious.plugin.auto.autoCIUtils.ParseUtils;
import com.insidious.plugin.auto.autoCIUtils.XlsxUtils;
import com.insidious.plugin.auto.entity.AutoAssertionResult;
import com.insidious.plugin.auto.entity.TestUnit;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AutoExecutorCITest {

    final private String testResourcesPath = "auto-test-resources/";
    final private boolean printOnlyFailing = false;

    @Test
    public void runAutoExecutorCI() {

        //create an agent client
        AgentClientLite agentClientLite = new AgentClientLite();
        if (!agentClientLite.isConnected()) {
            System.out.println("Stopping AutoExecutor test as agent is not connected");
            return;
        }

        //fetch and parse test resources.
        URL urlToFile = Thread.currentThread().getContextClassLoader()
                .getResource(testResourcesPath + "maven-demo-ground-truth-integration-mode.xlsx");

        XSSFWorkbook workbook = XlsxUtils.getWorkbook(urlToFile);
        assert workbook != null;

        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        int count = 0;
        int passing = 0;
        int failing = 0;

        System.out.println("\n");
        boolean overallStatus = true;

        while (rowIterator.hasNext()) {
            if (count == 0) {
                count++;
                rowIterator.next();
            } else {
                Row row = rowIterator.next();
                count++;

                //stop if a row elem is null, iterator will keep going
                if (row.getCell(0) == null) {
                    break;
                }

                String targetClassname = row.getCell(0).getStringCellValue();
                String targetMethodInfo = row.getCell(1).getStringCellValue();
                String selectedImplementation = row.getCell(2).getStringCellValue();
                String responseType = row.getCell(3).getStringCellValue();
                String methodInput = row.getCell(4).getStringCellValue();
                String methodAssertionType = row.getCell(5).getStringCellValue();
                String methodOutput = row.getCell(6).getStringCellValue();

                String[] methodParts = targetMethodInfo.split("\\n");
                assert methodParts.length == 2;

                String methodName = methodParts[0];
                String methodSignature = methodParts[1];

                List<String> types = new ArrayList<>();
                List<String> parameters = new ArrayList<>();
                if (!methodInput.equals("[]")) {
                    Map<String, List<String>> typesAndParams = ParseUtils.getTypeAndParameter(methodInput);
                    types = typesAndParams.get("types");
                    parameters = typesAndParams.get("parameters");
                }

                //Create an Agent Command request and execute with it.
                AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
                agentCommandRequest.setCommand(AgentCommand.EXECUTE);
                agentCommandRequest.setRequestType(AgentCommandRequestType.DIRECT_INVOKE);
                agentCommandRequest.setMethodSignature(methodSignature);
                agentCommandRequest.setClassName(selectedImplementation);
                agentCommandRequest.setMethodName(methodName);
                agentCommandRequest.setParameterTypes(types);
                agentCommandRequest.setMethodParameters(parameters);
                agentCommandRequest.setDeclaredMocks(new ArrayList<>());

                System.out.println("Agent cmd request : ");
                System.out.println(agentCommandRequest.toString());

                boolean shouldPrint = true;
//                if (count == 65) {
//                    shouldPrint = true;
//                    System.out.println("Method Input : " + methodInput);
//                    System.out.println("Agent cmd request : " + agentCommandRequest.toString());
//                    System.out.println("Types : " + agentCommandRequest.getParameterTypes());
//                    System.out.println("Parameters : " + agentCommandRequest.getMethodParameters());
//                }
                try {
                    AgentCommandResponse<String> agentCommandResponse = agentClientLite.executeCommand(agentCommandRequest);
                    TestUnit testUnit = new TestUnit(targetClassname, methodName, methodSignature,
                            methodInput, methodAssertionType, methodOutput, responseType, agentCommandRequest,
                            agentCommandResponse);
//                    if (shouldPrint) {
//                        System.out.println("Raw response : ");
//                        System.out.println(agentCommandResponse);
//                    }
                    AutoAssertionResult result = AssertionUtils.assertCase(testUnit);
                    if (!result.isPassing()) {
                        overallStatus = false;
                        failing++;
                    } else {
                        passing++;
                    }
                    if (printOnlyFailing && result.isPassing()) {
                        shouldPrint = false;
                    }
                    if (shouldPrint) {
                        System.out.println("[Case " + count + " (Row)] is [" + ((result.isPassing()) ? "Passing]]" : "Failing]]"));
                        System.out.println("Classname : " + targetClassname);
                        System.out.println("MethodName : " + methodName);
                        System.out.println("Implementation : " + selectedImplementation);
                        System.out.println("Assertion type : " + result.getAssertionType());
                        System.out.println("Message : " + result.getMessage());
                        if (!result.isPassing()) {
                            System.out.println("Raw Response : " + agentCommandResponse.getMethodReturnValue());
                        }
                        System.out.println("---------------------\n");
                    }
                } catch (IOException e) {
                    System.out.println("Execution failed " + e);
                }
            }
        }
        System.out.println("Total tests run : " + (count - 2));
        System.out.println("Total Passing : " + passing);
        System.out.println("Total Failing : " + failing);
        Assertions.assertEquals(true, overallStatus);
    }
}
