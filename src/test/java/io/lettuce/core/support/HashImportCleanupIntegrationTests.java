/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.support;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.HashImport;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * Cleanup coverage for {@code HIMPORT}: {@link HashImport#close()} must send a best-effort targeted {@code HIMPORT DISCARD} to
 * every physical connection the fieldset was prepared on, freeing the server-side session state. Server-side state is probed
 * directly with a <em>raw</em> {@code HIMPORT SET} — a plain command the outbound handler ignores, so it injects no
 * {@code PREPARE} — which succeeds while the fieldset is prepared on the connection and fails {@code no such fieldset} once it
 * has been discarded. This is the behavior the v1 design got wrong (pooled discard missed sibling connections).
 */
@Tag(INTEGRATION_TEST)
@EnabledOnCommand("HIMPORT")
class HashImportCleanupIntegrationTests extends TestSupport {

    private static RedisClient client;

    @BeforeAll
    static void setupClient() {
        client = RedisClient.create(TestClientResources.create(), RedisURI.Builder.redis(host, port).build());
    }

    @AfterAll
    static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    /**
     * On a single connection, {@code close()} discards the fieldset server-side: after it, a raw {@code SET} for the same
     * fieldset name draws {@code no such fieldset}.
     */
    @Test
    void closeFreesServerStateOnTheConnection() throws Exception {

        StatefulRedisConnection<String, String> connection = client.connect();
        try {
            connection.sync().flushall();
            HashImport<String> fieldset = HashImport.of("name", "email");

            assertThat(connection.sync().himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
            // The fieldset is prepared on this connection: a raw (unmanaged) SET for its name succeeds.
            assertThat(fieldsetPreparedOn(connection, fieldset)).isTrue();

            fieldset.close();

            // close() sent a DISCARD on this connection: the raw SET now fails "no such fieldset".
            assertThat(fieldsetPreparedOn(connection, fieldset)).isFalse();
        } finally {
            connection.close();
        }
    }

    /**
     * {@code close()} discards the fieldset on <em>every</em> pooled connection it was used on, not just one — the v1 pooled
     * discard bug. Both connections have the fieldset before {@code close()} and neither has it afterwards.
     */
    @Test
    void closeDiscardsOnEveryTouchedPooledConnection() throws Exception {

        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(2);
        GenericObjectPool<StatefulRedisConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> client.connect(), config, false);

        try {
            StatefulRedisConnection<String, String> c1 = pool.borrowObject();
            StatefulRedisConnection<String, String> c2 = pool.borrowObject();
            assertThat(c1).isNotSameAs(c2);
            c1.sync().flushall();

            HashImport<String> fieldset = HashImport.of("name", "email");

            // Use the fieldset on both connections, self-preparing it on each.
            assertThat(c1.sync().himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");
            assertThat(c2.sync().himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
            assertThat(fieldsetPreparedOn(c1, fieldset)).isTrue();
            assertThat(fieldsetPreparedOn(c2, fieldset)).isTrue();

            fieldset.close();

            // Both connections received the DISCARD.
            assertThat(fieldsetPreparedOn(c1, fieldset)).isFalse();
            assertThat(fieldsetPreparedOn(c2, fieldset)).isFalse();

            pool.returnObject(c1);
            pool.returnObject(c2);
        } finally {
            pool.close();
        }
    }

    /**
     * {@code close()} is idempotent and best-effort: closing a never-used fieldset is a no-op, and a second {@code close()}
     * does nothing — neither throws.
     */
    @Test
    void closeIsIdempotentAndSafeWithoutPreparedConnections() throws Exception {

        HashImport<String> neverUsed = HashImport.of("name", "email");
        assertThatCode(neverUsed::close).doesNotThrowAnyException();
        assertThat(neverUsed.isDiscarded()).isTrue();

        StatefulRedisConnection<String, String> connection = client.connect();
        try {
            connection.sync().himportSet("u:1", HashImport.of("name", "email"), "alice", "a@x.com");
            HashImport<String> used = HashImport.of("sku", "price");
            connection.sync().himportSet("s:1", used, "sku-1", "9.99");

            assertThatCode(used::close).doesNotThrowAnyException();
            assertThatCode(used::close).doesNotThrowAnyException();
            assertThat(fieldsetPreparedOn(connection, used)).isFalse();
        } finally {
            connection.close();
        }
    }

    /**
     * Probes whether {@code fieldset} is prepared on {@code connection} by dispatching a raw {@code HIMPORT SET} that the
     * outbound handler ignores (it is not a {@code HashImportSetCommand}, so no {@code PREPARE} is injected). Returns
     * {@code true} when the {@code SET} succeeds (fieldset present) and {@code false} on a {@code no such fieldset} error.
     */
    private static boolean fieldsetPreparedOn(StatefulRedisConnection<String, String> connection, HashImport<String> fieldset)
            throws Exception {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandType.SET).addKey("probe")
                .addKey(fieldset.name()).addValue("x").addValue("y");
        AsyncCommand<String, String, String> command = new AsyncCommand<>(
                new Command<>(CommandType.HIMPORT, new StatusOutput<>(StringCodec.UTF8), args));
        connection.dispatch(command);

        try {
            command.get(5, TimeUnit.SECONDS);
            return true;
        } catch (java.util.concurrent.ExecutionException ex) {
            String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            if (message != null && message.toLowerCase().contains("no such fieldset")) {
                return false;
            }
            throw ex;
        }
    }

}
