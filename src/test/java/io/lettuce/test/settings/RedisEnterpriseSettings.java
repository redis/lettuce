package io.lettuce.test.settings;

import io.lettuce.test.env.Endpoints;
import io.lettuce.test.env.Endpoints.Endpoint;

/**
 * Resolves the managed Redis Enterprise database the standard integration suite should target.
 *
 * <p>
 * When {@code RE_CLUSTER=true}, the database named by {@code RE_DB_NAME} (default {@code standalone}) is read from the
 * endpoints config referenced by {@code REDIS_ENDPOINTS_CONFIG_PATH} - the same format consumed by the scenario tests - and its
 * host, port, credentials and TLS flag are exposed to {@link TestSettings} and the default test client. When {@code RE_CLUSTER}
 * is unset the suite behaves unchanged (localhost defaults, no auth).
 * </p>
 */
public final class RedisEnterpriseSettings {

    private RedisEnterpriseSettings() {
    }

    public static boolean isEnabled() {
        return "true".equalsIgnoreCase(System.getenv("RE_CLUSTER"));
    }

    private static Endpoint endpoint() {
        String name = System.getenv("RE_DB_NAME");
        if (name == null || name.isEmpty()) {
            name = "standalone";
        }
        Endpoint endpoint = Endpoints.DEFAULT.getEndpoint(name);
        if (endpoint == null) {
            throw new IllegalStateException(
                    "RE_CLUSTER=true but database '" + name + "' was not found in REDIS_ENDPOINTS_CONFIG_PATH");
        }
        return endpoint;
    }

    public static String host() {
        Endpoint endpoint = endpoint();
        if (endpoint.getRawEndpoints() != null && !endpoint.getRawEndpoints().isEmpty()) {
            return endpoint.getRawEndpoints().get(0).getDnsName();
        }
        throw new IllegalStateException("No raw_endpoints found for the Redis Enterprise database");
    }

    public static int port() {
        Endpoint endpoint = endpoint();
        if (endpoint.getRawEndpoints() != null && !endpoint.getRawEndpoints().isEmpty()) {
            return endpoint.getRawEndpoints().get(0).getPort();
        }
        throw new IllegalStateException("No raw_endpoints found for the Redis Enterprise database");
    }

    public static String username() {
        return endpoint().getUsername();
    }

    public static String password() {
        return endpoint().getPassword();
    }

    public static boolean tls() {
        return endpoint().isTls();
    }

}
