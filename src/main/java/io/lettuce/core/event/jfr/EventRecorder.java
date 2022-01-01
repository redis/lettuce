/*
 * Copyright 2021-2022 the original author or authors.
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
    interface RecordableEvent {

        /**
         * Complete the event recording.
         */
        void record();

    }

}
