package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.*;

import com.lambdaworks.redis.internal.LettuceClassUtils;
import org.junit.Test;

import com.google.common.base.StandardSystemProperty;

public class JavaRuntimeTest {

    @Test
    public void testJava6() {
        assumeThat(StandardSystemProperty.JAVA_VERSION.value(), startsWith("1.6"));
        assertThat(JavaRuntime.AT_LEAST_JDK_6).isTrue();
        assertThat(JavaRuntime.AT_LEAST_JDK_7).isFalse();
        assertThat(JavaRuntime.AT_LEAST_JDK_8).isFalse();
    }

    @Test
    public void testJava7() {
        assumeThat(StandardSystemProperty.JAVA_VERSION.value(), startsWith("1.7"));
        assertThat(JavaRuntime.AT_LEAST_JDK_6).isTrue();
        assertThat(JavaRuntime.AT_LEAST_JDK_7).isTrue();
        assertThat(JavaRuntime.AT_LEAST_JDK_8).isFalse();
    }

    @Test
    public void testJava8() {
        assumeThat(StandardSystemProperty.JAVA_VERSION.value(), startsWith("1.8"));
        assertThat(JavaRuntime.AT_LEAST_JDK_6).isTrue();
        assertThat(JavaRuntime.AT_LEAST_JDK_7).isTrue();
        assertThat(JavaRuntime.AT_LEAST_JDK_8).isTrue();
    }

    @Test
    public void testJava9() {
        assumeThat(StandardSystemProperty.JAVA_VERSION.value(), startsWith("1.9"));
        assertThat(JavaRuntime.AT_LEAST_JDK_6).isTrue();
        assertThat(JavaRuntime.AT_LEAST_JDK_7).isTrue();
        assertThat(JavaRuntime.AT_LEAST_JDK_8).isTrue();
    }

    @Test
    public void testNotPresentClass() {
        assertThat(LettuceClassUtils.isPresent("total.fancy.class.name")).isFalse();
    }
}
