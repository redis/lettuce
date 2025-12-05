/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import io.lettuce.core.RedisClient;
import io.lettuce.core.TestSupport;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.event.connection.ReconnectAttemptEvent;
import io.netty.handler.codec.EncoderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.LettuceExtension;

/**
 * Integration tests for command encoding error scenarios with GET/SET commands against a Redis test instance.
 *
 * @author Lettuce Contributors
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag(INTEGRATION_TEST)
class CodecFailureIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    CodecFailureIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.connection = connection;
    }

    @BeforeEach
    void setUp() {
        this.connection.async().flushall();
    }

    @Test
    void testCommandsWithCustomCodecRuntimeException() {

        final Integer[] reconnects = { 0 };
        client.getResources().eventBus().get().subscribe(event -> {
            if (event instanceof ReconnectAttemptEvent) {
                reconnects[0]++;
            }
        });

        try (StatefulRedisConnection<String, String> customConnection = client.connect(failingCodec)) {
            RedisCommands<String, String> customRedis = customConnection.sync();

            // First, test that normal values work fine
            String normalKey = "normal-key";
            String normalValue = "normal-value";

            String result = customRedis.set(normalKey, normalValue);
            assertThat(result).isEqualTo("OK");

            String retrieved = customRedis.get(normalKey);
            assertThat(retrieved).isEqualTo(normalValue);

            // Now test that the specific failure value throws an exception
            String failingKey = "failing-key";
            String failingValue = "encoding_failure";

            assertThatThrownBy(() -> customRedis.set(failingKey, failingValue)).isInstanceOf(EncoderException.class)
                    .hasMessageContaining(
                            "Cannot encode command. Closing the connection as the connection state may be out of sync.");

            // test that commands are executed after reconnecting
            retrieved = customRedis.get(normalKey);
            assertThat(retrieved).isEqualTo(normalValue);

            // verify that we have reconnected after the exception
            assertThat(reconnects[0]).isEqualTo(1);
        }
    }

    @Test
    void testCommandsWithCustomCodecOutOfMemoryError() {

        try (StatefulRedisConnection<String, String> customConnection = client.connect(failingCodecOOM)) {
            RedisCommands<String, String> customRedis = customConnection.sync();

            // First, test that normal values work fine
            String normalKey = "normal-key";
            String normalValue = "normal-value";

            String result = customRedis.set(normalKey, normalValue);
            assertThat(result).isEqualTo("OK");

            String retrieved = customRedis.get(normalKey);
            assertThat(retrieved).isEqualTo(normalValue);

            // Now test that the specific failure value throws an exception
            String failingKey = "failing-key";
            String failingValue = "encoding_failure";

            assertThatThrownBy(() -> customRedis.set(failingKey, failingValue)).isInstanceOf(EncoderException.class)
                    .hasMessageContaining(
                            "Cannot encode command. Closing the connection as the connection state may be out of sync.");

            // test that we can get correct response after encoding failure
            retrieved = customRedis.get(normalKey);
            assertThat(retrieved).isEqualTo(normalValue);
        }
    }

    @Test
    void testDecodeFailureForReply() {
        // First, set a value using the normal connection
        String testKey = "decode-failure-key";
        String testValue = "decode_failure_trigger";

        connection.sync().set(testKey, testValue);

        // Create a codec that fails during value decoding for specific values
        RedisCodec<String, String> decodingFailureCodec = new RedisCodec<String, String>() {

            @Override
            public String decodeKey(ByteBuffer bytes) {
                return StandardCharsets.UTF_8.decode(bytes).toString();
            }

            @Override
            public String decodeValue(ByteBuffer bytes) {
                String value = StandardCharsets.UTF_8.decode(bytes).toString();
                // Throw exception when decoding specific value
                if ("decode_failure_trigger".equals(value)) {
                    throw new RuntimeException("Simulated decoding failure during value decoding");
                }
                return value;
            }

            @Override
            public ByteBuffer encodeKey(String key) {
                return StandardCharsets.UTF_8.encode(key);
            }

            @Override
            public ByteBuffer encodeValue(String value) {
                return StandardCharsets.UTF_8.encode(value);
            }

        };

        try (StatefulRedisConnection<String, String> customConnection = client.connect(decodingFailureCodec)) {
            RedisCommands<String, String> customRedis = customConnection.sync();

            // Test that normal values work fine
            String normalKey = "normal-decode-key";
            String normalValue = "normal-value";

            customRedis.set(normalKey, normalValue);
            String retrieved = customRedis.get(normalKey);
            assertThat(retrieved).isEqualTo(normalValue);

            // Now test that decoding the problematic value throws an exception
            // The command was executed on Redis (the value is there), but decoding fails
            assertThatThrownBy(() -> customRedis.get(testKey))
                    .hasMessageContaining("Simulated decoding failure during value decoding");

            // Verify that the connection remains usable after decode failure
            retrieved = customRedis.get(normalKey);
            assertThat(retrieved).isEqualTo(normalValue);

            // Verify the value is actually stored in Redis using the normal connection
            String valueFromNormalConnection = connection.sync().get(testKey);
            assertThat(valueFromNormalConnection).isEqualTo(testValue);
        }
    }

    @Test
    void testDecodeFailureForReplyOOM() {
        // First, set a value using the normal connection
        String testKey = "decode-failure-key";
        String testValue = "decode_failure_trigger";

        connection.sync().set(testKey, testValue);

        // Create a codec that fails during value decoding for specific values
        RedisCodec<String, String> decodingFailureCodec = new RedisCodec<String, String>() {

            @Override
            public String decodeKey(ByteBuffer bytes) {
                return StandardCharsets.UTF_8.decode(bytes).toString();
            }

            @Override
            public String decodeValue(ByteBuffer bytes) {
                String value = StandardCharsets.UTF_8.decode(bytes).toString();
                // Throw exception when decoding specific value
                if ("decode_failure_trigger".equals(value)) {
                    throw new OutOfMemoryError("Simulated decoding failure during value decoding");
                }
                return value;
            }

            @Override
            public ByteBuffer encodeKey(String key) {
                return StandardCharsets.UTF_8.encode(key);
            }

            @Override
            public ByteBuffer encodeValue(String value) {
                return StandardCharsets.UTF_8.encode(value);
            }

        };

        try (StatefulRedisConnection<String, String> customConnection = client.connect(decodingFailureCodec)) {
            RedisCommands<String, String> customRedis = customConnection.sync();

            // Test that normal values work fine
            String normalKey = "normal-decode-key";
            String normalValue = "normal-value";

            customRedis.set(normalKey, normalValue);
            String retrieved = customRedis.get(normalKey);
            assertThat(retrieved).isEqualTo(normalValue);

            // Now test that decoding the problematic value throws an exception
            // The command was executed on Redis (the value is there), but decoding fails
            assertThatThrownBy(() -> customRedis.get(testKey))
                    .hasMessageContaining("Simulated decoding failure during value decoding");

            // Verify that the connection remains usable after decode failure
            retrieved = customRedis.get(normalKey);
            assertThat(retrieved).isEqualTo(normalValue);

            // Verify the value is actually stored in Redis using the normal connection
            String valueFromNormalConnection = connection.sync().get(testKey);
            assertThat(valueFromNormalConnection).isEqualTo(testValue);
        }
    }

    // Create a codec that fails during value encoding with "encoding_failure" keyword
    RedisCodec<String, String> failingCodec = new RedisCodec<String, String>() {

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return StandardCharsets.UTF_8.encode(key);
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            // Only throw exception for specific value to test selective encoding failure
            if ("encoding_failure".equals(value)) {
                throw new RuntimeException("Simulated encoding failure during value encoding");
            }
            return StandardCharsets.UTF_8.encode(value);
        }

    };

    // Create a codec that fails during value encoding with "encoding_failure" keyword
    RedisCodec<String, String> failingCodecOOM = new RedisCodec<String, String>() {

        @Override
        public String decodeKey(ByteBuffer bytes) {
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }

        @Override
        public String decodeValue(ByteBuffer bytes) {
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }

        @Override
        public ByteBuffer encodeKey(String key) {
            return StandardCharsets.UTF_8.encode(key);
        }

        @Override
        public ByteBuffer encodeValue(String value) {
            // Only throw exception for specific value to test selective encoding failure
            if ("encoding_failure".equals(value)) {
                throw new OutOfMemoryError("JVM running out of memory during decoding");
            }
            return StandardCharsets.UTF_8.encode(value);
        }

    };

}
