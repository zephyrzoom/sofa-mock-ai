package com.mock.agent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientInterceptorTest {

    @Test
    void shouldHaveInterceptMethod() throws NoSuchMethodException {
        Method method = HttpClientInterceptor.class.getMethod(
                "intercept",
                java.util.concurrent.Callable.class,
                Object[].class);

        assertThat(method).isNotNull();
        assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
        assertThat(method.getReturnType()).isEqualTo(Object.class);
    }

    @Test
    void shouldHaveCorrectAnnotations() {
        Method[] methods = HttpClientInterceptor.class.getDeclaredMethods();
        assertThat(methods).isNotEmpty();

        Method interceptMethod = null;
        for (Method m : methods) {
            if (m.getName().equals("intercept")) {
                interceptMethod = m;
                break;
            }
        }

        assertThat(interceptMethod).isNotNull();
        assertThat(interceptMethod.isAnnotationPresent(
                net.bytebuddy.implementation.bind.annotation.RuntimeType.class)).isTrue();
    }

    @Test
    void shouldHaveFindFieldMethod() throws NoSuchMethodException {
        Method method = HttpClientInterceptor.class.getDeclaredMethod(
                "findField", Class.class, String.class);

        assertThat(method).isNotNull();
        assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    void shouldHaveReadInputStreamMethod() throws NoSuchMethodException {
        Method method = HttpClientInterceptor.class.getDeclaredMethod(
                "readInputStream", java.io.InputStream.class);

        assertThat(method).isNotNull();
        assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }
}
