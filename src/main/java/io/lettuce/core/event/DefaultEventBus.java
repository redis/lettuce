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

        final Event sourceEvent = (event instanceof EventRecorder.RecordableEvent)
                ? ((EventRecorder.RecordableEvent) event).getSource()
                : event;

        recorder.record(sourceEvent);

        Sinks.EmitResult emitResult;

        while ((emitResult = bus.tryEmitNext(sourceEvent)) == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            // busy-loop
        }

        if (emitResult != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            emitResult.orThrow();
        }
    }

}
