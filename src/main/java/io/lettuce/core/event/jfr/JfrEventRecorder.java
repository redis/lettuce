package io.lettuce.core.event.jfr;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import io.lettuce.core.event.Event;
import io.lettuce.core.event.RecordableEvent;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceClassUtils;

/**
 * Java Flight Recorder implementation of {@link EventRecorder}.
 * <p>
 * You can record data by launching the application with recording enabled:
 * {@code java -XX:StartFlightRecording:filename=recording.jfr,duration=10s -jar app.jar}.
 * <p>
 * JFR event forwarding tries to detect a JFR event class that is co-located with the actual event type in the same package
 * whose simple name is prefixed with {@code Jfr} (e.g. {@code ConnectedEvent} translates to {@code JfrConnectedEvent}). JFR
 * event implementations are expected to accept the original event type as constructor argument. Implementations can be
 * package-private.
 *
 * @author Mark Paluch
 */
class JfrEventRecorder implements EventRecorder {

    private final Map<Class<?>, Constructor<?>> constructorMap = new HashMap<>();

    @Override
    public void record(Event event) {

        LettuceAssert.notNull(event, "Event must not be null");

        jdk.jfr.Event jfrEvent = createEvent(event);

        if (jfrEvent != null) {
            jfrEvent.commit();
        }
    }

    @Override
    public RecordableEvent start(Event event) {

        LettuceAssert.notNull(event, "Event must not be null");

        JfrRecordableEvent jfrRecordableEvent = new JfrRecordableEvent(event);
        jdk.jfr.Event jfrEvent = jfrRecordableEvent.getJfrEvent();

        if (jfrEvent != null) {
            jfrEvent.begin();
            return jfrRecordableEvent;
        }

        return NoOpEventRecorder.INSTANCE;
    }

    /**
     * When the given event is an instance of RecordableEvent, this method works the same as {@link #record}.
     * Otherwise, do nothing.
     *
     * @param event the event to be published
     */
    @Override
    public void publish(Event event) {

        LettuceAssert.notNull(event, "Event must not be null");

        if (event instanceof RecordableEvent) {
            ((RecordableEvent) event).record();
        } else {
            record(event);
        }
    }

    private Constructor<?> getEventConstructor(Event event) throws NoSuchMethodException {

        Constructor<?> constructor;

        synchronized (constructorMap) {
            constructor = constructorMap.get(event.getClass());
        }

        if (constructor == null) {

            String jfrClassName = event.getClass().getPackage().getName() + ".Jfr" + event.getClass().getSimpleName();

            Class<?> eventClass = LettuceClassUtils.findClass(jfrClassName);

            if (eventClass == null) {
                constructor = Object.class.getConstructor();
            } else {
                constructor = eventClass.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
            }

            synchronized (constructorMap) {
                constructorMap.put(event.getClass(), constructor);
            }
        }

        return constructor;
    }

    private jdk.jfr.Event createEvent(Event event) {

        try {
            Constructor<?> constructor = getEventConstructor(event);

            if (constructor.getDeclaringClass() == Object.class) {
                return null;
            }

            return (jdk.jfr.Event) constructor.newInstance(event);

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    class JfrRecordableEvent implements RecordableEvent {

        private final Event originalEvent;

        private final jdk.jfr.Event jfrEvent;

        public JfrRecordableEvent(Event event) {
            this.originalEvent = event;
            this.jfrEvent = createEvent(event);
        }

        @Override
        public void record() {
            jfrEvent.end();
            jfrEvent.commit();
        }

        @Override
        public Event getSource() {
            return originalEvent;
        }

        public jdk.jfr.Event getJfrEvent() {
            return jfrEvent;
        }

    }

}
