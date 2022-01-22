package Network;

import net.minidev.json.JSONObject;
import okhttp3.*;

import java.io.IOException;

public class GETCalls {
private static final String BASE_URL = "http://194.195.115.67:8080/api/data/";
OkHttpClient client = new OkHttpClient();
String url;
Callback callback;

    public void getCall(String endPoint, Callback callback) throws Exception {
     url = BASE_URL + endPoint;
     this.callback = callback;
     run();
    }

    public void run() throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhcnRwYXJAZ21haWwuY29tIiwiaWF0IjoxNjQxMzc5OTc0LCJleHAiOjE2NDE0NjYzNzR9.Z4gRuD6MK8DNC6fMinJudgzfIaVexaNr-jWpiRjvhGQUaFKeIjMwRqQcSz0yXZxt8puyWk_sFVyy5OX0Fd7moQ")
                .build();

        client.newCall(request).enqueue(this.callback);
    }

}
