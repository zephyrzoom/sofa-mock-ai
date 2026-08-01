package com.demo;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class UserController {

    @Autowired
    private UserClient userClient;

    private final RestTemplate restTemplate = new RestTemplate();
    private final OkHttpClient okHttpClient = new OkHttpClient();

    @PostMapping("/test")
    public String test(@RequestBody String body) {
        return userClient.queryUser(body);
    }

    @PostMapping("/test-rest")
    public String testRest(@RequestBody String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://127.0.0.1:9999/user/query", request, String.class);
        return response.getBody();
    }

    @PostMapping("/test-httpclient")
    public String testHttpClient(@RequestBody String body) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost("http://127.0.0.1:9999/user/query");
            httpPost.setHeader("Content-Type", "application/json");
            StringEntity entity = new StringEntity(body);
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

    @PostMapping("/test-okhttp")
    public String testOkHttp(@RequestBody String body) throws Exception {
        okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(body, JSON);
        Request request = new Request.Builder()
                .url("http://127.0.0.1:9999/user/query")
                .post(requestBody)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
