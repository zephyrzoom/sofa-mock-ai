package com.mock.agent.registrar;

import com.mock.agent.RestTemplateInterceptor;
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

public class RestTemplateInterceptorRegistrar implements InterceptorRegistrar {

    @Override
    public void register(AgentBuilder baseBuilder, Instrumentation inst) {
        baseBuilder
                .type(hasSuperType(named("org.springframework.http.client.ClientHttpRequest")).and(not(isInterface())))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        MockAgentLogger.debug("transforming RestTemplate class: " + typeDescription.getName());
                        return builder.method(named("execute").and(ElementMatchers.takesArguments(0)))
                                .intercept(MethodDelegation.to(RestTemplateInterceptor.class));
                    }
                })
                .installOn(inst);

        MockAgentLogger.info("installed on RestTemplate ClientHttpRequest");
    }
}
