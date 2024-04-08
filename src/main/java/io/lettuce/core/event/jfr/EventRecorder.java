package io.lettuce.core.event.jfr;

import io.lettuce.core.event.DurationalEvent;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.RecordableEvent;

/**
 * Event recorder that can delegate events from the {@link io.lettuce.core.event.EventBus} into a recording facility such as
 * JFR and make dual publishing for {@link io.lettuce.core.event.DurationalEvent} to {@link io.lettuce.core.event.EventBus}.
 * Transforming an {@link Event} into a recordable event is subject to the actual {@link EventRecorder} implementation.
 * <p>
 * You can record data by launching the application with recording enabled:
 * {@code java -XX:StartFlightRecording:filename=recording.jfr,duration=10s -jar app.jar}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public interface EventRecorder {

    /**
     * Obtain an instance of the {@link EventRecorder}.
     *
     * @return an instance of the {@link EventRecorder}.
     */
    static EventRecorder getInstance() {
        return EventRecorderHolder.EVENT_RECORDER;
    }

    /**
     * Record an event.
     *
     * @param event the event to record, must not be {@code null}.
     */
    void record(Event event);

    /**
     * Record an event and publish an {@link DurationalEvent.EventStatus#COMPLETED} event to bus.
     *
     * @param event the event to record, must not be {@code null}.
     * @param eventBus the event bus for dual publishing, must not be {@code null}.
     */
    <T extends DurationalEvent & Event> void record(T event, EventBus eventBus);

    /**
     * Start recording an event. This method returns a {@link RecordableEvent} that can be recorded by calling
     * {@link RecordableEvent#record()}. These events can be used to measure time between start and record.
     *
     * @param event the event to record, must not be {@code null}.
     */
    RecordableEvent start(Event event);

    /**
     * Start recording an event and publish an {@link DurationalEvent.EventStatus#IN_PROGRESS} event to bus. This method returns a {@link RecordableEvent} that can be recorded by calling
     * {@link RecordableEvent#record()}. These events can be used to measure time between start and record.
     *
     * @param event the event to record, must not be {@code null}.
     * @param eventBus the event bus for dual publishing, must not be {@code null}.
     */
    <T extends DurationalEvent & Event> RecordableEvent start(T event, EventBus eventBus);

    /**
     * Interface defining a recordable event that is recorded on calling {@link #record()}.
     */
    interface RecordableEvent {

        /**
         * Complete the event recording.
         */
        void record();

    }

}
