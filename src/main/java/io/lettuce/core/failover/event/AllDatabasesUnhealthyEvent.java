package io.lettuce.core.failover.event;

import java.util.Collections;
import java.util.List;

import io.lettuce.core.RedisURI;
import io.lettuce.core.event.Event;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;

/**
 * Event that is fired when all configured databases are unhealthy and no failover target can be selected after exhausting all
 * retry attempts.
 * <p>
 * This event indicates that the MultiDbClient has attempted to find a healthy database multiple times (up to
 * {@code maxFailoverAttempts}) with delays between attempts, but all databases remain unhealthy.
 *
 * @author Ivo Gaydajiev
 * @since 7.4
 */
public class AllDatabasesUnhealthyEvent implements Event {

    private final int failedAttempts;

    private final List<RedisURI> unhealthyDatabases;

    private final StatefulRedisMultiDbConnection<?, ?> source;

    /**
     * Creates a new {@link AllDatabasesUnhealthyEvent}.
     *
     * @param failedAttempts the number of failed failover attempts
     * @param unhealthyDatabases the list of unhealthy database URIs
     * @param source the connection that fired the event
     */
    public AllDatabasesUnhealthyEvent(int failedAttempts, List<RedisURI> unhealthyDatabases,
            StatefulRedisMultiDbConnection<?, ?> source) {
        this.failedAttempts = failedAttempts;
        this.unhealthyDatabases = unhealthyDatabases != null ? Collections.unmodifiableList(unhealthyDatabases)
                : Collections.emptyList();
        this.source = source;
    }

    /**
     * Returns the number of failed failover attempts before this event was fired.
     *
     * @return the number of failed attempts
     */
    public int getFailedAttempts() {
        return failedAttempts;
    }

    /**
     * Returns an unmodifiable list of unhealthy database URIs at the time this event was fired.
     *
     * @return the list of unhealthy database URIs
     */
    public List<RedisURI> getUnhealthyDatabases() {
        return unhealthyDatabases;
    }

    /**
     * Returns the connection that fired this event.
     *
     * @return the source connection
     */
    public StatefulRedisMultiDbConnection<?, ?> getSource() {
        return source;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [failedAttempts=").append(failedAttempts);
        sb.append(", unhealthyDatabases=").append(unhealthyDatabases);
        sb.append(", source=").append(source);
        sb.append("]");
        return sb.toString();
    }

}
