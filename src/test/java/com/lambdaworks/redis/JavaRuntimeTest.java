package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.*;

import org.junit.Test;

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
        assertThat(JavaRuntime.isPresent("total.fancy.class.name")).isFalse();
    }
}
