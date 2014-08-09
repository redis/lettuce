package com.lambdaworks.redis.cluster.models.slots;

import java.io.Serializable;
import java.util.List;

import com.google.common.net.HostAndPort;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 15:20
 */
public class ClusterSlotRange implements Serializable {
    private int from;
    private int to;
    private HostAndPort master;
    private List<HostAndPort> slaves;

    protected ClusterSlotRange() {

    }

    public ClusterSlotRange(int from, int to, HostAndPort master, List<HostAndPort> slaves) {
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
