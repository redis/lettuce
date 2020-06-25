/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class ExceptionFactoryUnitTests {

    @Test
    void shouldCreateBusyException() {

        assertThat(ExceptionFactory.createExecutionException("BUSY foo bar")).isInstanceOf(RedisBusyException.class)
                .hasMessage("BUSY foo bar").hasNoCause();
        assertThat(ExceptionFactory.createExecutionException("BUSY foo bar", new IllegalStateException()))
                .isInstanceOf(RedisBusyException.class).hasMessage("BUSY foo bar")
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCreateNoscriptException() {

        assertThat(ExceptionFactory.createExecutionException("NOSCRIPT foo bar")).isInstanceOf(RedisNoScriptException.class)
                .hasMessage("NOSCRIPT foo bar").hasNoCause();
        assertThat(ExceptionFactory.createExecutionException("NOSCRIPT foo bar", new IllegalStateException()))
                .isInstanceOf(RedisNoScriptException.class).hasMessage("NOSCRIPT foo bar")
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCreateExecutionException() {

        assertThat(ExceptionFactory.createExecutionException("ERR foo bar")).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessage("ERR foo bar").hasNoCause();
        assertThat(ExceptionFactory.createExecutionException("ERR foo bar", new IllegalStateException()))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessage("ERR foo bar")
                .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(ExceptionFactory.createExecutionException(null, new IllegalStateException()))
                .isInstanceOf(RedisCommandExecutionException.class).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCreateLoadingException() {

        assertThat(ExceptionFactory.createExecutionException("LOADING foo bar")).isInstanceOf(RedisLoadingException.class)
                .hasMessage("LOADING foo bar").hasNoCause();
        assertThat(ExceptionFactory.createExecutionException("LOADING foo bar", new IllegalStateException()))
                .isInstanceOf(RedisLoadingException.class).hasMessage("LOADING foo bar")
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldFormatExactUnits() {

        assertThat(ExceptionFactory.formatTimeout(Duration.ofMinutes(2))).isEqualTo("2 minute(s)");
        assertThat(ExceptionFactory.formatTimeout(Duration.ofMinutes(1))).isEqualTo("1 minute(s)");
        assertThat(ExceptionFactory.formatTimeout(Duration.ofMinutes(0))).isEqualTo("no timeout");

        assertThat(ExceptionFactory.formatTimeout(Duration.ofSeconds(2))).isEqualTo("2 second(s)");
        assertThat(ExceptionFactory.formatTimeout(Duration.ofSeconds(1))).isEqualTo("1 second(s)");
        assertThat(ExceptionFactory.formatTimeout(Duration.ofSeconds(0))).isEqualTo("no timeout");

        assertThat(ExceptionFactory.formatTimeout(Duration.ofMillis(2))).isEqualTo("2 millisecond(s)");
        assertThat(ExceptionFactory.formatTimeout(Duration.ofMillis(1))).isEqualTo("1 millisecond(s)");
        assertThat(ExceptionFactory.formatTimeout(Duration.ofMillis(0))).isEqualTo("no timeout");
    }

    @Test
    void shouldFormatToMinmalApplicableTimeunit() {

        assertThat(ExceptionFactory.formatTimeout(Duration.ofMinutes(2).plus(Duration.ofSeconds(10))))
                .isEqualTo("130 second(s)");
        assertThat(ExceptionFactory.formatTimeout(Duration.ofSeconds(2).plus(Duration.ofMillis(5))))
                .isEqualTo("2005 millisecond(s)");
        assertThat(ExceptionFactory.formatTimeout(Duration.ofNanos(2))).isEqualTo("2 ns");
    }

}
