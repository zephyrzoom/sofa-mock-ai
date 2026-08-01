package com.mock.agent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import com.mock.agent.log.MockAgentLogger;

public class MockHttpClientResponseBuilder {

    public static Object build(ClassLoader cl, int statusCode, String body) throws Exception {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String reasonPhrase = getReasonPhrase(statusCode);

        // Load Apache HttpClient classes
        Class<?> statusLineClass = cl.loadClass("org.apache.http.StatusLine");
        Class<?> httpEntityClass = cl.loadClass("org.apache.http.HttpEntity");
        Class<?> basicStatusLineClass = cl.loadClass("org.apache.http.message.BasicStatusLine");
        Class<?> basicHttpResponseClass = cl.loadClass("org.apache.http.message.BasicHttpResponse");
        Class<?> basicHttpEntityClass = cl.loadClass("org.apache.http.entity.BasicHttpEntity");
        Class<?> closeableHttpResponseClass = cl.loadClass("org.apache.http.client.methods.CloseableHttpResponse");

        // Create StatusLine
        Class<?> protocolVersionClass = cl.loadClass("org.apache.http.ProtocolVersion");
        Object httpVersion = protocolVersionClass.getDeclaredConstructor(String.class, int.class, int.class)
                .newInstance("HTTP", 1, 1);
        Object statusLine = basicStatusLineClass.getDeclaredConstructor(
                protocolVersionClass, int.class, String.class)
                .newInstance(httpVersion, statusCode, reasonPhrase);

        // Create BasicHttpResponse
        Object basicResponse = basicHttpResponseClass.getDeclaredConstructor(statusLineClass)
                .newInstance(statusLine);

        // Create entity
        Object entity = basicHttpEntityClass.getDeclaredConstructor().newInstance();
        ByteArrayInputStream contentStream = new ByteArrayInputStream(bodyBytes);
        Method setContent = basicHttpEntityClass.getMethod("setContent", InputStream.class);
        setContent.invoke(entity, contentStream);
        Method setContentLength = basicHttpEntityClass.getMethod("setContentLength", long.class);
        setContentLength.invoke(entity, (long) bodyBytes.length);
        Class<?> abstractHttpEntityClass = cl.loadClass("org.apache.http.entity.AbstractHttpEntity");
        Method setContentType = abstractHttpEntityClass.getMethod("setContentType", String.class);
        setContentType.invoke(entity, "application/json");

        // Set entity on response
        Method setEntity = basicHttpResponseClass.getMethod("setEntity", httpEntityClass);
        setEntity.invoke(basicResponse, entity);

        // Create a proxy that implements CloseableHttpResponse
        Object closeableResponse = Proxy.newProxyInstance(cl, new Class<?>[]{closeableHttpResponseClass},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getStatusCode")) {
                        return statusCode;
                    }
                    if (name.equals("getEntity")) {
                        return entity;
                    }
                    if (name.equals("getStatusLine")) {
                        return statusLine;
                    }
                    if (name.equals("getLocale")) {
                        return java.util.Locale.getDefault();
                    }
                    if (name.equals("containsHeader")) {
                        return false;
                    }
                    if (name.equals("getFirstHeader")) {
                        return null;
                    }
                    if (name.equals("getHeaders")) {
                        return java.lang.reflect.Array.newInstance(cl.loadClass("org.apache.http.Header"), 0);
                    }
                    if (name.equals("getAllHeaders")) {
                        return java.lang.reflect.Array.newInstance(cl.loadClass("org.apache.http.Header"), 0);
                    }
                    if (name.equals("getLastHeader")) {
                        return null;
                    }
                    if (name.equals("addHeader")) {
                        return null;
                    }
                    if (name.equals("setHeader")) {
                        return null;
                    }
                    if (name.equals("setHeaders")) {
                        return null;
                    }
                    if (name.equals("removeHeader")) {
                        return false;
                    }
                    if (name.equals("removeHeaders")) {
                        return false;
                    }
                    if (name.equals("headerIterator")) {
                        return null;
                    }
                    if (name.equals("setEntity") && args != null && args.length == 1) {
                        return null;
                    }
                    if (name.equals("close")) {
                        return null;
                    }
                    if (name.equals("toString")) {
                        return "MockCloseableHttpResponse(" + statusCode + ")";
                    }
                    if (name.equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (name.equals("equals")) {
                        return proxy == args[0];
                    }
                    // Delegate to basicResponse for other methods
                    try {
                        return method.invoke(basicResponse, args);
                    } catch (Exception e) {
                        throw new UnsupportedOperationException("Unsupported method: " + name, e);
                    }
                });

        MockAgentLogger.debug("[HttpClient] built mock response: " + statusCode + " " + reasonPhrase);
        return closeableResponse;
    }

    private static String getReasonPhrase(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            default: return "HTTP " + statusCode;
        }
    }
}
