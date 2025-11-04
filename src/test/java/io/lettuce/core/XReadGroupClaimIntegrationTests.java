package io.lettuce.core;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.lettuce.core.models.stream.PendingMessages;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import javax.inject.Inject;
import org.junit.jupiter.api.extension.ExtendWith;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.RedisConditions;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Integration tests for XREADGROUP with CLAIM min-idle-time using a Redis at localhost:6379. These tests are tolerant to
 * servers that do not yet support CLAIM: if the server returns an error for CLAIM, the test will be skipped.
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class XReadGroupClaimIntegrationTests {

    private final RedisClient client;

    private StatefulRedisConnection<String, String> connection;

    private RedisCommands<String, String> sync;

    @Inject
    XReadGroupClaimIntegrationTests(RedisClient client) {
        this.client = client;
    }

    @BeforeEach
    void setup() {
        connection = client.connect();
        sync = connection.sync();
        // Require Redis 8.3.224+ (RC1) for XREADGROUP CLAIM support
        assumeTrue(RedisConditions.of(sync).hasVersionGreaterOrEqualsTo("8.3.224"),
                "Redis 8.3.224+ required for XREADGROUP CLAIM");
    }

    @AfterEach
    void tearDown() {
        if (connection != null)
            connection.close();
    }

    @Test
    void claimPendingEntriesReturnsClaimedStreamMessageWithMetadata() throws Exception {
        String key = "it:stream:claim:" + UUID.randomUUID();
        String group = "g";
        String c1 = "c1";
        String c2 = "c2";

        // Clean slate
        try {
            sync.xgroupDestroy(key, group);
        } catch (Exception ignore) {
        }
        sync.del(key);

        // Produce two entries
        Map<String, String> body = new HashMap<>();
        body.put("f", "v");
        sync.xadd(key, body);
        sync.xadd(key, body);

        // Create group at 0-0 and consume with c1 to move entries to PEL
        sync.xgroupCreate(XReadArgs.StreamOffset.from(key, "0-0"), group);
        sync.xreadgroup(Consumer.from(group, c1), XReadArgs.Builder.count(10), XReadArgs.StreamOffset.lastConsumed(key));

        // Ensure idle time
        Thread.sleep(51);

        // Produce fresh entries that are NOT claimed (not pending)
        sync.xadd(key, body);
        sync.xadd(key, body);

        try {
            XReadArgs readArgs = XReadArgs.Builder.claim(Duration.ofMillis(50));
            readArgs.count(10);
            List<StreamMessage<String, String>> res = sync.xreadgroup(Consumer.from(group, c2), readArgs,
                    XReadArgs.StreamOffset.lastConsumed(key));

            assertThat(res).isNotNull();
            assertThat(res).isNotEmpty();

            long claimedCount = res.stream().filter(StreamMessage::isClaimed).count();
            long freshCount = res.size() - claimedCount;

            assertThat(claimedCount).isEqualTo(2);
            assertThat(freshCount).isEqualTo(2);

            // Order: claimed entries first, then fresh entries
            int firstNonClaimedIdx = -1;
            for (int i = 0; i < res.size(); i++) {
                if (!res.get(i).isClaimed()) {
                    firstNonClaimedIdx = i;
                    break;
                }
            }
            assertThat(firstNonClaimedIdx).isGreaterThanOrEqualTo(0);
            for (int i = 0; i < firstNonClaimedIdx; i++) {
                assertThat(res.get(i).isClaimed()).isTrue();
            }
            for (int i = firstNonClaimedIdx; i < res.size(); i++) {
                assertThat(res.get(i).isClaimed()).isFalse();
            }

            // Validate metadata on one claimed entry
            ClaimedStreamMessage<String, String> first = (ClaimedStreamMessage<String, String>) res.stream()
                    .filter(StreamMessage::isClaimed).findFirst().get();
            assertThat(first.getMsSinceLastDelivery()).isGreaterThanOrEqualTo(51);
            assertThat(first.getRedeliveryCount()).isGreaterThanOrEqualTo(1);
            assertThat(first.getBody()).containsEntry("f", "v");
        } finally {
            try {
                sync.xgroupDestroy(key, group);
            } catch (Exception ignore) {
            }
            sync.del(key);
        }
    }

    @Test
    void claimMovesPendingFromC1ToC2AndRemainsPendingUntilAck() throws Exception {
        String key = "it:stream:claim:move:" + UUID.randomUUID();
        String group = "g";
        String c1 = "c1";
        String c2 = "c2";

        // Clean slate
        try {
            sync.xgroupDestroy(key, group);
        } catch (Exception ignore) {
        }
        sync.del(key);

        Map<String, String> body = new HashMap<>();
        body.put("f", "v");

        // Produce two entries
        String id1 = sync.xadd(key, body);
        String id2 = sync.xadd(key, body);

        // Create group and consume with c1 so entries become pending for c1
        sync.xgroupCreate(XReadArgs.StreamOffset.from(key, "0-0"), group);
        sync.xreadgroup(Consumer.from(group, c1), XReadArgs.Builder.count(10), XReadArgs.StreamOffset.lastConsumed(key));

        // Verify pending belongs to c1
        PendingMessages before = sync.xpending(key, group);
        assertThat(before.getCount()).isEqualTo(2);
        assertThat(before.getConsumerMessageCount().getOrDefault(c1, 0L)).isEqualTo(2);

        // Ensure idle time so entries are claimable
        Thread.sleep(51);

        try {
            // Claim with c2
            XReadArgs readArgs = XReadArgs.Builder.claim(Duration.ofMillis(50));
            readArgs.count(10);
            List<StreamMessage<String, String>> res = sync.xreadgroup(Consumer.from(group, c2), readArgs,
                    XReadArgs.StreamOffset.lastConsumed(key));

            assertThat(res).isNotNull();
            assertThat(res).isNotEmpty();
            long claimed = res.stream().filter(StreamMessage::isClaimed).count();
            assertThat(claimed).isEqualTo(2);

            // After claim: entries are pending for c2 (moved), not acked yet
            PendingMessages afterClaim = sync.xpending(key, group);
            assertThat(afterClaim.getCount()).isEqualTo(2);
            assertThat(afterClaim.getConsumerMessageCount().getOrDefault(c1, 0L)).isEqualTo(0);
            assertThat(afterClaim.getConsumerMessageCount().getOrDefault(c2, 0L)).isEqualTo(2);

            // XACK the claimed entries -> PEL should become empty
            long acked = sync.xack(key, group, res.get(0).getId(), res.get(1).getId());
            assertThat(acked).isEqualTo(2);

            PendingMessages afterAck = sync.xpending(key, group);
            assertThat(afterAck.getCount()).isEqualTo(0);
        } finally {
            try {
                sync.xgroupDestroy(key, group);
            } catch (Exception ignore) {
            }
            sync.del(key);
        }
    }

    @Test
    void claimWithNoackDoesNotCreatePendingAndRemovesClaimedFromPel() throws Exception {
        String key = "it:stream:claim:noack:" + UUID.randomUUID();
        String group = "g";
        String c1 = "c1";
        String c2 = "c2";

        // Clean slate
        try {
            sync.xgroupDestroy(key, group);
        } catch (Exception ignore) {
        }
        sync.del(key);

        // Produce two entries that will become pending for c1
        Map<String, String> body = new HashMap<>();
        body.put("f", "v");
        sync.xadd(key, body);
        sync.xadd(key, body);

        // Create group at 0-0 and consume with c1 to move entries to PEL
        sync.xgroupCreate(XReadArgs.StreamOffset.from(key, "0-0"), group);
        sync.xreadgroup(Consumer.from(group, c1), XReadArgs.Builder.count(10), XReadArgs.StreamOffset.lastConsumed(key));

        // Verify pending belongs to c1
        PendingMessages before = sync.xpending(key, group);
        assertThat(before.getCount()).isEqualTo(2);
        assertThat(before.getConsumerMessageCount().getOrDefault(c1, 0L)).isEqualTo(2);

        // Ensure idle time so entries are claimable
        Thread.sleep(51);

        // Also produce fresh entries that should not be added to PEL when NOACK is set
        sync.xadd(key, body);
        sync.xadd(key, body);

        try {
            // Claim with NOACK using c2
            XReadArgs readArgs = XReadArgs.Builder.claim(Duration.ofMillis(50));
            readArgs.noack(true).count(10);
            List<StreamMessage<String, String>> res = sync.xreadgroup(Consumer.from(group, c2), readArgs,
                    XReadArgs.StreamOffset.lastConsumed(key));

            assertThat(res).isNotNull();
            assertThat(res).isNotEmpty();

            long claimedCount = res.stream().filter(StreamMessage::isClaimed).count();
            long freshCount = res.size() - claimedCount;
            assertThat(claimedCount).isEqualTo(2);
            assertThat(freshCount).isEqualTo(2);

            // After NOACK read, nothing should remain pending in the group
            PendingMessages afterNoack = sync.xpending(key, group);
            assertThat(afterNoack.getCount()).isEqualTo(0);
            assertThat(afterNoack.getConsumerMessageCount().getOrDefault(c1, 0L)).isEqualTo(0);
            assertThat(afterNoack.getConsumerMessageCount().getOrDefault(c2, 0L)).isEqualTo(0);
        } finally {
            try {
                sync.xgroupDestroy(key, group);
            } catch (Exception ignore) {
            }
            sync.del(key);
        }
    }

}
