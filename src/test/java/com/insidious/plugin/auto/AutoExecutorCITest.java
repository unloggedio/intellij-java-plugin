package com.insidious.plugin.auto;

import com.insidious.plugin.agent.AgentCommand;
import com.insidious.plugin.agent.AgentCommandRequest;
import com.insidious.plugin.agent.AgentCommandRequestType;
import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.autoexecutor.testutils.autoCIUtils.AgentClientLite;
import com.insidious.plugin.autoexecutor.testutils.autoCIUtils.AssertionUtils;
import com.insidious.plugin.autoexecutor.testutils.autoCIUtils.ParseUtils;
import com.insidious.plugin.autoexecutor.testutils.autoCIUtils.XlsxUtils;
import com.insidious.plugin.autoexecutor.testutils.entity.AutoAssertionResult;
import com.insidious.plugin.autoexecutor.testutils.entity.TestResultSummary;
import com.insidious.plugin.autoexecutor.testutils.entity.TestUnit;
import com.insidious.plugin.mocking.DeclaredMock;
import org.apache.poi.ss.usermodel.Cell;
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
    public void runTests() {
        AgentClientLite agentClientLite = new AgentClientLite();
        if (!agentClientLite.isConnected()) {
            System.out.println("Stopping AutoExecutor test as agent is not connected");
            return;
        }
        URL pathToIntegrationResources = Thread.currentThread().getContextClassLoader()
                .getResource(testResourcesPath + "maven-demo-integration-resources.xlsx");
        URL pathToMockResources = Thread.currentThread().getContextClassLoader()
                .getResource(testResourcesPath + "maven-demo-mocked-resources.xlsx");

        System.out.println("\n-----Integration mode testing-----\n");
        TestResultSummary integrationResult = runAutoExecutorCI(pathToIntegrationResources, agentClientLite);
        System.out.println("\n-----Unit mode testing-----\n");
        TestResultSummary unitResult = runAutoExecutorCI(pathToMockResources, agentClientLite);

        System.out.println("\n-----Test Summary-----\n");
        System.out.println("    Integration Mode ->");
        System.out.println("    Total number of cases run : " + integrationResult.getNumberOfCases());
        System.out.println("    Number of Passing cases : " + integrationResult.getPassingCasesCount());
        System.out.println("    Number of Failing cases : " + integrationResult.getFailingCasesCount());

        System.out.println("\n    Unit Mode ->");
        System.out.println("    Total number of cases run : " + unitResult.getNumberOfCases());
        System.out.println("    Number of Passing cases : " + unitResult.getPassingCasesCount());
        System.out.println("    Number of Failing cases : " + unitResult.getFailingCasesCount());

        boolean overallStatus = (integrationResult.getFailingCasesCount() + unitResult.getFailingCasesCount()) == 0;
        Assertions.assertTrue(overallStatus);
    }

    public TestResultSummary runAutoExecutorCI(URL pathToUrl, AgentClientLite agentClientLite) {
        XSSFWorkbook workbook = XlsxUtils.getWorkbook(pathToUrl);
        assert workbook != null;

        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        int count = 0;
        int passing = 0;
        int failing = 0;

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
                Cell outputCell = row.getCell(6);
                String methodOutput = "";
                if (outputCell != null) {
                    //can be null when an empty value is present in cell
                    try {
                        methodOutput = row.getCell(6).getStringCellValue();
                    } catch (IllegalStateException illegalStateException) {
                        //get numeric value
                        methodOutput = String.valueOf(row.getCell(6).getNumericCellValue());
                    }
                }
                String declaredMocks = row.getCell(9).getStringCellValue();
                Cell commentCell = row.getCell(11);
                String caseComment = null;
                if (commentCell != null) {
                    caseComment = commentCell.getStringCellValue();
                }

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

                List<DeclaredMock> declaredMocksList = ParseUtils.getDeclaredMocksFrom(declaredMocks);
                agentCommandRequest.setDeclaredMocks(declaredMocksList);

                boolean shouldPrint = true;
                try {
                    AgentCommandResponse<String> agentCommandResponse = agentClientLite.executeCommand(agentCommandRequest);
                    TestUnit testUnit = new TestUnit(targetClassname, methodName, methodSignature,
                            methodInput, methodAssertionType, methodOutput, responseType, agentCommandRequest,
                            agentCommandResponse);
                    AutoAssertionResult result = AssertionUtils.assertCase(testUnit);
                    if (!result.isPassing()) {
                        failing++;
                    } else {
                        passing++;
                    }
                    if (printOnlyFailing && result.isPassing()) {
                        shouldPrint = false;
                    }
                    if (shouldPrint) {
                        System.out.println(">   [Case " + count + " (Row)] is [" + ((result.isPassing()) ? "Passing]]" : "Failing]]"));
                        System.out.println("    Classname : " + targetClassname);
                        System.out.println("    MethodName : " + methodName);
                        System.out.println("    Implementation : " + selectedImplementation);
                        System.out.println("    Assertion type : " + result.getAssertionType());
                        System.out.println("    Message : " + result.getMessage());
                        if (caseComment != null) {
                            System.out.println("    Case Comment : " + caseComment);
                        }
                        if (!result.isPassing()) {
                            System.out.println("    Raw Response : " + agentCommandResponse.getMethodReturnValue());
                        }
                        System.out.println("\n");
                    }
                } catch (IOException e) {
                    System.out.println("Execution failed " + e);
                }
            }
        }
        return new TestResultSummary(count - 2, passing, failing);
    }
}
