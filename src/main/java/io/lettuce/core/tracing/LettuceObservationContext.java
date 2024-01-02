/*
 * Copyright 2022-2024 the original author or authors.
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
package io.lettuce.core.tracing;

import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.tracing.Tracing.Endpoint;
import io.micrometer.observation.Observation;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.SenderContext;

/**
 * Micrometer {@link Observation.Context} holding Lettuce contextual details.
 *
 * @author Mark Paluch
 * @since 6.3
 */
public class LettuceObservationContext extends SenderContext<Object> {

    private volatile RedisCommand<?, ?, ?> command;

    private volatile Endpoint endpoint;

    /**
     * Create a new {@code LettuceObservationContext} given the {@code serviceName}.
     *
     * @param serviceName service name.
     */
    public LettuceObservationContext(String serviceName) {

        super((carrier, key, value) -> {
        }, Kind.CLIENT);

        setRemoteServiceName(serviceName);
    }

    /**
     * Returns the required {@link RedisCommand} or throws {@link IllegalStateException} if no command is associated with the
     * context. Use {@link #hasCommand()} to check if the command is available.
     *
     * @return the required {@link RedisCommand}.
     * @throws IllegalStateException if no command is associated with the context.
     */
    public RedisCommand<?, ?, ?> getRequiredCommand() {

        RedisCommand<?, ?, ?> local = command;

        if (local == null) {
            throw new IllegalStateException("LettuceObservationContext is not associated with a Command");
        }

        return local;
    }

    /**
     * Set the {@link RedisCommand}.
     *
     * @param command the traced command.
     */
    public void setCommand(RedisCommand<?, ?, ?> command) {
        this.command = command;
    }

    /**
     * @return {@code true} if the command is available;{@code false} otherwise.
     */
    public boolean hasCommand() {
        return this.command != null;
    }

    /**
     * Returns the required {@link Endpoint} or throws {@link IllegalStateException} if no endpoint is associated with the
     * context.
     *
     * @return the required {@link Endpoint}.
     * @throws IllegalStateException if no endpoint is associated with the context.
     */
    public Endpoint getRequiredEndpoint() {

        Endpoint local = endpoint;

        if (local == null) {
            throw new IllegalStateException("LettuceObservationContext is not associated with a Endpoint");
        }

        return local;
    }

    /**
     * Set the {@link Endpoint}.
     *
     * @param endpoint the traced endpoint.
     */
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [name=").append(getName());
        sb.append(", contextualName=").append(getContextualName());
        sb.append(", error=").append(getError());
        sb.append(", lowCardinalityKeyValues=").append(getLowCardinalityKeyValues());
        sb.append(", highCardinalityKeyValues=").append(getHighCardinalityKeyValues());
        sb.append(", parentObservation=").append(getParentObservation());
        sb.append(", command=").append(command);
        sb.append(", endpoint=").append(endpoint);
        sb.append(']');
        return sb.toString();
    }

}
