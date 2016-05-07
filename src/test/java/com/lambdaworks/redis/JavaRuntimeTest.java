package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assume.assumeThat;

import com.lambdaworks.redis.internal.LettuceClassUtils;
import org.junit.Test;

import com.lambdaworks.redis.internal.LettuceAssert;

public class JavaRuntimeTest {

    @Test
    public void testJava8() {
        assumeThat(System.getProperty("java.version"), startsWith("1.8"));
        assertThat(JavaRuntime.AT_LEAST_JDK_8).isTrue();
    }

    @Test
    public void testJava9() {
        assumeThat(System.getProperty("java.version"), startsWith("1.9"));
        assertThat(JavaRuntime.AT_LEAST_JDK_8).isTrue();
    }

    @Test
    public void testNotPresentClass() {
        assertThat(LettuceClassUtils.isPresent("total.fancy.class.name")).isFalse();
    }
}
