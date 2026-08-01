package com.mock.agent.registrar;

import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

public interface InterceptorRegistrar {

    void register(AgentBuilder builder, Instrumentation inst);
}
