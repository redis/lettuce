/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.lettuce.core.api.CommandsFactory;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.TransactionCommands;
import io.lettuce.core.api.async.RedisAsyncCommands;

/**
 * Unit tests for {@link TransactionCommands}, its {@link TransactionCommands#factory()} and {@link DefaultTransactionCommands}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class TransactionCommandsFactoryUnitTests {

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Mock
    private TransactionBuilder<String, String> builder;

    @Mock
    private RedisAsyncCommands<String, String> queue;

    @Mock
    private TransactionResult result;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void factoryReturnsStableSingletonKeyedForCaching() {
        CommandsFactory<StatefulRedisConnection<String, String>, TransactionCommands<String, String>> f = TransactionCommands
                .factory();

        // Same instance across calls so StatefulRedisConnection#commands(factory) caches under a stable key.
        assertThat(TransactionCommands.<String, String> factory()).isSameAs(f);
        assertThat(f.key()).isEqualTo(TransactionCommands.class);
    }

    @Test
    void factoryBuildsDefaultTransactionCommandsBoundToConnection() {
        TransactionCommands<String, String> tx = TransactionCommands.<String, String> factory().apply(connection);

        assertThat(tx).isInstanceOf(DefaultTransactionCommands.class);
    }

    @Test
    void createDelegatesToConnectionTransaction() {
        when(connection.transaction()).thenReturn(builder);

        TransactionCommands<String, String> tx = new DefaultTransactionCommands<>(connection);

        assertThat(tx.create()).isSameAs(builder);
        verify(connection).transaction();
    }

    @Test
    void createWithWatchKeysDelegatesToConnectionTransactionWithKeys() {
        when(connection.transaction("watched")).thenReturn(builder);

        TransactionCommands<String, String> tx = new DefaultTransactionCommands<>(connection);

        assertThat(tx.create("watched")).isSameAs(builder);
        verify(connection).transaction("watched");
    }

    @Test
    void transactionalDrainsBuilderAndExecutes() {
        when(connection.transaction()).thenReturn(builder);
        when(builder.queue()).thenReturn(queue);
        when(builder.execute()).thenReturn(result);

        TransactionCommands<String, String> tx = new DefaultTransactionCommands<>(connection);

        TransactionResult r = tx.transactional(t -> t.set("k", "v"));

        assertThat(r).isSameAs(result);
        verify(queue).set("k", "v"); // the body ran against the builder's queue
        verify(builder).execute();
    }

}
