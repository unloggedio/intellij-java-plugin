package com.insidious.plugin.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.factory.UsageInsightTracker;
import com.insidious.plugin.ui.Components.GPTResponse.ChatGPTResponse;
import com.insidious.plugin.ui.Components.GPTChatScaffold;
import com.insidious.plugin.ui.Components.UnloggedGPTNavigationBar;
import com.insidious.plugin.ui.Components.UnloggedGptListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.jcef.*;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.JBUI;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class UnloggedGPT implements UnloggedGptListener {

    private JPanel mainPanel;
    private JPanel borderParent;
    private JPanel navigationPanel;
    private JPanel mainContentPanel;
    private JPanel footerPanel;
    private JButton discordButton;
    public JBCefBrowser jbCefBrowser;
    private UnloggedGPTNavigationBar navigationBar;
    private PsiClass currentClass;
    private PsiMethod currentMethod;
    private String chatURL = "https://chat.openai.com/chat";
    public enum ChatGptMode {BROWSER, API}
    private ChatGptMode currentMode = ChatGptMode.API;
    public JComponent getComponent()
    {
        return mainPanel;
    }
    public GPTChatScaffold gptChatScaffold;
    private InsidiousService insidiousService;
    public UnloggedGPT(InsidiousService service)
    {
        this.insidiousService = service;
        loadNav();
        if(currentMode.equals(ChatGptMode.BROWSER)) {
            loadChatGPTBrowserView();
        }
        else
        {
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

    public void loadNav()
    {
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

    public void loadChatGPTBrowserView()
    {
        if (!JBCefApp.isSupported()) {
            return;
        }
        jbCefBrowser = new JBCefBrowser();
        this.borderParent.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
        JBCefCookieManager jbCefCookieManager = new JBCefCookieManager();
        JBCefClient client = jbCefBrowser.getJBCefClient();
        jbCefBrowser.loadURL(chatURL);
    }

    public void triggerClickBackground(String type)
    {
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

    public void triggerClick(String type)
    {
        String mode = type;
        String queryPrefix = "";
        String methodCode = "";

        if(currentMode.equals(ChatGptMode.BROWSER) &&
                jbCefBrowser==null)
        {
            return;
        }
        if(this.currentMethod==null)
        {
            InsidiousNotification.notifyMessage("Please select a method from the editor.",
                    NotificationType.INFORMATION);
            return;
        }
        String lastMethodCode = this.currentMethod.getText();
        methodCode = lastMethodCode;

        switch (mode.trim())
        {
            case "Optimize":
                queryPrefix = "Optimize the following code -\n"+methodCode;
                break;
            case "Find Bugs":
                queryPrefix = "Find possible bugs in the following code -\n"+methodCode;
                break;
            case "Refactor":
                queryPrefix = "Refactor the following code -\n"+methodCode;
                break;
            default:
                queryPrefix = "Explain the following code  -\n"+methodCode;
        }
        System.out.println("Query prefix -> "+queryPrefix);
        if(currentMode.equals(ChatGptMode.BROWSER))
        {
            System.out.println("BROWSER MODE ");
            String code = ("var textAreaE = document.getElementsByTagName(\"textArea\")[0];" +
                    "textAreaE.value = '"+queryPrefix+"';" +
                    "var btn = textAreaE.parentNode.childNodes[1];" +
                    "btn.click();"
            ).trim();
            code = code.replaceAll("[\r\n]+", " ");
            jbCefBrowser.getCefBrowser().executeJavaScript(code,jbCefBrowser.getCefBrowser().getURL(),0);
        }
        else
        {
            System.out.println("API MODE");
            //add entry to chatlist.
            if(gptChatScaffold!=null)
            {
                gptChatScaffold.addNewMessage(queryPrefix,"You",true);
                gptChatScaffold.setLoadingButtonState();
                navigationBar.setActionButtonLoadingState(type);
            }
            String response = makeOkHTTPRequestForPrompt(queryPrefix);
            if(gptChatScaffold!=null)
            {
                if(!response.isEmpty())
                {
                    gptChatScaffold.addNewMessage(response,"ChatGPT",true);
                    gptChatScaffold.setReadyButtonState();
                    navigationBar.setActionButtonReadyState(type);
                }
            }
            gptChatScaffold.scrollToBottomV2();

        }
    }

    @Override
    public void triggerCallOfType(String type) {

        if(true)
        {
            return;
        }
        triggerClick(type);
    }

    @Override
    public void refreshPage() {
        if(true)
        {
            return;
        }
        if(jbCefBrowser!=null)
        {
            jbCefBrowser.loadURL(chatURL);
        }
        else {
            loadChatGPTBrowserView();
        }
    }

    @Override
    public void goBack()
    {
        String code = ("history.back();").trim();
        code = code.replaceAll("[\r\n]+", " ");
        jbCefBrowser.getCefBrowser().executeJavaScript(code,jbCefBrowser.getCefBrowser().getURL(),0);
    }

    @Override
    public void testApiCall() {
        makeOkHTTPRequestForPrompt("What's your name?");
    }

    @Override
    public void makeApiCallForPrompt(String currentPrompt) {
//        makeOkHTTPRequestForPrompt(currentPrompt);
        processCustomPromptBackground(currentPrompt);
    }

    @Override
    public void triggerCallTypeForCurrentMethod(String type) {
        //get response and trigger ui update in scaffold.
        triggerClickBackground(type);
    }

    public void processCustomPromptBackground(String prompt)
    {
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

    public void processCustomPrompt(String prompt)
    {
        gptChatScaffold.addNewMessage(prompt,"You",true);
        gptChatScaffold.setLoadingButtonState();

        String response = makeOkHTTPRequestForPrompt(prompt);

        if(!response.isEmpty())
        {
            gptChatScaffold.addNewMessage(response,"ChatGPT",true);
            gptChatScaffold.setReadyButtonState();
            gptChatScaffold.resetPrompt();
        }
    }

    @Override
    public void triggerUpdate() {
        insidiousService.refreshGPTWindow();
    }

    public void updateUI(PsiClass psiClass, PsiMethod method) {
        this.currentMethod = method;
        this.currentClass = psiClass;
        if(this.navigationBar!=null)
        {
            this.navigationBar.updateSelection(psiClass.getName()+"."+method.getName()+"()");
        }
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
                "routeToDiscord_GPT",null);
    }

    public void loadChatComponent()
    {
        this.mainContentPanel.removeAll();
        gptChatScaffold = new GPTChatScaffold(this);
        this.mainContentPanel.add(gptChatScaffold.getComponent(), BorderLayout.CENTER);
        this.mainContentPanel.revalidate();
    }

    public String makeOkHTTPRequestForPrompt(String prompt)
    {
        System.out.println("Making API call to chatGPT");
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = buildHttpRequest(prompt);
            if (request==null)
            {
                return "";
            }
            Response response = client.newCall(request).execute();
            String responseBodyString = response.body().string();
            ChatGPTResponse response1 = getResponsePojo(responseBodyString);
            if(response1!=null) {
                System.out.println("Text from result : " + response1.choices.get(0).text);
            }
            return response1.choices.get(0).text;
        }
        catch (Exception e)
        {
            System.out.println("Exception sending api : "+ e);
            e.printStackTrace();
            return "Exception getting response for your prompt -> "+e.getMessage();
        }
    }

    public Request buildHttpRequest(String prompt) {

        String token = this.gptChatScaffold.getAPIkey();
        if(token.isEmpty())
        {
            InsidiousNotification.notifyMessage("Please enter a valid API Key",
                    NotificationType.ERROR);
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        TreeMap<Object,Object> body = new TreeMap<>();
        body.put("stop", Arrays.asList(" Human:"," AI:"));
        body.put("prompt", prompt);
        body.put("max_tokens", 256);
        body.put("temperature", 0.9);
        body.put("best_of", 1);
        body.put("frequency_penalty", 0);
        body.put("presence_penalty", 0.6);
        body.put("top_p", 1);
        body.put("stream", false);

        TreeMap<String,String> headers = new TreeMap<>();
        headers.put("Accept", "text/event-stream");
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + token);
        try {
            return new Request.Builder()
                    .url("https://api.openai.com/v1/engines/text-davinci-002/completions")
                    .headers(Headers.of(headers))
                    .post(RequestBody.create(
                            mapper
                                    .writerWithDefaultPrettyPrinter()
                                    .writeValueAsString(body),
                            MediaType.parse("application/json")))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to serialize request payload", e);
        }
    }

    public ChatGPTResponse getResponsePojo(String response)
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            ChatGPTResponse gptResponse = mapper.readValue(response,ChatGPTResponse.class);
            gptResponse.getChoices().get(0).setText(
                    gptResponse.getChoices().get(0).text.replaceAll("\n\n+", " "));
            return gptResponse;
        }
        catch (Exception e)
        {
            System.out.println("Exception when deserializing response "+e);
            e.printStackTrace();
            return null;
        }
    }
}
