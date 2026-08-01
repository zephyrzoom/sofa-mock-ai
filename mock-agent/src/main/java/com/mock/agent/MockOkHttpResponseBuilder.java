package com.mock.agent;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import com.mock.agent.log.MockAgentLogger;

public class MockOkHttpResponseBuilder {

    public static Object build(ClassLoader cl, int statusCode, String body) throws Exception {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String reasonPhrase = getReasonPhrase(statusCode);

        Class<?> responseBuilderClass = cl.loadClass("okhttp3.Response$Builder");
        Class<?> protocolClass = cl.loadClass("okhttp3.Protocol");

        Object protocol = protocolClass.getField("HTTP_1_1").get(null);

        // Create ResponseBody via static method
        Class<?> mediaTypeClass = cl.loadClass("okhttp3.MediaType");
        Class<?> responseBodyClass = cl.loadClass("okhttp3.ResponseBody");
        Object mediaType = mediaTypeClass.getMethod("parse", String.class)
                .invoke(null, "application/json; charset=utf-8");
        Object responseBody = responseBodyClass
                .getMethod("create", mediaTypeClass, byte[].class)
                .invoke(null, mediaType, bodyBytes);

        // Create dummy Request
        Class<?> requestClass = cl.loadClass("okhttp3.Request");
        Class<?> requestBuilderClass = cl.loadClass("okhttp3.Request$Builder");
        Object requestBuilder = requestBuilderClass.getDeclaredConstructor().newInstance();
        requestBuilder = requestBuilderClass.getMethod("url", String.class)
                .invoke(requestBuilder, "http://mock/");
        Object request = requestBuilderClass.getMethod("build").invoke(requestBuilder);

        // Build Response using Builder
        Object builder = responseBuilderClass.getDeclaredConstructor().newInstance();
        builder = responseBuilderClass.getMethod("request", requestClass).invoke(builder, request);
        builder = responseBuilderClass.getMethod("protocol", protocolClass).invoke(builder, protocol);
        builder = responseBuilderClass.getMethod("code", int.class).invoke(builder, statusCode);
        builder = responseBuilderClass.getMethod("message", String.class).invoke(builder, reasonPhrase);
        builder = responseBuilderClass.getMethod("body", responseBodyClass).invoke(builder, responseBody);

        Object response = responseBuilderClass.getMethod("build").invoke(builder);

        MockAgentLogger.debug("[OkHttp] built mock response: " + statusCode + " " + reasonPhrase);
        return response;
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
