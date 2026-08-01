package com.demo;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private UserClient userClient;

    private final RestTemplate restTemplate = new RestTemplate();
    private final OkHttpClient okHttpClient = new OkHttpClient();

    @GetMapping("/test")
    public String test() {
        Map<String, Object> req = new HashMap<>();
        req.put("uid", 1001);
        return userClient.queryUser(req);
    }

    @GetMapping("/test-rest")
    public String testRest() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("uid", 1001);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://127.0.0.1:9999/user/query", request, String.class);
        return response.getBody();
    }

    @GetMapping("/test-httpclient")
    public String testHttpClient() throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost("http://127.0.0.1:9999/user/query");
            httpPost.setHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity("{\"uid\":1001}");
            httpPost.setEntity(entity);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                return EntityUtils.toString(response.getEntity());
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    @GetMapping("/test-okhttp")
    public String testOkHttp() throws Exception {
        okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");
        String jsonBody = "{\"uid\":1001}";
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url("http://127.0.0.1:9999/user/query")
                .post(body)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
