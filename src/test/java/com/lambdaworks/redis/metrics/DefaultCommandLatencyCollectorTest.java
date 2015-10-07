package com.lambdaworks.redis.metrics;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.protocol.CommandType;
import io.netty.channel.local.LocalAddress;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultCommandLatencyCollectorTest {

    private static CommandLatencyCollectorOptions options = DefaultCommandLatencyCollectorOptions.create();

    private DefaultCommandLatencyCollector sut = new DefaultCommandLatencyCollector(options);

    @Test
    public void shutdown() throws Exception {
        sut.shutdown();

        assertThat(sut.isEnabled()).isFalse();
    }

    @Test
    public void verifyMetrics() throws Exception {

        setupData();

        Map<CommandLatencyId, CommandMetrics> latencies = sut.retrieveMetrics();
        assertThat(latencies).hasSize(1);

        Map.Entry<CommandLatencyId, CommandMetrics> entry = latencies.entrySet().iterator().next();

        assertThat(entry.getKey().commandType()).isSameAs(CommandType.BGSAVE);

        CommandMetrics metrics = entry.getValue();

        assertThat(metrics.getCount()).isEqualTo(3);
        assertThat(metrics.getCompletion().getMin()).isBetween(990000L, 1100000L);
        assertThat(metrics.getCompletion().getPercentiles()).hasSize(5);

        assertThat(metrics.getFirstResponse().getMin()).isBetween(90000L, 110000L);
        assertThat(metrics.getFirstResponse().getMax()).isBetween(290000L, 310000L);
        assertThat(metrics.getCompletion().getPercentiles()).containsKey(50.0d);

        assertThat(metrics.getTimeUnit()).isEqualTo(MICROSECONDS);

    }

    private void setupData() {
        sut.recordCommandLatency(LocalAddress.ANY, LocalAddress.ANY, CommandType.BGSAVE, MILLISECONDS.toNanos(100),
                MILLISECONDS.toNanos(1000));
        sut.recordCommandLatency(LocalAddress.ANY, LocalAddress.ANY, CommandType.BGSAVE, MILLISECONDS.toNanos(200),
                MILLISECONDS.toNanos(1000));
        sut.recordCommandLatency(LocalAddress.ANY, LocalAddress.ANY, CommandType.BGSAVE, MILLISECONDS.toNanos(300),
                MILLISECONDS.toNanos(1000));
    }
}
