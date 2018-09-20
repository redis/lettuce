/*
 * Copyright 2016-2018 the original author or authors.
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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.domain.Timeout;
import io.lettuce.core.dynamic.output.CommandOutputFactory;
import io.lettuce.core.dynamic.output.CommandOutputFactoryResolver;
import io.lettuce.core.dynamic.segment.AnnotationCommandSegmentFactory;
import io.lettuce.core.dynamic.segment.CommandSegments;
import io.lettuce.core.dynamic.support.ReflectionUtils;
import io.lettuce.core.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
class ReactiveCommandSegmentCommandFactoryUnitTests {

    @Mock
    private CommandOutputFactoryResolver outputFactoryResolver;

    @Mock
    private CommandOutputFactory commandOutputFactory;

    @BeforeEach
    void before() {
        when(outputFactoryResolver.resolveCommandOutput(any())).thenReturn(commandOutputFactory);
    }

    @Test
    void commandCreationWithTimeoutShouldFail() {

        try {
            createCommand("get", ReactiveWithTimeout.class, String.class, Timeout.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Reactive command methods do not support Timeout parameters");
        }
    }

    RedisCommand<?, ?, ?> createCommand(String methodName, Class<?> interfaceClass, Class<?>... parameterTypes) {

        Method method = ReflectionUtils.findMethod(interfaceClass, methodName, parameterTypes);

        CommandMethod commandMethod = DeclaredCommandMethod.create(method);

        AnnotationCommandSegmentFactory segmentFactory = new AnnotationCommandSegmentFactory();
        CommandSegments commandSegments = segmentFactory.createCommandSegments(commandMethod);

        ReactiveCommandSegmentCommandFactory factory = new ReactiveCommandSegmentCommandFactory(commandSegments, commandMethod,
                new StringCodec(), outputFactoryResolver);

        return factory.createCommand(null);
    }

    private static interface ReactiveWithTimeout {

        Publisher<String> get(String key, Timeout timeout);
    }
}
