package io.lettuce.core.failover.event;

import io.lettuce.core.RedisURI;
import io.lettuce.core.event.Event;

/**
 * Event that is fired when a database switch occurs.
 *
 * @author Ivo Gaydajiev
 * @since 7.4
 */
public class DatabaseSwitchEvent implements Event {

    private final SwitchReason reason;

    private final RedisURI fromDb;

    private final RedisURI toDb;

    public DatabaseSwitchEvent(SwitchReason reason, RedisURI fromDb, RedisURI toDb) {
        this.reason = reason;
        this.fromDb = fromDb;
        this.toDb = toDb;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [reason=").append(reason);
        sb.append(", fromDb=").append(fromDb);
        sb.append(", toDb=").append(toDb);
        sb.append("]");
        return sb.toString();
    }

}
