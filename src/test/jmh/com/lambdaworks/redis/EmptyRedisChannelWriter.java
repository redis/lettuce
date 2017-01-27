/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class EmptyRedisChannelWriter implements RedisChannelWriter {

    public final static EmptyRedisChannelWriter INSTANCE = new EmptyRedisChannelWriter();

    @Override
    public RedisCommand write(RedisCommand command) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void reset() {

    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler redisChannelHandler) {

    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {

    }

    @Override
    public void flushCommands() {

    }
}
