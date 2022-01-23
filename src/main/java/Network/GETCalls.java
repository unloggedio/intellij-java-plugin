package Network;

import actions.Constants;
import com.intellij.ide.util.PropertiesComponent;
import net.minidev.json.JSONObject;
import okhttp3.*;

import java.io.IOException;

public class GETCalls {
OkHttpClient client = new OkHttpClient();
String url;
Callback callback;

    public void getCall(String url, Callback callback) throws Exception {
     this.url = url;
     this.callback = callback;
     run();
    }

    public void run() throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + PropertiesComponent.getInstance().getValue(Constants.TOKEN))
                .build();

        client.newCall(request).enqueue(this.callback);
    }

}
