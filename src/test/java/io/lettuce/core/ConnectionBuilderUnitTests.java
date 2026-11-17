/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Collections;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.MaintenanceAwareExpiryWriter;

/**
 * Unit tests for {@link ConnectionBuilder}.
 */
@Tag(UNIT_TEST)
class ConnectionBuilderUnitTests {

    private final MaintenanceAwareExpiryWriter expiryWriter = mock(MaintenanceAwareExpiryWriter.class);

    @Test
    void shouldFindMaintenanceNotificationListenerWhenOutermost() {
        assertThat(ConnectionBuilder.findMaintenanceNotificationListener(expiryWriter)).isSameAs(expiryWriter);
    }

    @Test
    void shouldFindMaintenanceNotificationListenerBehindCommandListenerWriter() {

        RedisChannelWriter writer = new CommandListenerWriter(expiryWriter, Collections.emptyList());

        assertThat(ConnectionBuilder.findMaintenanceNotificationListener(writer)).isSameAs(expiryWriter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFindMaintenanceNotificationListenerBehindNestedDelegates() {

        RedisChannelWriter outer = mock(RedisChannelWriter.class, withSettings().extraInterfaces(Delegating.class));
        when(((Delegating<RedisChannelWriter>) outer).getDelegate())
                .thenReturn(new CommandListenerWriter(expiryWriter, Collections.emptyList()));

        assertThat(ConnectionBuilder.findMaintenanceNotificationListener(outer)).isSameAs(expiryWriter);
    }

    @Test
    void shouldNotFindMaintenanceNotificationListenerWithoutOne() {

        RedisChannelWriter endpoint = mock(RedisChannelWriter.class);

        assertThat(ConnectionBuilder.findMaintenanceNotificationListener(endpoint)).isNull();
        assertThat(ConnectionBuilder
                .findMaintenanceNotificationListener(new CommandListenerWriter(endpoint, Collections.emptyList()))).isNull();
    }

}
