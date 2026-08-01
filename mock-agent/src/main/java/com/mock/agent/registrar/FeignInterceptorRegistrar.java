package com.mock.agent.registrar;

import com.mock.agent.DecoderInterceptor;
import com.mock.agent.FeignInterceptor;
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

public class FeignInterceptorRegistrar implements InterceptorRegistrar {

    @Override
    public void register(AgentBuilder baseBuilder, Instrumentation inst) {
        baseBuilder
                .type(ElementMatchers.nameStartsWith("feign.Client").and(not(isInterface())))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        MockAgentLogger.debug("transforming Feign class: " + typeDescription.getName());
                        return builder.method(ElementMatchers.named("execute"))
                                .intercept(MethodDelegation.to(FeignInterceptor.class));
                    }
                })
                .installOn(inst);

        MockAgentLogger.info("installed on feign.Client");

        new AgentBuilder.Default()
                .type(ElementMatchers.nameContains("SynchronousMethodHandler"))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        MockAgentLogger.debug("transforming: " + typeDescription.getName());
                        return builder.method(ElementMatchers.named("executeAndDecode"))
                                .intercept(MethodDelegation.to(DecoderInterceptor.class));
                    }
                })
                .installOn(inst);

        new AgentBuilder.Default()
                .type(hasSuperType(named("feign.Decoder")).and(not(isInterface())))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                            TypeDescription typeDescription,
                                                            ClassLoader classLoader,
                                                            JavaModule module,
                                                            ProtectionDomain protectionDomain) {
                        MockAgentLogger.debug("transforming decoder: " + typeDescription.getName());
                        return builder.method(ElementMatchers.named("decode").and(ElementMatchers.takesArguments(2)))
                                .intercept(MethodDelegation.to(DecoderInterceptor.class));
                    }
                })
                .installOn(inst);

        MockAgentLogger.info("installed decoder interceptors");
    }
}
