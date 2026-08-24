package io.lettuce.test.resource;

import io.lettuce.core.RedisURI;
import io.lettuce.test.env.Endpoints;
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

        Endpoints.Endpoint endpoint = TestSettings.moduleEndpoint();

        if (endpoint != null && endpoint.isTls()) {
            builder.withSsl(true).withVerifyPeer(false);
        }

        if (endpoint != null && endpoint.getPassword() != null && !endpoint.getPassword().isEmpty()) {
            String username = endpoint.getUsername() != null && !endpoint.getUsername().isEmpty() ? endpoint.getUsername()
                    : "default";
            builder.withAuthentication(username, endpoint.getPassword());
        }

        return builder.build();
    }

}
