/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.masterslave;

import java.util.Collection;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.masterslave.MasterSlaveConnectionProvider.Intent;
import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Channel writer/dispatcher that dispatches commands based on the intent to different connections.
 *
 * @author Mark Paluch
 */
class MasterSlaveChannelWriter<K, V> implements RedisChannelWriter {

    private MasterSlaveConnectionProvider<K, V> masterSlaveConnectionProvider;
    private boolean closed = false;

    public MasterSlaveChannelWriter(MasterSlaveConnectionProvider<K, V> masterSlaveConnectionProvider) {
        this.masterSlaveConnectionProvider = masterSlaveConnectionProvider;
    }

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        LettuceAssert.notNull(command, "Command must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        Intent intent = getIntent(command.getType());
        StatefulRedisConnection<K, V> connection = (StatefulRedisConnection) masterSlaveConnectionProvider
                .getConnection(intent);

        return connection.dispatch(command);
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands) {

        LettuceAssert.notNull(commands, "Commands must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        // TODO: Retain order or retain Intent preference?
        // Currently: Retain order
        Intent intent = getIntent(commands);

        StatefulRedisConnection<K, V> connection = (StatefulRedisConnection) masterSlaveConnectionProvider
                .getConnection(intent);

        return connection.dispatch(commands);
    }

    /**
     * Optimization: Determine command intents and optimize for bulk execution preferring one node.
     * <p>
     * If there is only one intent, then we take the intent derived from the commands. If there is more than one intent, then
     * use {@link Intent#WRITE}.
     *
     * @param commands {@link Collection} of {@link RedisCommand commands}.
     * @return the intent.
     */
    static Intent getIntent(Collection<? extends RedisCommand<?, ?, ?>> commands) {

        boolean w = false;
        boolean r = false;
        Intent singleIntent = Intent.WRITE;

        for (RedisCommand<?, ?, ?> command : commands) {

            singleIntent = getIntent(command.getType());
            if (singleIntent == Intent.READ) {
                r = true;
            }

            if (singleIntent == Intent.WRITE) {
                w = true;
            }

            if (r && w) {
                return Intent.WRITE;
            }
        }

        return singleIntent;
    }

    private static Intent getIntent(ProtocolKeyword type) {
        return ReadOnlyCommands.isReadOnlyCommand(type) ? Intent.READ : Intent.WRITE;
    }

    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        if (masterSlaveConnectionProvider != null) {
            masterSlaveConnectionProvider.close();
            masterSlaveConnectionProvider = null;
        }
    }

    public MasterSlaveConnectionProvider<K, V> getMasterSlaveConnectionProvider() {
        return masterSlaveConnectionProvider;
    }

    @Override
    public void setConnectionFacade(ConnectionFacade connection) {
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        masterSlaveConnectionProvider.setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        masterSlaveConnectionProvider.flushCommands();
    }

    @Override
    public void reset() {
        masterSlaveConnectionProvider.reset();
    }

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     *
     * @param readFrom the read from setting, must not be {@literal null}
     */
    public void setReadFrom(ReadFrom readFrom) {
        masterSlaveConnectionProvider.setReadFrom(readFrom);
    }

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#MASTER} if not set.
     *
     * @return the read from setting
     */
    public ReadFrom getReadFrom() {
        return masterSlaveConnectionProvider.getReadFrom();
    }

}
