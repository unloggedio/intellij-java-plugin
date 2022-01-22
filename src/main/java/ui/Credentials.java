package ui;

import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Credentials {
    private JPanel panel1;
    private JTextField username;
    private JLabel usernameLable;
    private JLabel passwordLable;
    private JPasswordField password;
    private JTextField videobugServerURLTextField;
    private JButton signupSigninButton;
    private JLabel errorLable;
    OkHttpClient client;
    String usernameText;
    String videobugURL;
    Callback signinCallback;

    public Credentials() {
        signupSigninButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                 usernameText = username.getText();
                 String passwordText = new String(password.getPassword());
                 videobugURL = videobugServerURLTextField.getText();

                if (!isValidEmailAddress(usernameText)) {
                    errorLable.setText("Enter a valid email address");
                }

                if (passwordText == null) {
                    errorLable.setText("Enter a valid Password");
                }

                try {
                    signin(passwordText);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public JPanel getContent() {
        return panel1;
    }


    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public boolean isURL(String url) {
        try {
            (new java.net.URL(url)).openStream().close();
            return true;
        } catch (Exception ex) { }
        return false;
    }

    private void signin(String passwordText) throws IOException {

        signinCallback = new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                    JSONObject jsonObject = (JSONObject) JSONValue.parse(responseBody.string());

                    PropertiesComponent.getInstance().setValue(Constants.TOKEN, jsonObject.getAsString(Constants.TOKEN));

                    PropertiesComponent.getInstance().setValue(Constants.BASE_URL, videobugURL);

                    errorLable.setText("You are now signed in!");
                }
            }
        };

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", usernameText);
        jsonObject.put("password", passwordText);

        post(videobugURL.toString() + Constants.SIGN_IN, jsonObject.toJSONString());

    }
    private  void post(String url, String json) throws IOException {
        client = new OkHttpClient();
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(signinCallback);
    }

}
