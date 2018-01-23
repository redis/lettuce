/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class ExceptionFactoryTest {

    @Test
    public void shouldCreateBusyException() {

        assertThat(ExceptionFactory.createExecutionException("BUSY foo bar")).isInstanceOf(RedisBusyException.class)
                .hasMessage("BUSY foo bar").hasNoCause();
        assertThat(ExceptionFactory.createExecutionException("BUSY foo bar", new IllegalStateException()))
                .isInstanceOf(RedisBusyException.class).hasMessage("BUSY foo bar")
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldCreateNoscriptException() {

        assertThat(ExceptionFactory.createExecutionException("NOSCRIPT foo bar")).isInstanceOf(RedisNoScriptException.class)
                .hasMessage("NOSCRIPT foo bar").hasNoCause();
        assertThat(ExceptionFactory.createExecutionException("NOSCRIPT foo bar", new IllegalStateException()))
                .isInstanceOf(RedisNoScriptException.class).hasMessage("NOSCRIPT foo bar")
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldCreateExecutionException() {

        assertThat(ExceptionFactory.createExecutionException("ERR foo bar")).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessage("ERR foo bar").hasNoCause();
        assertThat(ExceptionFactory.createExecutionException("ERR foo bar", new IllegalStateException()))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessage("ERR foo bar")
                .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(ExceptionFactory.createExecutionException(null, new IllegalStateException())).isInstanceOf(
                RedisCommandExecutionException.class).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldCreateLoadingException() {

        assertThat(ExceptionFactory.createExecutionException("LOADING foo bar")).isInstanceOf(RedisLoadingException.class)
                .hasMessage("LOADING foo bar").hasNoCause();
        assertThat(ExceptionFactory.createExecutionException("LOADING foo bar", new IllegalStateException()))
                .isInstanceOf(RedisLoadingException.class).hasMessage("LOADING foo bar")
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }
}
