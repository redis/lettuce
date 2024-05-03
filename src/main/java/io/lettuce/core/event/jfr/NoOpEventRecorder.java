package io.lettuce.core.event.jfr;

import io.lettuce.core.event.Event;

/**
 * No-op implementation.
 *
 * @author Mark Paluch
 * @since 6.1
 */
enum NoOpEventRecorder implements EventRecorder, EventRecorder.RecordableEvent {

    INSTANCE;

    @Override
    public void record(Event event) {

    }

    @Override
    public RecordableEvent start(Event event) {
        return this;
    }

    @Override
    public void record() {

    }

}
