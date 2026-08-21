package io.lettuce.core.event.jfr;

import io.lettuce.core.event.Event;

/**
 * Event recorder that can delegate events from the {@link io.lettuce.core.event.EventBus} into a recording facility such as
 * JFR. Transforming an {@link Event} into a recordable event is subject to the actual {@link EventRecorder} implementation.
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
     * Start recording an event. This method returns a {@link RecordableEvent} that can be recorded by calling
     * {@link RecordableEvent#record()}. These events can be used to measure time between start and record.
     *
     * @param event the event to record, must not be {@code null}.
     */
    RecordableEvent start(Event event);

    /**
     * Interface defining a recordable event that is recorded on calling {@link #record()}.
     */
    interface RecordableEvent extends Event {

        /**
         * Complete the event recording.
         */
        void record();

        /**
         * Get the source event.
         */
        Event getSource();

    }

}
