/*
 * Copyright 2019-2020 the original author or authors.
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
package io.lettuce.core.protocol;

import java.util.concurrent.CompletionStage;

import io.netty.channel.Channel;

/**
 * Initialize a connection to prepare it for usage.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface ConnectionInitializer {

    /**
     * Initialize the connection for usage. This method is invoked after establishing the transport connection and before the
     * connection is used for user-space commands.
     *
     * @param channel the {@link Channel} to initialize.
     * @return the {@link CompletionStage} that completes once the channel is fully initialized.
     */
    CompletionStage<Void> initialize(Channel channel);

}
