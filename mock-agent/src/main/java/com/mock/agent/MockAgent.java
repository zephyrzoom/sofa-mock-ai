package com.mock.agent;

import com.mock.agent.log.MockAgentLogger;
import com.mock.agent.registrar.FeignInterceptorRegistrar;
import com.mock.agent.registrar.HttpClientInterceptorRegistrar;
import com.mock.agent.registrar.InterceptorRegistrar;
import com.mock.agent.registrar.OkHttpClientInterceptorRegistrar;
import com.mock.agent.registrar.RestTemplateInterceptorRegistrar;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

public class MockAgent {

    public static void premain(String args, Instrumentation inst) {
        MockAgentLogger.info("premain start");

        boolean byteBuddyDebug = "true".equals(System.getProperty("mock.agent.bytebuddy.debug"));

        AgentBuilder.Listener listener = byteBuddyDebug
                ? AgentBuilder.Listener.StreamWriting.toSystemOut()
                : AgentBuilder.Listener.NoOp.INSTANCE;

        AgentBuilder baseBuilder = new AgentBuilder.Default().with(listener);

        List<InterceptorRegistrar> registrars = Arrays.asList(
                new FeignInterceptorRegistrar(),
                new RestTemplateInterceptorRegistrar(),
                new HttpClientInterceptorRegistrar(),
                new OkHttpClientInterceptorRegistrar()
        );

        for (InterceptorRegistrar registrar : registrars) {
            try {
                registrar.register(baseBuilder, inst);
            } catch (Exception e) {
                MockAgentLogger.error("failed to register interceptor: " + registrar.getClass().getSimpleName(), e);
            }
        }

        MockAgentLogger.info("premain done");

        // Start management API and register agent as early as possible
        MockCaseLoader.earlyInit();
    }
}
