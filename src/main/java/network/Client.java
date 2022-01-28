package network;

import actions.Constants;
import callbacks.SignInCallback;
import com.intellij.ide.util.PropertiesComponent;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Client {
    public static final String SIGN_IN = "/api/auth/signin";
    public static final String SIGN_UP = "/api/auth/signin";
    private final String endpoint;
    OkHttpClient client = new OkHttpClient();

    public Client(String endpoint) {
        this.endpoint = endpoint;
    }

    public void signup(String username, String password, Callback callback) {
        JSONObject json = new JSONObject();
        json.put("email", username);
        json.put("password", password);
        post(endpoint + Constants.SIGN_UP, json.toJSONString(), callback);
    }

    public void signin(String username, String password, SignInCallback signInCallback) {
        JSONObject json = new JSONObject();
        json.put("email", username);
        json.put("password", password);
        post(endpoint + Constants.SIGN_IN, json.toJSONString(), new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                signInCallback.error();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                JSONObject jsonObject = (JSONObject) JSONValue.parse(response.body().string());
                String token = jsonObject.getAsString(Constants.TOKEN);

                signInCallback.success(token);
            }
        });
    }

    private void post(String url, String json, Callback callback) {
        RequestBody body = RequestBody.create(json, Constants.JSON); // new

        Request.Builder builder = new Request.Builder();

        builder.url(url);
        String token = PropertiesComponent.getInstance().getValue(Constants.TOKEN);
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        builder.post(body);

        Request request = builder.build();

        client.newCall(request).enqueue(callback);
    }

}
