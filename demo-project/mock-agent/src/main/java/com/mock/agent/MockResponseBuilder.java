package com.mock.agent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mock.agent.log.MockAgentLogger;

public class MockResponseBuilder {

    public static Object build(ClassLoader cl, int status, String body) throws Exception {
        Class<?> responseClass = cl.loadClass("feign.Response");
        Class<?> requestClass = cl.loadClass("feign.Request");

        Object builder = responseClass.getMethod("builder").invoke(null);
        builder.getClass().getMethod("status", int.class).invoke(builder, status);
        builder.getClass().getMethod("reason", String.class).invoke(builder, "OK");

        Map<String, Collection<String>> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", Collections.singletonList("application/json; charset=utf-8"));
        responseHeaders.put("Content-Length", Collections.singletonList(String.valueOf(body.getBytes(StandardCharsets.UTF_8).length)));
        builder.getClass().getMethod("headers", Map.class).invoke(builder, responseHeaders);

        setBody(builder, body);

        Object mockRequest = createMockRequest(cl, requestClass, "POST");
        builder.getClass().getMethod("request", requestClass).invoke(builder, mockRequest);

        Object response = builder.getClass().getMethod("build").invoke(builder);

        if (MockAgentLogger.isDebugEnabled()) {
            verifyBody(cl, response, status);
        }

        return response;
    }

    private static void verifyBody(ClassLoader cl, Object response, int status) {
        try {
            Object body = response.getClass().getMethod("body").invoke(response);
            if (body == null) {
                MockAgentLogger.debug("verifyBody: body is NULL");
                return;
            }
            MockAgentLogger.debug("verifyBody: body class = " + body.getClass().getName());

            byte[] bytes = null;
            if (body instanceof byte[]) {
                // Feign 9.x: body() returns byte[]
                bytes = (byte[]) body;
                MockAgentLogger.debug("verifyBody: body is byte[], length=" + bytes.length);
            } else {
                // Feign 10.x/11.x: body() returns ResponseBody object
                // Try bodyAsString() first (Feign 10.7+)
                try {
                    Method bodyAsStringMethod = response.getClass().getMethod("bodyAsString");
                    String bodyStr = (String) bodyAsStringMethod.invoke(response);
                    MockAgentLogger.debug("verifyBody: bodyAsString = "
                            + (bodyStr != null ? bodyStr.substring(0, Math.min(300, bodyStr.length())) : "NULL"));
                    return;
                } catch (NoSuchMethodException ignore) {
                }
                // Use ResponseBody interface to call asInputStream()
                Class<?> bodyInterface = null;
                try { bodyInterface = cl.loadClass("feign.Response$Body"); } catch (ClassNotFoundException e1) {
                    try { bodyInterface = cl.loadClass("feign.ResponseBody"); } catch (ClassNotFoundException e2) {
                        bodyInterface = body.getClass();
                    }
                }
                java.io.InputStream is = (java.io.InputStream) bodyInterface.getMethod("asInputStream").invoke(body);
                if (is != null) {
                    bytes = readAllBytes(is);
                    MockAgentLogger.debug("verifyBody: asInputStream, length=" + bytes.length);
                } else {
                    MockAgentLogger.debug("verifyBody: asInputStream is NULL");
                }
            }
            if (bytes != null && bytes.length > 0) {
                MockAgentLogger.debug("verifyBody: body content = " + new String(bytes, StandardCharsets.UTF_8).substring(0, Math.min(500, bytes.length)));
            }
        } catch (Exception e) {
            MockAgentLogger.debug("verifyBody failed: " + e);
        }
    }

    private static byte[] readAllBytes(java.io.InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] tmp = new byte[1024];
        int len;
        while ((len = is.read(tmp)) != -1) {
            buf.write(tmp, 0, len);
        }
        return buf.toByteArray();
    }

    private static void setBody(Object builder, String body) throws Exception {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        // Strategy 1: body(byte[]) — OpenFeign 10.x+ / 11.x+
        try {
            builder.getClass().getMethod("body", byte[].class).invoke(builder, bodyBytes);
            MockAgentLogger.debug("setBody via body(byte[]), length=" + bodyBytes.length);
            return;
        } catch (NoSuchMethodException e) {
            MockAgentLogger.debug("body(byte[]) not found, trying next strategy");
        } catch (Exception e) {
            MockAgentLogger.debug("body(byte[]) invocation failed: " + e);
        }

        // Strategy 2: body(InputStream, Integer) — some Feign forks
        try {
            builder.getClass().getMethod("body", InputStream.class, Integer.class)
                    .invoke(builder, new ByteArrayInputStream(bodyBytes), bodyBytes.length);
            MockAgentLogger.debug("setBody via body(InputStream, Integer), length=" + bodyBytes.length);
            return;
        } catch (NoSuchMethodException e) {
            MockAgentLogger.debug("body(InputStream, Integer) not found, trying next strategy");
        } catch (Exception e) {
            MockAgentLogger.debug("body(InputStream, Integer) invocation failed: " + e);
        }

        // Strategy 3: body(InputStream, int) — primitive int variant
        try {
            builder.getClass().getMethod("body", InputStream.class, int.class)
                    .invoke(builder, new ByteArrayInputStream(bodyBytes), (int) bodyBytes.length);
            MockAgentLogger.debug("setBody via body(InputStream, int), length=" + bodyBytes.length);
            return;
        } catch (NoSuchMethodException e) {
            MockAgentLogger.debug("body(InputStream, int) not found, trying next strategy");
        } catch (Exception e) {
            MockAgentLogger.debug("body(InputStream, int) invocation failed: " + e);
        }

        // Strategy 4: Set the internal 'body' field directly via reflection
        try {
            Field bodyField = builder.getClass().getDeclaredField("body");
            bodyField.setAccessible(true);
            // Try to create ResponseBody via InputStreamBody or ByteArrayBody
            Class<?> responseBodyClass = Class.forName("feign.Response$InputStreamBody", false, builder.getClass().getClassLoader());
            Object responseBody = responseBodyClass.getConstructor(InputStream.class, int.class)
                    .newInstance(new ByteArrayInputStream(bodyBytes), bodyBytes.length);
            bodyField.set(builder, responseBody);
            MockAgentLogger.debug("setBody via direct field access (InputStreamBody)");
            return;
        } catch (Exception e) {
            MockAgentLogger.debug("direct field access failed: " + e);
        }

        // Strategy 5: body(Object) — some versions accept Object
        try {
            builder.getClass().getMethod("body", Object.class).invoke(builder, bodyBytes);
            MockAgentLogger.debug("setBody via body(Object), length=" + bodyBytes.length);
            return;
        } catch (NoSuchMethodException e) {
            MockAgentLogger.debug("body(Object) not found");
        } catch (Exception e) {
            MockAgentLogger.debug("body(Object) invocation failed: " + e);
        }

        // Dump all available methods for debugging
        MockAgentLogger.error("Cannot set feign.Response body. Available builder methods:");
        for (Method m : builder.getClass().getMethods()) {
            if (m.getName().contains("body")) {
                MockAgentLogger.error("  " + m);
            }
        }
        throw new RuntimeException("Cannot set feign.Response body: no compatible body() method found on " + builder.getClass().getName());
    }

    private static Object createMockRequest(ClassLoader cl, Class<?> requestClass, String methodStr) throws Exception {
        // OpenFeign >= 10.x: Request.create(HttpMethod, String, Map, byte[], Charset, RequestTemplate)
        try {
            Class<?> httpMethodClass = cl.loadClass("feign.Request$HttpMethod");
            Object httpMethod = httpMethodClass.getMethod("valueOf", String.class).invoke(null, methodStr);
            Class<?> requestTemplateClass = cl.loadClass("feign.RequestTemplate");
            return requestClass.getMethod("create",
                            httpMethodClass, String.class, Map.class, byte[].class, Charset.class, requestTemplateClass)
                    .invoke(null, httpMethod, "mock", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        } catch (ClassNotFoundException e) {
            // fall through
        }

        // OpenFeign 9.x: Request.create(String, String, Map, byte[], Charset)
        try {
            return requestClass.getMethod("create", String.class, String.class, Map.class, byte[].class, Charset.class)
                    .invoke(null, methodStr, "mock", Collections.emptyMap(), null, StandardCharsets.UTF_8);
        } catch (NoSuchMethodException e) {
            // fall through
        }

        // OpenFeign 9.x (with RequestTemplate): Request.create(String, String, Map, byte[], Charset, RequestTemplate)
        try {
            Class<?> requestTemplateClass = cl.loadClass("feign.RequestTemplate");
            return requestClass.getMethod("create",
                            String.class, String.class, Map.class, byte[].class, Charset.class, requestTemplateClass)
                    .invoke(null, methodStr, "mock", Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException("Cannot create feign.Request: no compatible create() method found", e);
        }
    }
}
