package Network;

import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import net.minidev.json.JSONObject;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class GETCalls {
OkHttpClient client;
String url;
Callback callback;

    public void getCall(String url, Callback callback) throws Exception {
     this.url = url;
     this.callback = callback;
     run();
    }

    public void run() throws Exception {
        client = new OkHttpClient().newBuilder()
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + PropertiesComponent.getInstance().getValue(Constants.TOKEN))
                .build();


        client.newCall(request).enqueue(this.callback);
    }

}
