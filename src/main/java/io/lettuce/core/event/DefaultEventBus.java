/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import io.lettuce.core.event.jfr.EventRecorder;

/**
 * Default implementation for an {@link EventBus}. Events are published using a {@link Scheduler} and events are recorded
 * through {@link EventRecorder#record(Event) EventRecorder}.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class DefaultEventBus implements EventBus {

    private final Sinks.Many<Event> bus;

    private final Scheduler scheduler;

    private final EventRecorder recorder = EventRecorder.getInstance();

    public DefaultEventBus(Scheduler scheduler) {
        this.bus = Sinks.many().multicast().directBestEffort();
        this.scheduler = scheduler;
    }

    @Override
    public Flux<Event> get() {
        return bus.asFlux().onBackpressureDrop().publishOn(scheduler);
    }

    @Override
    public void publish(Event event) {

        recorder.record(event);

        Sinks.EmitResult emitResult;

        while ((emitResult = bus.tryEmitNext(event)) == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            // busy-loop
        }

        if (emitResult != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            emitResult.orThrow();
        }
    }

}
