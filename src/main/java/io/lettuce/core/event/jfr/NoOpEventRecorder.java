package io.lettuce.core.event.jfr;

import io.lettuce.core.event.Event;

/**
 * No-op implementation.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public final class NoOpEventRecorder implements EventRecorder, EventRecorder.RecordableEvent {

    public final static NoOpEventRecorder INSTANCE = new NoOpEventRecorder();

    private Event originalEvent = null;

    private NoOpEventRecorder() {
    }

    public NoOpEventRecorder(Event event) {
        this.originalEvent = event;
    }

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

    @Override
    public Event getSource() {
        return originalEvent;
    }

}
