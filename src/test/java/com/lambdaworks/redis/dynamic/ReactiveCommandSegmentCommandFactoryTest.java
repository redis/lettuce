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

        CommandMethod commandMethod = new CommandMethod(method);

        AnnotationCommandSegmentFactory segmentFactory = new AnnotationCommandSegmentFactory();
        CommandSegments commandSegments = segmentFactory.createCommandSegments(commandMethod);

        ReactiveCommandSegmentCommandFactory<String, String> factory = new ReactiveCommandSegmentCommandFactory<>(
                commandSegments, commandMethod, new StringCodec(), outputFactoryResolver);

        return factory.createCommand(null);
    }

    static interface ReactiveWithTimeout {

        Publisher<String> get(String key, Timeout timeout);
    }
}