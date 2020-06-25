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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.domain.Timeout;
import io.lettuce.core.dynamic.output.CommandOutputFactory;
import io.lettuce.core.dynamic.output.CommandOutputFactoryResolver;

/**
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchExecutableCommandLookupStrategyUnitTests {

    @Mock
    private RedisCommandsMetadata metadata;

    @Mock
    private StatefulRedisConnection<Object, Object> connection;

    @Mock
    private CommandOutputFactoryResolver outputFactoryResolver;

    @Mock
    private CommandOutputFactory outputFactory;

    private BatchExecutableCommandLookupStrategy sut;

    @BeforeEach
    void before() {
        sut = new BatchExecutableCommandLookupStrategy(Collections.singletonList(StringCodec.UTF8), outputFactoryResolver,
                CommandMethodVerifier.NONE, Batcher.NONE, connection);

        when(outputFactoryResolver.resolveCommandOutput(any())).thenReturn(outputFactory);
    }

    @Test
    void shouldCreateAsyncBatchCommand() throws Exception {

        ExecutableCommand result = sut.resolveCommandMethod(getMethod("async"), metadata);

        assertThat(result).isInstanceOf(BatchExecutableCommand.class);
    }

    @Test
    void shouldCreateSyncBatchCommand() throws Exception {

        ExecutableCommand result = sut.resolveCommandMethod(getMethod("justVoid"), metadata);

        assertThat(result).isInstanceOf(BatchExecutableCommand.class);
    }

    @Test
    void shouldNotAllowTimeoutParameter() {
        assertThatThrownBy(() -> sut.resolveCommandMethod(getMethod("withTimeout", String.class, Timeout.class), metadata))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotAllowSynchronousReturnTypes() {
        assertThatThrownBy(() -> sut.resolveCommandMethod(getMethod("withReturnType"), metadata))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CommandMethod getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return DeclaredCommandMethod.create(BatchingCommands.class.getDeclaredMethod(name, parameterTypes));
    }

    private static interface BatchingCommands {

        Future<String> async();

        String withTimeout(String key, Timeout timeout);

        String withReturnType();

        void justVoid();

    }

}
