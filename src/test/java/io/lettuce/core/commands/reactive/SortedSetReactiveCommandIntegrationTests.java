package io.lettuce.core.commands.reactive;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import io.lettuce.core.BZMoveArgs;
import io.lettuce.core.Range;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ZMoveArgs;
import io.lettuce.core.ZMoveMemberArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.commands.SortedSetCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for Sorted Sets via {@link io.lettuce.core.api.reactive.RedisReactiveCommands}.
 *
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
class SortedSetReactiveCommandIntegrationTests extends SortedSetCommandIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    SortedSetReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
        this.connection = connection;
    }

    @Test
    @EnabledOnCommand("ZMSCORE")
    public void zmscore() {

        connection.sync().zadd("zset1", 1.0, "a", 2.0, "b");

        reactive.zmscore("zset1", "a", "c", "b").as(StepVerifier::create).assertNext(actual -> {
            assertThat(actual).isEqualTo(list(1.0, null, 2.0));
        }).verifyComplete();
    }

    @Test
    @Disabled("API differences")
    @Override
    public void zrangeUnifiedStreaming() {
    }

    @Test
    @Disabled("API differences")
    @Override
    public void zrangeUnifiedWithScoresStreaming() {
    }

    // Nothing moved arrives as an empty Flux: these override the base tests that expect null.

    /**
     * A score range that selects nothing completes without an element, and creates no destination.
     */
    @Test
    @EnabledOnCommand("ZMOVE")
    @Override
    public void zmoveEmptyScoreRangeRepliesNull() {
        RedisCommands<String, String> sync = connection.sync();
        sync.zadd(SRC, 1.0, "a");

        reactive.zmove(SRC, DST, ZMoveArgs.Builder.byScore(Range.create(5, 6))).as(StepVerifier::create).verifyComplete();
        assertThat(sync.exists(DST)).isEqualTo(0);
    }

    /**
     * A missing source completes without an element for a move and a member move, also when the destination holds a string.
     */
    @Test
    @EnabledOnCommand("ZMOVEMEMBER")
    @Override
    public void moveFromMissingSourceRepliesNull() {

        reactive.zmove(SRC, DST, ZMoveArgs.Builder.min()).as(StepVerifier::create).verifyComplete();
        reactive.zmovemember(SRC, DST, "a").as(StepVerifier::create).verifyComplete();

        connection.sync().set(DST, "string");

        reactive.zmove(SRC, DST, ZMoveArgs.Builder.min()).as(StepVerifier::create).verifyComplete();
        reactive.zmovemember(SRC, DST, "a").as(StepVerifier::create).verifyComplete();
    }

    /**
     * A source that is also the destination completes without an element for each command, and keeps its member; a blocking
     * move on a missing key to itself completes without an element.
     */
    @Test
    @EnabledOnCommand("BZMOVE")
    @Override
    public void moveToSameKeyRepliesNull() {
        RedisCommands<String, String> sync = connection.sync();
        sync.zadd(SRC, 1.0, "a");

        reactive.zmove(SRC, SRC, ZMoveArgs.Builder.min()).as(StepVerifier::create).verifyComplete();
        reactive.bzmove(SRC, SRC, BZMoveArgs.Builder.min().timeout(0L)).as(StepVerifier::create).verifyComplete();
        reactive.zmovemember(SRC, SRC, "a").as(StepVerifier::create).verifyComplete();
        assertThat(sync.zscore(SRC, "a")).isEqualTo(1.0);

        reactive.bzmove("{z}missing", "{z}missing", BZMoveArgs.Builder.min().timeout(0L)).as(StepVerifier::create)
                .verifyComplete();
    }

    /**
     * XX with a missing destination completes without an element, creates no destination and leaves the source.
     */
    @Test
    @EnabledOnCommand("ZMOVEMEMBER")
    @Override
    public void zmovememberXxMissingDestinationRepliesNull() {
        RedisCommands<String, String> sync = connection.sync();
        sync.zadd(SRC, 5.0, "a");

        reactive.zmovemember(SRC, DST, ZMoveMemberArgs.Builder.xx(), "a").as(StepVerifier::create).verifyComplete();
        assertThat(sync.exists(DST)).isEqualTo(0);
        assertThat(sync.zscore(SRC, "a")).isEqualTo(5.0);
    }

    /**
     * A NaN bound errors with the server's message; an inverted range completes without an element.
     */
    @Test
    @EnabledOnCommand("ZMOVE")
    @Override
    public void zmoveNanBoundRejectedAndInvertedRangeRepliesNull() {
        connection.sync().zadd(SRC, 1.0, "a", 2.0, "b", 3.0, "c");

        reactive.zmove(SRC, DST, ZMoveArgs.Builder.byScore(Range.create(Double.NaN, 3.0))).as(StepVerifier::create)
                .expectErrorMatches(e -> e.getMessage().contains("ERR min or max is not a float")).verify();
        reactive.zmove(SRC, DST, ZMoveArgs.Builder.byScore(Range.create(3, 1))).as(StepVerifier::create).verifyComplete();
    }

    /**
     * A blocking move on a missing source with a fractional timeout completes without an element.
     */
    @Test
    @EnabledOnCommand("BZMOVE")
    @Override
    public void bzmoveFractionalTimeoutRepliesNull() {

        reactive.bzmove(SRC, DST, BZMoveArgs.Builder.min().timeout(0.1)).as(StepVerifier::create).verifyComplete();
    }

    /**
     * A positive timeout under one millisecond, and one with a long fraction, complete without an element on a missing source.
     */
    @Test
    @EnabledOnCommand("BZMOVE")
    @Override
    public void bzmoveSubMillisecondAndLongFractionTimeoutsRepliesNull() {

        reactive.bzmove(SRC, DST, BZMoveArgs.Builder.min().timeout(0.0005)).as(StepVerifier::create).verifyComplete();
        reactive.bzmove(SRC, DST, BZMoveArgs.Builder.min().timeout(1.000000001)).as(StepVerifier::create).verifyComplete();
    }

    /**
     * A blocking move on a missing source with a 100 ms timeout completes without an element well under a second.
     */
    @Test
    @EnabledOnCommand("BZMOVE")
    @Override
    public void bzmoveFractionalTimeoutIsNotRoundedToSeconds() {

        Duration elapsed = reactive.bzmove(SRC, DST, BZMoveArgs.Builder.min().timeout(0.1)).as(StepVerifier::create)
                .verifyComplete();
        assertThat(elapsed).isLessThan(Duration.ofMillis(900));
    }

    /**
     * Through the reactive API a missing source completes with no element and no error; moving the two highest members emits
     * them in descending order.
     */
    @Test
    @EnabledOnCommand("ZMOVE")
    public void zmoveReactiveEmitsPairsInOrder() {

        reactive.zmove(SRC, DST, ZMoveArgs.Builder.min()).as(StepVerifier::create).verifyComplete();

        connection.sync().zadd(SRC, 1.0, "a", 2.0, "b", 3.0, "c");

        reactive.zmove(SRC, DST, ZMoveArgs.Builder.max().count(2)).as(StepVerifier::create)
                .expectNext(ScoredValue.just(3.0, "c")).expectNext(ScoredValue.just(2.0, "b")).verifyComplete();
    }

}
