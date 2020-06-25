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
package io.lettuce.core.sentinel;

import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public abstract class SentinelTestSettings extends TestSupport {

    public static final RedisURI SENTINEL_URI = RedisURI.Builder.sentinel(TestSettings.host(), SentinelTestSettings.MASTER_ID)
            .build();

    public static final String MASTER_ID = "mymaster";

    private SentinelTestSettings() {
    }

}
