package io.lettuce.core.tracing;

import static io.lettuce.core.tracing.RedisObservation.*;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CompleteableCommand;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.tracing.Tracer.Span;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import reactor.core.publisher.Mono;

/**
 * {@link Tracing} adapter using Micrometer's {@link Observation}. This adapter integrates with Micrometer to propagate
 * observations into timers, distributed traces and any other registered handlers. Observations include a set of tags capturing
 * Redis runtime information.
 * <h3>Capturing full statements</h3> This adapter can capture full statements when enabling
 * {@code includeCommandArgsInSpanTags}. You should carefully consider the impact of this setting as all command arguments will
 * be captured in traces including these that may contain sensitive details.
 *
 * @author Mark Paluch, Tommy Luk
 * @since 6.3
 */
public class MicrometerTracing implements Tracing {

    private final ObservationRegistry observationRegistry;

    private final String serviceName;

    private final boolean includeCommandArgsInSpanTags;

    private final LettuceObservationConvention observationConvention;

    private final MicrometerTracer tracer;

    private final MicrometerTraceContextProvider contextProvider;

    /**
     * Create a new {@link MicrometerTracing} instance.
     *
     * @param observationRegistry must not be {@literal null}.
     * @param serviceName service name to be used.
     */
    public MicrometerTracing(ObservationRegistry observationRegistry, String serviceName) {
        this(observationRegistry, serviceName, false);
    }

    /**
     * Create a new {@link MicrometerTracing} instance.
     *
     * @param observationRegistry must not be {@literal null}.
     * @param serviceName service name to be used.
     * @param includeCommandArgsInSpanTags whether to attach the full command into the trace. Use this flag with caution as
     *        sensitive arguments will be captured in the observation spans and metric tags.
     */
    public MicrometerTracing(ObservationRegistry observationRegistry, String serviceName,
            boolean includeCommandArgsInSpanTags) {

        this(observationRegistry, serviceName, new DefaultLettuceObservationConvention(includeCommandArgsInSpanTags));
    }

    /**
     * Create a new {@link MicrometerTracing} instance.
     *
     * @param observationRegistry must not be {@literal null}.
     * @param serviceName service name to be used.
     * @param convention the observation convention to use
     */
    public MicrometerTracing(ObservationRegistry observationRegistry, String serviceName,
            LettuceObservationConvention convention) {

        LettuceAssert.notNull(observationRegistry, "ObservationRegistry must not be null");
        LettuceAssert.notEmpty(serviceName, "Service name must not be empty");
        LettuceAssert.notNull(convention, "LettuceObservationConvention must not be null");

        this.observationRegistry = observationRegistry;
        this.serviceName = serviceName;
        this.observationConvention = convention;
        this.includeCommandArgsInSpanTags = convention.includeCommandArgsInSpanTags();
        this.tracer = new MicrometerTracer(observationRegistry);
        this.contextProvider = new MicrometerTraceContextProvider(observationRegistry);
    }

    @Override
    public TracerProvider getTracerProvider() {
        return () -> this.tracer;
    }

    @Override
    public TraceContextProvider initialTraceContextProvider() {
        return this.contextProvider;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean includeCommandArgsInSpanTags() {
        return includeCommandArgsInSpanTags;
    }

    @Override
    public Endpoint createEndpoint(SocketAddress socketAddress) {
        return new SocketAddressEndpoint(socketAddress);
    }

    /**
     * {@link Tracer} implementation based on Micrometer's {@link ObservationRegistry}.
     */
    class MicrometerTracer extends Tracer {

        private final ObservationRegistry observationRegistry;

        public MicrometerTracer(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
        }

        @Override
        public Span nextSpan() {
            return new MicrometerSpan(serviceName, this::createObservation);
        }

        @Override
        public Span nextSpan(TraceContext traceContext) {

            if (traceContext instanceof MicrometerTraceContext) {

                MicrometerTraceContext micrometerTraceContext = (MicrometerTraceContext) traceContext;

                if (micrometerTraceContext.getObservation() == null) {
                    return nextSpan();
                }

                return new MicrometerSpan(serviceName, context -> {
                    context.setParentObservation(micrometerTraceContext.getObservation());
                    return createObservation(context);
                });
            }

            return nextSpan();
        }

        private Observation createObservation(LettuceObservationContext context) {
            return REDIS_COMMAND_OBSERVATION.observation(observationConvention, new DefaultLettuceObservationConvention(false),
                    () -> context, observationRegistry);
        }

    }

    /**
     * Micrometer {@link Observation}-based {@link Span} implementation.
     */
    static class MicrometerSpan extends Span {

        private final LettuceObservationContext context;

        private final Function<LettuceObservationContext, Observation> observationFactory;

        private Map<String, String> highCardinalityKeyValue;

        private Observation observation;

        public MicrometerSpan(String serviceName, Function<LettuceObservationContext, Observation> observationFactory) {
            this.context = new LettuceObservationContext(serviceName);
            this.observationFactory = observationFactory;
        }

        @Override
        public Span start(RedisCommand<?, ?, ?> command) {

            this.context.setCommand(command);
            this.observation = observationFactory.apply(context);
            if (this.highCardinalityKeyValue != null) {
                this.highCardinalityKeyValue.forEach(this.observation::highCardinalityKeyValue);
            }

            if (command instanceof CompleteableCommand<?>) {

                CompleteableCommand<?> completeableCommand = (CompleteableCommand<?>) command;

                completeableCommand.onComplete((o, throwable) -> {

                    if (command.getOutput() != null) {

                        String error = command.getOutput().getError();
                        if (error != null) {
                            this.observation.highCardinalityKeyValue(HighCardinalityCommandKeyNames.ERROR.withValue(error));
                        } else if (throwable != null) {
                            error(throwable);
                        }
                    }

                    finish();
                });
            } else {
                throw new IllegalArgumentException("Command " + command
                        + " must implement CompleteableCommand to attach Span completion to command completion");
            }

            this.observation.start();
            return this;
        }

        @Override
        public Span name(String name) {
            return this;
        }

        @Override
        public Span annotate(String annotation) {
            return this;
        }

        @Override
        public Span tag(String key, String value) {
            if (this.highCardinalityKeyValue == null) {
                this.highCardinalityKeyValue = new HashMap<>();
            }
            this.highCardinalityKeyValue.put(key, value);
            return this;
        }

        @Override
        public Span error(Throwable throwable) {
            this.observation.error(throwable);
            return this;
        }

        @Override
        public Span remoteEndpoint(Endpoint endpoint) {
            this.context.setEndpoint(endpoint);
            return this;
        }

        @Override
        public void finish() {
            this.observation.stop();
        }

    }

    /**
     * {@link TraceContextProvider} using {@link ObservationRegistry}.
     */
    static final class MicrometerTraceContextProvider implements TraceContextProvider {

        private final ObservationRegistry registry;

        MicrometerTraceContextProvider(ObservationRegistry registry) {
            this.registry = registry;
        }

        @Override
        public TraceContext getTraceContext() {

            Observation observation = registry.getCurrentObservation();

            if (observation == null) {
                return null;
            }

            return new MicrometerTraceContext(observation);
        }

        @Override
        public Mono<TraceContext> getTraceContextLater() {

            return Mono.deferContextual(Mono::justOrEmpty).filter((it) -> {
                return it.hasKey(TraceContext.class) || it.hasKey(Observation.class)
                        || it.hasKey(ObservationThreadLocalAccessor.KEY);
            }).map((it) -> {

                if (it.hasKey(Observation.class)) {
                    return new MicrometerTraceContext(it.get(Observation.class));
                }

                if (it.hasKey(TraceContext.class)) {
                    return it.get(TraceContext.class);
                }

                return new MicrometerTraceContext(it.get(ObservationThreadLocalAccessor.KEY));
            });
        }

        public ObservationRegistry getRegistry() {
            return registry;
        }

    }

    /**
     * {@link TraceContext} implementation using {@link Observation}.
     */
    static class MicrometerTraceContext implements TraceContext {

        private final Observation observation;

        public MicrometerTraceContext(Observation observation) {
            this.observation = observation;
        }

        public Observation getObservation() {
            return observation;
        }

    }

}
