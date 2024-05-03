package io.lettuce.core.event.command;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.protocol.RedisCommand;

/**
 * Event for succeeded command.
 *
 * @author Mikhael Sokolov
 * @since 6.1
 */
public class CommandSucceededEvent extends CommandBaseEvent {

    // times are in ms
    private final long started;

    private final long completed;

    public CommandSucceededEvent(RedisCommand<Object, Object, Object> command, Map<String, Object> context, long started,
            long completed) {
        super(command, context);
        this.started = started;
        this.completed = completed;
    }

    /**
     * @return execution duration.
     */
    public Duration getDuration() {
        return Duration.ofMillis(completed - started);
    }

    /**
     * @param unit requested {@link TimeUnit} to represent the duration.
     * @return execution duration in {@link TimeUnit}.
     */
    public long getDuration(TimeUnit unit) {
        return unit.convert(completed - started, TimeUnit.MILLISECONDS);
    }

    /**
     * @return {@link Instant} when the command was started.
     */
    public Instant getStarted() {
        return Instant.ofEpochMilli(started);
    }

    /**
     * @return {@link Instant} when the command was completed.
     */
    public Instant getCompleted() {
        return Instant.ofEpochMilli(completed);
    }

}
