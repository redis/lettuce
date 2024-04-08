package io.lettuce.core.event;


public interface DurationalEvent {
    enum EventStatus {
        /**
         * An IN_PROGRESS event indicates the start of series actions and might have future event indicating the completion.
         */
        IN_PROGRESS,
        /**
         * A COMPLETED event is in its final status and indicates the completion of previous event
         * and will not have future event indicating status change.
         */
        COMPLETED
    }
    void completeEvent();
    EventStatus getEventStatus();
}
