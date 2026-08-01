package com.mock.agent.registrar;

import com.mock.agent.HttpClientInterceptor;
import com.mock.agent.log.MockAgentLogger;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class HttpClientInterceptorRegistrar implements InterceptorRegistrar {

    @Override
    public void register(AgentBuilder baseBuilder, Instrumentation inst) {
        // Intercept Apache HttpClient.execute(HttpUriRequest) method
        baseBuilder
                .type(hasSuperType(named("org.apache.http.client.HttpClient")).and(not(isInterface())))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        MockAgentLogger.debug("transforming Apache HttpClient class: " + typeDescription.getName());
                        try {
                            Class<?> httpUriRequestClass = classLoader.loadClass("org.apache.http.client.methods.HttpUriRequest");
                            return builder.method(named("execute")
                                            .and(ElementMatchers.takesArguments(httpUriRequestClass)))
                                    .intercept(MethodDelegation.to(HttpClientInterceptor.class));
                        } catch (ClassNotFoundException e) {
                            MockAgentLogger.debug("HttpUriRequest class not found, skipping: " + e);
                            return builder;
                        }
                    }
                })
                .installOn(inst);

        MockAgentLogger.info("installed on Apache HttpClient");
    }
}
