package com.mock.agent;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.Callable;

import com.mock.agent.log.MockAgentLogger;

public class OkHttpClientInterceptor {

    @RuntimeType
    public static Object intercept(
            @This Object realCall,
            @SuperCall Callable<?> callable) throws Exception {

        ClassLoader cl = realCall.getClass().getClassLoader();

        // Extract the request from RealCall
        Object request = null;
        try {
            Method requestMethod = realCall.getClass().getMethod("request");
            request = requestMethod.invoke(realCall);
        } catch (Exception e) {
            MockAgentLogger.debug("[OkHttp] failed to extract request: " + e);
            return callable.call();
        }

        if (request == null) {
            MockAgentLogger.debug("[OkHttp] request is null, forwarding");
            return callable.call();
        }

        // Extract URL from Request
        Method urlMethod = request.getClass().getMethod("url");
        Object url = urlMethod.invoke(request);
        Method toStringMethod = url.getClass().getMethod("toString");
        String urlStr = (String) toStringMethod.invoke(url);
        URI uri = URI.create(urlStr);
        // URI.getPath() 已返回解码后的路径,无需再 URLDecoder.decode
        String path = uri.getPath();

        // Extract HTTP method
        Method methodMethod = request.getClass().getMethod("method");
        String method = (String) methodMethod.invoke(request);

        // Extract request body
        String requestBody = null;
        try {
            Method bodyMethod = request.getClass().getMethod("body");
            Object body = bodyMethod.invoke(request);
            if (body != null) {
                // Read body using contentLength and writeTo with okio.BufferedSink
                try {
                    Class<?> requestBodyClass = cl.loadClass("okhttp3.RequestBody");
                    Method contentLengthMethod = requestBodyClass.getMethod("contentLength");
                    long contentLength = (long) contentLengthMethod.invoke(body);
                    
                    if (contentLength > 0) {
                        // Create an okio.Buffer (implements BufferedSink) and write the body to it
                        Class<?> bufferedSinkClass = cl.loadClass("okio.BufferedSink");
                        Class<?> bufferClass = cl.loadClass("okio.Buffer");
                        Object buffer = bufferClass.getDeclaredConstructor().newInstance();
                        
                        // Find and invoke writeTo method that accepts BufferedSink
                        Method writeToMethod = body.getClass().getMethod("writeTo", bufferedSinkClass);
                        writeToMethod.invoke(body, buffer);
                        
                        // Read the buffer as UTF-8
                        Method readUtf8Method = bufferClass.getMethod("readUtf8");
                        requestBody = (String) readUtf8Method.invoke(buffer);
                    }
                } catch (Exception e) {
                    MockAgentLogger.debug("[OkHttp] failed to read body via okio: " + e);
                }
            }
        } catch (Exception e) {
            MockAgentLogger.debug("[OkHttp] failed to read request body: " + e);
        }

        MockAgentLogger.info("[OkHttp] intercept: " + method + " " + uri);
        if (requestBody != null) {
            MockAgentLogger.info("[OkHttp] requestBody: " + requestBody);
        }

        MockCase mockCase = MockCaseLoader.findMatch(method, path, requestBody);
        if (mockCase != null) {
            MockAgentLogger.info("[OkHttp] matched mock case: " + method + " " + path);
            return MockOkHttpResponseBuilder.build(cl, mockCase.getStatus(), mockCase.getBody());
        }

        MockAgentLogger.info("[OkHttp] no match, forwarding real call: " + method + " " + path);
        return callable.call();
    }
}
