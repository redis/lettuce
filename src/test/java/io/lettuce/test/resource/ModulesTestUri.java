package io.lettuce.test.resource;

import io.lettuce.core.RedisURI;
import io.lettuce.test.settings.TestSettings;

/**
 * Factory for the {@link RedisURI} of the modules-enabled (JSON, Search, TimeSeries, Bloom) standalone Redis instance used
 * while testing.
 */
public class ModulesTestUri {

    private ModulesTestUri() {
    }

    /**
     * @return the {@link RedisURI} of the modules-enabled standalone test instance, see {@link TestSettings#moduleHost()} and
     *         {@link TestSettings#modulePort()}.
     */
    public static RedisURI create() {

        RedisURI.Builder builder = RedisURI.Builder.redis(TestSettings.moduleHost()).withPort(TestSettings.modulePort());

        if (TestSettings.tls()) {
            builder.withSsl(true).withVerifyPeer(false);
        }

        CharSequence password = TestSettings.password();
        if (TestSettings.isProviderActive() && password != null && password.length() > 0) {
            builder.withAuthentication(TestSettings.username(), password);
        }

        return builder.build();
    }

}
