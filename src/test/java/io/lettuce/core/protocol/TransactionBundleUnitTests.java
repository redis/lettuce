/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.output.ValueOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link TransactionBundle}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class TransactionBundleUnitTests {

    private static final StringCodec codec = StringCodec.UTF8;

    private List<RedisCommand<String, String, ?>> commands;

    @BeforeEach
    void setUp() {
        commands = new ArrayList<>();
    }

    @Test
    void shouldEncodeMultiExecWithoutWatch() {
        // Create a simple GET command
        Command<String, String, String> getCmd = new Command<>(CommandType.GET, new ValueOutput<>(codec),
                new CommandArgs<>(codec).addKey("mykey"));
        commands.add(getCmd);

        TransactionBundle<String, String> bundle = new TransactionBundle<>(codec, commands, null);

        ByteBuf buf = Unpooled.directBuffer();
        bundle.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);
        buf.release();

        // Should contain MULTI + GET command + EXEC
        assertThat(encoded).contains("*1\r\n$5\r\nMULTI\r\n");
        assertThat(encoded).contains("$3\r\nGET\r\n");
        assertThat(encoded).contains("$5\r\nmykey\r\n");
        assertThat(encoded).contains("*1\r\n$4\r\nEXEC\r\n");

        // Should NOT contain WATCH
        assertThat(encoded).doesNotContain("WATCH");
    }

    @Test
    void shouldEncodeWatchMultiExec() {
        // Create a SET command
        Command<String, String, String> setCmd = new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("key1").addValue("value1"));
        commands.add(setCmd);

        String[] watchKeys = { "watchKey1", "watchKey2" };
        TransactionBundle<String, String> bundle = new TransactionBundle<>(codec, commands, watchKeys);

        ByteBuf buf = Unpooled.directBuffer();
        bundle.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);
        buf.release();

        // Should start with WATCH containing both keys
        assertThat(encoded).startsWith("*3\r\n$5\r\nWATCH\r\n");
        assertThat(encoded).contains("watchKey1");
        assertThat(encoded).contains("watchKey2");

        // Then MULTI
        assertThat(encoded).contains("*1\r\n$5\r\nMULTI\r\n");

        // Then SET command
        assertThat(encoded).contains("$3\r\nSET\r\n");

        // Finally EXEC
        assertThat(encoded).contains("*1\r\n$4\r\nEXEC\r\n");
    }

    @Test
    void shouldEncodeMultipleCommands() {
        // Create multiple commands
        commands.add(new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("key1").addValue("val1")));
        commands.add(new Command<>(CommandType.GET, new ValueOutput<>(codec), new CommandArgs<>(codec).addKey("key1")));
        commands.add(new Command<>(CommandType.INCR, new ValueOutput<>(codec), new CommandArgs<>(codec).addKey("counter")));

        TransactionBundle<String, String> bundle = new TransactionBundle<>(codec, commands, null);

        ByteBuf buf = Unpooled.directBuffer();
        bundle.encode(buf);
        String encoded = buf.toString(StandardCharsets.UTF_8);
        buf.release();

        // Should contain all commands in order
        int multiIdx = encoded.indexOf("MULTI");
        int setIdx = encoded.indexOf("SET");
        int getIdx = encoded.indexOf("GET");
        int incrIdx = encoded.indexOf("INCR");
        int execIdx = encoded.indexOf("EXEC");

        assertThat(multiIdx).isLessThan(setIdx);
        assertThat(setIdx).isLessThan(getIdx);
        assertThat(getIdx).isLessThan(incrIdx);
        assertThat(incrIdx).isLessThan(execIdx);
    }

    @Test
    void shouldReturnCorrectCommandCount() {
        commands.add(new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("key1").addValue("val1")));
        commands.add(new Command<>(CommandType.GET, new ValueOutput<>(codec), new CommandArgs<>(codec).addKey("key1")));

        TransactionBundle<String, String> bundle = new TransactionBundle<>(codec, commands, null);

        assertThat(bundle.getCommands()).hasSize(2);
    }

    @Test
    void shouldCalculateExpectedResponseCount() {
        commands.add(new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("key1").addValue("val1")));

        // Without WATCH: expect MULTI (1) + QUEUED (1) + EXEC (1) = 3 responses
        TransactionBundle<String, String> bundleNoWatch = new TransactionBundle<>(codec, commands, null);
        assertThat(bundleNoWatch.getExpectedResponseCount()).isEqualTo(3);

        // With WATCH: expect WATCH (1) + MULTI (1) + QUEUED (1) + EXEC (1) = 4 responses
        TransactionBundle<String, String> bundleWithWatch = new TransactionBundle<>(codec, commands,
                new String[] { "watchKey" });
        assertThat(bundleWithWatch.getExpectedResponseCount()).isEqualTo(4);
    }

    @Test
    void shouldResetOutputStateOnReEncode() {
        // Create a simple SET command
        Command<String, String, String> setCmd = new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("mykey").addValue("myvalue"));
        commands.add(setCmd);

        TransactionBundle<String, String> bundle = new TransactionBundle<>(codec, commands, null);
        io.lettuce.core.output.BundleOutput<String, String> output = (io.lettuce.core.output.BundleOutput<String, String>) bundle
                .getOutput();

        // Before any encoding or response processing
        assertThat(output.isResponseComplete()).isFalse();

        // First encode - simulates initial send
        ByteBuf buf1 = Unpooled.directBuffer();
        bundle.encode(buf1);
        String encoded1 = buf1.toString(StandardCharsets.UTF_8);
        buf1.release();

        // Simulate partial response processing that would happen before connection drop
        // For 1 command: expect MULTI (1) + QUEUED (1) + EXEC (1) = 3 responses
        output.complete(0); // Complete MULTI response - 1 of 3
        output.complete(0); // Complete QUEUED response - 2 of 3
        // Still not complete - missing EXEC
        assertThat(output.isResponseComplete()).isFalse();

        // Second encode - simulates retry after connection failure
        // This MUST reset the output state so the bundle can process responses fresh
        ByteBuf buf2 = Unpooled.directBuffer();
        bundle.encode(buf2);
        String encoded2 = buf2.toString(StandardCharsets.UTF_8);
        buf2.release();

        // Both encodes should produce the same wire format
        assertThat(encoded1).isEqualTo(encoded2);

        // After reset, response count should be back to 0 and isResponseComplete() should be false
        assertThat(output.isResponseComplete()).isFalse();

        // Now simulate processing all responses on the retry
        output.complete(0); // MULTI - 1 of 3
        output.complete(0); // QUEUED - 2 of 3
        output.multi(1); // EXEC array start
        output.complete(1); // EXEC array element
        output.complete(0); // EXEC complete - 3 of 3

        // Now should be complete
        assertThat(output.isResponseComplete()).isTrue();
    }

}
