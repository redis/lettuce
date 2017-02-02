/*
 * Copyright 2016-2017 the original author or authors.
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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Publisher;

import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.dynamic.domain.Timeout;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactory;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.segment.AnnotationCommandSegmentFactory;
import com.lambdaworks.redis.dynamic.segment.CommandSegments;
import com.lambdaworks.redis.dynamic.support.ReflectionUtils;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactiveCommandSegmentCommandFactoryTest {

    @Mock
    private CommandOutputFactoryResolver outputFactoryResolver;

    @Mock
    private CommandOutputFactory commandOutputFactory;

    @Before
    public void before() {
        when(outputFactoryResolver.resolveCommandOutput(any())).thenReturn(commandOutputFactory);
    }

    @Test
    public void commandCreationWithTimeoutShouldFail() {

        try {
            createCommand("get", ReactiveWithTimeout.class, String.class, Timeout.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Reactive command methods do not support Timeout parameters");
        }
    }

    protected RedisCommand<?, ?, ?> createCommand(String methodName, Class<?> interfaceClass, Class<?>... parameterTypes) {

        Method method = ReflectionUtils.findMethod(interfaceClass, methodName, parameterTypes);

        CommandMethod commandMethod = DeclaredCommandMethod.create(method);

        AnnotationCommandSegmentFactory segmentFactory = new AnnotationCommandSegmentFactory();
        CommandSegments commandSegments = segmentFactory.createCommandSegments(commandMethod);

        ReactiveCommandSegmentCommandFactory factory = new ReactiveCommandSegmentCommandFactory(commandSegments, commandMethod,
                new StringCodec(), outputFactoryResolver);

        return factory.createCommand(null);
    }

    static interface ReactiveWithTimeout {

        Publisher<String> get(String key, Timeout timeout);
    }
}
