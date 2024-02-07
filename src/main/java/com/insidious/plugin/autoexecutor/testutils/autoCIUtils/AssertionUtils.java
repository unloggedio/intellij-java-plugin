package com.insidious.plugin.autoexecutor.testutils.autoCIUtils;

import com.insidious.plugin.agent.AgentCommandResponse;
import com.insidious.plugin.agent.ResponseType;
import com.insidious.plugin.autoexecutor.testutils.entity.AutoAssertionResult;
import com.insidious.plugin.autoexecutor.testutils.entity.TestUnit;
import com.insidious.plugin.ui.methodscope.DifferenceResult;
import com.insidious.plugin.util.DiffUtils;

import java.util.HashMap;
import java.util.Map;

import static com.insidious.plugin.ui.methodscope.DiffResultType.SAME;

public class AssertionUtils {

    public static AutoAssertionResult assertCase(TestUnit testUnit) {
        AutoAssertionResult assertionResult = new AutoAssertionResult();
        String assertionType = testUnit.getAssertionType();
        String refOut = testUnit.getReferenceValue();

        AgentCommandResponse<String> agentCommandResponse = postProcessResponse(testUnit.getResponse());
        ResponseType actualResponseType = agentCommandResponse.getResponseType();
        String actualResponse = agentCommandResponse.getMethodReturnValue();

        boolean result = false;
        assertionResult.setAssertionType(assertionType);
        String message = "";
        switch (assertionType) {
            case "EQUAL":
                //absolute equal check
                if (refOut.equals(actualResponse)) {
                    result = true;
                    message = "Responses are Equal";
                } else {
                    message = "Responses are Not Equal";
                }
                break;
            case "SIMILAR":
                //similar regex check
                Map<String, String> diffResNormal = areTextsSimilar(refOut, actualResponse);
                if (diffResNormal.get("status").equals("true")) {
                    result = true;
                }
                message = diffResNormal.get("reason");
                break;
            case "SIMILAR EXCEPTION":
                //should be a similar exception
                Map<String, String> diffResException = areTextsSimilar(refOut, actualResponse);
                if (actualResponseType.equals(ResponseType.EXCEPTION)) {
                    if (diffResException.get("status").equals("true")) {
                        result = true;
                    }
                    message = diffResException.get("reason");
                } else {
                    message = "Response is not an Exception, Exception expected";
                }
                break;
            case "NO EXCEPTION":
                //should not throw an exception
                if (!actualResponseType.equals(ResponseType.EXCEPTION)) {
                    message = "Did not throw an Exception, as expected";
                    result = true;
                } else {
                    message = "Received Exception when it was not expected";
                }
                break;
            case "EQUAL EXCEPTION":
                //should be an equal exception
                if (actualResponseType.equals(ResponseType.EXCEPTION)
                        && actualResponse.equals(refOut)) {
                    result = true;
                    message = "Received Exception is the same as the Excepted Exception";
                } else {
                    message = "Received Exception is different from expected Exception";
                }
                break;
        }
        assertionResult.setPassing(result);
        assertionResult.setMessage(message);
        return assertionResult;
    }

    private static AgentCommandResponse<String> postProcessResponse(AgentCommandResponse<String> response) {
        ResponseType responseType = response.getResponseType();
        switch (responseType) {
            case FAILED:
                //failed to be marked as exceptions
                response.setResponseType(ResponseType.EXCEPTION);
                break;
            case EXCEPTION:
                //exception case post processing
                break;
            case NORMAL:
                //normal case post processing
                if (response.getMethodReturnValue() == null) {
                    response.setMethodReturnValue("null - from agent");
                }
                if (response.getMethodReturnValue().startsWith("Failed to serialize")) {
                    response.setResponseType(ResponseType.EXCEPTION);
                }
                break;
        }
        return response;
    }

    private static Map<String, String> areTextsSimilar(String expected, String actual) {
        Map<String, String> diffInfo = new HashMap<>();
        boolean similar = false;
        DifferenceResult differenceResult = DiffUtils.compareTexts(expected, actual);
        if (differenceResult.getDiffResultType().equals(SAME)) {
            similar = true;
            diffInfo.put("reason", "Response values are Equal");
        } else {
            //ensure lefts and rights are empty/null - no structural change
            if (differenceResult.getLeftOnly() != null && differenceResult.getRightOnly() != null) {
                if (differenceResult.getLeftOnly().isEmpty() && differenceResult.getRightOnly().isEmpty()) {
                    similar = true;
                    diffInfo.put("reason", "Response structures are similar");
                }
            } else {
                //for primitives, +1 check for null pending
                similar = true;
                diffInfo.put("reason", "Response types are same");
            }
        }
        diffInfo.put("status", String.valueOf(similar));
        if (!similar) {
            diffInfo.put("reason", "Responses are not similar");
        }
        return diffInfo;
    }
}
