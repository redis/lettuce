package com.lambdaworks.redis.cluster.models.slots;

import static com.google.common.base.Preconditions.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.google.common.net.HostAndPort;

/**
 * Represents a range of slots together with its master and slaves.
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 15:20
 */
@SuppressWarnings("serial")
public class ClusterSlotRange implements Serializable {
    private int from;
    private int to;
    private HostAndPort master;
    private List<HostAndPort> slaves = Collections.emptyList();

    public ClusterSlotRange() {

    }

    /**
     * Constructs a {@link ClusterSlotRange}
     * 
     * @param from from slot
     * @param to to slot
     * @param master master for the slots, may be {@literal null}
     * @param slaves list of slaves must not be {@literal null} but may be empty
     */
    public ClusterSlotRange(int from, int to, HostAndPort master, List<HostAndPort> slaves) {

        checkArgument(master != null, "master must not be null");
        checkArgument(slaves != null, "slaves must not be null");

        this.from = from;
        this.to = to;
        this.master = master;
        this.slaves = slaves;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public HostAndPort getMaster() {
        return master;
    }

    public List<HostAndPort> getSlaves() {
        return slaves;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public void setMaster(HostAndPort master) {
        checkArgument(master != null, "master must not be null");
        this.master = master;
    }

    public void setSlaves(List<HostAndPort> slaves) {

        checkArgument(slaves != null, "slaves must not be null");
        this.slaves = slaves;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [from=").append(from);
        sb.append(", to=").append(to);
        sb.append(", master=").append(master);
        sb.append(", slaves=").append(slaves);
        sb.append(']');
        return sb.toString();
    }
}
