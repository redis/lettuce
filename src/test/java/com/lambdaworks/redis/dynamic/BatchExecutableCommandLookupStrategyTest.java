/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.domain.Timeout;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactory;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactoryResolver;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class BatchExecutableCommandLookupStrategyTest {

    @Mock
    private RedisCommandsMetadata metadata;
    @Mock
    private StatefulRedisConnection<Object, Object> connection;

    @Mock
    private CommandOutputFactoryResolver outputFactoryResolver;

    @Mock
    private CommandOutputFactory outputFactory;

    private BatchExecutableCommandLookupStrategy sut;

    @Before
    public void before() {
        sut = new BatchExecutableCommandLookupStrategy(Collections.singletonList(StringCodec.UTF8), outputFactoryResolver,
                CommandMethodVerifier.NONE, Batcher.NONE, connection);

        when(outputFactoryResolver.resolveCommandOutput(any())).thenReturn(outputFactory);
    }

    @Test
    public void shouldCreateAsyncBatchCommand() throws Exception {

        ExecutableCommand result = sut.resolveCommandMethod(getMethod("async"), metadata);

        assertThat(result).isInstanceOf(BatchExecutableCommand.class);
    }

    @Test
    public void shouldCreateSyncBatchCommand() throws Exception {

        ExecutableCommand result = sut.resolveCommandMethod(getMethod("justVoid"), metadata);

        assertThat(result).isInstanceOf(BatchExecutableCommand.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowTimeoutParameter() throws Exception {
        sut.resolveCommandMethod(getMethod("withTimeout", String.class, Timeout.class), metadata);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowSynchronousReturnTypes() throws Exception {
        sut.resolveCommandMethod(getMethod("withReturnType"), metadata);
    }

    private CommandMethod getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return DeclaredCommandMethod.create(BatchingCommands.class.getDeclaredMethod(name, parameterTypes));
    }

    static interface BatchingCommands {

        Future<String> async();

        String withTimeout(String key, Timeout timeout);

        String withReturnType();

        void justVoid();
    }
}