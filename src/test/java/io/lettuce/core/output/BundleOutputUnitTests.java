/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.TransactionResult;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Unit tests for {@link BundleOutput}.
 *
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class BundleOutputUnitTests {

    private static final StringCodec codec = StringCodec.UTF8;

    private List<RedisCommand<String, String, ?>> commands;

    private BundleOutput<String, String> output;

    @BeforeEach
    void setUp() {
        commands = new ArrayList<>();
    }

    @Test
    void shouldTrackPhaseProgressionWithoutWatch() {
        commands.add(new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("key").addValue("value")));

        output = new BundleOutput<>(codec, commands, false);

        // Initial state
        assertThat(output.getCurrentPhase()).isEqualTo(BundleOutput.Phase.MULTI);
        assertThat(output.isResponseComplete()).isFalse();

        // Receive MULTI response: +OK
        output.set(ByteBuffer.wrap("OK".getBytes()));
        output.complete(0);
        assertThat(output.getCurrentPhase()).isEqualTo(BundleOutput.Phase.QUEUED);
        assertThat(output.isResponseComplete()).isFalse();

        // Receive QUEUED response for SET
        output.set(ByteBuffer.wrap("QUEUED".getBytes()));
        output.complete(0);
        assertThat(output.getCurrentPhase()).isEqualTo(BundleOutput.Phase.EXEC);

        // Receive EXEC response (array with results)
        output.multi(1); // EXEC returns array of 1 result
        output.set(ByteBuffer.wrap("OK".getBytes())); // Result of SET
        output.complete(1);
        output.complete(0); // EXEC response complete
        assertThat(output.isResponseComplete()).isTrue();
    }

    @Test
    void shouldTrackPhaseProgressionWithWatch() {
        commands.add(new Command<>(CommandType.GET, new ValueOutput<>(codec), new CommandArgs<>(codec).addKey("key")));

        output = new BundleOutput<>(codec, commands, true); // hasWatch = true

        // Initial state - starts at WATCH phase
        assertThat(output.getCurrentPhase()).isEqualTo(BundleOutput.Phase.WATCH);

        // Receive WATCH response: +OK
        output.set(ByteBuffer.wrap("OK".getBytes()));
        output.complete(0);
        assertThat(output.getCurrentPhase()).isEqualTo(BundleOutput.Phase.MULTI);

        // Receive MULTI response: +OK
        output.set(ByteBuffer.wrap("OK".getBytes()));
        output.complete(0);
        assertThat(output.getCurrentPhase()).isEqualTo(BundleOutput.Phase.QUEUED);

        // Receive QUEUED response for GET
        output.set(ByteBuffer.wrap("QUEUED".getBytes()));
        output.complete(0);
        assertThat(output.getCurrentPhase()).isEqualTo(BundleOutput.Phase.EXEC);

        // Receive EXEC response
        output.multi(1);
        output.set(ByteBuffer.wrap("myvalue".getBytes()));
        output.complete(1);
        output.complete(0);
        assertThat(output.isResponseComplete()).isTrue();
    }

    @Test
    void shouldHandleMultipleCommandsInExecResult() {
        commands.add(new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("key1").addValue("val1")));
        commands.add(new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("key2").addValue("val2")));
        commands.add(new Command<>(CommandType.GET, new ValueOutput<>(codec), new CommandArgs<>(codec).addKey("key1")));

        output = new BundleOutput<>(codec, commands, false);

        // MULTI response
        output.set(ByteBuffer.wrap("OK".getBytes()));
        output.complete(0);

        // QUEUED responses (3 commands)
        output.set(ByteBuffer.wrap("QUEUED".getBytes()));
        output.complete(0);
        output.set(ByteBuffer.wrap("QUEUED".getBytes()));
        output.complete(0);
        output.set(ByteBuffer.wrap("QUEUED".getBytes()));
        output.complete(0);

        assertThat(output.getCurrentPhase()).isEqualTo(BundleOutput.Phase.EXEC);

        // EXEC array with 3 results
        output.multi(3);
        output.set(ByteBuffer.wrap("OK".getBytes())); // SET result
        output.complete(1);
        output.set(ByteBuffer.wrap("OK".getBytes())); // SET result
        output.complete(1);
        output.set(ByteBuffer.wrap("val1".getBytes())); // GET result
        output.complete(1);
        output.complete(0); // Complete EXEC

        assertThat(output.isResponseComplete()).isTrue();

        // Verify result
        TransactionResult result = output.get();
        assertThat(result).isNotNull();
        assertThat(result.wasDiscarded()).isFalse();
    }

    @Test
    void shouldHandleNullExecOnWatchFailure() {
        commands.add(new Command<>(CommandType.SET, new StatusOutput<>(codec),
                new CommandArgs<>(codec).addKey("key").addValue("val")));

        output = new BundleOutput<>(codec, commands, true);

        // WATCH response
        output.set(ByteBuffer.wrap("OK".getBytes()));
        output.complete(0);
        // MULTI response
        output.set(ByteBuffer.wrap("OK".getBytes()));
        output.complete(0);
        // QUEUED response
        output.set(ByteBuffer.wrap("QUEUED".getBytes()));
        output.complete(0);

        // EXEC returns nil (null bulk string) when WATCH fails
        output.multi(-1); // -1 = null array
        output.complete(0);

        assertThat(output.isDiscarded()).isTrue();
        assertThat(output.get().wasDiscarded()).isTrue();
    }

}
