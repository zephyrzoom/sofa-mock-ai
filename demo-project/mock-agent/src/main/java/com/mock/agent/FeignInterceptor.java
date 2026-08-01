package com.mock.agent;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.Callable;

import com.mock.agent.log.MockAgentLogger;

public class FeignInterceptor {

    @RuntimeType
    public static Object intercept(
            @SuperCall Callable<?> callable,
            @AllArguments Object[] args) throws Exception {

        Object request = args[0];
        ClassLoader cl = request.getClass().getClassLoader();

        Method urlMethod = request.getClass().getMethod("url");
        String url = (String) urlMethod.invoke(request);
        // URI.create(url).getPath() 已返回解码后的路径,无需再 URLDecoder.decode
        String path = URI.create(url).getPath();

        String method;
        try {
            Object httpMethod = request.getClass().getMethod("httpMethod").invoke(request);
            method = httpMethod.toString();
        } catch (NoSuchMethodException e) {
            method = (String) request.getClass().getMethod("method").invoke(request);
        }

        String requestBody = null;
        try {
            byte[] bodyBytes = (byte[]) request.getClass().getMethod("body").invoke(request);
            if (bodyBytes != null && bodyBytes.length > 0) {
                requestBody = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            MockAgentLogger.debug("failed to read request body: " + e);
        }

        MockAgentLogger.info("intercept: " + method + " " + url);
        if (requestBody != null) {
            MockAgentLogger.info("requestBody: " + requestBody);
        }

        // Debug: check which Feign version is actually loaded
        try {
            Class<?> respClass = cl.loadClass("feign.Response");
            java.net.URL loc = respClass.getProtectionDomain().getCodeSource().getLocation();
            MockAgentLogger.debug("feign.Response loaded from: " + loc);
        } catch (Exception e) {
            MockAgentLogger.debug("cannot determine feign.Response location: " + e);
        }

        MockCase mockCase = MockCaseLoader.findMatch(method, path, requestBody);
        if (mockCase != null) {
            MockAgentLogger.info("matched mock case: " + method + " " + path);
            Object response = MockResponseBuilder.build(cl, mockCase.getStatus(), mockCase.getBody());

            // Verify the response body is readable before returning to decoder
            try {
                Object body = response.getClass().getMethod("body").invoke(response);
                if (body == null) {
                    MockAgentLogger.debug("response body is NULL!");
                } else if (body instanceof byte[]) {
                    byte[] bytes = (byte[]) body;
                    MockAgentLogger.debug("response body = byte[" + bytes.length + "]");
                    if (bytes.length > 0) {
                        MockAgentLogger.debug("response body preview: " + new String(bytes, java.nio.charset.StandardCharsets.UTF_8).substring(0, Math.min(300, bytes.length)));
                    }
                } else {
                    MockAgentLogger.debug("response body = " + body.getClass().getName());
                }
            } catch (Exception e) {
                MockAgentLogger.debug("body verification failed: " + e);
            }

            return response;
        }

        MockAgentLogger.info("no match, forwarding real call: " + method + " " + path);
        return callable.call();
    }
}
