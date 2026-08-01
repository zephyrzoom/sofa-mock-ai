package com.mock.agent.registrar;

import com.mock.agent.OkHttpClientInterceptor;
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

public class OkHttpClientInterceptorRegistrar implements InterceptorRegistrar {

    @Override
    public void register(AgentBuilder baseBuilder, Instrumentation inst) {
        // Intercept RealCall.execute() method - this is where the actual call happens
        baseBuilder
                .type(named("okhttp3.internal.connection.RealCall"))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        MockAgentLogger.debug("transforming OkHttp RealCall class: " + typeDescription.getName());
                        return builder.method(named("execute").and(takesArguments(0)))
                                .intercept(MethodDelegation.to(OkHttpClientInterceptor.class));
                    }
                })
                .installOn(inst);

        MockAgentLogger.info("installed on OkHttp RealCall");
    }
}
