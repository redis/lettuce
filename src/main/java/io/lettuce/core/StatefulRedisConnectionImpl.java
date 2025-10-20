/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core;

import static io.lettuce.core.ClientOptions.DEFAULT_JSON_PARSER;
import static io.lettuce.core.protocol.CommandType.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.output.MultiOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import reactor.core.publisher.Mono;

/**
 * A thread-safe connection to a Redis server. Multiple threads may share one {@link StatefulRedisConnectionImpl}
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class StatefulRedisConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements StatefulRedisConnection<K, V> {

    protected final RedisCodec<K, V> codec;

    protected final RedisCommands<K, V> sync;

    protected final RedisAsyncCommandsImpl<K, V> async;

    protected final RedisReactiveCommandsImpl<K, V> reactive;

    private final ConnectionState state = new ConnectionState();

    private final PushHandler pushHandler;

    private final Supplier<JsonParser> parser;

    protected MultiOutput<K, V> multi;

    private RedisAuthenticationHandler<K, V> authHandler = RedisAuthenticationHandler.createDefaultAuthenticationHandler();

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer.
     * @param pushHandler the handler for push notifications.
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     */
    public StatefulRedisConnectionImpl(RedisChannelWriter writer, PushHandler pushHandler, RedisCodec<K, V> codec,
            Duration timeout) {
        this(writer, pushHandler, codec, timeout, DEFAULT_JSON_PARSER);
    }

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer.
     * @param pushHandler the handler for push notifications.
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param parser the parser to use for JSON commands.
     */
    public StatefulRedisConnectionImpl(RedisChannelWriter writer, PushHandler pushHandler, RedisCodec<K, V> codec,
            Duration timeout, Supplier<JsonParser> parser) {

        super(writer, timeout);

        this.pushHandler = pushHandler;
        this.codec = codec;
        this.parser = parser;
        this.async = newRedisAsyncCommandsImpl();
        this.sync = newRedisSyncCommandsImpl();
        this.reactive = newRedisReactiveCommandsImpl();
    }

    public RedisCodec<K, V> getCodec() {
        return codec;
    }

    @Override
    public RedisAsyncCommands<K, V> async() {
        return async;
    }

    /**
     * Create a new instance of {@link RedisCommands}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisCommands<K, V> newRedisSyncCommandsImpl() {
        return syncHandler(async(), RedisCommands.class, RedisClusterCommands.class);
    }

    /**
     * Create a new instance of {@link RedisAsyncCommandsImpl}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisAsyncCommandsImpl<K, V> newRedisAsyncCommandsImpl() {
        return new RedisAsyncCommandsImpl<>(this, codec, parser);
    }

    @Override
    public RedisReactiveCommands<K, V> reactive() {
        return reactive;
    }

    /**
     * Create a new instance of {@link RedisReactiveCommandsImpl}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new RedisReactiveCommandsImpl<>(this, codec, parser);
    }

    @Override
    public RedisCommands<K, V> sync() {
        return sync;
    }

    /**
     * Add a new listener.
     *
     * @param listener Listener.
     */
    @Override
    public void addListener(PushListener listener) {
        pushHandler.addListener(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener Listener.
     */
    @Override
    public void removeListener(PushListener listener) {
        pushHandler.removeListener(listener);
    }

    @Override
    public boolean isMulti() {
        return multi != null;
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {

        RedisCommand<K, V, T> toSend = preProcessCommand(command);
        RedisCommand<K, V, T> result = super.dispatch(toSend);
        RedisCommand<K, V, T> finalCommand = postProcessCommand(result);

        return finalCommand;
    }

    @Override
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> commands) {

        Collection<RedisCommand<K, V, ?>> sentCommands = preProcessCommands(commands);

        Collection<RedisCommand<K, V, ?>> dispatchedCommands = super.dispatch(sentCommands);

        return this.postProcessCommands(dispatchedCommands);
    }

    protected Collection<RedisCommand<K, V, ?>> postProcessCommands(Collection<RedisCommand<K, V, ?>> commands) {
        authHandler.postProcess(commands);
        return commands;
    }

    protected <T> RedisCommand<K, V, T> postProcessCommand(RedisCommand<K, V, T> command) {
        authHandler.postProcess(command);
        return command;
    }

    protected Collection<RedisCommand<K, V, ?>> preProcessCommands(Collection<? extends RedisCommand<K, V, ?>> commands) {
        List<RedisCommand<K, V, ?>> sentCommands = new ArrayList<>(commands.size());

        commands.forEach(o -> {
            RedisCommand<K, V, ?> preprocessed = preProcessCommand(o);
            sentCommands.add(preprocessed);
        });

        return sentCommands;
    }

    // TODO [tihomir.mateev] Refactor to include as part of the Command interface
    // All these if statements clearly indicate this is a problem best solve by each command
    // (defining a pre and post processing behaviour of the command)
    protected <T> RedisCommand<K, V, T> preProcessCommand(RedisCommand<K, V, T> command) {

        RedisCommand<K, V, T> local = command;
        String commandType = command.getType().toString();

        if (commandType.equals(AUTH.name())) {
            local = attachOnComplete(local, status -> {
                if ("OK".equals(status)) {

                    List<char[]> args = CommandArgsAccessor.getCharArrayArguments(command.getArgs());

                    if (!args.isEmpty()) {
                        state.setUserNamePassword(args);
                    } else {

                        List<String> strings = CommandArgsAccessor.getStringArguments(command.getArgs());
                        state.setUserNamePassword(strings.stream().map(String::toCharArray).collect(Collectors.toList()));
                    }
                }
            });
        }

        if (commandType.equals(SELECT.name())) {
            local = attachOnComplete(local, status -> {
                if ("OK".equals(status)) {
                    Long db = CommandArgsAccessor.getFirstInteger(command.getArgs());
                    if (db != null) {
                        state.setDb(db.intValue());
                    }
                }
            });
        }

        if (commandType.equals(READONLY.name())) {
            local = attachOnComplete(local, status -> {
                if ("OK".equals(status)) {
                    state.setReadOnly(true);
                }
            });
        }

        if (commandType.equals(READWRITE.name())) {
            local = attachOnComplete(local, status -> {
                if ("OK".equals(status)) {
                    state.setReadOnly(false);
                }
            });
        }

        if (commandType.equals(DISCARD.name())) {
            if (multi != null) {
                multi.cancel();
                multi = null;
            }
        }

        if (commandType.equals(EXEC.name())) {
            MultiOutput<K, V> multiOutput = this.multi;
            this.multi = null;
            if (multiOutput == null) {
                multiOutput = new MultiOutput<>(codec);
            }
            local.setOutput((MultiOutput) multiOutput);
        }

        if (multi != null && !commandType.equals(MULTI.name()) && !commandType.equals(WATCH.name())) {
            // ignore MULTI and WATCH commands nested in another MULTI
            local = new TransactionalCommand<>(local);
            multi.add(local);
        }

        if (commandType.equals(MULTI.name())) {
            authHandler.startTransaction();
            multi = (multi == null ? new MultiOutput<>(codec) : multi);

            if (command instanceof CompleteableCommand) {
                ((CompleteableCommand<?>) command).onComplete((ignored, e) -> {
                    if (e != null) {
                        multi = null;
                        authHandler.endTransaction();
                    }
                });
            }
        }

        return local;
    }

    private <T> RedisCommand<K, V, T> attachOnComplete(RedisCommand<K, V, T> command, Consumer<T> consumer) {

        if (command instanceof CompleteableCommand) {
            CompleteableCommand<T> completeable = (CompleteableCommand<T>) command;
            completeable.onComplete(consumer);
        }
        return command;
    }

    /**
     * @param clientName
     * @deprecated since 6.0, use {@link RedisAsyncCommands#clientSetname(Object)}.
     */
    @Deprecated
    public void setClientName(String clientName) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.SETNAME).addValue(clientName);
        AsyncCommand<String, String, String> async = new AsyncCommand<>(
                new Command<>(CommandType.CLIENT, new StatusOutput<>(StringCodec.UTF8), args));
        state.setClientName(clientName);

        dispatch((RedisCommand) async);
    }

    public ConnectionState getConnectionState() {
        return state;
    }

    @Override
    public void activated() {
        super.activated();
        authHandler.subscribe();
    }

    @Override
    public void deactivated() {
        authHandler.unsubscribe();
        super.deactivated();
    }

    public void setAuthenticationHandler(RedisAuthenticationHandler<K, V> handler) {
        if (handler != null) {
            authHandler = handler;
        }
    }

}
