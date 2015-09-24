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

    private final SocketAddress localAddress;
    private final SocketAddress remoteAddress;
    private final ProtocolKeyword commandType;

    protected CommandLatencyId(SocketAddress localAddress, SocketAddress remoteAddress, ProtocolKeyword commandType) {
        checkArgument(localAddress != null, "localAddress must not be null");
        checkArgument(remoteAddress != null, "remoteAddress must not be null");
        checkArgument(commandType != null, "commandType must not be null");

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.commandType = commandType;
    }

    /**
     * Create a new instance of {@link CommandLatencyId}.
     * 
     * @param localAddress the local address
     * @param remoteAddress the remote address
     * @param commandType the command type
     * @return a new instance of {@link CommandLatencyId}
     */
    public static CommandLatencyId create(SocketAddress localAddress, SocketAddress remoteAddress, ProtocolKeyword commandType) {
        return new CommandLatencyId(localAddress, remoteAddress, commandType);
    }

    /**
     * Returns the local address.
     * 
     * @return the local address
     */
    public SocketAddress localAddress() {
        return localAddress;
    }

    /**
     * Returns the remote address.
     * 
     * @return the remote address
     */
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    /**
     * Returns the command type.
     * 
     * @return the command type
     */
    public ProtocolKeyword commandType() {
        return commandType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CommandLatencyId))
            return false;

        CommandLatencyId that = (CommandLatencyId) o;

        if (!localAddress.equals(that.localAddress))
            return false;
        if (!remoteAddress.equals(that.remoteAddress))
            return false;
        return commandType.equals(that.commandType);
    }

    @Override
    public int hashCode() {
        int result = localAddress.hashCode();
        result = 31 * result + remoteAddress.hashCode();
        result = 31 * result + commandType.hashCode();
        return result;
    }

    @Override
    public int compareTo(CommandLatencyId o) {

        if (o == null) {
            return -1;
        }

        int remoteResult = remoteAddress.toString().compareTo(o.remoteAddress.toString());
        if (remoteResult != 0) {
            return remoteResult;
        }

        int localResult = localAddress.toString().compareTo(o.localAddress.toString());
        if (localResult != 0) {
            return localResult;
        }

        return commandType.toString().compareTo(o.commandType.toString());
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("[").append(localAddress);
        sb.append(" -> ").append(remoteAddress);
        sb.append(", commandType=").append(commandType);
        sb.append(']');
        return sb.toString();
    }
}
