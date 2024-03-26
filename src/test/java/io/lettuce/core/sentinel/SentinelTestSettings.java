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
