package io.lettuce.core.failover.event;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.event.Event;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;

/**
 * Event that is fired when a database switch occurs.
 *
 * @author Ivo Gaydajiev
 * @since 7.4
 */
@Experimental
public class DatabaseSwitchEvent implements Event {

    private final SwitchReason reason;

    private final RedisURI fromDb;

    private final RedisURI toDb;

    private final StatefulRedisMultiDbConnection<?, ?> source;

    public DatabaseSwitchEvent(SwitchReason reason, RedisURI fromDb, RedisURI toDb,
            StatefulRedisMultiDbConnection<?, ?> source) {
        this.reason = reason;
        this.fromDb = fromDb;
        this.toDb = toDb;
        this.source = source;
    }

    public SwitchReason getReason() {
        return reason;
    }

    public RedisURI getFromDb() {
        return fromDb;
    }

    public RedisURI getToDb() {
        return toDb;
    }

    public StatefulRedisMultiDbConnection<?, ?> getSource() {
        return source;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [reason=").append(reason);
        sb.append(", fromDb=").append(fromDb);
        sb.append(", toDb=").append(toDb);
        sb.append(", source=").append(source);
        sb.append("]");
        return sb.toString();
    }

}
