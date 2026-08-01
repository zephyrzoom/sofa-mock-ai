package com.mock.agent;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

public class MockRestResponseBuilder {

    public static Object build(ClassLoader cl, int statusCode, String body) throws Exception {
        Class<?> responseClass = cl.loadClass("org.springframework.http.client.ClientHttpResponse");
        Class<?> httpHeadersClass = cl.loadClass("org.springframework.http.HttpHeaders");
        Class<?> httpStatusClass = cl.loadClass("org.springframework.http.HttpStatus");

        Object headers = httpHeadersClass.getDeclaredConstructor().newInstance();
        setContentType(cl, headers, httpHeadersClass, "application/json; charset=utf-8");

        Object httpStatus = httpStatusClass.getMethod("valueOf", int.class).invoke(null, statusCode);
        Method getReason = httpStatusClass.getMethod("getReasonPhrase");
        String reasonPhrase = (String) getReason.invoke(httpStatus);

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        return Proxy.newProxyInstance(cl, new Class<?>[]{responseClass},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getStatusCode")) return httpStatus;
                    if (name.equals("getRawStatusCode")) return statusCode;
                    if (name.equals("getStatusText")) return reasonPhrase;
                    if (name.equals("getBody")) return new ByteArrayInputStream(bodyBytes);
                    if (name.equals("getHeaders")) return headers;
                    if (name.equals("close")) return null;
                    if (name.equals("toString")) return "MockClientHttpResponse(" + statusCode + ")";
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    throw new UnsupportedOperationException(
                            "MockClientHttpResponse unsupported method: " + name);
                });
    }

    private static void setContentType(ClassLoader cl, Object headers, Class<?> httpHeadersClass, String contentType) throws Exception {
        // Prefer setContentType(MediaType) for proper internal state
        try {
            Class<?> mediaTypeClass = cl.loadClass("org.springframework.http.MediaType");
            Object mediaType = mediaTypeClass.getMethod("parseMediaType", String.class).invoke(null, contentType);
            httpHeadersClass.getMethod("setContentType", mediaTypeClass).invoke(headers, mediaType);
            return;
        } catch (Exception e) {
            // fall through
        }
        // Fallback: set("Content-Type", ...)
        try {
            httpHeadersClass.getMethod("set", String.class, String.class).invoke(headers, "Content-Type", contentType);
        } catch (NoSuchMethodException e) {
            httpHeadersClass.getMethod("add", String.class, String.class).invoke(headers, "Content-Type", contentType);
        }
    }
}
