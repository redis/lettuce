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
    void shouldFindMaintenanceAwareComponentWhenOutermost() {
        assertThat(ConnectionBuilder.findMaintenanceAwareComponent(expiryWriter)).isSameAs(expiryWriter);
    }

    @Test
    void shouldFindMaintenanceAwareComponentBehindCommandListenerWriter() {

        RedisChannelWriter writer = new CommandListenerWriter(expiryWriter, Collections.emptyList());

        assertThat(ConnectionBuilder.findMaintenanceAwareComponent(writer)).isSameAs(expiryWriter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFindMaintenanceAwareComponentBehindNestedDelegates() {

        RedisChannelWriter outer = mock(RedisChannelWriter.class, withSettings().extraInterfaces(Delegating.class));
        when(((Delegating<RedisChannelWriter>) outer).getDelegate())
                .thenReturn(new CommandListenerWriter(expiryWriter, Collections.emptyList()));

        assertThat(ConnectionBuilder.findMaintenanceAwareComponent(outer)).isSameAs(expiryWriter);
    }

    @Test
    void shouldNotFindMaintenanceAwareComponentWithoutOne() {

        RedisChannelWriter endpoint = mock(RedisChannelWriter.class);

        assertThat(ConnectionBuilder.findMaintenanceAwareComponent(endpoint)).isNull();
        assertThat(
                ConnectionBuilder.findMaintenanceAwareComponent(new CommandListenerWriter(endpoint, Collections.emptyList())))
                        .isNull();
    }

}
