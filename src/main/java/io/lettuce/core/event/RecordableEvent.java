package io.lettuce.core.event;

/**
 * Interface defining a recordable event that is recorded on calling {@link #record()}.
 */
public interface RecordableEvent extends Event {

    /**
     * Complete the event recording.
     */
    void record();

    /**
     * Get the source event.
     */
    Event getSource();

}
