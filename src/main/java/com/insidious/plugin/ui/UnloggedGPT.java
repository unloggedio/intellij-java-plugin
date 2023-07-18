package com.insidious.plugin.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.Components.GPTChatScaffold;
import com.insidious.plugin.ui.Components.GPTResponse.ChatGPTResponse;
import com.insidious.plugin.ui.Components.GPTResponse.ErrorResponse;
import com.insidious.plugin.ui.Components.UnloggedGPTNavigationBar;
import com.insidious.plugin.ui.Components.UnloggedGptListener;
import com.insidious.plugin.adapter.ClassAdapter;
import com.insidious.plugin.adapter.MethodAdapter;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.TreeMap;

public class UnloggedGPT implements UnloggedGptListener {

    public GPTChatScaffold gptChatScaffold;
    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel navigationPanel;
    private JPanel mainContentPanel;
    private JPanel footerPanel;
    private JButton discordButton;
    //public JBCefBrowser jbCefBrowser;
    private UnloggedGPTNavigationBar navigationBar;
    private ClassAdapter currentClass;
    private MethodAdapter currentMethod;
    private String chatURL = "https://chat.openai.com/chat";
    private ChatGptMode currentMode = ChatGptMode.API;
    private InsidiousService insidiousService;

    public UnloggedGPT(InsidiousService service) {
        this.insidiousService = service;
        loadNav();
        if (currentMode.equals(ChatGptMode.BROWSER)) {
            loadChatGPTBrowserView();
        } else {
            loadChatComponent();
            this.navigationBar.setControlPanelVisibility(false);
        }
        discordButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                routeToDiscord();
            }
        });
        discordButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public void loadNav() {
        this.navigationPanel.removeAll();
        navigationBar = new UnloggedGPTNavigationBar(this);
        GridLayout gridLayout = new GridLayout(1, 1);
        JPanel gridPanel = new JPanel(gridLayout);
        gridPanel.setBorder(JBUI.Borders.empty());
        GridConstraints constraints = new GridConstraints();
        constraints.setRow(0);
        gridPanel.add(navigationBar.getComponent(), constraints);
        this.navigationPanel.add(gridPanel, BorderLayout.CENTER);
        this.navigationPanel.revalidate();
    }

    public void loadChatGPTBrowserView() {
//        if (!JBCefApp.isSupported()) {
//            return;
//        }
//        jbCefBrowser = new JBCefBrowser();
//        this.borderParent.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
//        JBCefCookieManager jbCefCookieManager = new JBCefCookieManager();
//        JBCefClient client = jbCefBrowser.getJBCefClient();
//        jbCefBrowser.loadURL(chatURL);
    }

    public void triggerClickBackground(String type) {
        Task.Backgroundable task = new Task.Backgroundable(
                insidiousService.getProject(), "Unlogged, Inc.", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                //run read here
                ApplicationManager.getApplication()
                        .runReadAction(new Runnable() {
                            public void run() {
                                triggerClick(type);
                            }
                        });
            }
        };
        ProgressManager.getInstance()
                .run(task);
    }

    public void triggerClick(String type) {
        String mode = type;
        String queryPrefix = "";
        String methodCode = "";

//        if(currentMode.equals(ChatGptMode.BROWSER) &&
//                jbCefBrowser==null)
//        {
//            return;
//        }
        if (this.currentMethod == null) {
            InsidiousNotification.notifyMessage("Please select a method from the editor.",
                    NotificationType.INFORMATION);
            return;
        }

        String lastMethodCode = this.currentMethod.getText();
        methodCode = lastMethodCode;

        if(gptChatScaffold!=null)
        {
            if(gptChatScaffold.getApiMode().equals(GPTChatScaffold.API_MODE.ALPACA))
            {
                sendAlpacaRequestForMethod(type, methodCode);
                return;
            }
        }

        switch (mode.trim()) {
            case "Optimize":
                queryPrefix = "Optimize the following code -\n" + methodCode;
                break;
            case "Find Bugs":
                queryPrefix = "Find possible bugs in the following code -\n" + methodCode;
                break;
            case "Refactor":
                queryPrefix = "Refactor the following code -\n" + methodCode;
                break;
            default:
                queryPrefix = "Explain the following code  -\n" + methodCode;
        }
        System.out.println("Query prefix -> " + queryPrefix);
        if (currentMode.equals(ChatGptMode.BROWSER)) {
            System.out.println("BROWSER MODE ");
            String code = ("var textAreaE = document.getElementsByTagName(\"textArea\")[0];" +
                    "textAreaE.value = '" + queryPrefix + "';" +
                    "var btn = textAreaE.parentNode.childNodes[1];" +
                    "btn.click();"
            ).trim();
            code = code.replaceAll("[\r\n]+", " ");
            //jbCefBrowser.getCefBrowser().executeJavaScript(code,jbCefBrowser.getCefBrowser().getURL(),0);
        } else {
            System.out.println("API MODE");
            //add entry to chatlist.
            if (gptChatScaffold != null) {
                //gptChatScaffold.addNewMessage(queryPrefix,"You",true);
                gptChatScaffold.setLoadingButtonState();
                navigationBar.setActionButtonLoadingState(type);
            }
            String response = makeOkHTTPRequestForPrompt(queryPrefix);
            if (gptChatScaffold != null) {
                if (!response.isEmpty()) {
                    gptChatScaffold.addNewMessage(response, "ChatGPT", true);
                }
                gptChatScaffold.setReadyButtonState();
                navigationBar.setActionButtonReadyState(type);
            }
            gptChatScaffold.scrollToBottomV2();
        }
    }

    private void sendAlpacaRequestForMethod(String type ,String method)
    {
        String endpoint = "";
        String prompt = "";
        switch (type.trim()) {
            case "Optimize":
                prompt = "Optimize the following code";
                break;
            case "Find Bugs":
                prompt = "Find possible bugs in the following code";
                break;
            case "Refactor":
                prompt = "Refactor the following code";
                break;
            case "test":
                prompt = "Generate a unit test case for the following code";
            default:
                prompt = "Explain the following code";
        }
        if(gptChatScaffold!=null)
        {
            endpoint = gptChatScaffold.getTextFieldContent();
        }
        String response = makeOkHTTPRequestForMethod_alpaca(prompt ,method, endpoint);
        //add response
        if (gptChatScaffold != null) {
            if (!response.isEmpty()) {
                gptChatScaffold.addNewMessage(response, "Alpaca", true);
            }
            gptChatScaffold.setReadyButtonState();
            navigationBar.setActionButtonReadyState(type);
        }
        gptChatScaffold.scrollToBottomV2();
    }

    @Override
    public void triggerCallOfType(String type) {

        if (true) {
            return;
        }
        triggerClick(type);
    }

    @Override
    public void refreshPage() {
        if (true) {
            return;
        }
//        if(jbCefBrowser!=null)
//        {
//            jbCefBrowser.loadURL(chatURL);
//        }
//        else {
        loadChatGPTBrowserView();
//        }
    }

    @Override
    public void goBack() {
        String code = ("history.back();").trim();
        code = code.replaceAll("[\r\n]+", " ");
        //jbCefBrowser.getCefBrowser().executeJavaScript(code,jbCefBrowser.getCefBrowser().getURL(),0);
    }

    @Override
    public void testApiCall() {
        makeOkHTTPRequestForPrompt("What's your name?");
    }

    @Override
    public void makeApiCallForPrompt(String currentPrompt) {
//        makeOkHTTPRequestForPrompt(currentPrompt);
            if(gptChatScaffold.getApiMode().equals(GPTChatScaffold.API_MODE.ALPACA))
            {
                processCustomPromptBackground_Alpaca(currentPrompt);
            }
            else
            {
                processCustomPromptBackground(currentPrompt);
            }
    }

    @Override
    public void triggerCallTypeForCurrentMethod(String type) {
        //get response and trigger ui update in scaffold.
        triggerClickBackground(type);
    }

    public void processCustomPromptBackground(String prompt) {
        Task.Backgroundable task = new Task.Backgroundable(
                insidiousService.getProject(), "Unlogged, Inc.", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                //run read here
                ApplicationManager.getApplication()
                        .runReadAction(new Runnable() {
                            public void run() {
                                processCustomPrompt(prompt);
                            }
                        });
            }
        };
        ProgressManager.getInstance()
                .run(task);
    }

    public void processCustomPromptBackground_Alpaca(String prompt) {
        Task.Backgroundable task = new Task.Backgroundable(
                insidiousService.getProject(), "Unlogged, Inc.", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                //run read here
                ApplicationManager.getApplication()
                        .runReadAction(new Runnable() {
                            public void run() {
                                processCustomPromptAlpaca(prompt);
                            }
                        });
            }
        };
        ProgressManager.getInstance()
                .run(task);
    }

    public void processCustomPrompt(String prompt) {
        //gptChatScaffold.addNewMessage(prompt,"You",true);
        UsageInsightTracker.getInstance().RecordEvent(
                "CustomQuery", null);
        gptChatScaffold.setLoadingButtonState();

        String response = makeOkHTTPRequestForPrompt(prompt);
        if (!response.isEmpty()) {
            gptChatScaffold.resetPrompt();
        }
        gptChatScaffold.addNewMessage(response, "ChatGPT", true);
        gptChatScaffold.setReadyButtonState();
    }

    public void processCustomPromptAlpaca(String prompt) {
        //gptChatScaffold.addNewMessage(prompt,"You",true);
        UsageInsightTracker.getInstance().RecordEvent(
                "CustomQueryAlpaca", null);
        gptChatScaffold.setLoadingButtonState();

        String response = makeOkHTTPRequestForMethod_alpaca(prompt,"", gptChatScaffold.getTextFieldContent());
        if (!response.isEmpty()) {
            gptChatScaffold.resetPrompt();
        }
        gptChatScaffold.addNewMessage(response, "Alpaca", true);
        gptChatScaffold.setReadyButtonState();
    }

    @Override
    public void triggerUpdate() {
//        insidiousService.refreshGPTWindow();
    }

    private void routeToDiscord() {
        String link = "https://discord.gg/Hhwvay8uTa";
        if (Desktop.isDesktopSupported()) {
            try {
                java.awt.Desktop.getDesktop()
                        .browse(java.net.URI.create(link));
            } catch (Exception e) {
            }
        } else {
            //no browser
        }
        UsageInsightTracker.getInstance().RecordEvent(
                "routeToDiscord_GPT", null);
    }

    public void loadChatComponent() {
        this.mainContentPanel.removeAll();
        gptChatScaffold = new GPTChatScaffold(this);
        this.mainContentPanel.add(gptChatScaffold.getComponent(), BorderLayout.CENTER);
        this.mainContentPanel.revalidate();
    }

    public String makeOkHTTPRequestForPrompt(String prompt) {
        String responseBodyString = null;
        System.out.println("Making API call to chatGPT");
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = buildHttpRequest(prompt);
            if (request == null) {
                return "";
            }
            Response response = client.newCall(request).execute();
            responseBodyString = response.body().string();
            System.out.println("RAW RESPONSE -> " + responseBodyString);
            ChatGPTResponse response1 = getResponsePojo(responseBodyString);
            if (response1 != null) {
                System.out.println("Text from result : " + response1.choices.get(0).text);
                return response1.choices.get(0).text;
            } else {
                //process for api error
                ErrorResponse errorResponse = getErrorPojo(responseBodyString);
                return errorResponse.error.message;
            }
        } catch (Exception e) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("GPT_CALL_REQUEST_EXCEPTION", eventProperties);
            e.printStackTrace();
        }
        return "";
    }

    public String makeOkHTTPRequestForMethod_alpaca(String prompt, String method,String endpoint) {
        String responseBodyString = null;
        System.out.println("Making API call to chatGPT");
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = buildHttpRequest_alpaca(prompt, method, endpoint);
            if (request == null) {
                return "";
            }
            Response response = client.newCall(request).execute();
            responseBodyString = response.body().string();
            System.out.println("RAW RESPONSE [ALPACA] -> " + responseBodyString);
            return responseBodyString;
        } catch (Exception e) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("ALPACA_CALL_REQUEST_EXCEPTION", eventProperties);
            e.printStackTrace();
        }
        return "";
    }

    public String getErrorMessageFromResponse(String response) {
        System.out.println("ResponseBody : " + response);
        return "E";
    }

    public Request buildHttpRequest_alpaca(String prompt,String method,  String endpoint) {

        String token = this.gptChatScaffold.getAPIkey();
        if (token.isEmpty()) {
            InsidiousNotification.notifyMessage("Please enter a valid API Key",
                    NotificationType.ERROR);
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        TreeMap<Object, Object> body = new TreeMap<>();
        body.put("prompt", prompt);
        body.put("input_code", method);
        body.put("max_new_tokens", 2048);
        body.put("temperature", 0.1);
        body.put("num_beams", 4);
        body.put("top_p", 0.75);
        body.put("top_k", 40);

        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("Accept", "text/event-stream");
        headers.put("Content-Type", "application/json");
//        headers.put("Authorization", "Bearer " + token);
        try {
            return new Request.Builder()
                    .url(""+endpoint)
                    .headers(Headers.of(headers))
                    .post(RequestBody.create(
                            mapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(body),
                            MediaType.parse("application/json")))
                    .build();
        } catch (JsonProcessingException e) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("ALPACA_CALL_EXCEPTION", eventProperties);
            throw new RuntimeException("Unable to serialize request payload", e);
        }
    }

    public Request buildHttpRequest(String prompt) {

        String token = this.gptChatScaffold.getAPIkey();
        if (token.isEmpty()) {
            InsidiousNotification.notifyMessage("Please enter a valid API Key",
                    NotificationType.ERROR);
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        TreeMap<Object, Object> body = new TreeMap<>();
        body.put("stop", Arrays.asList(" Human:", " AI:"));
        body.put("prompt", prompt);
        body.put("max_tokens", 1024);
        body.put("temperature", 0.9);
        body.put("best_of", 1);
        body.put("frequency_penalty", 0);
        body.put("presence_penalty", 0.6);
        body.put("top_p", 1);
        body.put("stream", false);

        TreeMap<String, String> headers = new TreeMap<>();
        headers.put("Accept", "text/event-stream");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + token);
        try {
            return new Request.Builder()
                    .url("https://api.openai.com/v1/engines/text-davinci-003/completions")
                    .headers(Headers.of(headers))
                    .post(RequestBody.create(
                            mapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(body),
                            MediaType.parse("application/json")))
                    .build();
        } catch (JsonProcessingException e) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("GPT_CALL_EXCEPTION", eventProperties);
            throw new RuntimeException("Unable to serialize request payload", e);
        }
    }


    public ChatGPTResponse getResponsePojo(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ChatGPTResponse gptResponse = mapper.readValue(response, ChatGPTResponse.class);
            gptResponse.getChoices().get(0).setText(
                    gptResponse.getChoices().get(0).text.replaceAll("\n\n+", " "));
            return gptResponse;
        } catch (Exception e) {
            JSONObject eventProperties = new JSONObject();
            eventProperties.put("message", e.getMessage());
            UsageInsightTracker.getInstance().RecordEvent("GPT_CALL_EXCEPTION_JSON_FAIL", eventProperties);
            System.out.println("Exception when deserializing response " + e);
            e.printStackTrace();
            return null;
        }
    }

    public ErrorResponse getErrorPojo(String response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse gptResponse = mapper.readValue(response, ErrorResponse.class);
            return gptResponse;
        } catch (Exception e) {
            System.out.println("Exception when deserializing error response " + e);
            e.printStackTrace();
            return null;
        }
    }

    public enum ChatGptMode {BROWSER, API}
}
