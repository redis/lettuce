package com.lambdaworks.redis.metrics;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.net.SocketAddress;

import com.lambdaworks.redis.protocol.ProtocolKeyword;

/**
 * Identifier for a command latency. Consists of a local/remote tuple of {@link SocketAddress}es and a
 * {@link com.lambdaworks.redis.protocol.ProtocolKeyword commandType} part.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class CommandLatencyId implements Serializable, Comparable<CommandLatencyId> {

    private final SocketAddress local;
    private final SocketAddress remote;
    private final ProtocolKeyword commandType;

    protected CommandLatencyId(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType) {
        checkArgument(local != null, "local must not be null");
        checkArgument(remote != null, "remote must not be null");
        checkArgument(commandType != null, "commandType must not be null");

        this.local = local;
        this.remote = remote;
        this.commandType = commandType;
    }

    /**
     * Create a new instance of {@link CommandLatencyId}.
     * 
     * @param local the local address
     * @param remote the remote address
     * @param commandType the command type
     * @return a new instance of {@link CommandLatencyId}
     */
    public static CommandLatencyId create(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType) {
        return new CommandLatencyId(local, remote, commandType);
    }

    /**
     * Returns the local address.
     * 
     * @return the local address
     */
    public SocketAddress getLocal() {
        return local;
    }

    /**
     * Returns the remote address.
     * 
     * @return the remote address
     */
    public SocketAddress getRemote() {
        return remote;
    }

    /**
     * Returns the command type.
     * 
     * @return the command type
     */
    public ProtocolKeyword getCommandType() {
        return commandType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CommandLatencyId))
            return false;

        CommandLatencyId that = (CommandLatencyId) o;

        if (!local.equals(that.local))
            return false;
        if (!remote.equals(that.remote))
            return false;
        return commandType.equals(that.commandType);
    }

    @Override
    public int hashCode() {
        int result = local.hashCode();
        result = 31 * result + remote.hashCode();
        result = 31 * result + commandType.hashCode();
        return result;
    }

    @Override
    public int compareTo(CommandLatencyId o) {

        if (o == null) {
            return -1;
        }

        int remoteResult = local.toString().compareTo(o.local.toString());

        if (remoteResult != 0) {
            return remoteResult;
        }
        int localResult = local.toString().compareTo(o.local.toString());

        if (localResult != 0) {
            return localResult;
        }
        return commandType.toString().compareTo(o.commandType.toString());
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("[").append(local);
        sb.append(" -> ").append(remote);
        sb.append(", commandType=").append(commandType);
        sb.append(']');
        return sb.toString();
    }
}
