package io.lettuce.core.tracing;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

/**
 * @author Mark Paluch
 */
class TestConfig {

    static final MeterRegistry METER_REGISTRY = new SimpleMeterRegistry();

    static final ObservationRegistry OBSERVATION_REGISTRY = ObservationRegistry.create();

    static {
        OBSERVATION_REGISTRY.observationConfig().observationHandler(new DefaultMeterObservationHandler(METER_REGISTRY));
    }

}
