package com.mock.agent;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import com.mock.agent.log.MockAgentLogger;

public class DecoderInterceptor {

    @RuntimeType
    public static Object intercept(
            @SuperCall Callable<?> callable,
            @AllArguments Object[] args,
            @Origin Method method) throws Exception {

        String methodName = method.getName();
        MockAgentLogger.debug("===== " + methodName + " =====");
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Type) {
                MockAgentLogger.debug("  arg[" + i + "] Type: " + arg);
            } else if (arg != null) {
                MockAgentLogger.debug("  arg[" + i + "] " + arg.getClass().getSimpleName() + ": "
                        + arg.toString().substring(0, Math.min(200, arg.toString().length())));
            } else {
                MockAgentLogger.debug("  arg[" + i + "] null");
            }
        }

        Object result = callable.call();

        MockAgentLogger.debug(methodName + " result: " + (result != null ? result.getClass().getName() : "NULL"));
        if (result != null) {
            try {
                String s = result.toString();
                MockAgentLogger.debug(methodName + " result detail: " + s.substring(0, Math.min(500, s.length())));
            } catch (Exception e) {
                MockAgentLogger.debug("cannot toString result: " + e);
            }
        }
        MockAgentLogger.debug("===== " + methodName + " DONE =====");

        return result;
    }
}
