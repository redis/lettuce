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
package io.lettuce.core.commands.transactional;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.commands.BitCommandTest;
import io.lettuce.core.commands.BitStringCodec;

/**
 * @author Mark Paluch
 */
public class BitTxCommandTest extends BitCommandTest {

    @Override
    public RedisCommands<String, String> connect() {
        bitstring = TxSyncInvocationHandler.sync(client.connect(new BitStringCodec()));
        return TxSyncInvocationHandler.sync(client.connect());
    }
}
