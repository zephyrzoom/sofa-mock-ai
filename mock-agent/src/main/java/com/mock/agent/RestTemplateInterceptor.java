package com.mock.agent;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.net.URI;
import java.util.concurrent.Callable;

import com.mock.agent.log.MockAgentLogger;

public class RestTemplateInterceptor {

    @RuntimeType
    public static Object intercept(
            @This Object request,
            @SuperCall Callable<?> callable) throws Exception {

        java.lang.reflect.Method uriMethod = request.getClass().getMethod("getURI");
        uriMethod.setAccessible(true);
        URI uri = (URI) uriMethod.invoke(request);
        java.lang.reflect.Method httpMethod = request.getClass().getMethod("getMethodValue");
        httpMethod.setAccessible(true);
        String method = (String) httpMethod.invoke(request);
        // URI.getPath() 已返回解码后的路径,无需再 URLDecoder.decode
        String path = uri.getPath();

        String requestBody = null;
        try {
            // Try getBody() first
            Object bodyObj = null;
            try {
                java.lang.reflect.Method getBodyMethod = request.getClass().getMethod("getBody");
                getBodyMethod.setAccessible(true);
                bodyObj = getBodyMethod.invoke(request);
            } catch (Exception ignored) {
                // assertNotExecuted() may throw if request state is inconsistent
            }

            // Fallback: read bufferedOutput field directly from AbstractBufferingClientHttpRequest
            if (bodyObj == null) {
                try {
                    java.lang.reflect.Field field = findField(request.getClass(), "bufferedOutput");
                    if (field != null) {
                        field.setAccessible(true);
                        bodyObj = field.get(request);
                    }
                } catch (Exception ignored) {
                }
            }

            if (bodyObj instanceof java.io.ByteArrayOutputStream) {
                java.io.ByteArrayOutputStream baos = (java.io.ByteArrayOutputStream) bodyObj;
                if (baos.size() > 0) {
                    requestBody = baos.toString("UTF-8");
                }
            } else if (bodyObj != null) {
                try {
                    java.lang.reflect.Method writeTo = bodyObj.getClass().getMethod("writeTo", java.io.OutputStream.class);
                    java.io.ByteArrayOutputStream capture = new java.io.ByteArrayOutputStream();
                    writeTo.invoke(bodyObj, capture);
                    if (capture.size() > 0) {
                        requestBody = capture.toString("UTF-8");
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Exception e) {
            // body may not be available for non-buffering requests, ignore
        }

        MockAgentLogger.info("[RestTemplate] intercept: " + method + " " + path);
        if (requestBody != null) {
            MockAgentLogger.info("[RestTemplate] request body: " + requestBody);
        }

        MockCase mockCase = MockCaseLoader.findMatch(method, path, requestBody);
        if (mockCase != null) {
            MockAgentLogger.info("[RestTemplate] matched mock case: " + method + " " + path);
            return MockRestResponseBuilder.build(
                    request.getClass().getClassLoader(),
                    mockCase.getStatus(),
                    mockCase.getBody());
        }

        MockAgentLogger.info("[RestTemplate] no match, forwarding: " + method + " " + path);
        return callable.call();
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
