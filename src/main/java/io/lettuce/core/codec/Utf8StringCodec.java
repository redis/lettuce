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
package io.lettuce.core.codec;

import io.lettuce.core.protocol.LettuceCharsets;

/**
 * A {@link RedisCodec} that handles UTF-8 encoded keys and values.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @see StringCodec
 * @see LettuceCharsets#UTF8
 * @deprecated since 5.2, use {@link StringCodec#UTF8} instead.
 */
@Deprecated
public class Utf8StringCodec extends StringCodec implements RedisCodec<String, String> {

    /**
     * Initialize a new instance that encodes and decodes strings using the UTF-8 charset;
     */
    public Utf8StringCodec() {
        super(LettuceCharsets.UTF8);
    }

}
