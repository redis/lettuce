/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.metrics;

import java.io.Serializable;
import java.net.SocketAddress;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Identifier for a command latency. Consists of a local/remote tuple of {@link SocketAddress}es and a
 * {@link io.lettuce.core.protocol.ProtocolKeyword commandType} part.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class CommandLatencyId implements Serializable, Comparable<CommandLatencyId> {

    private final SocketAddress localAddress;

    private final SocketAddress remoteAddress;

    private final ProtocolKeyword commandType;

    private final String commandName;

    protected CommandLatencyId(SocketAddress localAddress, SocketAddress remoteAddress, ProtocolKeyword commandType) {
        LettuceAssert.notNull(localAddress, "LocalAddress must not be null");
        LettuceAssert.notNull(remoteAddress, "RemoteAddress must not be null");
        LettuceAssert.notNull(commandType, "CommandType must not be null");

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.commandType = commandType;
        this.commandName = commandType.name();
    }

    /**
     * Create a new instance of {@link CommandLatencyId}.
     *
     * @param localAddress the local address.
     * @param remoteAddress the remote address.
     * @param commandType the command type.
     * @return a new instance of {@link CommandLatencyId}.
     */
    public static CommandLatencyId create(SocketAddress localAddress, SocketAddress remoteAddress,
            ProtocolKeyword commandType) {
        return new CommandLatencyId(localAddress, remoteAddress, commandType);
    }

    /**
     * Returns the local address.
     *
     * @return the local address.
     */
    public SocketAddress localAddress() {
        return localAddress;
    }

    /**
     * Returns the remote address.
     *
     * @return the remote address.
     */
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    /**
     * Returns the command type.
     *
     * @return the command type.
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
        return commandName.equals(that.commandName);
    }

    @Override
    public int hashCode() {
        int result = localAddress.hashCode();
        result = 31 * result + remoteAddress.hashCode();
        result = 31 * result + commandName.hashCode();
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

        return commandName.compareTo(o.commandName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(localAddress);
        sb.append(" -> ").append(remoteAddress);
        sb.append(", commandType=").append(commandType);
        sb.append(']');
        return sb.toString();
    }

}
