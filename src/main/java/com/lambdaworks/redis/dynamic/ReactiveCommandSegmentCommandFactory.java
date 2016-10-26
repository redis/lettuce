/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.dynamic;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactory;
import com.lambdaworks.redis.dynamic.output.CommandOutputFactoryResolver;
import com.lambdaworks.redis.dynamic.output.OutputSelector;
import com.lambdaworks.redis.dynamic.parameter.ExecutionSpecificParameters;
import com.lambdaworks.redis.dynamic.segment.CommandSegments;

/**
 * {@link CommandSegmentCommandFactory} for Reactive Command execution.
 * 
 * @author Mark Paluch
 */
class ReactiveCommandSegmentCommandFactory<K, V> extends CommandSegmentCommandFactory<K, V> {

    private boolean streamingExecution;

    public ReactiveCommandSegmentCommandFactory(CommandSegments commandSegments, CommandMethod commandMethod,
            RedisCodec<K, V> redisCodec, CommandOutputFactoryResolver outputResolver) {

        super(commandSegments, commandMethod, redisCodec, outputResolver);

        if (commandMethod.getParameters() instanceof ExecutionSpecificParameters) {

            ExecutionSpecificParameters executionAwareParameters = (ExecutionSpecificParameters) commandMethod.getParameters();

            if (executionAwareParameters.hasTimeoutIndex()) {
                throw new CommandCreationException(commandMethod, "Reactive command methods do not support Timeout parameters");
            }
        }
    }

    @Override
    protected CommandOutputFactory resolveCommandOutputFactory(OutputSelector outputSelector) {

        CommandOutputFactory factory = getOutputResolver().resolveStreamingCommandOutput(outputSelector);

        if (factory != null) {
            streamingExecution = true;
            return factory;
        }

        return super.resolveCommandOutputFactory(outputSelector);
    }

    /**
     * @return {@literal true} if the resolved {@link com.lambdaworks.redis.output.CommandOutput} should use streaming.
     */
    public boolean isStreamingExecution() {
        return streamingExecution;
    }
}
