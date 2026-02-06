/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.support.caching;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link DefaultRedisCache}.
 */
@Tag(UNIT_TEST)
class DefaultRedisCacheUnitTests {

    @Test
    @SuppressWarnings("unchecked")
    void listenerShouldBeRemovedOnClose() {

        StatefulRedisConnectionImpl<String, String> connection = mock(StatefulRedisConnectionImpl.class);
        RedisCommands<String, String> commands = mock(RedisCommands.class);
        when(connection.sync()).thenReturn(commands);

        DefaultRedisCache<String, String> cache = new DefaultRedisCache<>(connection, StringCodec.UTF8);

        cache.addInvalidationListener(key -> {
        });

        cache.close();

        verify(connection).removeListener(any(PushListener.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sharedConnectionShouldNotBeClosed() {

        StatefulRedisConnectionImpl<String, String> connection = mock(StatefulRedisConnectionImpl.class);
        RedisCommands<String, String> commands = mock(RedisCommands.class);
        when(connection.sync()).thenReturn(commands);

        DefaultRedisCache<String, String> cache = new DefaultRedisCache<>(connection, StringCodec.UTF8, false);

        cache.close();

        verify(connection, never()).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void defaultBehaviorShouldCloseConnection() {

        StatefulRedisConnectionImpl<String, String> connection = mock(StatefulRedisConnectionImpl.class);
        RedisCommands<String, String> commands = mock(RedisCommands.class);
        when(connection.sync()).thenReturn(commands);

        DefaultRedisCache<String, String> cache = new DefaultRedisCache<>(connection, StringCodec.UTF8);

        cache.close();

        verify(connection).close();
    }

}
