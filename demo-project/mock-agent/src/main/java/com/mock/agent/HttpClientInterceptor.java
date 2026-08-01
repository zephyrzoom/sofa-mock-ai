package com.mock.agent;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.Callable;

import com.mock.agent.log.MockAgentLogger;

public class HttpClientInterceptor {

    @RuntimeType
    public static Object intercept(
            @SuperCall Callable<?> callable,
            @AllArguments Object[] args) throws Exception {

        Object request = args[0];
        ClassLoader cl = request.getClass().getClassLoader();

        // Extract URI from HttpRequest
        Method getURI = request.getClass().getMethod("getURI");
        URI uri = (URI) getURI.invoke(request);
        // URI.getPath() 已返回解码后的路径,无需再 URLDecoder.decode(否则 %xx 和 + 会被二次解码/误转成空格)
        String path = uri.getPath();

        // Extract HTTP method
        Method getMethod = request.getClass().getMethod("getMethod");
        String method = (String) getMethod.invoke(request);

        // Extract request body
        String requestBody = null;
        try {
            Object entity = null;
            try {
                Method getEntity = request.getClass().getMethod("getEntity");
                entity = getEntity.invoke(request);
            } catch (NoSuchMethodException ignored) {
            }

            if (entity != null) {
                // Try to read from HttpEntity using getContent()
                try {
                    Method getContent = entity.getClass().getMethod("getContent");
                    java.io.InputStream is = (java.io.InputStream) getContent.invoke(entity);
                    if (is != null) {
                        requestBody = readInputStream(is);
                    }
                } catch (Exception ignored) {
                }

                // Fallback: try to read from ByteArrayEntity or StringEntity via reflection
                if (requestBody == null) {
                    try {
                        // Try ByteArrayOutputStream field
                        java.lang.reflect.Field contentField = findField(entity.getClass(), "b");
                        if (contentField != null) {
                            contentField.setAccessible(true);
                            Object content = contentField.get(entity);
                            if (content instanceof byte[]) {
                                requestBody = new String((byte[]) content, java.nio.charset.StandardCharsets.UTF_8);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            MockAgentLogger.debug("[HttpClient] failed to read request body: " + e);
        }

        MockAgentLogger.info("[HttpClient] intercept: " + method + " " + uri);
        if (requestBody != null) {
            MockAgentLogger.info("[HttpClient] requestBody: " + requestBody);
        }

        MockCase mockCase = MockCaseLoader.findMatch(method, path, requestBody);
        if (mockCase != null) {
            MockAgentLogger.info("[HttpClient] matched mock case: " + method + " " + path);
            return MockHttpClientResponseBuilder.build(cl, mockCase.getStatus(), mockCase.getBody());
        }

        MockAgentLogger.info("[HttpClient] no match, forwarding real call: " + method + " " + path);
        return callable.call();
    }

    private static String readInputStream(java.io.InputStream is) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        is.close();
        return baos.toString("UTF-8");
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
