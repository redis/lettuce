package io.lettuce.core.tracing;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
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
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.SampleTestRunner;
import org.junit.jupiter.api.Tag;
import reactor.test.StepVerifier;

/**
 * Collection of tests that log metrics and tracing using the reactive API. This guards against regressions in the
 * {@link MicrometerTracing.MicrometerTraceContextProvider#getTraceContextAsync} path used by
 * {@link io.lettuce.core.AbstractRedisReactiveCommands#withTraceContext()}.
 * <p>
 * The {@link SampleTestRunner} framework manages an {@link Observation} on the ThreadLocal. Since {@code getTraceContextAsync}
 * deliberately does not fall back to ThreadLocal when a Reactor context map is provided, this test propagates the current
 * {@link Observation} into the Reactor context via {@code contextWrite} using the {@link Observation Observation.class} key,
 * then verifies that the resulting Lettuce spans are children of the propagated observation (i.e. share its trace id).
 *
 * @author Ali Takavci
 */
@Tag(INTEGRATION_TEST)
public class ReactiveIntegrationTests extends SampleTestRunner {

    ReactiveIntegrationTests() {
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

    public static void main(String[] args) throws Exception {
        ReactiveIntegrationTests tests = new ReactiveIntegrationTests();
        TracingSetup[] setups = tests.getTracingSetup();
        TracingSetup setup = setups[0];
        Class<ReactiveIntegrationTests> c = ReactiveIntegrationTests.class;
        Method m = c.getSuperclass().getDeclaredMethod("run", TracingSetup.class);
        m.setAccessible(true);
        tests.setupRegistry();
        m.invoke(tests, setup);
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

            // Grab the trace id of the SampleTestRunner's current span so we can verify
            // that Lettuce spans become children of this trace.
            io.micrometer.tracing.TraceContext c = tracer.getTracer().currentSpan().context();
            String ctxTraceId = c.traceId();
            String ctxSpanId = c.spanId();

            // Propagate the Observation via the Observation.class key.
            // This exercises the Observation.class branch in getTraceContextAsync.
            connection.reactive().ping()
                    .contextWrite(ctx -> ctx.put(Observation.class, observationRegistry.getCurrentObservation()))
                    .as(StepVerifier::create).expectNext("PONG").verifyComplete();

            connection.close();
            FastShutdown.shutdown(redisClient);
            FastShutdown.shutdown(clientResources);

            assertThat(tracer.getFinishedSpans()).isNotEmpty();

            for (FinishedSpan finishedSpan : tracer.getFinishedSpans()) {
                assertThat(finishedSpan.getTags()).containsEntry("db.system", "redis")
                        .containsEntry("net.sock.peer.addr", TestSettings.host())
                        .containsEntry("net.sock.peer.port", "" + TestSettings.port());
                assertThat(finishedSpan.getTags()).containsKeys("db.operation");
            }

            assertThat(tracer.getFinishedSpans()).anySatisfy(span -> {
                assertThat(span.getTraceId()).isEqualTo(ctxTraceId);
                assertThat(span.getParentId()).isEqualTo(ctxSpanId);
            });

            assertThat(commands).extracting(RedisCommand::getType).contains(CommandType.PING, CommandType.HELLO);
        };
    }

}
