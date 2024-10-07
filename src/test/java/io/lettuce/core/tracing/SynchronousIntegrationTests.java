package io.lettuce.core.tracing;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.LinkedBlockingQueue;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.SampleTestRunner;

/**
 * Collection of tests that log metrics and tracing using the synchronous API.
 *
 * @author Mark Paluch
 */
public class SynchronousIntegrationTests extends SampleTestRunner {

    SynchronousIntegrationTests() {
        super(SampleRunnerConfig.builder().build());
    }

    @Override
    protected MeterRegistry createMeterRegistry() {
        return TestConfig.METER_REGISTRY;
    }

    @Override
    protected ObservationRegistry createObservationRegistry() {
        return TestConfig.OBSERVATION_REGISTRY;
    }

    @Override
    public SampleTestRunnerConsumer yourCode() {

        LinkedBlockingQueue<RedisCommand<?, ?, ?>> commands = new LinkedBlockingQueue<>();
        ObservationRegistry observationRegistry = createObservationRegistry();
        observationRegistry.observationConfig().observationPredicate((s, context) -> {

            if (context instanceof LettuceObservationContext) {
                commands.add(((LettuceObservationContext) context).getRequiredCommand());
            }

            return true;
        });
        ClientResources clientResources = ClientResources.builder()
                .tracing(new MicrometerTracing(observationRegistry, "Redis", true)).build();

        return (tracer, meterRegistry) -> {

            RedisURI redisURI = RedisURI.create(TestSettings.host(), TestSettings.port());
            RedisClient redisClient = RedisClient.create(clientResources, redisURI);
            StatefulRedisConnection<String, String> connection = redisClient.connect();

            connection.sync().ping();

            connection.close();
            FastShutdown.shutdown(redisClient);
            FastShutdown.shutdown(clientResources);

            assertThat(tracer.getFinishedSpans()).isNotEmpty();

            assertThat(tracer.getFinishedSpans()).isNotEmpty();

            for (FinishedSpan finishedSpan : tracer.getFinishedSpans()) {
                assertThat(finishedSpan.getTags()).containsEntry("db.system", "redis")
                        .containsEntry("net.sock.peer.addr", TestSettings.host())
                        .containsEntry("net.sock.peer.port", "" + TestSettings.port());
                assertThat(finishedSpan.getTags()).containsKeys("db.operation");
                assertThat(finishedSpan.getTags()).containsKeys("server.address");
                assertThat(finishedSpan.getTags()).containsKeys("db.namespace");
            }

            assertThat(commands).extracting(RedisCommand::getType).contains(CommandType.PING, CommandType.HELLO);
        };
    }

}
